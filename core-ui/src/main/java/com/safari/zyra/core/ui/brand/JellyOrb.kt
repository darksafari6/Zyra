package com.safari.zyra.core.ui.brand

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun JellyOrb(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "Voice assistant",
    stateDescription: String = "Ready",
    content: @Composable BoxScope.() -> Unit
) {
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        while (enabled) { idle = !idle; delay(1800) }
        idle = false
    }
    val targetX = if (idle) 1.025f else 1f
    val targetY = if (idle) .98f else 1f
    val sx by animateFloatAsState(targetX, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "orbScaleX")
    val sy by animateFloatAsState(targetY, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "orbScaleY")
    val interaction = if (onClick != null) Modifier.clickable(
        enabled = enabled,
        role = Role.Button,
        onClick = onClick,
        onClickLabel = "Activate voice assistant"
    ) else Modifier
    Box(
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer { scaleX = sx; scaleY = sy; translationY = if (idle) -1.5f else 0f }
            .then(interaction)
            .semantics { this.contentDescription = contentDescription; this.stateDescription = stateDescription },
        contentAlignment = Alignment.Center,
        content = content
    )
}
