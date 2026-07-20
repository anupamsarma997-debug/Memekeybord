package com.example

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.MemeDialogue
import com.example.data.MemeRepository
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.text.BasicTextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.util.Locale

class MemeInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, TextToSpeech.OnInitListener {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store by lazy { ViewModelStore() }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var database: AppDatabase
    private lateinit var repository: MemeRepository
    private lateinit var sharedPrefs: SharedPreferences

    // Text To Speech
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        database = AppDatabase.getDatabase(this)
        repository = MemeRepository(database.memeDao())
        sharedPrefs = getSharedPreferences("desi_meme_prefs", Context.MODE_PRIVATE)

        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            isTtsInitialized = true
        }
    }

    private fun speakDialogue(text: String) {
        if (isTtsInitialized && tts != null) {
            val pitch = sharedPrefs.getFloat("speech_pitch", 1.0f)
            val rate = sharedPrefs.getFloat("speech_rate", 1.0f)
            tts?.setPitch(pitch)
            tts?.setSpeechRate(rate)

            val cleanedText = text
                .replace("Bhaai", "Bhai")
                .replace("baba", "baabaa")
                .replace("pagli", "paglee")
                .replace("ricks", "riks")
                .replace("pappi", "pappee")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, "meme_service_${System.currentTimeMillis()}")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    private fun triggerVibration(mood: String) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator == null || !vibrator.hasVibrator()) return

        val multiplier = sharedPrefs.getFloat("vibration_strength", 1.0f)
        val timings: LongArray
        val amplitudes: IntArray?

        when (mood) {
            "LIGHT" -> {
                timings = longArrayOf(0, 15)
                amplitudes = intArrayOf(0, 80)
            }
            "FUNNY" -> {
                timings = longArrayOf(0, (60 * multiplier).toLong(), 40, (60 * multiplier).toLong(), 40, (80 * multiplier).toLong())
                amplitudes = intArrayOf(0, 150, 0, 200, 0, 255)
            }
            "SWAG" -> {
                timings = longArrayOf(0, (180 * multiplier).toLong(), 100, (180 * multiplier).toLong())
                amplitudes = intArrayOf(0, 255, 0, 255)
            }
            "SAD" -> {
                timings = longArrayOf(0, (400 * multiplier).toLong())
                amplitudes = intArrayOf(0, 100)
            }
            "SHOCK" -> {
                timings = longArrayOf(0, (80 * multiplier).toLong(), 100, (250 * multiplier).toLong())
                amplitudes = intArrayOf(0, 255, 0, 180)
            }
            "LOVE" -> {
                timings = longArrayOf(0, (150 * multiplier).toLong(), 80, (150 * multiplier).toLong())
                amplitudes = intArrayOf(0, 180, 0, 180)
            }
            "ANGRY" -> {
                timings = longArrayOf(0, (40 * multiplier).toLong(), 30, (40 * multiplier).toLong(), 30, (40 * multiplier).toLong(), 30, (100 * multiplier).toLong())
                amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
            }
            else -> {
                timings = longArrayOf(0, (50 * multiplier).toLong())
                amplitudes = intArrayOf(0, 120)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } catch (e: Exception) {
                vibrator.vibrate(timings[1])
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings[1])
        }
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MemeInputMethodService)
            setViewTreeViewModelStoreOwner(this@MemeInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@MemeInputMethodService)
        }

        composeView.setContent {
            MyApplicationTheme {
                SystemKeyboardView(
                    repository = repository,
                    onCommitText = { commitTextIntoApp(it) },
                    onBackspace = { performBackspace() },
                    onSpace = { commitTextIntoApp(" ") },
                    onDone = { performDoneAction() },
                    onSpeak = { speakDialogue(it) },
                    onVibrate = { triggerVibration(it) },
                    sharedPrefs = sharedPrefs,
                    serviceScope = serviceScope
                )
            }
        }

        return composeView
    }

    private fun commitTextIntoApp(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    private fun performBackspace() {
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
    }

    private fun performDoneAction() {
        val ic = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null) {
            val actionId = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            if (actionId != EditorInfo.IME_ACTION_NONE) {
                ic.performEditorAction(actionId)
            } else {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// --- High-fidelity custom System Keyboard Composable ---

val KeyboardPurple = Color(0xFF673AB7)
val KeyboardPink = Color(0xFFE91E63)
val KeyboardYellow = Color(0xFFFFC107)
val KeyboardGreen = Color(0xFF4CAF50)
val KeyboardCharcoal = Color(0xFF121214)
val KeyboardWhite = Color(0xFFF9F9FA)

@Composable
fun SystemKeyboardView(
    repository: MemeRepository,
    onCommitText: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onDone: () -> Unit,
    onSpeak: (String) -> Unit,
    onVibrate: (String) -> Unit,
    sharedPrefs: SharedPreferences,
    serviceScope: CoroutineScope
) {
    var activeTab by remember { mutableStateOf("⌨️ Keys") }
    var selectedCategoryTab by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // QWERTY Soft Keyboard layout States
    var shiftState by remember { mutableStateOf(false) }
    var numberMode by remember { mutableStateOf(false) }

    // Gemini AI state
    var aiTopic by remember { mutableStateOf("") }
    var aiCategory by remember { mutableStateOf("Funny Dialogue 🤣") }
    var generatedAiText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    // Custom creation form state
    var newEmoji by remember { mutableStateOf("") }
    var newDialogue by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("😂 Hasna") }
    var newMood by remember { mutableStateOf("FUNNY") }

    // Real-time collection from DB flow
    val allDialoguesState = repository.allDialogues.collectAsState(initial = emptyList())
    val dialogues = allDialoguesState.value

    // Filtering logic
    val filteredDialogues = remember(dialogues, selectedCategoryTab, searchQuery, activeTab) {
        var list = dialogues
        if (activeTab == "😂 Memes") {
            if (selectedCategoryTab != "All") {
                if (selectedCategoryTab == "Favorites") {
                    list = list.filter { it.isFavorite }
                } else {
                    list = list.filter { it.category.equals(selectedCategoryTab, ignoreCase = true) }
                }
            }
            list = list.filter { it.category != "📜 Shayari" }
        } else if (activeTab == "📜 Shayari") {
            list = list.filter { it.category == "📜 Shayari" }
        }

        if (searchQuery.isNotEmpty()) {
            list = list.filter {
                it.dialogue.contains(searchQuery, ignoreCase = true) ||
                it.emoji.contains(searchQuery) ||
                it.mood.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    val insertMode = remember { mutableStateOf(sharedPrefs.getString("insert_mode", "DIALOGUE") ?: "DIALOGUE") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(Color(0xFF0F0F11))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        // Upper Navigation Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("⌨️ Keys", "😂 Memes", "📜 Shayari", "🔥 Trending", "✨ AI Writer", "🎨 Custom").forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) KeyboardPurple else Color(0xFF1A1A1E))
                        .clickable {
                            activeTab = tab
                            searchQuery = ""
                            onVibrate("LIGHT")
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) KeyboardWhite else Color.Gray
                    )
                }
            }
        }

        Divider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(bottom = 6.dp))

        // Content Area depending on tab selected
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                "⌨️ Keys" -> {
                    SimulatedServiceKeyboard(
                        shiftState = shiftState,
                        numberMode = numberMode,
                        onKeyTyped = { char ->
                            val letter = if (shiftState) char.uppercase() else char.lowercase()
                            onCommitText(letter)
                            if (shiftState) shiftState = false
                            onVibrate("LIGHT")
                        },
                        onBackspace = {
                            onBackspace()
                            onVibrate("LIGHT")
                        },
                        onSpace = {
                            onSpace()
                            onVibrate("LIGHT")
                        },
                        onShiftToggle = {
                            shiftState = !shiftState
                            onVibrate("LIGHT")
                        },
                        onNumberModeToggle = {
                            numberMode = !numberMode
                            onVibrate("LIGHT")
                        },
                        onDone = {
                            onDone()
                            onVibrate("LIGHT")
                        }
                    )
                }
                "🔥 Trending" -> {
                    SystemKeyboardTrendingSection(
                        repository = repository,
                        onCommitText = onCommitText,
                        onSpeak = onSpeak,
                        onVibrate = onVibrate,
                        sharedPrefs = sharedPrefs,
                        serviceScope = serviceScope
                    )
                }
                "😂 Memes" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Category Tabs Filter Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "All",
                                "😂 Hasna",
                                "😎 Swag",
                                "😭 Dukh",
                                "😱 Shock",
                                "❤️ Pyaar",
                                "😡 Gussa",
                                "🤝 Dosti",
                                "Favorites"
                            ).forEach { cat ->
                                val isSelected = selectedCategoryTab == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) KeyboardPink else Color(0xFF1E1E22))
                                        .clickable {
                                            selectedCategoryTab = cat
                                            onVibrate("LIGHT")
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) KeyboardWhite else Color.Gray
                                    )
                                }
                            }
                        }

                        // Search box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1A1A1E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search dialogues...", color = Color.Gray, fontSize = 11.sp)
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = KeyboardWhite, fontSize = 11.sp)
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }

                        if (filteredDialogues.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No meme keys found!", color = Color.Gray, fontSize = 11.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredDialogues, key = { it.id }) { item ->
                                    ServiceMemeKey(
                                        item = item,
                                        onClick = {
                                            val text = if (insertMode.value == "DIALOGUE") {
                                                "${item.emoji} ${item.dialogue} "
                                            } else {
                                                "${item.emoji} "
                                            }
                                            onCommitText(text)
                                            onSpeak(item.dialogue)
                                            onVibrate(item.mood)
                                        },
                                        onToggleFavorite = {
                                            serviceScope.launch {
                                                repository.update(item.copy(isFavorite = !item.isFavorite))
                                            }
                                            onVibrate("LIGHT")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "📜 Shayari" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (filteredDialogues.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No Shayaris found!", color = Color.Gray, fontSize = 11.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredDialogues, key = { it.id }) { item ->
                                    ServiceShayariKey(
                                        item = item,
                                        onClick = {
                                            onCommitText("${item.dialogue} ")
                                            onSpeak(item.dialogue)
                                            onVibrate("LOVE")
                                        },
                                        onToggleFavorite = {
                                            serviceScope.launch {
                                                repository.update(item.copy(isFavorite = !item.isFavorite))
                                            }
                                            onVibrate("LIGHT")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                "✨ AI Writer" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "✨ WRITE WITH GEMINI AI ON-THE-GO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = KeyboardYellow
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Attitude 😎", "Romantic ❤️", "Sad 😭", "Funny 🤣", "Dosti 🤝").forEach { cat ->
                                val isSelected = aiCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) KeyboardYellow else Color(0xFF1E1E22))
                                        .clickable {
                                            aiCategory = cat
                                            onVibrate("LIGHT")
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) KeyboardCharcoal else Color.Gray
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1A1A1E), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (aiTopic.isEmpty()) {
                                    Text("Topic: e.g. Sher, Mohabbat, Dushman...", color = Color.Gray, fontSize = 11.sp)
                                }
                                BasicTextField(
                                    value = aiTopic,
                                    onValueChange = { aiTopic = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = KeyboardWhite, fontSize = 11.sp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (aiTopic.trim().isNotEmpty()) {
                                        isAiLoading = true
                                        generatedAiText = ""
                                        serviceScope.launch {
                                            val prompt = "Write a catchy single-line Desi Hinglish (Roman Hindi) meme dialogue or Shayari. " +
                                                         "Category is '$aiCategory' and topic is '${aiTopic.trim()}'. " +
                                                         "Keep it short and punchy (1-2 lines maximum). Return ONLY the Hindi line itself."
                                            val result = executeGeminiCall(prompt)
                                            generatedAiText = result
                                            isAiLoading = false
                                            if (result.isNotEmpty() && !result.startsWith("Error")) {
                                                onSpeak(result)
                                            }
                                        }
                                    }
                                },
                                enabled = !isAiLoading && aiTopic.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = KeyboardYellow),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (isAiLoading) "Cooking..." else "Write ✨",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = KeyboardCharcoal
                                )
                            }
                        }

                        if (isAiLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(Color(0xFF1A1A1E), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = KeyboardYellow, modifier = Modifier.size(16.dp))
                            }
                        } else if (generatedAiText.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, KeyboardYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = generatedAiText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KeyboardWhite,
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                onCommitText("$generatedAiText ")
                                                onVibrate("SWAG")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = KeyboardPurple),
                                            modifier = Modifier.height(26.dp).weight(1f),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("➕ Insert", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { onSpeak(generatedAiText) },
                                            colors = ButtonDefaults.buttonColors(containerColor = KeyboardPink),
                                            modifier = Modifier.height(26.dp).weight(1f),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("🗣️ Speak", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "🎨 Custom" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🎨 DESIGN CUSTOM DIALOGUE BUTTON",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = KeyboardGreen
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .background(Color(0xFF1A1A1E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (newEmoji.isEmpty()) {
                                    Text("Emoji", color = Color.Gray, fontSize = 11.sp)
                                }
                                BasicTextField(
                                    value = newEmoji,
                                    onValueChange = { newEmoji = it.take(2) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = KeyboardWhite, fontSize = 11.sp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1A1A1E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (newDialogue.isEmpty()) {
                                    Text("Dialogue words...", color = Color.Gray, fontSize = 11.sp)
                                }
                                BasicTextField(
                                    value = newDialogue,
                                    onValueChange = { newDialogue = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = KeyboardWhite, fontSize = 11.sp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("😂 Hasna", "😎 Swag", "😭 Dukh", "😱 Shock", "❤️ Pyaar", "😡 Gussa", "🤝 Dosti").forEach { cat ->
                                val isSelected = newCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) KeyboardGreen else Color(0xFF1A1A1E))
                                        .clickable {
                                            newCategory = cat
                                            onVibrate("LIGHT")
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) KeyboardCharcoal else Color.Gray
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (newEmoji.trim().isNotEmpty() && newDialogue.trim().isNotEmpty()) {
                                    serviceScope.launch {
                                        repository.insert(
                                            MemeDialogue(
                                                emoji = newEmoji.trim(),
                                                dialogue = newDialogue.trim(),
                                                category = newCategory,
                                                mood = newMood,
                                                isCustom = true
                                            )
                                        )
                                        newEmoji = ""
                                        newDialogue = ""
                                    }
                                    onVibrate("FUNNY")
                                }
                            },
                            enabled = newEmoji.trim().isNotEmpty() && newDialogue.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = KeyboardGreen),
                            modifier = Modifier.fillMaxWidth().height(30.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Create Key 🚀", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KeyboardCharcoal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulatedServiceKeyboard(
    shiftState: Boolean,
    numberMode: Boolean,
    onKeyTyped: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onShiftToggle: () -> Unit,
    onNumberModeToggle: () -> Unit,
    onDone: () -> Unit
) {
    val row1 = if (numberMode) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    } else {
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    }

    val row2 = if (numberMode) {
        listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
    } else {
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    }

    val row3 = if (numberMode) {
        listOf("!", "\"", "'", ":", ";", "/", "?", ",")
    } else {
        listOf("z", "x", "c", "v", "b", "n", "m")
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
        ) {
            row1.forEach { char ->
                ServiceKey(char, modifier = Modifier.weight(1f)) { onKeyTyped(char) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
        ) {
            if (!numberMode) Spacer(modifier = Modifier.width(6.dp))
            row2.forEach { char ->
                ServiceKey(char, modifier = Modifier.weight(1f)) { onKeyTyped(char) }
            }
            if (!numberMode) Spacer(modifier = Modifier.width(6.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (shiftState) KeyboardPurple else Color(0xFF24242A))
                    .clickable { onShiftToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text("⇧", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KeyboardWhite)
            }

            row3.forEach { char ->
                ServiceKey(char, modifier = Modifier.weight(1f)) { onKeyTyped(char) }
            }

            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF24242A))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Text("⌫", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KeyboardWhite)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.8f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF24242A))
                    .clickable { onNumberModeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (numberMode) "ABC" else "?123",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = KeyboardWhite
                )
            }

            Box(
                modifier = Modifier
                    .weight(5f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2A2A30))
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text("Space", fontSize = 11.sp, color = Color.LightGray)
            }

            Box(
                modifier = Modifier
                    .weight(2.2f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(KeyboardPurple, KeyboardPink)))
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text("ENTER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = KeyboardWhite)
            }
        }
    }
}

