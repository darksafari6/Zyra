package com.safari.zyra.assistant.service

import android.os.Bundle
import android.service.voice.VoiceInteractionService

/**
 * System-owned entry point shown in Android's Default digital assistant selector.
 * This service never starts microphone capture or a wake-word listener by itself.
 */
class ZyraVoiceInteractionService : VoiceInteractionService() {
    override fun onLaunchVoiceAssistFromKeyguard() {
        showSession(Bundle(), SHOW_WITH_ASSIST)
    }
}