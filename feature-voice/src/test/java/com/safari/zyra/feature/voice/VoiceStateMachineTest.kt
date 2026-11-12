package com.safari.zyra.feature.voice

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceStateMachineTest {
    @Test
    fun followsListeningThinkingSpeakingAndStopTransitions() = runTest {
        val state = MutableStateFlow<VoiceState>(VoiceState.Idle)
        state.test {
            assertEquals(VoiceState.Idle, awaitItem())
            state.value = VoiceStateMachine.reduce(state.value, VoiceStateMachine.Event.StartListening)
            assertEquals(VoiceState.Listening(), awaitItem())
            state.value = VoiceStateMachine.reduce(state.value, VoiceStateMachine.Event.PartialTranscript("hello"))
            assertEquals(VoiceState.Listening(transcript = "hello"), awaitItem())
            state.value = VoiceStateMachine.reduce(state.value, VoiceStateMachine.Event.StartThinking("hello"))
            assertEquals(VoiceState.Thinking("hello"), awaitItem())
            state.value = VoiceStateMachine.reduce(state.value, VoiceStateMachine.Event.Speak("hello", "Hi there"))
            assertEquals(VoiceState.Speaking("hello", "Hi there"), awaitItem())
            state.value = VoiceStateMachine.reduce(state.value, VoiceStateMachine.Event.Stop)
            assertEquals(VoiceState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