@Composable
fun ServiceKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E1E22))
            .clickable { onClick() }
            .border(0.5.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KeyboardWhite
        )
    }
}

@Composable
fun ServiceMemeKey(
    item: MemeDialogue,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable { onClick() }
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.emoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.dialogue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KeyboardWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) KeyboardPink else Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun ServiceShayariKey(
    item: MemeDialogue,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
            .border(1.dp, KeyboardPink.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            Row(
                modifier = Modifier.fillMaxSize().padding(end = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.emoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.dialogue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = KeyboardWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) KeyboardPink else Color.Gray,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

private suspend fun executeGeminiCall(prompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "API Key required in AI Studio Secrets panel!"
    }
    
    val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
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

@Composable
fun SystemKeyboardTrendingSection(
    repository: MemeRepository,
    onCommitText: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onVibrate: (String) -> Unit,
    sharedPrefs: SharedPreferences,
    serviceScope: CoroutineScope
) {
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
        MemeDialogue(emoji = "💔", dialogue = "Toba toba, saara mood kharab kar diya!", category = "😡 Gussa", mood = "ANGRY")
    )
    
    val startIndex = day % pool.size
    val dailyTrends = remember {
        val resultList = mutableListOf<MemeDialogue>()
        for (i in 0 until 6) {
            val idx = (startIndex + i) % pool.size
            resultList.add(pool[idx])
        }
        resultList
    }

    var trendingMemes by remember { mutableStateOf<List<MemeDialogue>>(dailyTrends) }
    var isApiLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔥 TODAY'S DESI TRENDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = KeyboardPink
                )
                Text(
                    text = "Insert, speak, or save to keyboard",
                    fontSize = 8.sp,
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isApiLoading) Color.DarkGray else KeyboardPurple)
                    .clickable(enabled = !isApiLoading) {
                        isApiLoading = true
                        serviceScope.launch(Dispatchers.IO) {
                            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                            if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                                val client = okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                                    
                                val prompt = "Find and summarize the top 6 absolute most popular viral Desi/Hindi/Hinglish meme dialogues, social media trends, movie dialogue catchphrases, or slang phrases currently trending in India right now in 2026. " +
                                             "For each trending meme, specify a suitable single emoji, the dialogue text (in Hinglish, i.e., Roman script, like 'Moye Moye' or 'Just looking like a wow'), a category (choose exactly from: '😂 Hasna', '😎 Swag', '😭 Dukh', '😱 Shock', '❤️ Pyaar', '😡 Gussa', '🤝 Dosti'), and the mood (choose exactly from: 'FUNNY', 'SWAG', 'SAD', 'SHOCK', 'LOVE', 'ANGRY'). " +
                                             "Format the output strictly as a JSON array of objects. Do not include any other text, markdown blocks, or translations. " +
                                             "Example: [ { \"emoji\": \"😭\", \"dialogue\": \"Yeh dukh kaahe khatam nahi hota be\", \"category\": \"😭 Dukh\", \"mood\": \"SAD\" } ]"

                                val jsonBody = org.json.JSONObject().apply {
                                    put("contents", org.json.JSONArray().apply {
                                        put(org.json.JSONObject().apply {
                                            put("parts", org.json.JSONArray().apply {
                                                put(org.json.JSONObject().apply {
                                                    put("text", prompt)
                                                })
                                            })
                                        })
                                    })
                                    put("tools", org.json.JSONArray().apply {
                                        put(org.json.JSONObject().apply {
                                            put("googleSearchRetrieval", org.json.JSONObject())
                                        })
                                    })
                                    put("generationConfig", org.json.JSONObject().apply {
                                        put("responseMimeType", "application/json")
                                        put("temperature", 0.4)
                                    })
                                }
                                
                                val mediaType = "application/json; charset=utf-8".toMediaType()
                                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                                val request = okhttp3.Request.Builder()
                                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                                    .post(requestBody)
                                    .build()
                                    
                                try {
                                    client.newCall(request).execute().use { response ->
                                        if (response.isSuccessful) {
                                            val body = response.body?.string() ?: ""
                                            val jsonResponse = org.json.JSONObject(body)
                                            val candidates = jsonResponse.optJSONArray("candidates")
                                            if (candidates != null && candidates.length() > 0) {
                                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                                val parts = content?.optJSONArray("parts")
                                                val resultText = parts?.getJSONObject(0)?.optString("text", "") ?: ""
                                                
                                                var cleanedJson = resultText.trim()
                                                if (cleanedJson.startsWith("```json")) {
                                                    cleanedJson = cleanedJson.removePrefix("```json").trim()
                                                }
                                                if (cleanedJson.endsWith("```")) {
                                                    cleanedJson = cleanedJson.removeSuffix("```").trim()
                                                }
                                                
                                                val jsonArray = org.json.JSONArray(cleanedJson)
                                                val parsedList = mutableListOf<MemeDialogue>()
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
                                                if (parsedList.isNotEmpty()) {
                                                    withContext(Dispatchers.Main) {
                                                        trendingMemes = parsedList
                                                        Toast.makeText(context, "Trends updated live! ⚡", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            withContext(Dispatchers.Main) {
                                isApiLoading = false
                            }
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isApiLoading) "Updating..." else "Live ⚡",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(trendingMemes) { item: MemeDialogue ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF222225), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(KeyboardPurple.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, KeyboardPurple.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.emoji, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.dialogue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KeyboardWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(item.category, fontSize = 7.sp, color = KeyboardPink)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onSpeak(item.dialogue)
                                    onVibrate(item.mood)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Speak",
                                    tint = KeyboardGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onSpeak(item.dialogue)
                                    onVibrate(item.mood)
                                    val insertModeVal = sharedPrefs.getString("insert_mode", "DIALOGUE") ?: "DIALOGUE"
                                    val textToInsert = if (insertModeVal == "DIALOGUE") {
                                        "${item.emoji} ${item.dialogue} "
                                    } else {
                                        "${item.emoji} "
                                    }
                                    onCommitText(textToInsert)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Use",
                                    tint = KeyboardYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    serviceScope.launch {
                                        repository.insert(item.copy(id = 0, isCustom = true))
                                        Toast.makeText(context, "Saved to Memes Tab! 📥", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Save",
                                    tint = Color.Cyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

