package com.safari.zyra.core.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safari.zyra.core.ui.theme.*

@Composable
fun ZyraScaffold(modifier: Modifier = Modifier, topBar: @Composable () -> Unit = {}, snackbarHost: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = ZyraColors.background, topBar = topBar, snackbarHost = snackbarHost, contentWindowInsets = WindowInsets.safeDrawing, content = content)
}
@Composable
fun GlassSurface(modifier: Modifier = Modifier, shape: RoundedCornerShape = ZyraRadius.card, glow: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val borderBrush = Brush.linearGradient(listOf(ZyraColors.cyan.copy(.55f), ZyraColors.violet.copy(.38f), ZyraColors.magenta.copy(.28f)))
    Surface(modifier = modifier.then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(.5.dp) else Modifier).border(1.dp, borderBrush, shape), shape = shape, color = ZyraColors.glass, shadowElevation = if (glow) 8.dp else 0.dp) {
        Column(Modifier.padding(ZyraSpacing.md), content = content)
    }
}
enum class ZyraButtonStyle { Filled, Tonal, Text }
@Composable
fun ZyraButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: ZyraButtonStyle = ZyraButtonStyle.Filled, enabled: Boolean = true) {
    when (style) {
        ZyraButtonStyle.Filled -> Button(onClick, modifier, enabled = enabled) { Text(text) }
        ZyraButtonStyle.Tonal -> FilledTonalButton(onClick, modifier, enabled = enabled) { Text(text) }
        ZyraButtonStyle.Text -> TextButton(onClick, modifier, enabled = enabled) { Text(text) }
    }
}
@Composable
fun ZyraTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = singleLine)
}
@Composable
fun StatusPill(text: String, color: Color = ZyraColors.cyan, modifier: Modifier = Modifier) {
    Surface(modifier, color.copy(alpha = .12f), shape = ZyraRadius.pill, contentColor = color) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ZyraColors.cyan)
        Spacer(Modifier.height(ZyraSpacing.sm)); content()
    }
}
@Composable
fun EmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(ZyraSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = ZyraColors.text)
        Spacer(Modifier.height(ZyraSpacing.xs)); Text(description, style = MaterialTheme.typography.bodyMedium, color = ZyraColors.muted)
    }
}
@Composable
fun ErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(ZyraRadius.medium).background(ZyraColors.error.copy(.12f)).padding(ZyraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        Text(message, Modifier.weight(1f), color = ZyraColors.error, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onRetry) { Text("Retry", color = ZyraColors.error) }
    }
}
@Composable
fun ZyraSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data -> Snackbar(snackbarData = data, containerColor = ZyraColors.surfaceElevated, contentColor = ZyraColors.text) }
}
@Composable
fun ZyraTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = ZyraSpacing.md, vertical = ZyraSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = ZyraColors.text); actions()
    }
}
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun GlassPreview() { ZyraTheme { GlassSurface { Text("Glass surface", color = ZyraColors.text) } } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun ButtonsPreview() { ZyraTheme { Column { ZyraButton("Filled", {}); ZyraButton("Tonal", {}, style = ZyraButtonStyle.Tonal); ZyraButton("Text", {}, style = ZyraButtonStyle.Text) } } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun TextFieldPreview() { ZyraTheme { ZyraTextField("", {}, "Message") } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun StatusPreview() { ZyraTheme { StatusPill("READY") } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun SectionPreview() { ZyraTheme { SectionCard("Section") { Text("Content") } } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun EmptyPreview() { ZyraTheme { EmptyState("Nothing here", "Start a conversation.") } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun ErrorPreview() { ZyraTheme { ErrorBanner("Something went wrong", {}) } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun SnackbarPreview() { ZyraTheme { ZyraSnackbarHost(SnackbarHostState()) } }
@Preview(showBackground = true, backgroundColor = 0xFF05060A) @Composable private fun TopBarPreview() { ZyraTheme { ZyraTopBar("Settings", {}) } }
