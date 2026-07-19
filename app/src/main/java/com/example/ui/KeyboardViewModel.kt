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

class KeyboardViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: MemeRepository
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    // Input text in the meme message board
    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

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
        if (isTtsInitialized) {
            tts?.setSpeechRate(rate)
        }
    }

    fun updateSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
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
    }

    fun setVibrationStrength(multiplier: Float) {
        _vibrationStrengthMultiplier.value = multiplier
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
