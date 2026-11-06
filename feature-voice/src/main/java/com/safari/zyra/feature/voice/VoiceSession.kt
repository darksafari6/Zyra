package com.safari.zyra.feature.voice

import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.chat.ChatMessage
import com.safari.zyra.core.data.chat.ChatRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.network.GeminiClient
import com.safari.zyra.core.network.GeminiTurn
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class VoiceSession @Inject constructor(
    private val recorder: AudioRecorder,
    private val speech: SpeechToText,
    private val tts: ZyraTextToSpeech,
    private val settings: SettingsRepository,
    private val chat: ChatRepository,
    private val keys: ApiKeyStore,
    private val gemini: GeminiClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = mutableState.asStateFlow()
    private var responseJob: Job? = null
    private var amplitudeJob: Job? = null
    private var sentenceBuffer = StringBuilder()
    private var bargeInStarted = false

    fun startListening() {
        if (!speech.isAvailable()) {
            mutableState.value = VoiceState.Error("Speech recognition is unavailable. Install or enable a speech recognition service.")
            return
        }
        scope.launch {
            stopInternal()
            val language = settings.settings.first().voiceLanguage
            try {
                recorder.start { speech.stopListening() }
                mutableState.value = VoiceState.Listening()
                amplitudeJob = launch {
                    recorder.amplitude.collect { amplitude ->
                        val current = mutableState.value
                        if (current is VoiceState.Listening) mutableState.value = current.copy(amplitude = amplitude)
                    }
                }
                speech.start(
                    languageTag = language,
                    onPartial = { text ->
                        val current = mutableState.value
                        if (current is VoiceState.Listening) mutableState.value = current.copy(transcript = text)
                    },
                    onFinal = { text -> scope.launch { recorder.stop(); amplitudeJob?.cancel(); handleTranscript(text) } },
                    onFailure = { message ->
                        scope.launch {
                            recorder.stop()
                            amplitudeJob?.cancel()
                            if (mutableState.value !is VoiceState.Speaking) mutableState.value = VoiceState.Error(message)
                        }
                    }
                )
            } catch (failure: SecurityException) {
                mutableState.value = VoiceState.Error("Microphone permission is required.")
            } catch (failure: Exception) {
                mutableState.value = VoiceState.Error(failure.message ?: "Could not start the microphone.")
            }
        }
    }

    private suspend fun handleTranscript(raw: String) {
        val transcript = raw.trim()
        if (transcript.isBlank()) {
            mutableState.value = VoiceState.Error("I couldn't hear that. Tap the orb and try again.")
            return
        }
        if (transcript.equals("stop", true) || transcript.equals("stop speaking", true)) {
            tts.stop()
            mutableState.value = VoiceState.Idle
            return
        }
        mutableState.value = VoiceState.Thinking(transcript)
        chat.add(ChatMessage(role = "user", text = transcript))
        val config = settings.settings.first()
        sentenceBuffer = StringBuilder()
        bargeInStarted = false
        responseJob = scope.launch {
            val response = StringBuilder()
            try {
                val history = chat.recentForPrompt(21).dropLast(1).map { GeminiTurn(it.role, it.text) }
                gemini.streamReply(
                    apiKey = keys.read(),
                    modelId = config.textModelId,
                    systemPrompt = config.systemPrompt,
                    history = history,
                    userText = transcript
                ).collect { chunk ->
                    response.append(chunk)
                    mutableState.value = VoiceState.Speaking(transcript, response.toString())
                    enqueueSentences(chunk, config.voiceLanguage, config.voiceSpeed, config.voicePitch)
                }
                flushSentence(config.voiceLanguage, config.voiceSpeed, config.voicePitch)
                val answer = response.toString().trim()
                if (answer.isBlank()) error("Zyra received an empty response.")
                chat.add(ChatMessage(role = "assistant", text = answer))
                while (tts.isSpeaking()) delay(120)
                mutableState.value = VoiceState.Idle
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = VoiceState.Error(failure.message ?: "Voice reply failed.")
            }
        }
    }

    private fun enqueueSentences(chunk: String, language: String, speed: Float, pitch: Float) {
        sentenceBuffer.append(chunk)
        val all = sentenceBuffer.toString()
        val boundary = Regex("""[.!?。！？](?:["'”’)]*)\s+""")
        var consumed = 0
        boundary.findAll(all).forEach { match ->
            val sentence = all.substring(consumed, match.range.last + 1).trim()
            if (sentence.isNotBlank()) {
                tts.speak(sentence, language, speed, pitch)
                if (!bargeInStarted) {
                    bargeInStarted = true
                    speech.start(language, {}, { spoken ->
                        if (spoken.trim().equals("stop", true) || spoken.trim().equals("stop speaking", true)) stopSpeaking()
                    }, {})
                }
            }
            consumed = match.range.last + 1
        }
        if (consumed > 0) sentenceBuffer = StringBuilder(all.substring(consumed))
    }

    private fun flushSentence(language: String, speed: Float, pitch: Float) {
        sentenceBuffer.toString().trim().takeIf { it.isNotBlank() }?.let { tts.speak(it, language, speed, pitch) }
        sentenceBuffer.clear()
    }

    fun stopSpeaking() {
        tts.stop()
        responseJob?.cancel()
        speech.cancel()
        scope.launch { recorder.stop() }
        mutableState.value = VoiceState.Idle
    }

    fun stop() { scope.launch { stopInternal() } }

    private suspend fun stopInternal() {
        speech.cancel()
        tts.stop()
        responseJob?.cancel()
        amplitudeJob?.cancel()
        recorder.stop()
        mutableState.value = VoiceState.Idle
    }
}