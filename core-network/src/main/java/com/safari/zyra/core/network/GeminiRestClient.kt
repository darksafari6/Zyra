package com.safari.zyra.core.network

import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.request.contentType
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readLine
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GeminiRestClient @Inject constructor(
    private val http: HttpClient
) : GeminiClient {

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
        history.takeLast(20).forEach { turn ->
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
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            .put("contents", contents)

        try {
            val response = http.post(endpoint(modelId)) {
                header("x-goog-api-key", apiKey.trim())
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            if (response.status != HttpStatusCode.OK) {
                val errorChannel = response.bodyAsChannel()
                val errorBody = buildString { while (true) append(errorChannel.readLine() ?: break) }
                throw mapHttpFailure(response.status.value, errorBody)
            }
            val channel = response.bodyAsChannel()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isBlank() || payload == "[DONE]") continue
                val chunk = runCatching {
                    val root = JSONObject(payload)
                    val parts = root.optJSONArray("candidates")
                        ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    buildString {
                        if (parts != null) for (index in 0 until parts.length()) {
                            append(parts.optJSONObject(index)?.optString("text").orEmpty())
                        }
                    }
                }.getOrDefault("")
                if (chunk.isNotEmpty()) emit(chunk)
            }
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
        require(clean.isNotBlank() && '/' !in clean && '?' !in clean && '#' !in clean) {
            "Model ID must be a model name such as the IDs listed in Google's Gemini API model catalog."
        }
        return "https://generativelanguage.googleapis.com/v1beta/models/$clean:streamGenerateContent?alt=sse"
    }

    private fun mapHttpFailure(code: Int, body: String): GeminiFailure {
        val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }
            .getOrNull().orEmpty()
        return when (code) {
            401, 403 -> GeminiFailure.Unauthorized()
            429 -> GeminiFailure.RateLimited()
            else -> GeminiFailure.Http(message.ifBlank { "Gemini API error (HTTP $code)." })
        }
    }
}