package com.safari.zyra.core.ui
import android.os.Build

enum class PerformanceTier {
    BASIC, MID, HIGH;
    companion object {
        fun current(): PerformanceTier = when {
            Build.VERSION.SDK_INT >= 33 -> HIGH
            Build.VERSION.SDK_INT >= 31 -> MID
            else -> BASIC
        }
    }
}
