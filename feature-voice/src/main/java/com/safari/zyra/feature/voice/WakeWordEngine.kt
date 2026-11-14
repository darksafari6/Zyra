package com.safari.zyra.feature.voice

import kotlinx.coroutines.flow.Flow

/**
 * Offline-only keyword detector contract. Implementations must perform inference
 * locally and must not persist or upload pre-trigger audio.
 */
interface WakeWordEngine {
    val isAvailable: Boolean
    val phrase: String
    suspend fun start(audio: Flow<ByteArray>, sensitivity: Float): Flow<Unit>
    suspend fun stop()
}

/**
 * Safe default until a licensed, packaged Hey Zyra model is present. It intentionally
 * does not subscribe to microphone audio and therefore cannot trigger accidentally.
 */
class UnconfiguredWakeWordEngine : WakeWordEngine {
    override val isAvailable: Boolean = false
    override val phrase: String = "Hey Zyra"
    override suspend fun start(audio: Flow<ByteArray>, sensitivity: Float): Flow<Unit> {
        throw IllegalStateException(
            "Offline Hey Zyra model is not installed. Wake word remains disabled; Fast voice is still available."
        )
    }
    override suspend fun stop() = Unit
}
