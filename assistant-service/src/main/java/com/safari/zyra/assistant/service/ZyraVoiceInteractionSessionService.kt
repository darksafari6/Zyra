package com.safari.zyra.assistant.service

import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class ZyraVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: android.os.Bundle?): VoiceInteractionSession =
        ZyraVoiceInteractionSession(this)
}