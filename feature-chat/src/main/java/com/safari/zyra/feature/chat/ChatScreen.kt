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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.safari.zyra.core.ui.brand.JellyOrb
import com.safari.zyra.core.ui.brand.ZyraLogo
import com.safari.zyra.core.ui.PerformanceTier
import com.safari.zyra.core.ui.theme.ZyraColors
import coil3.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    sharedText: String? = null,
    sharedImage: Uri? = null,
    onSharedContentConsumed: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lowPerformance = remember { PerformanceTier.current(context).usesSimpleMotion }
    val haptic = LocalHapticFeedback.current
    var showMicRationale by remember { mutableStateOf(false) }
    var requestingLiveVoice by remember { mutableStateOf(false) }
    var showVoiceMode by remember { mutableStateOf(false) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.startVoice()
            showVoiceMode = true
        } else viewModel.voicePermissionDenied()
    }
    val liveMicPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startLiveVoice() else viewModel.voicePermissionDenied()
    }
    val listening = state.voiceState is VoiceState.Listening
    LaunchedEffect(listening) {
        if (listening) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        else if (state.voiceState is VoiceState.Thinking) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    var draft by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(sharedText, sharedImage) {
        if (sharedText != null) draft = sharedText
        if (sharedImage != null) selectedImage = sharedImage
        if (sharedText != null || sharedImage != null) onSharedContentConsumed()
    }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showConversationPanel by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var renameConversationId by remember { mutableStateOf<Long?>(null) }
    var renameConversationTitle by remember { mutableStateOf("") }
    var deleteConversationId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isAtBottom by remember { derivedStateOf { listState.layoutInfo.totalItemsCount == 0 || listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1 } }
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
            TextButton(onClick = { showConversationPanel = true }) { Text("Chats", color = ZyraColors.cyan) }
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
                        Text(stringResource(R.string.chat_greeting), color = ZyraColors.text, fontSize = MaterialTheme.typography.headlineSmall.fontSize, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.chat_subtitle), color = ZyraColors.muted, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 18.dp)) {
                            listOf(stringResource(R.string.chat_suggestion_anything), stringResource(R.string.chat_suggestion_photo), stringResource(R.string.chat_suggestion_day)).forEach { suggestion ->
                                AssistChip(onClick = { draft = suggestion }, label = { Text(suggestion) }, colors = AssistChipDefaults.assistChipColors(labelColor = ZyraColors.cyan))
                            }
                        }
                        if (!state.hasApiKey || state.settings.textModelId.isBlank()) {
                            Text(stringResource(R.string.chat_setup_hint), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.labelSmall.fontSize, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
            state.messages.forEachIndexed { index, message ->
                val dayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(message.timestamp))
                val previousDay = state.messages.getOrNull(index - 1)?.timestamp?.let { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it)) }
                if (index == 0 || previousDay != dayKey) {
                    item(key = "day_$dayKey") {
                        Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(message.timestamp)), color = ZyraColors.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                }
                item(key = "message_${message.id}") {
                    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }), modifier = Modifier.animateItem()) {
                        MessageBubble(message.role == "user", message.text, message.imagePath, timestamp = message.timestamp, onDelete = { viewModel.deleteMessage(message.id) }, onRegenerate = if (message.role == "assistant") viewModel::regenerateLastReply else null)
                    }
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

        if (!isAtBottom) {
            TextButton(onClick = { coroutineScope.launch { val last = listState.layoutInfo.totalItemsCount - 1; if (last >= 0) listState.animateScrollToItem(last) } }, modifier = Modifier.align(Alignment.End)) { Text("↓ Latest message", color = ZyraColors.cyan) }
        }

        if (selectedImage != null) {
            Row(
                Modifier.fillMaxWidth().background(ZyraColors.surfaceElevated, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.chat_image_attached), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.labelSmall.fontSize, modifier = Modifier.weight(1f))
                TextButton(onClick = { selectedImage = null }) { Text(stringResource(R.string.chat_remove)) }
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
                    DropdownMenuItem(text = { Text(stringResource(R.string.chat_gallery)) }, onClick = { showAttachMenu = false; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.chat_camera)) }, onClick = { showAttachMenu = false; cameraPreview.launch(null) })
                }
            }
            Box(Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) Text(stringResource(R.string.chat_input_hint), color = ZyraColors.muted, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
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
                reduceEffects = state.reduceEffects || lowPerformance,
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
            TextButton(onClick = {
                if (state.liveVoiceState.status != "Idle") viewModel.stopLiveVoice()
                else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.startLiveVoice()
                else { requestingLiveVoice = true; showMicRationale = true }
            }, enabled = !state.isStreaming) {
                Text(if (state.liveVoiceState.status == "Idle") "LIVE" else "STOP LIVE", color = ZyraColors.cyan)
            }
            Spacer(Modifier.width(4.dp))
            JellyOrb(
                modifier = Modifier.size(42.dp),
                enabled = !state.isStreaming && (draft.isNotBlank() || selectedImage != null),
                contentDescription = "Send message",
                stateDescription = if (draft.isNotBlank() || selectedImage != null) "Ready to send" else "Nothing to send",
                reduceEffects = state.reduceEffects || lowPerformance,
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

        if (state.liveVoiceState.status != "Idle") {
            val liveText = state.liveVoiceState.outputCaption.ifBlank { state.liveVoiceState.inputCaption }.ifBlank { state.liveVoiceState.error.orEmpty() }
            VoiceStatusBubble("Live · ${state.liveVoiceState.status}", liveText.ifBlank { "Connected to Gemini Live audio." })
        }
        ContactDock()
        Spacer(Modifier.height(5.dp))
    }

    if (showVoiceMode) {
        Dialog(onDismissRequest = { viewModel.stopVoice(); showVoiceMode = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            VoiceModeScreen(state.voiceState, reduceEffects = state.reduceEffects || lowPerformance, onStop = { viewModel.stopVoice(); showVoiceMode = false }, onDismiss = { viewModel.stopVoice(); showVoiceMode = false })
        }
    }


    if (showConversationPanel) {
        AlertDialog(
            onDismissRequest = { showConversationPanel = false; searchQuery = ""; viewModel.clearSearch() },
            title = { Text("Conversations") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; viewModel.searchMessages(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search all messages") },
                        singleLine = true
                    )
                    if (searchQuery.isNotBlank()) {
                        state.searchResults.forEach { result ->
                            TextButton(onClick = {
                                viewModel.openConversation(result.conversation_id)
                                showConversationPanel = false
                                searchQuery = ""
                                viewModel.clearSearch()
                            }) {
                                Text(result.text.take(100).ifBlank { "Image message" }, color = ZyraColors.text)
                            }
                        }
                    } else {
                        state.conversations.forEach { conversation ->
                            Column(Modifier.fillMaxWidth().background(ZyraColors.glass, RoundedCornerShape(12.dp)).padding(8.dp)) {
                                Text(conversation.title, color = ZyraColors.text, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { viewModel.openConversation(conversation.id); showConversationPanel = false }) { Text("Open") }
                                    TextButton(onClick = {
                                        renameConversationId = conversation.id
                                        renameConversationTitle = conversation.title
                                    }) { Text("Rename") }
                                    TextButton(onClick = { deleteConversationId = conversation.id }) { Text("Delete", color = ZyraColors.error) }
                                }
                            }
                        }
                    }
                    Button(onClick = { viewModel.newConversation(); showConversationPanel = false }) { Text("New chat") }
                }
            },
            confirmButton = { TextButton(onClick = { showConversationPanel = false; searchQuery = ""; viewModel.clearSearch() }) { Text("Close") } },
            containerColor = ZyraColors.surface,
            titleContentColor = ZyraColors.text,
            textContentColor = ZyraColors.muted
        )
    }

    renameConversationId?.let { id ->
        AlertDialog(
            onDismissRequest = { renameConversationId = null },
            title = { Text("Rename conversation") },
            text = { OutlinedTextField(renameConversationTitle, { renameConversationTitle = it }, label = { Text("Conversation name") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { viewModel.renameConversation(id, renameConversationTitle); renameConversationId = null }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameConversationId = null }) { Text("Cancel") } }
        )
    }

    deleteConversationId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConversationId = null },
            title = { Text("Delete conversation?") },
            text = { Text("All messages in this conversation will be deleted from this device.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteConversation(id); deleteConversationId = null }) { Text("Delete", color = ZyraColors.error) } },
            dismissButton = { TextButton(onClick = { deleteConversationId = null }) { Text("Cancel") } }
        )
    }

    if (showMicRationale) {
        AlertDialog(
            onDismissRequest = { showMicRationale = false; requestingLiveVoice = false },
            title = { Text("Allow microphone access?") },
            text = { Text("Fast voice uses Android's speech service. Live voice streams microphone audio to Gemini only while Live is active. Zyra does not save raw microphone recordings. Stop Live to end the stream; you can revoke microphone access anytime in Android Settings.") },
            confirmButton = {
                Button(onClick = {
                    showMicRationale = false
                    if (requestingLiveVoice) liveMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                    else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    requestingLiveVoice = false
                }) { Text("Continue") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showMicRationale = false; requestingLiveVoice = false }) { Text("Not now") }
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
        color = ZyraColors.glass,
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
    var showImagePreview by remember { mutableStateOf(false) }
    var imageScale by remember { mutableStateOf(1f) }
    val imageTransform = rememberTransformableState { zoom, _, _ -> imageScale = (imageScale * zoom).coerceIn(1f, 5f) }
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
                if (imagePath != null) AsyncImage(model = File(imagePath), contentDescription = "Open attached image", modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(12.dp)).clickable { showImagePreview = true }.padding(bottom = 7.dp))
                SelectionContainer { ZyraMarkdown(text) }
                if (streaming) Text("●", color = ZyraColors.cyan, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
        }
        if (!streaming) {
            Row(Modifier.padding(start = 8.dp, top = 3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                timestamp?.let { Text(SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(it)), color = ZyraColors.muted, style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = { val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Zyra message", text)) }) { Text(stringResource(R.string.chat_copy)) }
                TextButton(onClick = { val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text); context.startActivity(Intent.createChooser(send, "Share message")) }) { Text(stringResource(R.string.chat_share)) }
                onRegenerate?.let { TextButton(onClick = it) { Text(stringResource(R.string.chat_regenerate)) } }
                onDelete?.let { TextButton(onClick = it) { Text(stringResource(R.string.chat_delete), color = ZyraColors.error) } }
            }
        }
    }
    if (showImagePreview && imagePath != null) {
        Dialog(onDismissRequest = { showImagePreview = false; imageScale = 1f }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(ZyraColors.background), contentAlignment = Alignment.Center) {
                AsyncImage(model = File(imagePath), contentDescription = "Zoomable attached image", modifier = Modifier.fillMaxSize().transformable(imageTransform).graphicsLayer { scaleX = imageScale; scaleY = imageScale })
                TextButton(onClick = { showImagePreview = false; imageScale = 1f }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Text("Close", color = ZyraColors.text) }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        Modifier.background(ZyraColors.glass, RoundedCornerShape(18.dp)).padding(horizontal = 17.dp, vertical = 15.dp),
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
                    contentDescription = item.second,
                    stateDescription = "Open contact link",
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
                        modifier = Modifier.size(43.dp).border(1.dp, ZyraColors.cyan.copy(alpha = .53f), CircleShape),
                        shape = CircleShape,
                        color = ZyraColors.glass
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
