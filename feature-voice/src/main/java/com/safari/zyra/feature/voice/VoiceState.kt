package com.safari.zyra.feature.voice

sealed interface VoiceState {
    data object Idle : VoiceState
    data class Listening(val transcript: String = "", val amplitude: Float = 0f) : VoiceState
    data class Thinking(val transcript: String) : VoiceState
    data class Speaking(val transcript: String, val response: String) : VoiceState
    data class Error(val message: String) : VoiceState
}