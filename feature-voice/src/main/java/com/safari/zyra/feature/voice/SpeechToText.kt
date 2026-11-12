package com.safari.zyra.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechToText @Inject constructor(@ApplicationContext private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private var partial: ((String) -> Unit)? = null
    private var final: ((String) -> Unit)? = null
    private var failure: ((String) -> Unit)? = null
    private var rmsChanged: ((Float) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeLanguage = "en-US"
    private var usingOnDevice = false
    private var fallbackAttempted = false
    private var sessionGeneration = 0L

    fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(languageTag: String, onPartial: (String) -> Unit, onFinal: (String) -> Unit, onFailure: (String) -> Unit, onRms: (Float) -> Unit = {}) {
        if (!isAvailable()) { onFailure("Speech recognition is not available on this device."); return }
        partial = onPartial
        final = onFinal
        failure = onFailure
        rmsChanged = onRms
        activeLanguage = languageTag.ifBlank { Locale.getDefault().toLanguageTag() }
        fallbackAttempted = false
        sessionGeneration += 1
        startRecognizer(preferOnDevice = true, generation = sessionGeneration)
    }

    private fun startRecognizer(preferOnDevice: Boolean, generation: Long) {
        runCatching {
            recognizer?.destroy()
            usingOnDevice = preferOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            recognizer = if (usingOnDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(listener(generation))
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
            }
            recognizer?.startListening(intent)
        }.onFailure { if (generation == sessionGeneration) failure?.invoke(it.message ?: "Could not start speech recognition.") }
    }

    private fun listener(generation: Long) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) { if (generation == sessionGeneration) rmsChanged?.invoke(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
        override fun onPartialResults(results: Bundle?) {
            if (generation != sessionGeneration) return
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { partial?.invoke(it) }
        }
        override fun onResults(results: Bundle?) {
            if (generation != sessionGeneration) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isBlank()) failure?.invoke("I couldn't hear that clearly. Please try again.") else final?.invoke(text)
        }
        override fun onError(error: Int) {
            if (generation != sessionGeneration) return
            val languageUnavailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
            if (usingOnDevice && languageUnavailable && !fallbackAttempted) {
                fallbackAttempted = true
                startRecognizer(preferOnDevice = false, generation = generation)
            } else failure?.invoke(errorMessage(error))
        }
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post { action() }
    }
    fun stopListening() { onMain { runCatching { recognizer?.stopListening() } } }
    fun cancel() { onMain { sessionGeneration += 1; runCatching { recognizer?.cancel() } } }
    fun destroy() { onMain { sessionGeneration += 1; runCatching { recognizer?.destroy() }; recognizer = null } }

    private fun errorMessage(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs a working network connection."
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that. Try speaking a little closer to the microphone."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Try again in a moment."
        else -> "Speech recognition failed (code $code)."
    }
}