package com.safari.zyra.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color

@Composable
fun ZyraTheme(pureBlack: Boolean = false, content: @Composable () -> Unit) {
    SideEffect { ZyraColors.pureBlack = pureBlack }
    val scheme = darkColorScheme(
        primary = ZyraCyan,
        onPrimary = Color(0xFF05060A),
        secondary = ZyraViolet,
        onSecondary = ZyraText,
        tertiary = ZyraMagenta,
        background = if (pureBlack) Color.Black else Color(0xFF05060A),
        onBackground = ZyraText,
        surface = if (pureBlack) Color(0xFF050505) else Color(0xFF0E1220),
        onSurface = ZyraText,
        surfaceVariant = if (pureBlack) Color(0xFF080808) else Color(0xFF0E1220),
        onSurfaceVariant = ZyraMutedText
    )
    MaterialTheme(colorScheme = scheme, typography = ZyraTypography, shapes = ZyraShapes, content = content)
}
