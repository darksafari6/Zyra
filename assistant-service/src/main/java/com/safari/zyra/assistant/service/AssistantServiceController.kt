package com.safari.zyra.assistant.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AssistantServiceController {
    fun start()
    fun stop()
    fun talk()
}

/** start() must be called from a visible Activity after RECORD_AUDIO has been granted. */
class AndroidAssistantServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) : AssistantServiceController {
    override fun start() {
        check(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Grant microphone permission before starting Zyra's microphone service."
        }
        val intent = Intent(context, ZyraAssistantService::class.java).setAction(ZyraAssistantService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    override fun stop() {
        context.stopService(Intent(context, ZyraAssistantService::class.java))
    }

    override fun talk() {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            ?.putExtra(ZyraAssistantService.EXTRA_OPEN_VOICE, true)
        if (launch != null) context.startActivity(launch)
    }
}