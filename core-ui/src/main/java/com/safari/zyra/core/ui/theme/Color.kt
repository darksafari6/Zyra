package com.safari.zyra.core.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ZyraColors {
    var pureBlack by mutableStateOf(false)
    val background: Color get() = if (pureBlack) Color.Black else Color(0xFF05060A)
    val surface: Color get() = if (pureBlack) Color(0xFF050505) else Color(0xFF0E1220)
    val surfaceElevated = Color(0xFF151D30)
    val glass: Color get() = if (pureBlack) Color(0xE6050505) else Color(0xCC0E1220)
    val border = Color(0x553C5B83)
    val text = Color(0xFFEAF0FF)
    val muted = Color(0xFF9BA7C2)
    val cyan = Color(0xFF00E5FF)
    val violet = Color(0xFF7C4DFF)
    val magenta = Color(0xFFFF2BD6)
    val success = Color(0xFF72E7BE)
    val warning = Color(0xFFFFC56D)
    val error = Color(0xFFFF8A8A)
}
val ZyraBackground get() = ZyraColors.background
val ZyraSurface get() = ZyraColors.surface
val ZyraCyan = ZyraColors.cyan
val ZyraViolet = ZyraColors.violet
val ZyraMagenta = ZyraColors.magenta
val ZyraText = ZyraColors.text
val ZyraMutedText = ZyraColors.muted
