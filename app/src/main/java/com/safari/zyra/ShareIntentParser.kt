package com.safari.zyra

import android.content.Intent
import android.net.Uri
import android.os.Build

data class SharedZyraContent(val text: String?, val image: Uri?)

fun parseSharedContent(intent: Intent?): SharedZyraContent? {
    if (intent?.action != Intent.ACTION_SEND) return null
    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf(String::isNotBlank)
    val image = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    }
    if (text == null && image == null) return null
    return SharedZyraContent(text, image)
}