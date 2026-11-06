package com.safari.zyra.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safari.zyra.core.ui.brand.JellyOrb
import com.safari.zyra.core.ui.brand.ZyraLogo
import kotlinx.coroutines.delay

@Composable
fun ZyraSplash(onFinished: () -> Unit) {
    val reveal = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val letterSpace = remember { Animatable(12f) }
    val tagline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        delay(250)
        nameAlpha.animateTo(1f, tweenSpec())
        letterSpace.animateTo(5f, tweenSpec())
        delay(160)
        tagline.animateTo(1f, tweenSpec())
        delay(650)
        onFinished()
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        JellyOrb(modifier = Modifier.size(220.dp), enabled = true) {
            ZyraLogo(progress = reveal.value, modifier = Modifier.fillMaxSize())
        }
        Text("ZYRA", modifier = Modifier.alpha(nameAlpha.value), style = TextStyle(color = Color(0xFFEAF0FF), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = letterSpace.value.sp))
        Text("by Safari", modifier = Modifier.alpha(tagline.value), style = TextStyle(color = Color(0xFF00E5FF), fontSize = 13.sp, letterSpacing = 2.sp))
    }
}

private fun tweenSpec() = androidx.compose.animation.core.tween<Float>(durationMillis = 420, easing = FastOutSlowInEasing)
