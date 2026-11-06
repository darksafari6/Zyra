package com.safari.zyra.core.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ZyraDarkColors = darkColorScheme(
    primary = ZyraCyan,
    onPrimary = ZyraBackground,
    secondary = ZyraViolet,
    onSecondary = ZyraText,
    tertiary = ZyraMagenta,
    background = ZyraBackground,
    onBackground = ZyraText,
    surface = ZyraSurface,
    onSurface = ZyraText,
    surfaceVariant = ZyraSurface,
    onSurfaceVariant = ZyraMutedText
)

@Composable
fun ZyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZyraDarkColors,
        typography = ZyraTypography,
        shapes = ZyraShapes,
        content = content
    )
}
