package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSettingsDialog(
    viewModel: KeyboardViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val audioPlaybackEnabled by viewModel.audioPlaybackEnabled.collectAsStateWithLifecycle()
    val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()

    val vibrationStrength by viewModel.vibrationStrengthMultiplier.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()

    // Helper to check if keyboard is enabled in system settings
    fun isKeyboardEnabled(): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledList = imm?.enabledInputMethodList ?: emptyList()
        return enabledList.any { it.packageName == context.packageName }
    }

    var isSystemEnabled by remember { mutableStateOf(isKeyboardEnabled()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .border(2.dp, DesiPurple, RoundedCornerShape(20.dp))
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️ DATASTORE KEYBOARD SETTINGS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = DesiWhite)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 1: System Input Method Placement (Make as system keyboard)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isSystemEnabled) DesiGreen else DesiYellow, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⌨️ SET AS ORIGINAL PHONE KEYBOARD", fontSize = 12.sp, fontWeight = FontWeight.Black, color = DesiYellow)
                        }
                        Text(
                            "Enable Desi Meme Keyboard to replace your default phone keyboard in WhatsApp, Instagram, Notes, and all apps!",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                    context.startActivity(intent)
                                    isSystemEnabled = isKeyboardEnabled()
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DesiOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("1. Enable in Settings ⚙️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.showInputMethodPicker()
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DesiPurple),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("2. Select Keyboard 📲", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("DATASTORE PREFERENCES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(6.dp))

                // SECTION 2: Vibration Feedback Toggle (DataStore)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📳 Vibration Feedback", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { viewModel.toggleVibration(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DesiOrange
                                )
                            )
                        }
                        if (vibrationEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Haptic Intensity: ${(vibrationStrength * 100).toInt()}%", fontSize = 9.sp, color = Color.Gray)
                                TextButton(
                                    onClick = { viewModel.onEmojiKeyPress(com.example.data.MemeDialogue(emoji = "⚡", dialogue = "Testing Vibration", category = "All", mood = "FUNNY")) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Test Haptic 📳", fontSize = 10.sp, color = DesiYellow)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SECTION 3: Audio Dialogue Playback Toggle (DataStore)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔊 Audio Dialogue Playback", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Switch(
                                checked = audioPlaybackEnabled,
                                onCheckedChange = { viewModel.toggleAudioPlayback(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DesiPurple
                                )
                            )
                        }
                        if (audioPlaybackEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TTS Voice Engine Active", fontSize = 9.sp, color = Color.Gray)
                                TextButton(
                                    onClick = { viewModel.playTtsForText("Aaye haaye, kya baat hai!") },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Test Voice 🔊", fontSize = 10.sp, color = DesiYellow)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SECTION 4: Dark Theme Toggle (DataStore)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌙 Dark Theme Canvas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Switch(
                            checked = darkThemeEnabled,
                            onCheckedChange = { viewModel.toggleDarkTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = DesiGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DesiCharcoal),
                    border = BorderStroke(1.dp, Color.Gray),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done & Save Preferences 💾", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
