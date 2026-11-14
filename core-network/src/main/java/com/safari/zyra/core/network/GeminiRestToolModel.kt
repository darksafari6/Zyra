package com.safari.zyra.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject

class GeminiRestToolModel @Inject constructor(private val http: HttpClient) {
    suspend fun streamGenerate(
        apiKey: String, modelId: String, systemPrompt: String,
        contents: List<GeminiToolTurn>, declarations: List<GeminiFunctionDeclaration>, googleSearchGrounding: Boolean = false
    ): Flow<List<GeminiPart>> = flow {
        require(apiKey.isNotBlank()) { "Add your Gemini API key in Settings." }
        val cleanModel = modelId.trim().removePrefix("models/")
        require(Regex("^[A-Za-z0-9._-]+$").matches(cleanModel)) { "Invalid Gemini model ID." }
        val contentJson = JSONArray()
        contents.forEach { turn ->
            val parts = JSONArray()
            turn.parts.forEach { part ->
                when (part) {
                    is GeminiPart.Text -> {
                        val parsed = runCatching { JSONObject(part.value) }.getOrNull()
                        if (parsed?.has("functionResponse") == true) parts.put(parsed)
                        else parts.put(JSONObject().put("text", part.value))
                    }
                    is GeminiPart.FunctionCall -> parts.put(JSONObject().put("functionCall", JSONObject()
                        .put("name", part.value.name).put("args", part.value.arguments)
                        .apply { part.value.id?.let { put("id", it) } }))
                }
            }
            contentJson.put(JSONObject().put("role", turn.role).put("parts", parts))
        }
        val functionDeclarations = JSONArray()
        declarations.forEach { declaration ->
            functionDeclarations.put(JSONObject().put("name", declaration.name)
                .put("description", declaration.description).put("parameters", declaration.parameters))
        }
        val tools = JSONArray()
        if (functionDeclarations.length() > 0) tools.put(JSONObject().put("functionDeclarations", functionDeclarations))
        if (googleSearchGrounding) tools.put(JSONObject().put("google_search", JSONObject()))
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            .put("contents", contentJson)
            .put("tools", tools)
        val response = http.post("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:streamGenerateContent?alt=sse") {
            header("x-goog-api-key", apiKey)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body.toString())
        }
        if (response.status != HttpStatusCode.OK) {
            val error = response.bodyAsChannel().readUTF8Line().orEmpty()
            error("Gemini function-calling request failed (HTTP ${response.status.value}): ${error.take(400)}")
        }
        val channel = response.bodyAsChannel()
        val collected = mutableListOf<GeminiPart>()
        val groundingSources = linkedSetOf<Pair<String, String>>()
        while (true) {
            val line = channel.readUTF8Line() ?: break
            if (!line.startsWith("data:")) continue
            val payload = line.removePrefix("data:").trim()
            if (payload.isBlank() || payload == "[DONE]") continue
            val root = JSONObject(payload)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
            val chunks = candidate?.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
            if (chunks != null) for (i in 0 until chunks.length()) {
                val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
                val uri = web.optString("uri")
                val title = web.optString("title").ifBlank { uri }
                if (uri.startsWith("https://")) groundingSources += title to uri
            }
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts") ?: continue
            for (i in 0 until parts.length()) {
                val item = parts.optJSONObject(i) ?: continue
                item.optString("text").takeIf { it.isNotBlank() }?.let { collected += GeminiPart.Text(it) }
                item.optJSONObject("functionCall")?.let { function ->
                    collected += GeminiPart.FunctionCall(GeminiFunctionCall(
                        function.optString("id").takeIf { it.isNotBlank() },
                        function.optString("name"),
                        function.optJSONObject("args") ?: JSONObject()
                    ))
                }
            }
        }
        if (groundingSources.isNotEmpty()) {
            val sources = groundingSources.joinToString("\n") { pair -> "- " + pair.first + ": " + pair.second }
            collected += GeminiPart.Text("\n\nSources (Google Search grounding):\n" + sources)
        }
        emit(collected)
    }.flowOn(Dispatchers.IO)
}
