package com.safari.zyra.feature.voice

import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.network.GeminiLiveEvent
import com.safari.zyra.core.network.GeminiLiveTransport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class LiveVoiceUiState(
    val status: String = "Idle",
    val inputCaption: String = "",
    val outputCaption: String = "",
    val error: String? = null
)

/**
 * Owns a complete explicit Gemini Live microphone session. No connection is made
 * and no microphone is opened until start() is called by a visible, permissioned UI.
 */
@Singleton
class LiveVoiceSession @Inject constructor(
    private val transport: GeminiLiveTransport,
    private val audio: LiveAudioEngine,
    private val settings: SettingsRepository,
    private val keys: ApiKeyStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(LiveVoiceUiState())
    val state: StateFlow<LiveVoiceUiState> = mutableState.asStateFlow()
    private var captureJob: Job? = null
    private var eventsJob: Job? = null

    fun start() {
        scope.launch {
            stopInternal()
            mutableState.value = LiveVoiceUiState(status = "Connecting")
            try {
                val config = settings.settings.first()
                transport.connect(keys.read(), config.liveModelId, config.systemPrompt)
                mutableState.value = LiveVoiceUiState(status = "Listening")
                eventsJob = scope.launch {
                    transport.events().collect { event ->
                        when (event) {
                            GeminiLiveEvent.Ready -> mutableState.value = mutableState.value.copy(status = "Listening")
                            is GeminiLiveEvent.Audio -> audio.playPcm24kMono(event.pcm24Khz)
                            is GeminiLiveEvent.InputCaption -> mutableState.value = mutableState.value.copy(
                                inputCaption = mutableState.value.inputCaption + event.text
                            )
                            is GeminiLiveEvent.OutputCaption -> mutableState.value = mutableState.value.copy(
                                outputCaption = mutableState.value.outputCaption + event.text
                            )
                            GeminiLiveEvent.Interrupted -> audio.interruptPlayback()
                            is GeminiLiveEvent.GoAway -> {
                                captureJob?.cancel()
                                audio.close()
                                mutableState.value = mutableState.value.copy(
                                    status = "Session ending",
                                    error = "Gemini is ending this Live session. Stop and reconnect to continue."
                                )
                            }
                            is GeminiLiveEvent.Failure -> {
                                captureJob?.cancel()
                                audio.close()
                                mutableState.value = mutableState.value.copy(status = "Error", error = event.message)
                            }
                        }
                    }
                }
                captureJob = scope.launch {
                    try {
                        audio.capturePcm16kMono().collect { pcm -> transport.sendPcm16kMono(pcm) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        mutableState.value = mutableState.value.copy(
                            status = "Error", error = failure.message ?: "Live microphone capture stopped."
                        )
                        eventsJob?.cancel()
                        transport.close()
                        audio.close()
                    }
                }
            } catch (failure: Exception) {
                mutableState.value = LiveVoiceUiState(status = "Error", error = failure.message ?: "Could not start Gemini Live.")
                stopInternal()
            }
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        scope.launch {
            runCatching { transport.sendText(text.trim()) }
                .onFailure { mutableState.value = mutableState.value.copy(error = it.message ?: "Could not send text.") }
        }
    }

    fun stop() {
        scope.launch {
            stopInternal()
            mutableState.value = LiveVoiceUiState()
        }
    }

    private fun stopInternal() {
        captureJob?.cancel()
        eventsJob?.cancel()
        captureJob = null
        eventsJob = null
        transport.close()
        audio.close()
    }
}