package com.safari.zyra.feature.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioManager
import android.media.AudioTrack.Builder
import android.media.AudioRecord.Builder as RecordBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Platform boundary for Gemini Live PCM. Calling capturePcm16kMono is the explicit
 * point at which microphone capture begins; connecting a WebSocket never opens it.
 */
interface LiveAudioEngine : Closeable {
    fun capturePcm16kMono(): Flow<ByteArray>
    suspend fun playPcm24kMono(pcm: ByteArray)
    fun interruptPlayback()
}

class AndroidLiveAudioEngine @Inject constructor() : LiveAudioEngine {
    private val capturing = AtomicBoolean(false)
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var player: AudioTrack? = null
    @Volatile private var echoCanceler: AcousticEchoCanceler? = null
    @Volatile private var noiseSuppressor: NoiseSuppressor? = null

    override fun capturePcm16kMono(): Flow<ByteArray> = flow {
        check(capturing.compareAndSet(false, true)) { "Microphone capture is already active." }
        val minBuffer = AudioRecord.getMinBufferSize(
            INPUT_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            capturing.set(false)
            error("This device does not support 16 kHz PCM recording.")
        }
        val audioRecord = try { RecordBuilder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(INPUT_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build())
            .setBufferSizeInBytes(maxOf(minBuffer * 2, CHUNK_BYTES * 2))
            .build() } catch (failure: Throwable) {
                capturing.set(false)
                throw failure
            }
        recorder = audioRecord
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioRecord.audioSessionId)?.apply { enabled = true }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioRecord.audioSessionId)?.apply { enabled = true }
            }
            audioRecord.startRecording()
            val chunk = ByteArray(CHUNK_BYTES)
            while (capturing.get()) {
                currentCoroutineContext().ensureActive()
                val count = withContext(Dispatchers.IO) { audioRecord.read(chunk, 0, chunk.size) }
                if (count > 0) {
                    val usableBytes = count - (count % 2)
                    if (usableBytes > 0) emit(if (usableBytes == chunk.size) chunk.copyOf() else chunk.copyOf(usableBytes))
                }
                else if (count < 0) error("Microphone read failed: $count")
            }
        } finally {
            capturing.set(false)
            runCatching { audioRecord.stop() }
            audioRecord.release()
            recorder = null
            echoCanceler?.release()
            noiseSuppressor?.release()
            echoCanceler = null
            noiseSuppressor = null
        }
    }

    override suspend fun playPcm24kMono(pcm: ByteArray) = withContext(Dispatchers.IO) {
        require(pcm.isNotEmpty() && pcm.size % 2 == 0)
        val track = player ?: run {
            val minBuffer = AudioTrack.getMinBufferSize(
                OUTPUT_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            check(minBuffer > 0) { "This device does not support 24 kHz PCM playback." }
            Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(OUTPUT_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build())
                .setBufferSizeInBytes(maxOf(minBuffer, pcm.size * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .also { player = it; it.play() }
        }
        var offset = 0
        while (offset < pcm.size) {
            val written = track.write(pcm, offset, pcm.size - offset)
            check(written > 0) { "AudioTrack write failed: $written" }
            offset += written
        }
    }

    override fun interruptPlayback() {
        player?.let { runCatching { it.pause(); it.flush(); it.play() } }
    }

    override fun close() {
        capturing.set(false)
        // Stop unblocks the capture loop; its finally block owns recorder/effect release.
        recorder?.let { runCatching { it.stop() } }
        recorder = null
        player?.let { runCatching { it.pause(); it.flush(); it.stop() }; it.release() }
        player = null
    }

    private companion object {
        const val INPUT_RATE = 16_000
        const val OUTPUT_RATE = 24_000
        const val CHUNK_BYTES = 3_200 // 100 ms, 16-bit mono at 16 kHz
    }
}
