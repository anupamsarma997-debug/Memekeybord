package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MemeDialogue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// High-contrast, saturated colors for the Desi pop-art aesthetic
val DesiCharcoal = Color(0xFF121214)
val DesiWhite = Color(0xFFF9F9FA)
val DesiOrange = Color(0xFFFF5722)
val DesiYellow = Color(0xFFFFC107)
val DesiPink = Color(0xFFE91E63)
val DesiGreen = Color(0xFF4CAF50)
val DesiPurple = Color(0xFF673AB7)
val DesiBlue = Color(0xFF00BCD4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeKeyboardApp(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val typedText by viewModel.typedText.collectAsStateWithLifecycle()
    val filteredDialogues by viewModel.filteredDialogues.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeDialogueEvent by viewModel.activeDialogueEvent.collectAsStateWithLifecycle()

    val insertMode by viewModel.insertMode.collectAsStateWithLifecycle()
    val vibrationStrength by viewModel.vibrationStrengthMultiplier.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechPitch by viewModel.speechPitch.collectAsStateWithLifecycle()

    var showCreatorSheet by remember { mutableStateOf(false) }
    var showTunerPanel by remember { mutableStateOf(false) }

    // Categories definition
    val categories = listOf(
        "All" to "🔥",
        "😂 Hasna" to "😂",
        "😎 Swag" to "😎",
        "😭 Dukh" to "😭",
        "😱 Shock" to "😱",
        "❤️ Pyaar" to "❤️",
        "😡 Gussa" to "😡",
        "Favorites" to "⭐"
    )

    // Clear active dialogue overlay after 2 seconds
    LaunchedEffect(activeDialogueEvent) {
        if (activeDialogueEvent != null) {
            delay(1800)
            viewModel.clearActiveDialogueEvent()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesiCharcoal)
            .statusBarsPadding()
    ) {
        // Main Scrollable Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Header Hero Area
            HeaderHeroSection(onResetDefaults = {
                viewModel.resetToDefaults()
                Toast.makeText(context, "Meme Keyboard reset to default dialogues!", Toast.LENGTH_SHORT).show()
            })

            // Onboarding System Keyboard Activation Banner
            SystemKeyboardActivationCard()

            // Main Workspace Board Card
            MessageBoardCard(
                typedText = typedText,
                onTextChange = { viewModel.updateTypedText(it) },
                onCopy = { viewModel.copyToClipboard() },
                onClear = { viewModel.clearText() },
                onShare = {
                    if (typedText.isNotEmpty()) {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, typedText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Meme Dialogue")
                        context.startActivity(shareIntent)
                    } else {
                        Toast.makeText(context, "Nothing to share! Type some dialogues.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Settings Tuner & Creator Expandable Row
            QuickActionsRow(
                showTuner = showTunerPanel,
                onToggleTuner = { showTunerPanel = !showTunerPanel },
                onOpenCreator = { showCreatorSheet = true }
            )

            // Tuning Sliders Panel
            AnimatedVisibility(
                visible = showTunerPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TuningPanel(
                    insertMode = insertMode,
                    vibrationStrength = vibrationStrength,
                    speechRate = speechRate,
                    speechPitch = speechPitch,
                    onSetInsertMode = { viewModel.setInsertMode(it) },
                    onSetVibrate = { viewModel.setVibrationStrength(it) },
                    onSetRate = { viewModel.updateSpeechRate(it) },
                    onSetPitch = { viewModel.updateSpeechPitch(it) }
                )
            }

            // Interactive Smart Keyboard Panel
            InteractiveKeyboardPanel(
                viewModel = viewModel,
                categories = categories,
                filteredDialogues = filteredDialogues,
                selectedTab = selectedTab,
                searchQuery = searchQuery,
                onTabSelect = { viewModel.updateSelectedTab(it) },
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )
        }

        // Active Dialogue Event Splash Pop-up Overlay
        AnimatedVisibility(
            visible = activeDialogueEvent != null,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            activeDialogueEvent?.let { item ->
                DialogueSplashPopup(item = item)
            }
        }

        // Custom Meme Creator Dialog (Custom Bottom Sheet/Form Dialog)
        if (showCreatorSheet) {
            MemeCreatorDialog(
                onDismiss = { showCreatorSheet = false },
                onAddDialogue = { emoji, dialogue, category, mood ->
                    viewModel.addNewDialogue(emoji, dialogue, category, mood)
                    showCreatorSheet = false
                    Toast.makeText(context, "Meme dialogue added! 🚀", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun SystemKeyboardActivationCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(2.dp, DesiYellow, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1C15))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🌐 USE KEYBOARD IN WHATSAPP / TELEGRAM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = DesiYellow
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Type Desi Memes & Shayaris globally! Follow these 2 easy steps:",
                fontSize = 10.sp,
                color = Color.LightGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Settings couldn't be opened. Please enable manually.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesiOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("1. Enable Keyboard ⚙️", fontSize = 10.sp, fontWeight = FontWeight.Black, color = DesiWhite)
                }

                Button(
                    onClick = {
                        try {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Couldn't open keyboard switcher.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesiGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("2. Switch Keyboard 🔄", fontSize = 10.sp, fontWeight = FontWeight.Black, color = DesiWhite)
                }
            }
        }
    }
}

@Composable
fun HeaderHeroSection(onResetDefaults: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .drawBehind {
                // Subtle stylish dots backdrop pattern
                val columns = 12
                val rows = 4
                val cellWidth = size.width / columns
                val cellHeight = size.height / rows
                for (c in 0..columns) {
                    for (r in 0..rows) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.04f),
                            radius = 3.dp.toPx(),
                            center = Offset(c * cellWidth, r * cellHeight)
                        )
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DESI MEME BOARD",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = DesiWhite,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.rotate(angle)
                    )
                    Text(
                        text = " 🎧🔥",
                        fontSize = 22.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Type messages with iconic Desi dialogue audio & custom vibrations!",
                    fontSize = 11.sp,
                    color = Color.LightGray.copy(alpha = 0.8f)
                )
            }

            IconButton(
                onClick = onResetDefaults,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DesiCharcoal.copy(alpha = 0.5f))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Defaults",
                    tint = DesiOrange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBoardCard(
    typedText: String,
    onTextChange: (String) -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .border(2.dp, DesiPurple, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(DesiGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MEME MESSAGE BOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                if (typedText.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = typedText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .testTag("typed_text_input"),
                placeholder = {
                    Text(
                        text = "Tap on any Desi emoji below to speak & type iconic dialogues!",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DesiWhite
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("copy_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DesiPurple),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DesiOrange),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    showTuner: Boolean,
    onToggleTuner: () -> Unit,
    onOpenCreator: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle Settings Tuner Button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleTuner() }
                .background(if (showTuner) DesiPurple.copy(alpha = 0.2f) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Tuning Board",
                tint = if (showTuner) DesiPurple else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Vibe & Speech Tuner",
                fontSize = 12.sp,
                color = if (showTuner) DesiWhite else Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (showTuner) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle",
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }

        // Custom Creator Dialog Trigger Button
        Button(
            onClick = onOpenCreator,
            colors = ButtonDefaults.buttonColors(containerColor = DesiCharcoal),
            border = BorderStroke(1.5.dp, DesiYellow),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add custom dialogue",
                tint = DesiYellow,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Dialogue", fontSize = 11.sp, color = DesiYellow, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TuningPanel(
    insertMode: String,
    vibrationStrength: Float,
    speechRate: Float,
    speechPitch: Float,
    onSetInsertMode: (String) -> Unit,
    onSetVibrate: (Float) -> Unit,
    onSetRate: (Float) -> Unit,
    onSetPitch: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161619))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Mode Select Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keyboard Typing Mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .padding(2.dp)
                ) {
                    listOf("DIALOGUE" to "Dialogue + Emoji", "EMOJI" to "Emoji Only").forEach { (mode, label) ->
                        val isSelected = insertMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) DesiPurple else Color.Transparent)
                                .clickable { onSetInsertMode(mode) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DesiWhite else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vibration Level Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Vibration, contentDescription = "Vibration", tint = DesiPink, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vibe Shock Power: ${(vibrationStrength * 100).toInt()}%", fontSize = 11.sp, color = DesiWhite, modifier = Modifier.width(130.dp))
                Slider(
                    value = vibrationStrength,
                    onValueChange = onSetVibrate,
                    valueRange = 0.0f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = DesiPink,
                        activeTrackColor = DesiPink,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Speech Rate Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = "Speed", tint = DesiBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dialogue Speed: ${"%.1f".format(speechRate)}x", fontSize = 11.sp, color = DesiWhite, modifier = Modifier.width(130.dp))
                Slider(
                    value = speechRate,
                    onValueChange = onSetRate,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = DesiBlue,
                        activeTrackColor = DesiBlue,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Speech Pitch Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hearing, contentDescription = "Pitch", tint = DesiYellow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voice Pitch: ${"%.1f".format(speechPitch)}x", fontSize = 11.sp, color = DesiWhite, modifier = Modifier.width(130.dp))
                Slider(
                    value = speechPitch,
                    onValueChange = onSetPitch,
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = DesiYellow,
                        activeTrackColor = DesiYellow,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FilterAndSearchSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    categories: List<Pair<String, String>>,
    selectedTab: String,
    onTabSelect: (String) -> Unit
) {
    Column {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .testTag("search_bar"),
            placeholder = { Text("Search dialogue, emoji, or mood...", fontSize = 12.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(16.dp)) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = DesiWhite),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF161619),
                unfocusedContainerColor = Color(0xFF161619),
                focusedBorderColor = DesiPurple.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Category Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (tabName, emoji) ->
                val isSelected = selectedTab.equals(tabName, ignoreCase = true)
                val chipBg = if (isSelected) DesiPurple else Color(0xFF1E1E22)
                val chipBorder = if (isSelected) DesiPurple else Color.Gray.copy(alpha = 0.2f)
                val textColor = if (isSelected) DesiWhite else Color.Gray

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                        .clickable { onTabSelect(tabName) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tabName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun MemeKeyCard(
    item: MemeDialogue,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)?
) {
    // Get unique vibrant mood background accents
    val moodColor = when (item.mood) {
        "FUNNY" -> DesiYellow
        "SWAG" -> DesiGreen
        "SAD" -> DesiBlue
        "SHOCK" -> DesiPurple
        "LOVE" -> DesiPink
        "ANGRY" -> DesiOrange
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .testTag("meme_key_${item.emoji}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Visual indicators
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .align(Alignment.CenterStart)
            ) {
                drawRect(color = moodColor)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Emoji & Mood Pill & Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.emoji,
                            fontSize = 26.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Mood Tag Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(moodColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.mood,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = moodColor
                            )
                        }
                    }

                    // Card top-right buttons (Favorite / Delete)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (item.isFavorite) DesiPink else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Dialogue text
                Text(
                    text = item.dialogue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesiWhite,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun DialogueSplashPopup(item: MemeDialogue) {
    val infiniteTransition = rememberInfiniteTransition()
    val scaleMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val moodColor = when (item.mood) {
        "FUNNY" -> DesiYellow
        "SWAG" -> DesiGreen
        "SAD" -> DesiBlue
        "SHOCK" -> DesiPurple
        "LOVE" -> DesiPink
        "ANGRY" -> DesiOrange
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .width(280.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .border(3.dp, moodColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141417))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Exploding Emoji Image Icon
            Text(
                text = item.emoji,
                fontSize = 80.sp,
                modifier = Modifier
                    .animateContentSize()
                    .rotate(scaleMultiplier * 4f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mood tag banner
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(moodColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${item.mood} VIBE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = moodColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Large Dramatic Speech dialogue
            Text(
                text = "\"${item.dialogue}\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DesiWhite,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontFamily = FontFamily.Serif
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeCreatorDialog(
    onDismiss: () -> Unit,
    onAddDialogue: (String, String, String, String) -> Unit
) {
    var emoji by remember { mutableStateOf("") }
    var dialogue by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("😂 Hasna") }
    var mood by remember { mutableStateOf("FUNNY") }

    val categories = listOf("😂 Hasna", "😎 Swag", "😭 Dukh", "😱 Shock", "❤️ Pyaar", "😡 Gussa")
    val moods = listOf("FUNNY", "SWAG", "SAD", "SHOCK", "LOVE", "ANGRY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "➕ ADD DESI DIALOGUE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = DesiYellow
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Create your own customized dialogue button to type, play, and shake!",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                // Emoji input
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(2) }, // Limit to 1-2 emoji chars
                    label = { Text("Emoji (e.g. 🤩)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesiYellow, unfocusedBorderColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dialogue input
                OutlinedTextField(
                    value = dialogue,
                    onValueChange = { dialogue = it },
                    label = { Text("Dialogue Text", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Jalwa hai hamara yahan!", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesiYellow, unfocusedBorderColor = Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category select
                Text("Select Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) DesiYellow else Color(0xFF222225))
                                .clickable { category = cat }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                cat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DesiCharcoal else Color.Gray
                            )
                        }
                    }
                }

                // Mood select
                Text("Select Vibe Mood (Vibration Style):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    moods.forEach { md ->
                        val isSelected = mood == md
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) DesiYellow else Color(0xFF222225))
                                .clickable { mood = md }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                md,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DesiCharcoal else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emoji.trim().isEmpty() || dialogue.trim().isEmpty()) {
                        return@Button
                    }
                    onAddDialogue(emoji.trim(), dialogue.trim(), category, mood)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DesiYellow),
                enabled = emoji.trim().isNotEmpty() && dialogue.trim().isNotEmpty()
            ) {
                Text("Add Button 🔥", color = DesiCharcoal, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E22),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun EmptyStateView(
    searchQuery: String,
    selectedTab: String,
    onResetSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🤠", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No Desi Dialogues Found!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DesiWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try searching for something else or reset your filter.",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onResetSearch,
            colors = ButtonDefaults.buttonColors(containerColor = DesiPurple)
        ) {
            Text("Reset Search & Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- Dynamic Keyboard Layout & Composables ---

@Composable
fun InteractiveKeyboardPanel(
    viewModel: KeyboardViewModel,
    categories: List<Pair<String, String>>,
    filteredDialogues: List<MemeDialogue>,
    selectedTab: String,
    searchQuery: String,
    onTabSelect: (String) -> Unit,
    onQueryChange: (String) -> Unit
) {
    var activeKeyboardTab by remember { mutableStateOf("⌨️ Text") }

    val shiftState by viewModel.keyboardShiftState.collectAsStateWithLifecycle()
    val numberMode by viewModel.keyboardNumberMode.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val generatedAiText by viewModel.generatedAiText.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .border(2.dp, DesiPurple.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("⌨️ Text", "😂 Memes", "📜 Shayari", "✨ AI Writer", "🎨 Custom").forEach { tab ->
                    val isSelected = activeKeyboardTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DesiPurple else Color(0xFF222225))
                            .clickable { activeKeyboardTab = tab }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) DesiWhite else Color.Gray
                        )
                    }
                }
            }

            Divider(color = Color.DarkGray.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(bottom = 10.dp))

            when (activeKeyboardTab) {
                "⌨️ Text" -> {
                    SimulatedQwertyKeyboard(
                        shiftState = shiftState,
                        numberMode = numberMode,
                        onKeyTyped = { viewModel.onKeyTyped(it) },
                        onBackspace = { viewModel.onBackspacePressed() },
                        onSpace = { viewModel.onSpacePressed() },
                        onShiftToggle = { viewModel.onShiftToggle() },
                        onNumberModeToggle = { viewModel.onNumberModeToggle() },
                        onDone = { viewModel.copyToClipboard() }
                    )
                }
                "😂 Memes" -> {
                    Column {
                        FilterAndSearchSection(
                            searchQuery = searchQuery,
                            onQueryChange = onQueryChange,
                            categories = categories,
                            selectedTab = selectedTab,
                            onTabSelect = onTabSelect
                        )

                        val memeDialogues = filteredDialogues.filter { it.category != "📜 Shayari" }
                        if (memeDialogues.isEmpty()) {
                            EmptyStateView(
                                searchQuery = searchQuery,
                                selectedTab = selectedTab,
                                onResetSearch = {
                                    onQueryChange("")
                                    onTabSelect("All")
                                }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(memeDialogues, key = { it.id }) { item ->
                                    MemeKeyCard(
                                        item = item,
                                        onClick = { viewModel.onEmojiKeyPress(item) },
                                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                                        onDelete = if (item.isCustom) {
                                            { viewModel.deleteDialogue(item) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
                "📜 Shayari" -> {
                    val shayariList = filteredDialogues.filter { it.category == "📜 Shayari" }
                    if (shayariList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No Shayaris Found! Add custom ones in Custom tab.", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        Column {
                            Text(
                                "📜 POPULAR ROMAN HINDI SHAYARIS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DesiPink,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(shayariList, key = { it.id }) { item ->
                                    ShayariKeyCard(
                                        item = item,
                                        onClick = { viewModel.onEmojiKeyPress(item) },
                                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                                    )
                                }
                            }
                        }
                    }
                }
                "✨ AI Writer" -> {
                    AiWriterSection(
                        isLoading = isAiLoading,
                        generatedText = generatedAiText,
                        onGenerate = { cat, topic -> viewModel.generateAiDialogue(cat, topic) },
                        onUseText = { viewModel.useGeneratedAiText() },
                        onPlayTts = { viewModel.playTtsForText(it) }
                    )
                }
                "🎨 Custom" -> {
                    InlineMemeCreator(onAddDialogue = { emoji, dialogue, category, mood ->
                        viewModel.addNewDialogue(emoji, dialogue, category, mood)
                    })
                }
            }
        }
    }
}

@Composable
fun SimulatedQwertyKeyboard(
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
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161619))
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            row1.forEach { char ->
                val displayChar = if (shiftState && !numberMode) char.uppercase() else char
                KeyboardKey(displayChar, modifier = Modifier.weight(1f)) { onKeyTyped(char) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            if (!numberMode) Spacer(modifier = Modifier.width(10.dp))
            row2.forEach { char ->
                val displayChar = if (shiftState && !numberMode) char.uppercase() else char
                KeyboardKey(displayChar, modifier = Modifier.weight(1f)) { onKeyTyped(char) }
            }
            if (!numberMode) Spacer(modifier = Modifier.width(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (shiftState) DesiPurple else Color(0xFF2E2E33))
                    .clickable { onShiftToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text("⇧", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DesiWhite)
            }

            row3.forEach { char ->
                val displayChar = if (shiftState && !numberMode) char.uppercase() else char
                KeyboardKey(displayChar, modifier = Modifier.weight(1f)) { onKeyTyped(char) }
            }

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2E2E33))
                    .clickable { onBackspace() },
                contentAlignment = Alignment.Center
            ) {
                Text("⌫", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DesiWhite)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(2f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2D2D31))
                    .clickable { onNumberModeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (numberMode) "ABC" else "?123",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesiWhite
                )
            }

            Box(
                modifier = Modifier
                    .weight(5f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF38383D))
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text("Space", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.LightGray)
            }

            Box(
                modifier = Modifier
                    .weight(2.5f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(DesiPurple, DesiPink)))
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = DesiWhite)
            }
        }
    }
}

@Composable
fun KeyboardKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF28282C))
            .clickable { onClick() }
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DesiWhite
        )
    }
}

@Composable
fun ShayariKeyCard(
    item: MemeDialogue,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .border(1.dp, DesiPink.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(end = 40.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DesiPink.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SHAYARI",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = DesiPink
                        )
                    }
                }

                Text(
                    text = item.dialogue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesiWhite,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) DesiPink else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWriterSection(
    isLoading: Boolean,
    generatedText: String,
    onGenerate: (String, String) -> Unit,
    onUseText: () -> Unit,
    onPlayTts: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Attitude Status 😎") }
    var topic by remember { mutableStateOf("") }

    val categories = listOf(
        "Attitude Status 😎",
        "Romantic Shayari ❤️",
        "Sad Shayari 😭",
        "Funny Dialogue 🤣",
        "Dosti Status 🤝"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "✨ GENIUS AI SHAYARI & MEME WRITER (GEMINI)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = DesiYellow
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) DesiYellow else Color(0xFF222225))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DesiCharcoal else Color.Gray
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                placeholder = { Text("Topic: e.g. Dosti, Sher, Mohabbat...", fontSize = 11.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(42.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1F1F23),
                    unfocusedContainerColor = Color(0xFF1F1F23),
                    focusedBorderColor = DesiYellow,
                    unfocusedBorderColor = Color.DarkGray
                ),
                textStyle = TextStyle(fontSize = 12.sp, color = DesiWhite)
            )

            Button(
                onClick = { if (topic.trim().isNotEmpty()) onGenerate(selectedCategory, topic.trim()) },
                enabled = !isLoading && topic.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = DesiYellow),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Text(
                    text = if (isLoading) "Writing..." else "Write ✨",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = DesiCharcoal
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF1E1E22), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DesiYellow, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gemini is cooking some pure Desi words...", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        } else if (generatedText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DesiYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "\"$generatedText\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DesiWhite,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onUseText,
                            colors = ButtonDefaults.buttonColors(containerColor = DesiPurple),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("➕ Use in Board", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onPlayTts(generatedText) },
                            colors = ButtonDefaults.buttonColors(containerColor = DesiPink),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("🗣️ Play Audio", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineMemeCreator(
    onAddDialogue: (String, String, String, String) -> Unit
) {
    var emoji by remember { mutableStateOf("") }
    var dialogue by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("😂 Hasna") }
    var mood by remember { mutableStateOf("FUNNY") }

    val categories = listOf("😂 Hasna", "😎 Swag", "😭 Dukh", "😱 Shock", "❤️ Pyaar", "😡 Gussa", "🤝 Dosti")
    val moods = listOf("FUNNY", "SWAG", "SAD", "SHOCK", "LOVE", "ANGRY")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🎨 DESIGN CUSTOM DIALOGUE BUTTON",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = DesiGreen
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it.take(2) },
                placeholder = { Text("Emoji: 🤩", fontSize = 11.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.width(100.dp).height(42.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1F1F23),
                    unfocusedContainerColor = Color(0xFF1F1F23),
                    focusedBorderColor = DesiGreen,
                    unfocusedBorderColor = Color.DarkGray
                ),
                textStyle = TextStyle(fontSize = 12.sp, color = DesiWhite)
            )

            OutlinedTextField(
                value = dialogue,
                onValueChange = { dialogue = it },
                placeholder = { Text("Dialogue text...", fontSize = 11.sp, color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(42.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1F1F23),
                    unfocusedContainerColor = Color(0xFF1F1F23),
                    focusedBorderColor = DesiGreen,
                    unfocusedBorderColor = Color.DarkGray
                ),
                textStyle = TextStyle(fontSize = 12.sp, color = DesiWhite)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = category == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) DesiGreen else Color(0xFF222225))
                        .clickable { category = cat }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DesiCharcoal else Color.Gray
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            moods.forEach { md ->
                val isSelected = mood == md
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) DesiGreen else Color(0xFF222225))
                        .clickable { mood = md }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = md,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DesiCharcoal else Color.Gray
                    )
                }
            }
        }

        Button(
            onClick = {
                if (emoji.trim().isNotEmpty() && dialogue.trim().isNotEmpty()) {
                    onAddDialogue(emoji.trim(), dialogue.trim(), category, mood)
                    emoji = ""
                    dialogue = ""
                }
            },
            enabled = emoji.trim().isNotEmpty() && dialogue.trim().isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = DesiGreen),
            modifier = Modifier.fillMaxWidth().height(36.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Create Custom Key Button 🚀", fontSize = 11.sp, fontWeight = FontWeight.Black, color = DesiCharcoal)
        }
    }
}
