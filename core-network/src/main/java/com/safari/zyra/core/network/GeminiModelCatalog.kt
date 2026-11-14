package com.safari.zyra.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import javax.inject.Inject
import org.json.JSONObject

enum class GeminiModelCapability { TEXT, LIVE }

data class GeminiModel(
    val id: String,
    val displayName: String,
    val inputTokenLimit: Int?,
    val outputTokenLimit: Int?,
    val capabilities: Set<GeminiModelCapability>
)

class GeminiModelCatalog @Inject constructor(private val http: HttpClient) {
    suspend fun list(apiKey: String): List<GeminiModel> {
        require(apiKey.isNotBlank()) { "Save a Gemini API key before loading models." }
        val found = linkedMapOf<String, GeminiModel>()
        var pageToken: String? = null
        do {
            val response = http.get("$BASE_URL/models") {
                header("x-goog-api-key", apiKey.trim())
                parameter("pageSize", PAGE_SIZE)
                pageToken?.let { parameter("pageToken", it) }
            }
            if (response.status.value !in 200..299) {
                throw GeminiFailure.Http("Could not load Gemini models (HTTP ${response.status.value}). Check the API key and network.")
            }
            val root = JSONObject(response.bodyAsText())
            val models = root.optJSONArray("models")
            if (models != null) for (index in 0 until models.length()) {
                val item = models.optJSONObject(index) ?: continue
                val methods = item.optJSONArray("supportedGenerationMethods") ?: continue
                val supported = buildSet {
                    for (methodIndex in 0 until methods.length()) add(methods.optString(methodIndex))
                }
                val capabilities = buildSet {
                    if ("generateContent" in supported) add(GeminiModelCapability.TEXT)
                    if ("bidiGenerateContent" in supported) add(GeminiModelCapability.LIVE)
                }
                if (capabilities.isEmpty()) continue
                val name = item.optString("name").removePrefix("models/").takeIf(String::isNotBlank) ?: continue
                found[name] = GeminiModel(
                    id = name,
                    displayName = item.optString("displayName").ifBlank { name },
                    inputTokenLimit = item.optInt("inputTokenLimit").takeIf { item.has("inputTokenLimit") },
                    outputTokenLimit = item.optInt("outputTokenLimit").takeIf { item.has("outputTokenLimit") },
                    capabilities = capabilities
                )
            }
            pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() && it != "null" }
        } while (pageToken != null)
        return found.values.sortedBy { it.displayName.lowercase() }
    }

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        const val PAGE_SIZE = 100
    }
}
