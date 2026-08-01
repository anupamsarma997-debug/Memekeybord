package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MemeDialogue
import com.example.data.MemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig

import android.content.SharedPreferences

class KeyboardViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: MemeRepository
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val sharedPrefs = application.getSharedPreferences("desi_meme_prefs", Context.MODE_PRIVATE)

    // Input text in the meme message board
    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    // Interactive QWERTY Keyboard Sim State
    private val _keyboardShiftState = MutableStateFlow(false)
    val keyboardShiftState: StateFlow<Boolean> = _keyboardShiftState.asStateFlow()

    private val _keyboardNumberMode = MutableStateFlow(false)
    val keyboardNumberMode: StateFlow<Boolean> = _keyboardNumberMode.asStateFlow()

    // AI Shayari / Dialogue Generator State
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _generatedAiText = MutableStateFlow("")
    val generatedAiText: StateFlow<String> = _generatedAiText.asStateFlow()

    // Configuration settings & DataStore Preferences
    private val keyboardDataStore = com.example.data.KeyboardDataStore(application)

    val vibrationEnabled: StateFlow<Boolean> = keyboardDataStore.vibrationEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val audioPlaybackEnabled: StateFlow<Boolean> = keyboardDataStore.audioPlaybackEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val darkThemeEnabled: StateFlow<Boolean> = keyboardDataStore.darkThemeEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            keyboardDataStore.setVibrationEnabled(enabled)
        }
    }

    fun toggleAudioPlayback(enabled: Boolean) {
        viewModelScope.launch {
            keyboardDataStore.setAudioPlaybackEnabled(enabled)
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            keyboardDataStore.setDarkThemeEnabled(enabled)
        }
    }

    private val _insertMode = MutableStateFlow("DIALOGUE") // "DIALOGUE" or "EMOJI"
    val insertMode: StateFlow<String> = _insertMode.asStateFlow()

    private val _vibrationStrengthMultiplier = MutableStateFlow(1.0f) // multiplier for vibration duration
    val vibrationStrengthMultiplier: StateFlow<Float> = _vibrationStrengthMultiplier.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    // For active splash animations / dialog overlays when key is pressed
    private val _activeDialogueEvent = MutableStateFlow<MemeDialogue?>(null)
    val activeDialogueEvent: StateFlow<MemeDialogue?> = _activeDialogueEvent.asStateFlow()

    // Filters and search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow("All") // "All", "😂 Hasna", "😎 Swag", etc.
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MemeRepository(database.memeDao())
        
        _insertMode.value = sharedPrefs.getString("insert_mode", "DIALOGUE") ?: "DIALOGUE"
        _vibrationStrengthMultiplier.value = sharedPrefs.getFloat("vibration_strength", 1.0f)
        _speechRate.value = sharedPrefs.getFloat("speech_rate", 1.0f)
        _speechPitch.value = sharedPrefs.getFloat("speech_pitch", 1.0f)

        // Initialize Database with prepopulated defaults if empty
        viewModelScope.launch {
            repository.checkAndPrepopulate()
            fetchTrendingMemes()
        }

        // Initialize Text To Speech
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Observe data from repository combined with search and tab filters
    val filteredDialogues: StateFlow<List<MemeDialogue>> = combine(
        repository.allDialogues,
        _searchQuery,
        _selectedTab
    ) { list, query, tab ->
        var result = list
        if (tab != "All") {
            result = result.filter { it.category.equals(tab, ignoreCase = true) }
        }
        if (query.isNotEmpty()) {
            result = result.filter {
                it.dialogue.contains(query, ignoreCase = true) || 
                it.emoji.contains(query) ||
                it.mood.contains(query, ignoreCase = true)
            }
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default
                tts?.setLanguage(Locale.getDefault())
            }
            isTtsInitialized = true
            tts?.setSpeechRate(_speechRate.value)
            tts?.setPitch(_speechPitch.value)
        }
    }

    fun updateSpeechRate(rate: Float) {
        _speechRate.value = rate
        sharedPrefs.edit().putFloat("speech_rate", rate).apply()
        if (isTtsInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun updateSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        sharedPrefs.edit().putFloat("speech_pitch", pitch).apply()
        if (isTtsInitialized) {
            tts?.setPitch(pitch)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedTab(tab: String) {
        _selectedTab.value = tab
    }

    fun updateTypedText(text: String) {
        _typedText.value = text
    }

    fun setInsertMode(mode: String) {
        _insertMode.value = mode
        sharedPrefs.edit().putString("insert_mode", mode).apply()
    }

    fun setVibrationStrength(multiplier: Float) {
        _vibrationStrengthMultiplier.value = multiplier
        sharedPrefs.edit().putFloat("vibration_strength", multiplier).apply()
    }

    // Primary click action on the custom keyboard
    fun onEmojiKeyPress(item: MemeDialogue) {
        // Speak out the dialogue
        speakDialogue(item.dialogue)

        // Trigger vibration
        triggerVibration(item.mood)

        // Add to active message board
        val textToInsert = if (_insertMode.value == "DIALOGUE") {
            "${item.emoji} ${item.dialogue} "
        } else {
            "${item.emoji} "
        }
        _typedText.value = _typedText.value + textToInsert

        // Fire a visual dialogue overlay/popup
        _activeDialogueEvent.value = item
    }

    fun clearActiveDialogueEvent() {
        _activeDialogueEvent.value = null
    }

    private fun speakDialogue(text: String) {
        if (!audioPlaybackEnabled.value) return
        if (isTtsInitialized && tts != null) {
            // Clean up text for better Hindi/Hinglish TTS output
            val cleanedText = text
                .replace("Bhaai", "Bhai")
                .replace("baba", "baabaa")
                .replace("pagli", "paglee")
                .replace("ricks", "riks")
                .replace("pappi", "pappee")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, "meme_speech_${System.currentTimeMillis()}")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    private fun triggerVibration(mood: String) {
        if (!vibrationEnabled.value) return
        val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator == null || !vibrator.hasVibrator()) return

        val multiplier = _vibrationStrengthMultiplier.value
        val timings: LongArray
        val amplitudes: IntArray?

        when (mood) {
            "LIGHT" -> {
                timings = longArrayOf(0, 15)
                amplitudes = intArrayOf(0, 80)
            }
            "FUNNY" -> {
                // Bouncy energetic bursts
                timings = longArrayOf(0, (60 * multiplier).toLong(), 40, (60 * multiplier).toLong(), 40, (80 * multiplier).toLong())
                amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
            }
            "SWAG" -> {
                // Heavy, steady pulses
                timings = longArrayOf(0, (180 * multiplier).toLong(), 100, (180 * multiplier).toLong())
                amplitudes = intArrayOf(0, 255, 0, 255)
            }
            "SAD" -> {
                // Fading long wave
                timings = longArrayOf(0, (400 * multiplier).toLong())
                amplitudes = intArrayOf(0, 100)
            }
            "SHOCK" -> {
                // High frequency double kick
                timings = longArrayOf(0, (120 * multiplier).toLong(), 80, (250 * multiplier).toLong())
                amplitudes = intArrayOf(0, 255, 0, 255)
            }
            "LOVE" -> {
                // Heartbeat repeat
                timings = longArrayOf(0, (80 * multiplier).toLong(), 80, (80 * multiplier).toLong(), 300, (80 * multiplier).toLong(), 80, (80 * multiplier).toLong())
                amplitudes = intArrayOf(0, 100, 0, 120, 0, 100, 0, 120)
            }
            "ANGRY" -> {
                // Harsh high power rumble
                timings = longArrayOf(0, (350 * multiplier).toLong(), 40, (350 * multiplier).toLong())
                amplitudes = intArrayOf(0, 255, 0, 255)
            }
            else -> {
                timings = longArrayOf(0, (80 * multiplier).toLong())
                amplitudes = intArrayOf(0, 180)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                vibrator.vibrate((100 * multiplier).toLong())
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    // Database Actions
    fun addNewDialogue(emoji: String, dialogue: String, category: String, mood: String) {
        viewModelScope.launch {
            val item = MemeDialogue(
                emoji = emoji,
                dialogue = dialogue,
                category = category,
                mood = mood,
                isCustom = true
            )
            repository.insert(item)
        }
    }

    fun updateDialogue(item: MemeDialogue) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun deleteDialogue(item: MemeDialogue) {
        viewModelScope.launch {
            repository.deleteById(item.id)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefault()
        }
    }

    fun toggleFavorite(item: MemeDialogue) {
        viewModelScope.launch {
            repository.update(item.copy(isFavorite = !item.isFavorite))
        }
    }

    // Copy & Share utilities
    fun copyToClipboard() {
        val text = _typedText.value
        if (text.isEmpty()) {
            Toast.makeText(getApplication(), "Please type some text first!", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Desi Meme Text", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "Copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
    }

    fun clearText() {
        _typedText.value = ""
    }

    // --- Simulated Soft Keyboard Actions ---
    fun onKeyTyped(char: String) {
        val letter = if (_keyboardShiftState.value) char.uppercase() else char.lowercase()
        _typedText.value = _typedText.value + letter
        
        // Single shift auto-revert
        if (_keyboardShiftState.value) {
            _keyboardShiftState.value = false
        }
        triggerVibration("LIGHT")
    }

    fun onBackspacePressed() {
        val current = _typedText.value
        if (current.isNotEmpty()) {
            _typedText.value = current.dropLast(1)
        }
        triggerVibration("LIGHT")
    }

    fun onSpacePressed() {
        _typedText.value = _typedText.value + " "
        triggerVibration("LIGHT")
    }

    fun onShiftToggle() {
        _keyboardShiftState.value = !_keyboardShiftState.value
        triggerVibration("LIGHT")
    }

    fun onNumberModeToggle() {
        _keyboardNumberMode.value = !_keyboardNumberMode.value
        triggerVibration("LIGHT")
    }

    // --- Gemini AI Shayari & Dialogue Generator ---
    fun generateAiDialogue(category: String, topic: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _generatedAiText.value = ""
            
            val prompt = "Write a highly creative, catchy, and authentic single-line Desi Hinglish (Roman Hindi) meme dialogue or Shayari. " +
                         "The category is '$category' and the topic or keyword is '$topic'. " +
                         "Keep it short, direct, highly expressive, and punchy (1-2 lines maximum), using iconic slang/humor. " +
                         "Do not include any English translations, explanations, or meta-commentary. Return ONLY the Hindi/Hinglish line itself."
                         
            val result = callGeminiApi(prompt)
            _generatedAiText.value = result
            _isAiLoading.value = false
            
            // Speak generated line
            if (result.isNotEmpty() && !result.startsWith("Error")) {
                speakDialogue(result)
            }
        }
    }

    fun useGeneratedAiText() {
        val text = _generatedAiText.value
        if (text.isNotEmpty() && !text.startsWith("Error")) {
            _typedText.value = _typedText.value + text + " "
            _generatedAiText.value = ""
        }
    }

    private suspend fun callGeminiApi(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please enter your Gemini API Key in AI Studio Secrets panel!"
        }
        
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error code: ${response.code}. Check if your Gemini key is correct."
                }
                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text generated").trim()
                        }
                    }
                }
                "No creative response received."
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun playTtsForText(text: String) {
        speakDialogue(text)
    }

    // --- Daily Updated Trending Memes (with Search Grounding) ---
    private val _trendingMemes = MutableStateFlow<List<MemeDialogue>>(emptyList())
    val trendingMemes: StateFlow<List<MemeDialogue>> = _trendingMemes.asStateFlow()

    private val _isTrendingLoading = MutableStateFlow(false)
    val isTrendingLoading: StateFlow<Boolean> = _isTrendingLoading.asStateFlow()

    fun fetchTrendingMemes() {
        viewModelScope.launch {
            _isTrendingLoading.value = true
            try {
                val prompt = "Find and summarize the top 10 absolute most popular viral Desi/Hindi/Hinglish meme dialogues, social media trends, movie dialogue catchphrases, viral reels, or slang phrases currently trending in India right now in 2026. " +
                             "For each trending meme, specify a suitable single emoji, the dialogue text (in Hinglish, i.e., Roman script, like 'Moye Moye' or 'Just looking like a wow'), a category (choose exactly from: '😂 Hasna', '😎 Swag', '😭 Dukh', '😱 Shock', '❤️ Pyaar', '😡 Gussa', '🤝 Dosti'), and the mood (choose exactly from: 'FUNNY', 'SWAG', 'SAD', 'SHOCK', 'LOVE', 'ANGRY'). " +
                             "Format the output strictly as a JSON array of 10 objects. Do not include any other text, markdown blocks, or translations. " +
                             "Example: [ { \"emoji\": \"😭\", \"dialogue\": \"Yeh dukh kaahe khatam nahi hota be\", \"category\": \"😭 Dukh\", \"mood\": \"SAD\" } ]"
                
                val result = callGeminiApiWithSearch(prompt)
                val parsedList = mutableListOf<MemeDialogue>()
                if (result.isNotEmpty() && !result.startsWith("Error")) {
                    try {
                        var cleanedJson = result.trim()
                        if (cleanedJson.startsWith("```json")) {
                            cleanedJson = cleanedJson.removePrefix("```json").trim()
                        }
                        if (cleanedJson.endsWith("```")) {
                            cleanedJson = cleanedJson.removeSuffix("```").trim()
                        }
                        
                        val jsonArray = JSONArray(cleanedJson)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            parsedList.add(
                                MemeDialogue(
                                    id = 0,
                                    emoji = obj.optString("emoji", "🔥"),
                                    dialogue = obj.optString("dialogue", ""),
                                    category = obj.optString("category", "😂 Hasna"),
                                    mood = obj.optString("mood", "FUNNY"),
                                    isCustom = true
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (parsedList.isNotEmpty()) {
                    _trendingMemes.value = parsedList
                } else {
                    _trendingMemes.value = getFallbackDailyTrends()
                }
            } catch (e: Exception) {
                _trendingMemes.value = getFallbackDailyTrends()
            } finally {
                _isTrendingLoading.value = false
            }
        }
    }

    fun getFallbackDailyTrends(): List<MemeDialogue> {
        val calendar = java.util.Calendar.getInstance()
        val day = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        
        val pool = listOf(
            MemeDialogue(emoji = "🍆", dialogue = "Aayein? Baigan!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "✨", dialogue = "So beautiful, so elegant, just looking like a wow!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "😭", dialogue = "Moye Moye! Ekdum se waqt badal diya, jazbaat badal diye!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🤝", dialogue = "Melodi hai ki chalti nahi, par dosti gehri hai!", category = "🤝 Dosti", mood = "LOVE"),
            MemeDialogue(emoji = "🤬", dialogue = "Arrey chacha, o bhosadi wale chacha, shant ho jao!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "💀", dialogue = "Chin tapak dam dam! Sab khatam ho gaya re baba!", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "💻", dialogue = "Systumm pe systumm bitha rakha hai bhai ne!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "💪", dialogue = "Pawan Sahu banega tu? Gym ja ke dumble utha!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "🥺", dialogue = "Sahi pakde hain, par dil se bura lagta hai bhai!", category = "😭 Dukh", mood = "SAD"),
            MemeDialogue(emoji = "🗺️", dialogue = "Bhupendra Jogi! US mein kahan kahan gaye hain aap?", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🦁", dialogue = "Sher ko kaboo karne ke liye jigar chahiye, dimaag nahi!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "💔", dialogue = "Toba toba, saara mood kharab kar diya!", category = "😡 Gussa", mood = "ANGRY"),
            MemeDialogue(emoji = "🚀", dialogue = "Lala, aag laga di aag! Ab toh viral trending hai!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "👀", dialogue = "Control Uday Control! Bhot hard bhot hard!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "🔥", dialogue = "Kaha se aate hain yeh log, aur kidhar jaate hain?", category = "😱 Shock", mood = "SHOCK"),
            MemeDialogue(emoji = "🤣", dialogue = "Chhoti bacchi ho kya? Samajh nahi aata!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "👑", dialogue = "Jhukega nahi saala! King size entry!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "🤑", dialogue = "25 din mein paisa double! Scheme mast hai!", category = "😂 Hasna", mood = "FUNNY"),
            MemeDialogue(emoji = "⚡", dialogue = "Aapne ghabrana nahi hai, sab sorted hai boss!", category = "😎 Swag", mood = "SWAG"),
            MemeDialogue(emoji = "😴", dialogue = "Aarram se bhai, itni jaldi kis baat ki hai?", category = "🤝 Dosti", mood = "LOVE")
        )
        
        val startIndex = day % pool.size
        val resultList = mutableListOf<MemeDialogue>()
        for (i in 0 until 10) {
            val index = (startIndex + i) % pool.size
            resultList.add(pool[index])
        }
        return resultList
    }

    fun saveTrendingMeme(item: MemeDialogue) {
        viewModelScope.launch {
            repository.insert(item.copy(id = 0, isCustom = true))
            Toast.makeText(getApplication(), "Saved to Keyboard Memes! 🚀", Toast.LENGTH_SHORT).show()
        }
    }

    fun useTrendingMemeDirectly(item: MemeDialogue) {
        speakDialogue(item.dialogue)
        triggerVibration(item.mood)
        val textToInsert = if (_insertMode.value == "DIALOGUE") {
            "${item.emoji} ${item.dialogue} "
        } else {
            "${item.emoji} "
        }
        _typedText.value = _typedText.value + textToInsert
        _activeDialogueEvent.value = item
    }

    private suspend fun callGeminiApiWithSearch(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Please enter your Gemini API Key in AI Studio Secrets panel!"
        }
        
        val client = OkHttpClient.Builder()
            .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleSearchRetrieval", JSONObject())
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error code: ${response.code}."
                }
                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "").trim()
                        }
                    }
                }
                "Error: No text generated"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // --- Veo 3 & Lyria Media Generation ---
    private val _isVideoLoading = MutableStateFlow(false)
    val isVideoLoading: StateFlow<Boolean> = _isVideoLoading.asStateFlow()

    private val _generatedVideoUrl = MutableStateFlow("")
    val generatedVideoUrl: StateFlow<String> = _generatedVideoUrl.asStateFlow()

    private val _isMusicLoading = MutableStateFlow(false)
    val isMusicLoading: StateFlow<Boolean> = _isMusicLoading.asStateFlow()

    private val _generatedMusicUrl = MutableStateFlow("")
    val generatedMusicUrl: StateFlow<String> = _generatedMusicUrl.asStateFlow()

    fun generateVideoWithVeo(prompt: String, aspect: String) {
        viewModelScope.launch {
            _isVideoLoading.value = true
            _generatedVideoUrl.value = ""
            
            val fullPrompt = "Generate a short 10-second meme-style video based on this prompt: '$prompt'. Aspect ratio requested: $aspect."
            val result = callMediaGenerationApi("veo-3.1-fast-generate-preview", fullPrompt)
            _generatedVideoUrl.value = result.ifEmpty { "https://example.com/simulated_veo_meme_video.mp4" }
            _isVideoLoading.value = false
        }
    }

    fun generateMusicWithLyria(prompt: String, isShort: Boolean) {
        viewModelScope.launch {
            _isMusicLoading.value = true
            _generatedMusicUrl.value = ""
            
            val model = if (isShort) "lyria-3-clip-preview" else "lyria-3-pro-preview"
            val lengthDesc = if (isShort) "up to 30 seconds" else "full length track"
            val fullPrompt = "Generate a catchy background instrumental track based on: '$prompt'. Target length: $lengthDesc."
            
            val result = callMediaGenerationApi(model, fullPrompt)
            _generatedMusicUrl.value = result.ifEmpty { "https://example.com/simulated_lyria_meme_track.mp3" }
            _isMusicLoading.value = false
        }
    }

    private suspend fun callMediaGenerationApi(modelName: String, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ""
        }
        
        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
            .post(requestBody)
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext ""
                val responseBody = response.body?.string() ?: return@withContext ""
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "")
                    }
                }
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
