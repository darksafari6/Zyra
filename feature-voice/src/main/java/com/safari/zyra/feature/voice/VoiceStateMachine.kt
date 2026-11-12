package com.safari.zyra.feature.voice

object VoiceStateMachine {
    sealed interface Event {
        data object StartListening : Event
        data class PartialTranscript(val text: String) : Event
        data class Amplitude(val value: Float) : Event
        data class StartThinking(val transcript: String) : Event
        data class Speak(val transcript: String, val response: String) : Event
        data class Fail(val message: String) : Event
        data object Stop : Event
    }

    fun reduce(current: VoiceState, event: Event): VoiceState = when (event) {
        Event.StartListening -> VoiceState.Listening()
        is Event.PartialTranscript -> (current as? VoiceState.Listening)?.copy(transcript = event.text) ?: current
        is Event.Amplitude -> (current as? VoiceState.Listening)?.copy(amplitude = event.value.coerceIn(0f, 1f)) ?: current
        is Event.StartThinking -> VoiceState.Thinking(event.transcript)
        is Event.Speak -> VoiceState.Speaking(event.transcript, event.response)
        is Event.Fail -> VoiceState.Error(event.message)
        Event.Stop -> VoiceState.Idle
    }
}
