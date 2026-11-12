package com.safari.zyra.feature.voice

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.Executor

class InstalledVoiceLocaleCatalog(
    private val context: Context,
    private val onUpdate: (List<Locale>) -> Unit
) : AutoCloseable {
    private val locales = linkedSetOf<Locale>()
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val mainExecutor: Executor = context.mainExecutor

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.availableLanguages.orEmpty().forEach(locales::add)
                publish()
            }
        }
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            if (Build.VERSION.SDK_INT >= 33) {
                val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                recognizer?.checkRecognitionSupport(request, mainExecutor, object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        (recognitionSupport.installedOnDeviceLanguages + recognitionSupport.onlineLanguages)
                            .mapTo(locales) { Locale.forLanguageTag(it) }
                        publish()
                    }
                    override fun onError(error: Int) = publish()
                })
            } else {
                @Suppress("DEPRECATION")
                context.sendOrderedBroadcast(
                    Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS),
                    null,
                    object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            val supported = resultExtras(true)
                                ?.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
                                .orEmpty()
                            supported.mapTo(locales) { Locale.forLanguageTag(it) }
                            publish()
                        }
                    },
                    null,
                    Activity.RESULT_OK,
                    null,
                    null
                )
            }
        }
        locales.add(Locale.getDefault())
        publish()
    }

    private fun publish() {
        val result = locales.filter { it.language.isNotBlank() }
            .distinctBy { it.toLanguageTag() }
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }
        onUpdate(result)
    }

    override fun close() {
        recognizer?.destroy()
        recognizer = null
        tts?.shutdown()
        tts = null
    }
}
