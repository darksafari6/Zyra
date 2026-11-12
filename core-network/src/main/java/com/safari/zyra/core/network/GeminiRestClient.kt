package com.safari.zyra.core.network

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GeminiRestClient @Inject constructor(
    private val http: HttpClient
) : GeminiClient {

    @Suppress("DEPRECATION")
    override fun streamReply(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<GeminiTurn>,
        userText: String,
        image: GeminiImage?
    ): Flow<String> = flow {
        requireCredentials(apiKey, modelId)
        val contents = JSONArray()
        val summary = history.firstOrNull { it.role == "summary" }?.text.orEmpty()
        var remainingTokens = HISTORY_TOKEN_BUDGET
        val budgetedTurns = history.asSequence()
            .filter { it.role != "summary" }
            .toList()
            .asReversed()
            .takeWhile { turn ->
                val cost = estimateTokens(turn.text)
                if (cost > remainingTokens) false else { remainingTokens -= cost; true }
            }
            .asReversed()
        budgetedTurns.forEach { turn ->
            contents.put(JSONObject()
                .put("role", if (turn.role == "assistant" || turn.role == "model") "model" else "user")
                .put("parts", JSONArray().put(JSONObject().put("text", turn.text))))
        }
        val currentParts = JSONArray().put(JSONObject().put("text", userText))
        image?.let {
            currentParts.put(JSONObject().put("inlineData", JSONObject()
                .put("mimeType", it.mimeType)
                .put("data", Base64.encodeToString(it.bytes, Base64.NO_WRAP))))
        }
        contents.put(JSONObject().put("role", "user").put("parts", currentParts))
        val instructions = if (summary.isBlank()) systemPrompt else "$systemPrompt\\n\\nRolling conversation summary (older turns):\\n$summary"
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instructions))))
            .put("contents", contents)

        var emittedAnyText = false
        var attempt = 0
        while (true) {
            try {
                val response = http.post(endpoint(modelId)) {
                    header("x-goog-api-key", apiKey.trim())
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(body.toString())
                }
                if (response.status != HttpStatusCode.OK) {
                    val errorChannel = response.bodyAsChannel()
                    val errorBody = buildString {
                        while (true) append(errorChannel.readUTF8Line() ?: break)
                    }
                    val retryable = response.status.value in RETRYABLE_STATUS_CODES
                    if (retryable && attempt < MAX_RETRIES && !emittedAnyText) {
                        delay(RETRY_DELAYS_MS[attempt])
                        attempt++
                        continue
                    }
                    throw mapHttpFailure(response.status.value, errorBody)
                }

                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isBlank() || payload == "[DONE]") continue
                    val root = runCatching { JSONObject(payload) }.getOrElse {
                        throw GeminiFailure.Http("Gemini returned an unreadable streaming response.")
                    }
                    root.optJSONObject("promptFeedback")?.optString("blockReason")
                        ?.takeIf { it.isNotBlank() && it != "null" && it != "BLOCK_REASON_UNSPECIFIED" }
                        ?.let { reason -> throw GeminiFailure.Http("Gemini blocked this prompt ($reason). Please rephrase it.") }

                    val candidates = root.optJSONArray("candidates")
                    val candidate = candidates?.optJSONObject(0)
                    val finishReason = candidate?.optString("finishReason").orEmpty()
                    when (finishReason) {
                        "SAFETY" -> throw GeminiFailure.Http("Gemini stopped this response because of its safety filters.")
                        "RECITATION" -> throw GeminiFailure.Http("Gemini could not provide this response because of a recitation restriction.")
                    }
                    val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
                    val chunk = buildString {
                        if (parts != null) for (index in 0 until parts.length()) {
                            append(parts.optJSONObject(index)?.optString("text").orEmpty())
                        }
                    }
                    if (chunk.isNotEmpty()) {
                        emittedAnyText = true
                        emit(chunk)
                    }
                    if (finishReason == "MAX_TOKENS") {
                        // The partial answer is useful, but callers should not mistake it for a complete response.
                        emit("\n\n[Response reached the model's output limit.]")
                    }
                }
                break
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: GeminiFailure) {
                throw failure
            } catch (failure: IOException) {
                throw GeminiFailure.Offline(failure)
            } catch (failure: Exception) {
                if (failure.message?.contains("Unable to resolve host", true) == true ||
                    failure.message?.contains("network", true) == true) {
                    throw GeminiFailure.Offline(failure)
                }
                throw GeminiFailure.Http(failure.message ?: "Gemini request failed.")
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(apiKey: String, modelId: String, systemPrompt: String): String =
        withContext(Dispatchers.IO) {
            val chunks = StringBuilder()
            streamReply(apiKey, modelId, systemPrompt, emptyList(), "Reply with the single word OK.")
                .collect { chunks.append(it) }
            chunks.toString().ifBlank { throw GeminiFailure.EmptyResponse() }
        }

    private fun requireCredentials(apiKey: String, modelId: String) {
        if (apiKey.isBlank()) throw GeminiFailure.MissingKey()
        if (modelId.isBlank()) throw GeminiFailure.MissingModel()
    }

    private fun endpoint(modelId: String): String {
        val clean = modelId.trim().removePrefix("models/")
        require(Regex("^[A-Za-z0-9._-]+$").matches(clean)) {
            "Model ID must be a model name such as the IDs listed in Google's Gemini API model catalog."
        }
        return "https://generativelanguage.googleapis.com/v1beta/models/$clean:streamGenerateContent?alt=sse"
    }

    private fun estimateTokens(text: String): Int = (text.length + 3) / 4 + 4

    private fun mapHttpFailure(code: Int, body: String): GeminiFailure {
        val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }
            .getOrNull().orEmpty()
        return when (code) {
            401, 403 -> GeminiFailure.Unauthorized()
            429 -> GeminiFailure.RateLimited()
            else -> GeminiFailure.Http(message.ifBlank { "Gemini API error (HTTP $code)." })
        }
    }

    private companion object {
        val RETRYABLE_STATUS_CODES = setOf(429, 500, 503)
        val RETRY_DELAYS_MS = longArrayOf(700L, 1_600L)
        const val MAX_RETRIES = 2
        const val HISTORY_TOKEN_BUDGET = 12_000
    }
}
