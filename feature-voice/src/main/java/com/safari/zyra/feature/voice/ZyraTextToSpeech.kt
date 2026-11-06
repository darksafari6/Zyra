package com.safari.zyra.feature.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayDeque
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZyraTextToSpeech @Inject constructor(@ApplicationContext context: Context) : TextToSpeech.OnInitListener {
    private var engine: TextToSpeech? = TextToSpeech(context, this)
    @Volatile private var ready = false
    private var currentLanguage = ""
    private val pending = ArrayDeque<SpeechRequest>()

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) = Unit
        })
        if (ready) synchronized(pending) {
            while (pending.isNotEmpty()) speakNow(pending.removeFirst())
        }
    }

    fun speak(sentence: String, languageTag: String, speed: Float, pitch: Float) {
        if (sentence.isBlank()) return
        val request = SpeechRequest(sentence.trim(), languageTag.ifBlank { "en-US" }, speed, pitch)
        if (!ready) {
            synchronized(pending) { pending.addLast(request) }
            return
        }
        speakNow(request)
    }

    @Synchronized
    private fun speakNow(request: SpeechRequest) {
        val tts = engine ?: return
        if (currentLanguage != request.language) {
            val locale = Locale.forLanguageTag(request.language)
            if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) tts.language = locale
            currentLanguage = request.language
        }
        tts.setSpeechRate(request.speed.coerceIn(0.65f, 1.5f))
        tts.setPitch(request.pitch.coerceIn(0.7f, 1.4f))
        tts.speak(request.text, TextToSpeech.QUEUE_ADD, null, "zyra-" + System.nanoTime())
    }

    fun isSpeaking(): Boolean = runCatching { engine?.isSpeaking == true }.getOrDefault(false)
    fun stop() {
        synchronized(pending) { pending.clear() }
        runCatching { engine?.stop() }
    }
    fun shutdown() { stop(); engine?.shutdown(); engine = null; ready = false }

    private data class SpeechRequest(val text: String, val language: String, val speed: Float, val pitch: Float)
}