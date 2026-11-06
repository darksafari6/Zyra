package com.safari.zyra.feature.chat

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Material
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.safari.zyra.core.ui.brand.JellyOrb
import com.safari.zyra.core.ui.brand.ZyraLogo

@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    val listState = rememberLazyListState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        selectedImage = it
    }
    LaunchedEffect(state.messages.size, state.streamingText, state.isStreaming) {
        val extra = if (state.isStreaming && state.streamingText.isNotEmpty()) 1 else 0
        val target = state.messages.size + extra - 1
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF05060A)).padding(horizontal = 14.dp),
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
                Text("ZYRA", color = Color(0xFFEAF0FF), fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("by Safari", color = Color(0xFF8C9AB8), fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Surface(
                color = if (state.hasApiKey) Color(0x2220D8A0) else Color(0x22FFB74D),
                shape = CircleShape,
                modifier = Modifier.border(1.dp, if (state.hasApiKey) Color(0x6648E0B0) else Color(0x66FFB74D), CircleShape)
            ) {
                Text(
                    if (state.hasApiKey) "READY" else "SETUP KEY",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    color = if (state.hasApiKey) Color(0xFF72E7BE) else Color(0xFFFFC56D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(painterResource(R.drawable.ic_zyra_settings), contentDescription = "Settings", tint = Color(0xFFEAF0FF))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.messages.isEmpty() && !state.isStreaming) {
                item(key = "welcome") {
                    Column(Modifier.fillMaxWidth().padding(top = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        ZyraLogo(progress = 1f, modifier = Modifier.size(132.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Hey, I'm Zyra.", color = Color(0xFFEAF0FF), fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ask me anything. Let's make it happen.", color = Color(0xFF9BA7C2), fontSize = 13.sp)
                        if (!state.hasApiKey || state.settings.textModelId.isBlank()) {
                            Text("Add your Gemini key and model ID in Settings to start.", color = Color(0xFF00E5FF), fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
            items(state.messages, key = { "message_${it.id}" }) { message ->
                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })) {
                    MessageBubble(message.role == "user", message.text, message.imagePath)
                }
            }
            if (state.isStreaming) {
                item(key = "streaming") {
                    if (state.streamingText.isBlank()) TypingIndicator()
                    else MessageBubble(false, state.streamingText, null, streaming = true)
                }
            }
            state.error?.let { message ->
                item(key = "error") {
                    Row(
                        Modifier.fillMaxWidth().background(Color(0x22FF5252), RoundedCornerShape(14.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, color = Color(0xFFFFA0A0), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissError) { Text("Dismiss", color = Color(0xFFFFA0A0)) }
                    }
                }
            }
        }

        if (selectedImage != null) {
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF10182B), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Image attached", color = Color(0xFF00E5FF), fontSize = 12.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { selectedImage = null }) { Text("Remove") }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color(0xCC0E1220), RoundedCornerShape(28.dp))
                .border(1.dp, Color(0x553C5B83), RoundedCornerShape(28.dp))
                .padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(painterResource(R.drawable.ic_zyra_attach), contentDescription = "Attach image")
            }
            Box(Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                if (draft.isEmpty()) Text("Message Zyra...", color = Color(0xFF7E8BA6), fontSize = 14.sp)
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isStreaming,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFEAF0FF), fontSize = 14.sp),
                    maxLines = 5
                )
            }
            JellyOrb(modifier = Modifier.size(39.dp), enabled = false) {
                Icon(painterResource(R.drawable.ic_zyra_mic), contentDescription = "Voice input coming later", modifier = Modifier.size(20.dp).alpha(.35f))
            }
            Spacer(Modifier.width(4.dp))
            JellyOrb(
                modifier = Modifier.size(42.dp),
                enabled = draft.isNotBlank() || selectedImage != null,
                onClick = {
                    viewModel.sendMessage(draft, selectedImage)
                    draft = ""
                    selectedImage = null
                }
            ) {
                Surface(
                    modifier = Modifier.size(39.dp),
                    shape = CircleShape,
                    color = Color(0xFF00E5FF)
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
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String, imagePath: String?, streaming: Boolean = false) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) Color(0xFF14243A) else Color(0xCC101522),
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 5.dp,
                bottomEnd = if (isUser) 5.dp else 18.dp
            ),
            modifier = Modifier.fillMaxWidth(.88f)
                .border(1.dp, if (isUser) Color(0x5544BDE0) else Color(0x334A5B7D), RoundedCornerShape(18.dp))
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                if (imagePath != null) Text("▧  Image attached", color = Color(0xFF00E5FF), fontSize = 11.sp, modifier = Modifier.padding(bottom = 5.dp))
                Text(
                    markdownText(text),
                    color = Color(0xFFEAF0FF),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
                if (streaming) Text("●", color = Color(0xFF00E5FF), fontSize = 8.sp)
            }
        }
        if (!isUser && !streaming && text.isNotBlank()) {
            Text(
                "COPY",
                color = Color(0xFF7183A3),
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp).clickable {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Zyra message", text))
                }.padding(4.dp)
            )
        }
    }
}

private fun markdownText(source: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("(\\*\\*.+?\\*\\*|`[^`]+`)")
    var cursor = 0
    regex.findAll(source).forEach { match ->
        append(source.substring(cursor, match.range.first))
        val token = match.value
        if (token.startsWith("**")) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(token.removePrefix("**").removeSuffix("**"))
            pop()
        } else {
            pushStyle(SpanStyle(color = Color(0xFF73E8F5), background = Color(0xFF18283A)))
            append(token.trim('`'))
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
        repeat(3) { index ->
            Box(Modifier.size(6.dp).background(Color(0xFF00E5FF).copy(alpha = .45f + index * .2f), CircleShape))
        }
        Text("Zyra is thinking", color = Color(0xFF9BA7C2), fontSize = 11.sp)
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
                Text(item.second, color = Color(0xFF8290AA), fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
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
    if (fallback.resolveActivity(context.packageManager) != null) context.startActivity(fallback)
}
