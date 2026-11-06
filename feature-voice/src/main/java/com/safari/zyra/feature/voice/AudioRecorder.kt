package com.safari.zyra.feature.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Singleton
class AudioRecorder @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableAmplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = mutableAmplitude.asStateFlow()
    private var record: AudioRecord? = null
    private var job: Job? = null
    @Volatile private var voiceDetected = false
    @Volatile private var lastVoiceAt = 0L

    @Synchronized
    fun start(onSilence: () -> Unit) {
        if (job?.isActive == true) return
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minBuffer > 0) { "This device cannot initialize 16 kHz microphone capture." }
        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer, FRAME_SAMPLES * 4))
        if (audio.state != AudioRecord.STATE_INITIALIZED) {
            audio.release()
            throw IllegalStateException("Microphone recorder could not be initialized.")
        }
        record = audio
        voiceDetected = false
        lastVoiceAt = 0L
        audio.startRecording()
        job = scope.launch {
            val frame = ShortArray(FRAME_SAMPLES)
            try {
                while (isActive && audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val count = audio.read(frame, 0, frame.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) continue
                    var sum = 0.0
                    for (i in 0 until count) {
                        val value = frame[i].toDouble() / Short.MAX_VALUE
                        sum += value * value
                    }
                    val rms = sqrt(sum / count).toFloat()
                    mutableAmplitude.value = (rms * 5f).coerceIn(0f, 1f)
                    val now = System.currentTimeMillis()
                    if (rms >= VOICE_THRESHOLD) {
                        voiceDetected = true
                        lastVoiceAt = now
                    } else if (voiceDetected && now - lastVoiceAt >= SILENCE_MS) {
                        voiceDetected = false
                        onSilence()
                    }
                }
            } finally { mutableAmplitude.value = 0f }
        }
    }

    suspend fun stop() {
        val pair = synchronized(this) { val oldJob = job; val oldRecord = record; job = null; record = null; oldJob to oldRecord }
        pair.second?.let { audio ->
            runCatching { if (audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) audio.stop() }
        }
        pair.first?.cancelAndJoin()
        pair.second?.release()
        mutableAmplitude.value = 0f
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val FRAME_SAMPLES = 320
        private const val VOICE_THRESHOLD = 0.018
        private const val SILENCE_MS = 1_050L
    }
}