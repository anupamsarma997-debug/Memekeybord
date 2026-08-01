package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.MemeDialogue
import java.io.File
import java.io.FileOutputStream

// Canvas background presets
enum class MemeCanvasStyle(val title: String, val bgGradient: List<Color>, val cardBorderColor: Color, val accentColor: Color) {
    POP_SUNSET("Pop Sunset", listOf(Color(0xFFFF5722), Color(0xFFE91E63)), Color(0xFFFFC107), Color(0xFFFFE082)),
    NEON_CYBER("Neon Cyber", listOf(Color(0xFF673AB7), Color(0xFF00BCD4)), Color(0xFF18FFFF), Color(0xFF80DEEA)),
    CLASSIC_DARK("Classic Dark", listOf(Color(0xFF18181C), Color(0xFF282830)), Color(0xFFFFC107), Color(0xFFFFD54F)),
    DESI_PINK("Desi Pink", listOf(Color(0xFFEC407A), Color(0xFFAB47BC)), Color(0xFFF8BBD0), Color(0xFFF48FB1)),
    GOLDEN_ROYAL("Golden Royal", listOf(Color(0xFFB78103), Color(0xFFE65100)), Color(0xFFFFD700), Color(0xFFFFE57F))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeCanvasEditorDialog(
    initialMeme: MemeDialogue?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var topText by remember { mutableStateOf("WHEN YOU REALISE...") }
    var mainText by remember { mutableStateOf(initialMeme?.dialogue ?: "Yeh dukh kaahe khatam nahi hota be!") }
    var bottomText by remember { mutableStateOf("@DesiMemeKeyboard") }
    var stickerBadgeText by remember { mutableStateOf("🔥 VIRAL") }
    var selectedEmoji by remember { mutableStateOf(initialMeme?.emoji ?: "😭") }
    var selectedStyle by remember { mutableStateOf(MemeCanvasStyle.POP_SUNSET) }
    var textColor by remember { mutableStateOf(Color.White) }
    var customImageUri by remember { mutableStateOf<Uri?>(null) }

    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSharePreview by remember { mutableStateOf(false) }

    // Launcher for picking custom background image from gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customImageUri = uri
            Toast.makeText(context, "Custom photo added to meme!", Toast.LENGTH_SHORT).show()
        }
    }

    val emojiList = listOf("😭", "😂", "😎", "😱", "🔥", "👑", "❤️", "😡", "🤝", "🚀")
    val stickerBadgeList = listOf("🔥 VIRAL", "👑 DESI KING", "💯 LEGEND", "😎 SWAG", "🚨 TRENDING", "⚡ EPIC")
    val textColorList = listOf(
        Color.White to "White",
        Color(0xFFFFEB3B) to "Yellow",
        Color(0xFF00E676) to "Green",
        Color(0xFF18FFFF) to "Cyan",
        Color(0xFFFF4081) to "Pink"
    )

    // Render Canvas Bitmap whenever text, images or styles change
    LaunchedEffect(topText, mainText, bottomText, stickerBadgeText, selectedEmoji, selectedStyle, textColor, customImageUri) {
        renderedBitmap = renderMemeCanvasBitmap(
            context = context,
            topText = topText,
            mainText = mainText,
            bottomText = bottomText,
            stickerBadgeText = stickerBadgeText,
            emoji = selectedEmoji,
            style = selectedStyle,
            textColor = textColor,
            customImageUri = customImageUri
        )
    }

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
                .border(2.dp, DesiOrange, RoundedCornerShape(20.dp))
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎨 MEME CANVAS & STICKER STUDIO", fontSize = 13.sp, fontWeight = FontWeight.Black, color = DesiYellow)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Canvas Realtime Preview Box
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .border(3.dp, selectedStyle.cardBorderColor, RoundedCornerShape(16.dp))
                ) {
                    renderedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Rendered Meme Canvas",
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DesiOrange)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Image / Gallery Photo Picker Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DesiPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Image", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (customImageUri != null) "Change Image 🖼️" else "Add Custom Photo 🖼️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (customImageUri != null) {
                        OutlinedButton(
                            onClick = { customImageUri = null },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.Red)
                        ) {
                            Text("Remove ❌", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Overlay Text Inputs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("TEXT OVERLAYS & STICKER TEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                    OutlinedTextField(
                        value = topText,
                        onValueChange = { topText = it.take(40) },
                        label = { Text("Top Header Text", fontSize = 10.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesiOrange,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = Color(0xFF1C1C20),
                            unfocusedContainerColor = Color(0xFF1C1C20)
                        )
                    )

                    OutlinedTextField(
                        value = mainText,
                        onValueChange = { mainText = it },
                        label = { Text("Main Dialogue Punchline", fontSize = 10.sp) },
                        maxLines = 3,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth().height(68.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesiOrange,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = Color(0xFF1C1C20),
                            unfocusedContainerColor = Color(0xFF1C1C20)
                        )
                    )

                    OutlinedTextField(
                        value = bottomText,
                        onValueChange = { bottomText = it.take(30) },
                        label = { Text("Watermark Subtext", fontSize = 10.sp) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesiOrange,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedContainerColor = Color(0xFF1C1C20),
                            unfocusedContainerColor = Color(0xFF1C1C20)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Color Picker
                Text("TEXT COLOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    textColorList.forEach { (colorItem, name) ->
                        val isSelected = textColor == colorItem
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colorItem)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) DesiOrange else Color.DarkGray, CircleShape)
                                .clickable { textColor = colorItem }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sticker Badge Selector
                Text("STICKER BADGE OVERLAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stickerBadgeList.forEach { badge ->
                        val isSel = stickerBadgeText == badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) DesiYellow else Color(0xFF222226))
                                .clickable { stickerBadgeText = badge }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) DesiCharcoal else Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Style Preset Selector
                Text("BACKGROUND CANVAS STYLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MemeCanvasStyle.values().forEach { style ->
                        val isSel = selectedStyle == style
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(style.bgGradient))
                                .border(if (isSel) 2.dp else 0.dp, if (isSel) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedStyle = style }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(style.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Emoji Badge Selector
                Text("EMOJI STICKER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojiList.forEach { em ->
                        val isSel = selectedEmoji == em
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSel) DesiOrange else Color(0xFF222226))
                                .clickable { selectedEmoji = em },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(em, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Share Preview Dialog Launcher
                Button(
                    onClick = {
                        if (renderedBitmap != null) {
                            showSharePreview = true
                        } else {
                            Toast.makeText(context, "Rendering canvas, please wait...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DesiOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Preview & Export 📲", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    // Share Preview Dialog
    if (showSharePreview && renderedBitmap != null) {
        MemeSharePreviewDialog(
            bitmap = renderedBitmap!!,
            mainText = mainText,
            onDismiss = { showSharePreview = false }
        )
    }
}

// Low-level Canvas Bitmap Renderer
fun renderMemeCanvasBitmap(
    context: Context,
    topText: String,
    mainText: String,
    bottomText: String,
    stickerBadgeText: String,
    emoji: String,
    style: MemeCanvasStyle,
    textColor: Color,
    customImageUri: Uri?
): Bitmap {
    val size = 800
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // 1. Draw Background Gradient or Custom Selected Photo
    var customBitmapLoaded = false
    if (customImageUri != null) {
        try {
            val inputStream = context.contentResolver.openInputStream(customImageUri)
            val customBmp = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (customBmp != null) {
                // Draw custom bitmap scaled & cropped to 800x800
                val srcRect = Rect(0, 0, customBmp.width, customBmp.height)
                val destRect = Rect(0, 0, size, size)
                canvas.drawBitmap(customBmp, srcRect, destRect, null)
                customBitmapLoaded = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (!customBitmapLoaded) {
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = android.graphics.LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                style.bgGradient[0].toArgbInt(), style.bgGradient[1].toArgbInt(),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
    }

    // 2. Draw Decorative Border Frame
    val framePaint = Paint().apply {
        isAntiAlias = true
        color = style.cardBorderColor.toArgbInt()
        this.style = Paint.Style.STROKE
        strokeWidth = 18f
    }
    canvas.drawRoundRect(15f, 15f, size - 15f, size - 15f, 24f, 24f, framePaint)

    // 3. Draw Inner Translucent Card Frame if custom image is loaded (to keep meme text readable)
    if (customBitmapLoaded) {
        val overlayPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            alpha = 150
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), overlayPaint)
    } else {
        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#121215")
            alpha = 200
        }
        canvas.drawRoundRect(45f, 45f, size - 45f, size - 45f, 20f, 20f, cardPaint)
    }

    // 4. Draw Emoji Badge Circle (Top Left / Center)
    val emojiBgPaint = Paint().apply {
        isAntiAlias = true
        color = style.cardBorderColor.toArgbInt()
    }
    canvas.drawCircle(size / 2f, 120f, 45f, emojiBgPaint)

    val emojiTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = 55f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(emoji, size / 2f, 138f, emojiTextPaint)

    // Draw Sticker Badge Tag (e.g. 🔥 VIRAL or 👑 DESI KING)
    if (stickerBadgeText.isNotBlank()) {
        val stickerPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FFD54F")
        }
        canvas.drawRoundRect(size - 220f, 50f, size - 50f, 100f, 12f, 12f, stickerPaint)

        val stickerTextPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(stickerBadgeText, size - 135f, 82f, stickerTextPaint)
    }

    // 5. Top Header Text
    if (topText.isNotBlank()) {
        val topPaint = Paint().apply {
            isAntiAlias = true
            color = style.accentColor.toArgbInt()
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(topText.uppercase(), size / 2f, 210f, topPaint)
    }

    // 6. Main Dialogue Text with Thick Black Outline (Classic Impact Meme Style)
    if (mainText.isNotBlank()) {
        val strokePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            this.style = Paint.Style.STROKE
            strokeWidth = 10f
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = textColor.toArgbInt()
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        // Multi-line word wrap logic
        val words = mainText.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        val maxLineWidth = size - 140f

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) <= maxLineWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        var startY = 370f - ((lines.size - 1) * 25f)
        for (line in lines) {
            canvas.drawText(line, size / 2f, startY, strokePaint)
            canvas.drawText(line, size / 2f, startY, textPaint)
            startY += 58f
        }
    }

    // 7. Bottom Watermark Subtext
    if (bottomText.isNotBlank()) {
        val bottomPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.LTGRAY
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(bottomText, size / 2f, size - 80f, bottomPaint)
    }

    // 8. Brand Footer Tag
    val footerPaint = Paint().apply {
        isAntiAlias = true
        color = style.cardBorderColor.toArgbInt()
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("🔥 DESI MEME KEYBOARD STUDIO", size / 2f, size - 45f, footerPaint)

    return bitmap
}

// Extension helper to convert Compose Color to ARGB Int
fun Color.toArgbInt(): Int {
    return android.graphics.Color.argb(
        (this.alpha * 255).toInt(),
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt()
    )
}

// ==========================================
// SHARE PREVIEW DIALOG WITH APP MOCKUPS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemeSharePreviewDialog(
    bitmap: Bitmap,
    mainText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activePreviewTab by remember { mutableStateOf("WHATSAPP") } // WHATSAPP, FACEBOOK, INSTAGRAM

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .wrapContentHeight()
                .border(2.dp, DesiPurple, RoundedCornerShape(20.dp))
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📲 LIVE APP SHARE PREVIEW", fontSize = 14.sp, fontWeight = FontWeight.Black, color = DesiWhite)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // App Mockup Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1E22))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "WHATSAPP" to "WhatsApp 💬",
                        "FACEBOOK" to "Facebook 📘",
                        "INSTAGRAM" to "Insta Story 📸"
                    ).forEach { (tabKey, label) ->
                        val isSelected = activePreviewTab == tabKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DesiPurple else Color.Transparent)
                                .clickable { activePreviewTab = tabKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // App Specific Realistic Mockup Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (activePreviewTab) {
                                "WHATSAPP" -> Color(0xFF0B141A) // WhatsApp Dark Chat Background
                                "FACEBOOK" -> Color(0xFF18191A) // FB Dark Post Background
                                else -> Color(0xFF121212)       // Instagram Story Background
                            }
                        )
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (activePreviewTab) {
                        "WHATSAPP" -> WhatsAppChatMockup(bitmap = bitmap, mainText = mainText)
                        "FACEBOOK" -> FacebookPostMockup(bitmap = bitmap, mainText = mainText)
                        "INSTAGRAM" -> InstagramStoryMockup(bitmap = bitmap)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, Color.Gray)
                    ) {
                        Text("Edit More ✏️", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            shareGeneratedMemeImage(context, bitmap, mainText)
                        },
                        modifier = Modifier.weight(1.4f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DesiGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm & Share 🚀", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// WHATSAPP MOCKUP
// ------------------------------------------
@Composable
fun WhatsAppChatMockup(bitmap: Bitmap, mainText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        // WhatsApp Chat Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1F2C34))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00A884)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔥", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Desi Meme Group 🇮🇳", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("online • tap for info", fontSize = 8.sp, color = Color(0xFF8696A0))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sent Chat Bubble with Meme Image
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .width(220.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 2.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(Color(0xFF005C4B))
                .padding(4.dp)
        ) {
            Column {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Meme Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Desi Meme Sticker", fontSize = 9.sp, color = Color(0xFFE9EDEF))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("10:42 AM", fontSize = 8.sp, color = Color(0xFF8696A0))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("✓✓", fontSize = 9.sp, color = Color(0xFF53BDEB), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// FACEBOOK POST MOCKUP
// ------------------------------------------
@Composable
fun FacebookPostMockup(bitmap: Bitmap, mainText: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF242526))
            .padding(10.dp)
    ) {
        // FB Post Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(DesiOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("🇮🇳", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Desi Meme Keyboard", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Just now • 🌐", fontSize = 9.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(mainText, fontSize = 10.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(6.dp))

        // Image Container
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Meme Preview",
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Like / Comment / Share Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("👍 Like", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("💬 Comment", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("↗️ Share", fontSize = 10.sp, color = Color(0xFF4267B2), fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------
// INSTAGRAM STORY MOCKUP
// ------------------------------------------
@Composable
fun InstagramStoryMockup(bitmap: Bitmap) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Full Image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Instagram Story Preview",
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        // Story Top Header Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFE1306C), CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("😎", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Your Story", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("1m", fontSize = 9.sp, color = Color.LightGray)
        }

        // Story Bottom Message Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Send message...", fontSize = 10.sp, color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("❤️", fontSize = 12.sp)
                Text("🔥", fontSize = 12.sp)
                Text("✈️", fontSize = 12.sp)
            }
        }
    }
}

// Share Image File via Android FileProvider Intent
fun shareGeneratedMemeImage(context: Context, bitmap: Bitmap, caption: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "desi_meme_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "$caption\n\nMade with Desi Meme Keyboard 🚀")
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Desi Meme Image"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not share image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
