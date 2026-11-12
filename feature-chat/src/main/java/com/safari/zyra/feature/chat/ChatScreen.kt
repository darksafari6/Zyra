package com.safari.zyra.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.ContextCompat
import com.safari.zyra.feature.voice.VoiceState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.safari.zyra.core.ui.brand.JellyOrb
import com.safari.zyra.core.ui.brand.ZyraLogo
import com.safari.zyra.core.ui.theme.ZyraColors
import coil3.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showMicRationale by remember { mutableStateOf(false) }
    var showVoiceMode by remember { mutableStateOf(false) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.startVoice()
            showVoiceMode = true
        } else viewModel.voicePermissionDenied()
    }
    val listening = state.voiceState is VoiceState.Listening
    LaunchedEffect(listening) {
        if (listening) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        else if (state.voiceState is VoiceState.Thinking) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    var draft by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var showAttachMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { selectedImage = it }
    val cameraPreview = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val photo = File(context.cacheDir, "zyra_camera_${System.currentTimeMillis()}.jpg")
            photo.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
            selectedImage = Uri.fromFile(photo)
        }
    }
    LaunchedEffect(state.messages.size, state.isStreaming, state.streamingText, state.voiceState) {
        kotlinx.coroutines.delay(60)
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().background(ZyraColors.background).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                ZyraLogo(progress = 1f, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("ZYRA", color = ZyraColors.text, fontSize = MaterialTheme.typography.bodyLarge.fontSize, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("by Safari", color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize, letterSpacing = 1.sp)
            }
            Surface(
                color = if (state.hasApiKey && state.settings.textModelId.isNotBlank()) ZyraColors.success.copy(alpha = .12f) else ZyraColors.warning.copy(alpha = .12f),
                shape = CircleShape,
                modifier = Modifier.border(1.dp, if (state.hasApiKey && state.settings.textModelId.isNotBlank()) ZyraColors.success.copy(alpha = .4f) else ZyraColors.warning.copy(alpha = .4f), CircleShape)
            ) {
                Text(
                    when (state.voiceState) {
                        is VoiceState.Listening -> "LISTENING"
                        is VoiceState.Thinking -> "THINKING"
                        is VoiceState.Speaking -> "SPEAKING"
                        is VoiceState.Error -> "VOICE ERROR"
                        VoiceState.Idle -> if (state.hasApiKey && state.settings.textModelId.isNotBlank()) "READY" else "SETUP"
                    },
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    color = if (state.hasApiKey && state.settings.textModelId.isNotBlank()) ZyraColors.success else ZyraColors.warning,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(painterResource(R.drawable.ic_zyra_settings), contentDescription = "Settings", tint = ZyraColors.text)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 760.dp).wrapContentWidth(Alignment.CenterHorizontally),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.messages.isEmpty() && !state.isStreaming) {
                item(key = "welcome") {
                    Column(Modifier.fillMaxWidth().padding(top = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        ZyraLogo(progress = 1f, modifier = Modifier.size(132.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Hey, I'm Zyra.", color = ZyraColors.text, fontSize = MaterialTheme.typography.headlineSmall.fontSize, fontWeight = FontWeight.SemiBold)
                        Text("Ask me anything. Let's make it happen.", color = ZyraColors.muted, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp)) {
                            listOf("Ask anything", "Describe a photo", "Plan my day").forEach { suggestion ->
                                AssistChip(onClick = { draft = suggestion }, label = { Text(suggestion) }, colors = AssistChipDefaults.assistChipColors(labelColor = ZyraColors.cyan))
                            }
                        }
                        if (!state.hasApiKey || state.settings.textModelId.isBlank()) {
                            Text("Add your Gemini key and model ID in Settings to start.", color = ZyraColors.cyan, fontSize = MaterialTheme.typography.labelSmall.fontSize, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
            items(state.messages, key = { "message_${it.id}" }) { message ->
                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }), modifier = Modifier.animateItem()) {
                    MessageBubble(message.role == "user", message.text, message.imagePath, timestamp = message.timestamp, onDelete = { viewModel.deleteMessage(message.id) }, onRegenerate = if (message.role == "assistant") viewModel::regenerateLastReply else null)
                }
            }
            if (state.isStreaming) {
                item(key = "streaming") {
                    if (state.streamingText.isBlank()) TypingIndicator()
                    else MessageBubble(false, state.streamingText, null, streaming = true)
                }
            }
            when (val voice = state.voiceState) {
                is VoiceState.Listening -> item(key = "voice-live-transcript") {
                    VoiceStatusBubble("Listening", voice.transcript.ifBlank { "I'm listening…" })
                }
                is VoiceState.Thinking -> item(key = "voice-thinking") {
                    VoiceStatusBubble("Thinking", "Preparing a reply…")
                }
                is VoiceState.Speaking -> item(key = "voice-speaking") {
                    if (voice.response.isNotBlank()) MessageBubble(false, voice.response, null, streaming = true) else VoiceStatusBubble("Speaking", "Preparing a reply…")
                }
                is VoiceState.Error -> item(key = "voice-error") {
                    VoiceStatusBubble("Voice", voice.message)
                }
                VoiceState.Idle -> Unit
            }
            state.error?.let { message ->
                item(key = "error") {
                    Row(
                        Modifier.fillMaxWidth().background(ZyraColors.error.copy(alpha = .12f), RoundedCornerShape(14.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, color = ZyraColors.error, fontSize = MaterialTheme.typography.labelSmall.fontSize, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissError) { Text("Dismiss", color = ZyraColors.error) }
                    }
                }
            }
        }

        if (selectedImage != null) {
            Row(
                Modifier.fillMaxWidth().background(ZyraColors.surfaceElevated, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Image attached", color = ZyraColors.cyan, fontSize = MaterialTheme.typography.labelSmall.fontSize, modifier = Modifier.weight(1f))
                TextButton(onClick = { selectedImage = null }) { Text("Remove") }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(ZyraColors.glass, RoundedCornerShape(28.dp))
                .border(1.dp, ZyraColors.border, RoundedCornerShape(28.dp))
                .padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { showAttachMenu = true }) { Icon(painterResource(R.drawable.ic_zyra_attach), contentDescription = "Attach image or take photo") }
                DropdownMenu(expanded = showAttachMenu, onDismissRequest = { showAttachMenu = false }) {
                    DropdownMenuItem(text = { Text("Choose from gallery") }, onClick = { showAttachMenu = false; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    DropdownMenuItem(text = { Text("Take a photo") }, onClick = { showAttachMenu = false; cameraPreview.launch(null) })
                }
            }
            Box(Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) Text("Message Zyra...", color = ZyraColors.muted, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isStreaming,
                    textStyle = androidx.compose.ui.text.TextStyle(color = ZyraColors.text, fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, keyboardType = KeyboardType.Text),
                    keyboardActions = KeyboardActions(onSend = { if (!state.isStreaming && (draft.isNotBlank() || selectedImage != null)) { viewModel.sendMessage(draft, selectedImage); draft = ""; selectedImage = null } })
                )
            }
            val amplitude = (state.voiceState as? VoiceState.Listening)?.amplitude ?: 0f
            JellyOrb(
                modifier = Modifier.size(42.dp).graphicsLayer {
                    scaleX = 1f + amplitude * 0.18f
                    scaleY = 1f - amplitude * 0.10f
                },
                enabled = !state.isStreaming,
                contentDescription = "Voice input",
                reduceEffects = state.reduceEffects,
                stateDescription = when (state.voiceState) {
                    is VoiceState.Listening -> "Listening"
                    is VoiceState.Thinking -> "Thinking"
                    is VoiceState.Speaking -> "Speaking"
                    is VoiceState.Error -> "Voice error"
                    VoiceState.Idle -> "Ready"
                },
                onClick = {
                    when (state.voiceState) {
                        is VoiceState.Speaking -> viewModel.stopSpeaking()
                        is VoiceState.Listening, is VoiceState.Thinking -> viewModel.stopVoice()
                        else -> {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.startVoice()
                                showVoiceMode = true
                            } else showMicRationale = true
                        }
                    }
                }
            ) {
                Surface(
                    modifier = Modifier.size(39.dp).border(
                        1.dp,
                        if (listening) ZyraColors.cyan else ZyraColors.cyan.copy(alpha = .4f),
                        CircleShape
                    ),
                    shape = CircleShape,
                    color = if (listening) ZyraColors.surfaceElevated else ZyraColors.surfaceElevated
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(R.drawable.ic_zyra_mic),
                            contentDescription = if (listening) "Stop listening" else "Start voice chat",
                            modifier = Modifier.size(20.dp),
                            tint = if (listening) ZyraColors.cyan else ZyraColors.text
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            JellyOrb(
                modifier = Modifier.size(42.dp),
                enabled = !state.isStreaming && (draft.isNotBlank() || selectedImage != null),
                onClick = {
                    viewModel.sendMessage(draft, selectedImage)
                    draft = ""
                    selectedImage = null
                }
            ) {
                Surface(
                    modifier = Modifier.size(39.dp),
                    shape = CircleShape,
                    color = if (!state.isStreaming && (draft.isNotBlank() || selectedImage != null)) ZyraColors.cyan else ZyraColors.border
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_zyra_send), contentDescription = "Send", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        ContactDock()
        Spacer(Modifier.height(5.dp))
    }

    if (showVoiceMode) {
        Dialog(onDismissRequest = { viewModel.stopVoice(); showVoiceMode = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            VoiceModeScreen(state.voiceState, onStop = { viewModel.stopVoice(); showVoiceMode = false }, onDismiss = { viewModel.stopVoice(); showVoiceMode = false })
        }
    }

    if (showMicRationale) {
        AlertDialog(
            onDismissRequest = { showMicRationale = false },
            title = { Text("Allow microphone access?") },
            text = { Text("ZYRA uses your microphone only when you tap the voice orb. The Android speech service may process speech on-device or online, depending on your device and language pack. ZYRA does not save raw microphone recordings. You can revoke access anytime in Android Settings.") },
            confirmButton = {
                Button(onClick = {
                    showMicRationale = false
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }) { Text("Continue") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showMicRationale = false }) { Text("Not now") }
            },
            containerColor = ZyraColors.surface,
            titleContentColor = ZyraColors.text,
            textContentColor = ZyraColors.muted
        )
    }
}

@Composable
private fun VoiceStatusBubble(title: String, body: String) {
    Surface(
        color = Color(0xCC101522),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(.92f).border(1.dp, ZyraColors.cyan.copy(alpha = .3f), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
            Text(title.uppercase(), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.labelSmall.fontSize, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(body, color = ZyraColors.text, fontSize = MaterialTheme.typography.bodyMedium.fontSize, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String, imagePath: String?, streaming: Boolean = false, timestamp: Long? = null, onDelete: (() -> Unit)? = null, onRegenerate: (() -> Unit)? = null) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) ZyraColors.surfaceElevated else ZyraColors.glass,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 5.dp,
                bottomEnd = if (isUser) 5.dp else 18.dp
            ),
            modifier = Modifier.fillMaxWidth(.88f)
                .border(1.dp, if (isUser) ZyraColors.cyan.copy(alpha = .32f) else ZyraColors.border, RoundedCornerShape(18.dp))
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                if (imagePath != null) AsyncImage(model = File(imagePath), contentDescription = "Attached image", modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(12.dp)).clickable { /* image viewer is opened from the image preview action */ }.padding(bottom = 7.dp))
                SelectionContainer { ZyraMarkdown(text) }
                if (streaming) Text("●", color = ZyraColors.cyan, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        }
        if (!streaming) {
            Row(Modifier.padding(start = 8.dp, top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                timestamp?.let { Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)), color = ZyraColors.muted, style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = { val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Zyra message", text)) }) { Text("Copy") }
                TextButton(onClick = { val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text); context.startActivity(Intent.createChooser(send, "Share message")) }) { Text("Share") }
                onRegenerate?.let { TextButton(onClick = it) { Text("Regenerate") } }
                onDelete?.let { TextButton(onClick = it) { Text("Delete", color = ZyraColors.error) } }
            }
        }
    }
}

private fun markdownText(source: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("(\\*\\*.+?\\*\\*|`[^`]+`|\\*[^*\\n]+\\*)")
    var cursor = 0
    regex.findAll(source).forEach { match ->
        append(source.substring(cursor, match.range.first))
        val token = match.value
        if (token.startsWith("**")) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(token.removePrefix("**").removeSuffix("**"))
            pop()
        } else if (token.startsWith("`")) {
            pushStyle(SpanStyle(color = Color(0xFF73E8F5), background = Color(0xFF18283A)))
            append(token.trim('`'))
            pop()
        } else {
            pushStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            append(token.trim('*'))
            pop()
        }
        cursor = match.range.last + 1
    }
    append(source.substring(cursor))
}

@Composable
private fun TypingIndicator() {
    Row(
        Modifier.background(Color(0xCC101522), RoundedCornerShape(18.dp)).padding(horizontal = 17.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val transition = rememberInfiniteTransition(label = "typing-dots")
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = .25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(480, delayMillis = index * 140), RepeatMode.Reverse),
                label = "typing-dot-$index"
            )
            Box(Modifier.size(6.dp).background(ZyraColors.cyan.copy(alpha = alpha), CircleShape))
        }
        Text("Zyra is thinking", color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
    }
}

@Composable
private fun ContactDock() {
    val context = LocalContext.current
    val contacts = listOf(
        Triple(R.drawable.ic_contact_tiktok, "TikTok", "com.zhiliaoapp.musically"),
        Triple(R.drawable.ic_contact_instagram, "Instagram", "com.instagram.android"),
        Triple(R.drawable.ic_contact_whatsapp, "WhatsApp", "com.whatsapp"),
        Triple(R.drawable.ic_contact_email, "Email", "")
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        contacts.forEachIndexed { index, item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JellyOrb(
                    modifier = Modifier.size(46.dp),
                    onClick = {
                        val urls = listOf(
                            "https://www.tiktok.com/@t_safari",
                            "https://www.instagram.com/ig_safari",
                            "https://wa.me/923170702027",
                            "mailto:darksafari6@gmail.com"
                        )
                        openContact(context, urls[index], item.third)
                    }
                ) {
                    Surface(
                        modifier = Modifier.size(43.dp).border(1.dp, Color(0x8845CBE8), CircleShape),
                        shape = CircleShape,
                        color = Color(0xCC111A2C)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(item.first), contentDescription = item.second, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Text(item.second, color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

private fun openContact(context: android.content.Context, rawUrl: String, packageName: String) {
    val uri = Uri.parse(rawUrl)
    if (packageName.isNotBlank()) {
        val nativeIntent = Intent(Intent.ACTION_VIEW, uri).setPackage(packageName)
        if (nativeIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(nativeIntent)
            return
        }
    }
    val fallback = if (rawUrl.startsWith("mailto:")) Intent(Intent.ACTION_SENDTO, uri)
    else Intent(Intent.ACTION_VIEW, uri)
    if (fallback.resolveActivity(context.packageManager) != null) {
        context.startActivity(fallback)
    } else if (rawUrl.startsWith("mailto:")) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/mail/?view=cm&to=darksafari6%40gmail.com")))
    }
}
