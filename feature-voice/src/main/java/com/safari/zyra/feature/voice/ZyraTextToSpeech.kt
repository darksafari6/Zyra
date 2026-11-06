package com.safari.zyra.feature.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZyraTextToSpeech @Inject constructor(@ApplicationContext context: Context) : TextToSpeech.OnInitListener {
    private var engine: TextToSpeech? = TextToSpeech(context, this)
    @Volatile private var ready = false
    private var currentLanguage = ""

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) = Unit
        })
    }

    fun speak(sentence: String, languageTag: String, speed: Float, pitch: Float) {
        if (!ready || sentence.isBlank()) return
        val tts = engine ?: return
        val tag = languageTag.ifBlank { "en-US" }
        if (currentLanguage != tag) {
            tts.language = Locale.forLanguageTag(tag)
            currentLanguage = tag
        }
        tts.setSpeechRate(speed.coerceIn(0.65f, 1.5f))
        tts.setPitch(pitch.coerceIn(0.7f, 1.4f))
        tts.speak(sentence.trim(), TextToSpeech.QUEUE_ADD, null, "zyra-" + System.nanoTime())
    }

    fun stop() { runCatching { engine?.stop() } }
    fun shutdown() { stop(); engine?.shutdown(); engine = null; ready = false }
}