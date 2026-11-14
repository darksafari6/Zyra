package com.safari.zyra.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.safari.zyra.core.ui.theme.ZyraColors
import com.safari.zyra.core.ui.theme.ZyraTheme

@Composable
fun ZyraMarkdown(source: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lines = source.split('\n')
    var index = 0
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        while (index < lines.size) {
            val line = lines[index]
            if (line.trimStart().startsWith("|")) {
                val rows = mutableListOf<List<String>>()
                while (index < lines.size && lines[index].trimStart().startsWith("|")) {
                    val row = lines[index].trim().trim('|').split("|").map { it.trim() }
                    if (row.none { it.matches(Regex(":?-{3,}:?")) }) rows += row
                    index++
                }
                Column(Modifier.fillMaxWidth().background(ZyraColors.surfaceElevated, RoundedCornerShape(8.dp)).padding(8.dp)) {
                    rows.forEach { cells -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { cells.forEach { cell -> Text(cell, Modifier.weight(1f), color = ZyraColors.text, style = MaterialTheme.typography.bodySmall) } } }
                }
            } else if (line.trimStart().startsWith("```")) {
                val code = buildString {
                    index++
                    while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                        if (isNotEmpty()) append('\n')
                        append(lines[index]); index++
                    }
                    if (index < lines.size) index++
                }
                Column(Modifier.fillMaxWidth().background(ZyraColors.surfaceElevated, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                        }) { Text("Copy code", color = ZyraColors.cyan) }
                    }
                    Text(code, color = ZyraColors.cyan, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()))
                }
            } else {
                val trimmed = line.trimStart()
                val heading = trimmed.takeWhile { it == '#' }.length
                val content = when {
                    heading in 1..6 -> trimmed.drop(heading).trim()
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> "•  " + trimmed.drop(2)
                    trimmed.matches(Regex("^\\d+[.)]\\s+.*")) -> trimmed
                    else -> line
                }
                val annotated = inlineMarkdown(content)
                ClickableText(
                    text = annotated,
                    style = (when { heading == 1 -> MaterialTheme.typography.titleLarge; heading in 2..3 -> MaterialTheme.typography.titleMedium; else -> MaterialTheme.typography.bodyMedium }).copy(color = ZyraColors.text),
                    onClick = { offset ->
                        annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.item))) } }
                    }
                )
                index++
            }
        }
    }
}

private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|`[^`]+`|\\[[^]]+\\]\\(https?://[^)]+\\))")
    var cursor = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") -> { pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(token.removePrefix("**").removeSuffix("**")); pop() }
            token.startsWith("*") -> { pushStyle(SpanStyle(fontWeight = FontWeight.Medium)); append(token.trim('*')); pop() }
            token.startsWith("`") -> { pushStyle(SpanStyle(color = ZyraColors.cyan, background = ZyraColors.surfaceElevated, fontFamily = FontFamily.Monospace)); append(token.trim('`')); pop() }
            token.startsWith("[") -> {
                val label = token.substringAfter('[').substringBefore(']')
                val url = token.substringAfter("](").removeSuffix(")")
                pushStringAnnotation("URL", url)
                pushStyle(SpanStyle(color = ZyraColors.cyan, textDecoration = TextDecoration.Underline))
                append(label); pop(); pop()
            }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

@Preview(showBackground = true, backgroundColor = 0xFF05060A)
@Composable private fun MarkdownPreview() {
    ZyraTheme { ZyraMarkdown("# Hello\n- A list item\n**Bold** and `code`\n[OpenAI](https://openai.com)") }
}
