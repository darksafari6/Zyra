package com.safari.zyra.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AssistantServiceController {
    fun start()
    fun stop()
    fun talk()
}

/** Starts only in response to an explicit user action; callers must obtain permissions first. */
class AndroidAssistantServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) : AssistantServiceController {
    override fun start() {
        val intent = Intent(context, ZyraAssistantService::class.java).setAction(ZyraAssistantService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    override fun stop() {
        context.startService(Intent(context, ZyraAssistantService::class.java).setAction(ZyraAssistantService.ACTION_STOP))
    }

    override fun talk() {
        context.startService(Intent(context, ZyraAssistantService::class.java).setAction(ZyraAssistantService.ACTION_TALK))
    }
}
