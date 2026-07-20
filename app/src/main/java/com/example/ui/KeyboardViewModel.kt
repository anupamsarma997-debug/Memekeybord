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

    // Configuration settings
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
