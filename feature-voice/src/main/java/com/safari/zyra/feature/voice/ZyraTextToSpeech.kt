package com.safari.zyra.feature.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZyraTextToSpeech @Inject constructor(@ApplicationContext context: Context) : TextToSpeech.OnInitListener {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> stop()
            AudioManager.AUDIOFOCUS_GAIN -> Unit
            else -> Unit
        }
    }
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var engine: TextToSpeech? = TextToSpeech(context, this)
    @Volatile private var ready = false
    private var currentLanguage = ""
    private val pending = ArrayDeque<SpeechRequest>()
    private val activeUtterances = AtomicInteger(0)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = utteranceFinished()
            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) = utteranceFinished()
            override fun onError(utteranceId: String?, errorCode: Int) = utteranceFinished()
        })
        if (ready) synchronized(pending) {
            while (pending.isNotEmpty()) speakNow(pending.removeFirst())
        }
    }

    fun speak(sentence: String, languageTag: String, speed: Float, pitch: Float) {
        val cleanText = cleanForSpeech(sentence)
        if (cleanText.isBlank()) return
        val request = SpeechRequest(cleanText, languageTag.ifBlank { "en-US" }, speed, pitch)
        if (!ready) {
            synchronized(pending) { pending.addLast(request) }
            return
        }
        speakNow(request)
    }

    @Synchronized
    private fun speakNow(request: SpeechRequest) {
        val tts = engine ?: return
        if (!requestAudioFocus()) return
        if (currentLanguage != request.language) {
            val locale = Locale.forLanguageTag(request.language)
            if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) tts.language = locale
            currentLanguage = request.language
        }
        tts.setSpeechRate(request.speed.coerceIn(0.65f, 1.5f))
        tts.setPitch(request.pitch.coerceIn(0.7f, 1.4f))
        val utteranceId = "zyra-" + System.nanoTime()
        if (tts.speak(request.text, TextToSpeech.QUEUE_ADD, null, utteranceId) == TextToSpeech.SUCCESS) {
            activeUtterances.incrementAndGet()
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener)
                .setWillPauseWhenDucked(false)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        hasAudioFocus = granted
        return granted
    }

    private fun utteranceFinished() {
        val remaining = activeUtterances.updateAndGet { count -> (count - 1).coerceAtLeast(0) }
        if (remaining == 0 && synchronized(pending) { pending.isEmpty() }) abandonAudioFocus()
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        hasAudioFocus = false
    }

    fun isSpeaking(): Boolean =
        synchronized(pending) { pending.isNotEmpty() } ||
            activeUtterances.get() > 0 ||
            runCatching { engine?.isSpeaking == true }.getOrDefault(false)

    @Synchronized
    fun stop() {
        synchronized(pending) { pending.clear() }
        activeUtterances.set(0)
        runCatching { engine?.stop() }
        abandonAudioFocus()
    }

    fun shutdown() {
        stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private fun cleanForSpeech(source: String): String = source
        .replace(Regex("""https?://\S+|www\.\S+""", RegexOption.IGNORE_CASE), "link")
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("[*_~\u0060]"), "")
        .replace(Regex("""[\uD83C-\uDBFF][\uDC00-\uDFFF]"""), "")
        .replace(Regex("""[\uFE0E\uFE0F\u200D]"""), "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private data class SpeechRequest(val text: String, val language: String, val speed: Float, val pitch: Float)
}
