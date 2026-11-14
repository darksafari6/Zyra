package com.safari.zyra.core.network

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.URLProtocol
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import io.ktor.websocket.readText
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

sealed interface GeminiLiveEvent {
    data object Ready : GeminiLiveEvent
    data class Audio(val pcm24Khz: ByteArray, val mimeType: String?) : GeminiLiveEvent
    data class InputCaption(val text: String, val finished: Boolean) : GeminiLiveEvent
    data class OutputCaption(val text: String, val finished: Boolean) : GeminiLiveEvent
    data object Interrupted : GeminiLiveEvent
    data class GoAway(val timeLeft: String?) : GeminiLiveEvent
    data class Failure(val message: String) : GeminiLiveEvent
}

interface GeminiLiveTransport : java.io.Closeable {
    suspend fun connect(apiKey: String, modelId: String, systemInstruction: String)
    suspend fun sendPcm16kMono(pcm: ByteArray)
    suspend fun sendText(text: String)
    fun events(): Flow<GeminiLiveEvent>
}

/**
 * Low-level Gemini Live transport. Audio capture/playback deliberately lives outside
 * this class so the Android microphone is never opened as a side effect of connecting.
 *
 * The caller supplies the model ID and system instruction from the user's settings.
 */
@Singleton
class GeminiLiveClient @Inject constructor(
    private val httpClient: HttpClient
) : GeminiLiveTransport {
    private var socket: WebSocketSession? = null
    private val connected = AtomicBoolean(false)

    override suspend fun connect(apiKey: String, modelId: String, systemInstruction: String) {
        require(apiKey.isNotBlank()) { "Gemini API key is missing." }
        require(modelId.isNotBlank()) { "Choose a Live model in Settings first." }
        close()
        val liveSocket = httpClient.webSocketSession {
            url {
                protocol = URLProtocol.WSS
                host = "generativelanguage.googleapis.com"
                path("ws", "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent")
                parameters.append("key", apiKey)
            }
        }
        socket = liveSocket
        val setup = JSONObject()
            .put("setup", JSONObject()
                .put("model", if (modelId.startsWith("models/")) modelId else "models/$modelId")
                .put("generationConfig", JSONObject().put("responseModalities", org.json.JSONArray().put("AUDIO")))
                .put("systemInstruction", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", systemInstruction))))
                .put("inputAudioTranscription", JSONObject())
                .put("outputAudioTranscription", JSONObject()))
        liveSocket.send(Frame.Text(setup.toString()))
        try {
            val first = withTimeout(15_000) { liveSocket.incoming.receive() }
            val text = (first as? Frame.Text)?.readText().orEmpty()
            if (!JSONObject(text).has("setupComplete")) {
                throw IllegalStateException("Gemini Live did not confirm session setup.")
            }
            connected.set(true)
        } catch (error: Throwable) {
            close()
            throw error
        }
    }

    override suspend fun sendPcm16kMono(pcm: ByteArray) {
        check(connected.get()) { "Gemini Live session is not ready." }
        require(pcm.isNotEmpty() && pcm.size % 2 == 0) { "PCM must contain complete 16-bit samples." }
        val message = JSONObject().put("realtimeInput", JSONObject().put(
            "audio", JSONObject()
                .put("mimeType", "audio/pcm;rate=16000")
                .put("data", Base64.encodeToString(pcm, Base64.NO_WRAP))
        ))
        socket?.send(Frame.Text(message.toString()))
    }

    override suspend fun sendText(text: String) {
        check(connected.get()) { "Gemini Live session is not ready." }
        socket?.send(Frame.Text(JSONObject().put("realtimeInput", JSONObject().put("text", text)).toString()))
    }

    /** Emits every audio part and caption in each server event; consumers must clear playback on Interrupted. */
    override fun events(): Flow<GeminiLiveEvent> = flow {
        val active = socket ?: return@flow
        try {
            for (frame in active.incoming) {
                if (frame !is Frame.Text) continue
                val root = JSONObject(frame.readText())
                if (root.has("goAway")) {
                    emit(GeminiLiveEvent.GoAway(root.optJSONObject("goAway")?.optString("timeLeft")))
                }
                val server = root.optJSONObject("serverContent") ?: continue
                if (server.optBoolean("interrupted")) emit(GeminiLiveEvent.Interrupted)
                server.optJSONObject("inputTranscription")?.let {
                    emit(GeminiLiveEvent.InputCaption(it.optString("text"), it.optBoolean("finished", true)))
                }
                server.optJSONObject("outputTranscription")?.let {
                    emit(GeminiLiveEvent.OutputCaption(it.optString("text"), it.optBoolean("finished", true)))
                }
                val parts = server.optJSONObject("modelTurn")?.optJSONArray("parts") ?: continue
                for (index in 0 until parts.length()) {
                    val inline = parts.optJSONObject(index)?.optJSONObject("inlineData") ?: continue
                    val encoded = inline.optString("data")
                    if (encoded.isNotBlank()) {
                        emit(GeminiLiveEvent.Audio(
                            Base64.decode(encoded, Base64.DEFAULT),
                            inline.optString("mimeType").takeIf(String::isNotBlank)
                        ))
                    }
                }
            }
        } catch (error: Throwable) {
            emit(GeminiLiveEvent.Failure(error.message ?: "Gemini Live connection ended."))
        } finally {
            connected.set(false)
        }
    }

    override fun close() {
        connected.set(false)
        val active = socket
        socket = null
        active?.let { it.cancel() }
    }
}
