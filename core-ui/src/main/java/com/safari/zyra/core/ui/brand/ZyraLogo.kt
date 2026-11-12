package com.safari.zyra.core.ui.brand

import com.safari.zyra.core.ui.theme.ZyraColors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.unit.dp
import com.safari.zyra.core.ui.theme.ZyraColors
import kotlin.math.min

@Composable
fun ZyraLogo(progress: Float, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier.size(180.dp)) {
        val d = min(size.width, size.height)
        val c = Offset(size.width / 2f, size.height / 2f)
        val radius = d * .44f
        drawCircle(Brush.radialGradient(listOf(ZyraColors.violet.copy(alpha = .82f), ZyraColors.surface.copy(alpha = .9f), ZyraColors.background.copy(alpha = 0f)), c, radius * 1.35f), radius * 1.35f, c)
        drawCircle(ZyraColors.cyan.copy(alpha = .72f), radius, c, style = Stroke(width = d * .012f))
        drawCircle(ZyraColors.violet.copy(alpha = .22f), radius * .91f, c, style = Stroke(width = d * .008f))
        val ribbon = Path().apply {
            moveTo(c.x - radius * .52f, c.y - radius * .43f)
            cubicTo(c.x - radius * .05f, c.y - radius * .76f, c.x + radius * .65f, c.y - radius * .55f, c.x + radius * .58f, c.y - radius * .18f)
            cubicTo(c.x + radius * .52f, c.y + radius * .12f, c.x - radius * .55f, c.y + radius * .47f, c.x - radius * .48f, c.y + radius * .58f)
            cubicTo(c.x - radius * .36f, c.y + radius * .76f, c.x + radius * .28f, c.y + radius * .35f, c.x + radius * .58f, c.y + radius * .08f)
        }
        val measure = PathMeasure()
        measure.setPath(ribbon, false)
        val drawn = Path()
        measure.getSegment(0f, measure.length * p, drawn, true)
        drawPath(drawn, ZyraColors.violet.copy(alpha = .35f), style = Stroke(width = d * .075f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(drawn, Brush.linearGradient(listOf(ZyraColors.cyan, ZyraColors.violet, ZyraColors.magenta)), style = Stroke(width = d * .035f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(drawn, Color.White.copy(alpha = .78f), style = Stroke(width = d * .008f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}