package com.safari.zyra

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.safari.zyra.assistant.service.ZyraAssistantService

object ZyraShortcuts {
    fun publish(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val icon = Icon.createWithResource(context, R.mipmap.ic_zyra)
        val ask = ShortcutInfo.Builder(context, "zyra-ask")
            .setShortLabel("Ask Zyra")
            .setLongLabel("Start a conversation with Zyra")
            .setIcon(icon)
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            .build()
        val voice = ShortcutInfo.Builder(context, "zyra-voice")
            .setShortLabel("Voice chat")
            .setLongLabel("Talk to Zyra")
            .setIcon(icon)
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(ZyraAssistantService.EXTRA_OPEN_VOICE, true))
            .build()
        runCatching { manager.dynamicShortcuts = listOf(ask, voice) }
    }
}