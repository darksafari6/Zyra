package com.safari.zyra.core.ui.brand

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

@Composable
fun JellyOrb(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        while (enabled) {
            idle = !idle
            delay(1800)
        }
    }
    val targetX = when { pressed -> .91f; idle -> 1.025f; else -> 1f }
    val targetY = when { pressed -> 1.08f; idle -> .98f; else -> 1f }
    val sx by animateFloatAsState(targetX, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "orbScaleX")
    val sy by animateFloatAsState(targetY, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "orbScaleY")
    Box(
        modifier = modifier.graphicsLayer { scaleX = sx; scaleY = sy }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onPress = {
                    pressed = true
                    try { tryAwaitRelease() } finally { pressed = false }
                })
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}