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

    fun startListening(conversationId: Long = 1) {
        if (!speech.isAvailable()) {
            mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Fail("Speech recognition is unavailable. Install or enable a speech recognition service."))
            return
        }
        scope.launch {
            stopInternal()
            val language = settings.settings.first().voiceLanguage
            try {
                mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.StartListening)
                speech.start(
                    languageTag = language,
                    onPartial = { text ->
                        val current = mutableState.value
                        if (current is VoiceState.Listening) mutableState.value = VoiceStateMachine.reduce(current, VoiceStateMachine.Event.PartialTranscript(text))
                    },
                    onFinal = { text -> scope.launch { handleTranscript(text, conversationId) } },
                    onFailure = { message ->
                        scope.launch {
                            if (mutableState.value !is VoiceState.Speaking) mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Fail(message))
                        }
                    },
                    onRms = { amplitude ->
                        val current = mutableState.value
                        if (current is VoiceState.Listening) mutableState.value = VoiceStateMachine.reduce(current, VoiceStateMachine.Event.Amplitude(amplitude))
                    }
                )
            } catch (failure: SecurityException) {
                mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Fail("Microphone permission is required."))
            } catch (failure: Exception) {
                mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Fail(failure.message ?: "Could not start the microphone."))
            }
        }
    }

    private suspend fun handleTranscript(raw: String, conversationId: Long) {
        val transcript = raw.trim()
        if (transcript.isBlank()) {
            mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Fail("I couldn't hear that. Tap the orb and try again."))
            return
        }
        if (isStopCommand(transcript)) {
            tts.stop()
            speech.cancel()
            speech.destroy()
            mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Stop)
            return
        }
        mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.StartThinking(transcript))
        chat.add(ChatMessage(role = "user", text = transcript, conversation_id = conversationId))
        val config = settings.settings.first()
        sentenceBuffer = StringBuilder()
        bargeInStarted = false
        responseJob = scope.launch {
            val response = StringBuilder()
            try {
                val recent = chat.recentForPrompt(500, conversationId)
                val conversation = chat.getConversation(conversationId)
                val previousSummaryThrough = conversation?.summaryThroughMessageId ?: 0L
                val olderTurns = recent.filter { it.id > previousSummaryThrough }.dropLast(24)
                var summary = conversation?.summary.orEmpty()
                var summaryThrough = previousSummaryThrough
                if (olderTurns.isNotEmpty()) {
                    val olderText = olderTurns.joinToString("\n") { turn ->
                        "${turn.role}: ${turn.text.take(360)}" + if (turn.imagePath != null) " [Earlier image omitted]" else ""
                    }
                    summary = (summary + "\n" + olderText).trim().takeLast(6_000)
                    summaryThrough = olderTurns.last().id
                    chat.updateSummary(conversationId, summary, summaryThrough)
                }
                val history = buildList {
                    if (summary.isNotBlank()) add(GeminiTurn("summary", summary))
                    addAll(recent.filter { it.id > summaryThrough }.dropLast(1).map { turn ->
                        GeminiTurn(turn.role, turn.text + if (turn.imagePath != null) " [Earlier image omitted]" else "")
                    })
                }
                gemini.streamReply(
                    apiKey = keys.read(),
                    modelId = config.textModelId,
                    systemPrompt = config.systemPrompt,
                    history = history,
                    userText = transcript
                ).collect { chunk ->
                    response.append(chunk)
                    mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Speak(transcript, response.toString()))
                    enqueueSentences(chunk, config.voiceLanguage, config.voiceSpeed, config.voicePitch, config.voiceName, config.listenForStopWhileSpeaking)
                }
                flushSentence(config.voiceLanguage, config.voiceSpeed, config.voicePitch, config.voiceName, config.listenForStopWhileSpeaking)
                val answer = response.toString().trim()
                if (answer.isBlank()) error("Zyra received an empty response.")
                while (tts.isSpeaking()) delay(120)
                speech.cancel()
                speech.destroy()
                chat.add(ChatMessage(role = "assistant", text = answer, conversation_id = conversationId))
                mutableState.value = VoiceState.Idle
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = VoiceStateMachine.reduce(mutableState.value, VoiceStateMachine.Event.Fail(failure.message ?: "Voice reply failed."))
            }
        }
    }

    private fun enqueueSentences(chunk: String, language: String, speed: Float, pitch: Float, voiceName: String, listenForStop: Boolean) {
        sentenceBuffer.append(chunk)
        val all = sentenceBuffer.toString()
        val boundary = Regex("""[.!?。！？](?:["'”’)]*)\s+""")
        var consumed = 0
        boundary.findAll(all).forEach { match ->
            val sentence = all.substring(consumed, match.range.last + 1).trim()
            if (sentence.isNotBlank()) {
                tts.speak(sentence, language, speed, pitch, voiceName)
                if (listenForStop) armBargeIn(language)
            }
            consumed = match.range.last + 1
        }
        if (consumed > 0) sentenceBuffer = StringBuilder(all.substring(consumed))
    }

    private fun flushSentence(language: String, speed: Float, pitch: Float, voiceName: String, listenForStop: Boolean) {
        sentenceBuffer.toString().trim().takeIf { it.isNotBlank() }?.let { sentence ->
            tts.speak(sentence, language, speed, pitch, voiceName)
            if (listenForStop) armBargeIn(language)
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
        speech.destroy()
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
        val normalized = raw.lowercase()
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return normalized in setOf("stop", "stop speaking", "cancel", "bas", "bas karo", "ruk jao", "ruko")
    }
}