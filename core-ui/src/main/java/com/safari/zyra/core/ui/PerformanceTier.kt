package com.safari.zyra.core.ui

import android.app.ActivityManager
import android.content.Context

enum class PerformanceTier {
    BASIC, MID, HIGH;

    val allowsBlur: Boolean get() = this != BASIC
    val allowsParticles: Boolean get() = this == HIGH
    val usesSimpleMotion: Boolean get() = this == BASIC

    companion object {
        fun current(context: Context): PerformanceTier {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryClassMb = manager.memoryClass
            return when {
                manager.isLowRamDevice || memoryClassMb <= 128 -> BASIC
                memoryClassMb <= 256 -> MID
                else -> HIGH
            }
        }
    }
}
