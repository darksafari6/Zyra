package com.safari.zyra.ui.splash

import com.safari.zyra.core.ui.theme.ZyraColors

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safari.zyra.core.ui.brand.JellyOrb
import com.safari.zyra.core.ui.brand.ZyraLogo
import kotlinx.coroutines.delay

@Composable
fun ZyraSplash(onFinished: () -> Unit) {
    var finished by remember { mutableStateOf(false) }
    val finish = { if (!finished) { finished = true; onFinished() } }
    val reveal = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val letterSpace = remember { Animatable(12f) }
    val tagline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        delay(250)
        nameAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        letterSpace.animateTo(5f, tween(420, easing = FastOutSlowInEasing))
        delay(160)
        tagline.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        delay(650)
        finish()
    }
    Box(Modifier.fillMaxSize().clickable { finish() }) {
        Canvas(Modifier.fillMaxSize()) {
            val dust = listOf(
                .12f to .18f, .82f to .24f, .20f to .72f,
                .78f to .78f, .50f to .12f, .90f to .55f,
                .08f to .48f, .66f to .40f, .34f to .88f
            )
            dust.forEachIndexed { index, point ->
                drawCircle(
                    ZyraColors.cyan.copy(alpha = .10f + (index % 3) * .045f),
                    radius = (1.1f + (index % 2) * .7f).dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * point.first, size.height * point.second)
                )
            }
        }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            JellyOrb(
                modifier = Modifier.size(220.dp).graphicsLayer {
                    val scale = .12f + .88f * reveal.value
                    scaleX = scale
                    scaleY = scale
                },
                enabled = true
            ) {
                ZyraLogo(progress = reveal.value, modifier = Modifier.size(180.dp))
            }
            Text(
                "ZYRA",
                modifier = Modifier.alpha(nameAlpha.value),
                style = TextStyle(
                    color = ZyraColors.text,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = letterSpace.value.sp
                )
            )
            Text(
                "by Safari",
                modifier = Modifier.alpha(tagline.value),
                style = TextStyle(color = ZyraColors.cyan, fontSize = MaterialTheme.typography.bodyMedium.fontSize, letterSpacing = 2.sp)
            )
        }
    }
}
