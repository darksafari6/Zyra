package com.safari.zyra.feature.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.safari.zyra.core.ui.brand.JellyOrb
import com.safari.zyra.core.ui.theme.ZyraColors
import com.safari.zyra.core.ui.theme.ZyraTheme
import com.safari.zyra.feature.voice.VoiceState

@Composable
fun VoiceModeScreen(state: VoiceState, reduceEffects: Boolean = false, onStop: () -> Unit, onDismiss: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val backdrop = remember { Animatable(0f) }
    val waveform = remember { Animatable(0f) }
    LaunchedEffect(reduceEffects, lifecycleOwner) {
        if (reduceEffects) { backdrop.snapTo(0f); waveform.snapTo(0f) }
        else lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            kotlinx.coroutines.coroutineScope {
            launch { while (true) { backdrop.animateTo(1f, tween(7000, easing = FastOutSlowInEasing)); backdrop.animateTo(0f, tween(7000, easing = FastOutSlowInEasing)) } }
            launch { while (true) { waveform.animateTo(1f, tween(550, easing = FastOutSlowInEasing)); waveform.animateTo(0f, tween(550, easing = FastOutSlowInEasing)) } }
            }
        }
    }
    val offset = backdrop.value
    val wave = waveform.value
    val amplitude = (state as? VoiceState.Listening)?.amplitude?.coerceIn(0f, 1f) ?: 0f
    val status = when(state) {
        is VoiceState.Listening -> "Listening"
        is VoiceState.Thinking -> "Thinking"
        is VoiceState.Speaking -> "Speaking"
        is VoiceState.Error -> "Voice error"
        VoiceState.Idle -> "Ready"
    }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(ZyraColors.background, ZyraColors.surface, ZyraColors.violet.copy(alpha=.18f)), start = androidx.compose.ui.geometry.Offset(0f, offset*1200f), end = androidx.compose.ui.geometry.Offset(900f, 1200f)))
        .pointerInput(Unit) {
            var drag = 0f
            detectVerticalDragGestures(onVerticalDrag = { _, amount -> drag += amount }, onDragEnd = { if (drag > 90f) onDismiss(); drag = 0f }, onDragCancel = { drag = 0f })
        }) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.voice_title), color = ZyraColors.cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.height(12.dp))
                Text(status.uppercase(), color = ZyraColors.text, style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JellyOrb(
                    modifier = Modifier.size(220.dp),
                    enabled = status == "Listening",
                    reduceEffects = reduceEffects,
                    contentDescription = "Voice assistant orb",
                    stateDescription = status
                ) {
                    Box(Modifier.fillMaxSize().background(
                        Brush.radialGradient(listOf(ZyraColors.cyan.copy(alpha=.55f + amplitude*.35f), ZyraColors.violet.copy(alpha=.55f), ZyraColors.surface), radius = 360f),
                        CircleShape
                    ), contentAlignment = Alignment.Center) {
                        when (state) {
                            is VoiceState.Thinking -> CircularProgressIndicator(color = ZyraColors.violet, strokeWidth = 4.dp, modifier = Modifier.size(172.dp))
                            is VoiceState.Speaking -> Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                repeat(7) { i -> Box(Modifier.width(5.dp).height((16 + ((i*13)%28) + (wave * if (i % 2 == 0) 16 else 5)).dp).background(ZyraColors.magenta, CircleShape)) }
                            }
                            else -> Box(Modifier.size(112.dp).background(ZyraColors.cyan.copy(alpha=.18f), CircleShape))
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                val caption = when(state) {
                    is VoiceState.Listening -> state.transcript.ifBlank { "I'm listening…" }
                    is VoiceState.Thinking -> "Preparing a reply…"
                    is VoiceState.Speaking -> state.response
                    is VoiceState.Error -> state.message
                    VoiceState.Idle -> "Swipe down to close"
                }
                Text(caption, color = ZyraColors.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Live voice captions: $caption" })
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onStop, colors = ButtonDefaults.buttonColors(containerColor = ZyraColors.magenta), modifier = Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text(stringResource(R.string.voice_stop), color = ZyraColors.text) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.voice_dismiss), color = ZyraColors.muted) }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A)
@Composable private fun VoiceModePreview() { ZyraTheme { VoiceModeScreen(VoiceState.Idle, reduceEffects = true, onStop = {}, onDismiss = {}) } }
