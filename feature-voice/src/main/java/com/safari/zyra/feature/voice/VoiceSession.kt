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
                mutableState.value = VoiceState.Listening()
                speech.start(
                    languageTag = language,
                    onPartial = { text ->
                        val current = mutableState.value
                        if (current is VoiceState.Listening) mutableState.value = current.copy(transcript = text)
                    },
                    onFinal = { text -> scope.launch { handleTranscript(text) } },
                    onFailure = { message ->
                        scope.launch {
                            if (mutableState.value !is VoiceState.Speaking) mutableState.value = VoiceState.Error(message)
                        }
                    },
                    onRms = { amplitude ->
                        val current = mutableState.value
                        if (current is VoiceState.Listening) mutableState.value = current.copy(amplitude = amplitude)
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
        if (isStopCommand(transcript)) {
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
                while (tts.isSpeaking()) delay(120)
                chat.add(ChatMessage(role = "assistant", text = answer))
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
                armBargeIn(language)
            }
            consumed = match.range.last + 1
        }
        if (consumed > 0) sentenceBuffer = StringBuilder(all.substring(consumed))
    }

    private fun flushSentence(language: String, speed: Float, pitch: Float) {
        sentenceBuffer.toString().trim().takeIf { it.isNotBlank() }?.let { sentence ->
            tts.speak(sentence, language, speed, pitch)
            armBargeIn(language)
        }
        sentenceBuffer.clear()
    }

    private fun armBargeIn(language: String) {
        if (bargeInStarted) return
        bargeInStarted = true
        speech.start(
            language,
            {},
            { spoken ->
                if (isStopCommand(spoken)) {
                    stopSpeaking()
                } else if (mutableState.value is VoiceState.Speaking) {
                    bargeInStarted = false
                    scope.launch { delay(250); armBargeIn(language) }
                }
            },
            { message ->
                if (message.contains("didn't catch", ignoreCase = true) && mutableState.value is VoiceState.Speaking) {
                    bargeInStarted = false
                    scope.launch { delay(350); armBargeIn(language) }
                }
            }
        )
    }

    fun stopSpeaking() {
        tts.stop()
        responseJob?.cancel()
        speech.cancel()
        mutableState.value = VoiceState.Idle
    }

    fun stop() { scope.launch { stopInternal() } }

    private suspend fun stopInternal() {
        speech.cancel()
        speech.destroy()
        tts.stop()
        responseJob?.cancel()
        mutableState.value = VoiceState.Idle
    }

    private fun isStopCommand(raw: String): Boolean {
        val normalized = raw.lowercase().replace(Regex("[^\\\\p{L}\\\\p{N}\\\\s]"), " ").replace(Regex("\\\\s+"), " ").trim()
        return normalized in setOf("stop", "stop speaking", "cancel", "bas", "bas karo", "ruk jao", "ruko")
    }
}