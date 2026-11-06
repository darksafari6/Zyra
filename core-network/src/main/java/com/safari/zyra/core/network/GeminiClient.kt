package com.safari.zyra.core.network

import kotlinx.coroutines.flow.Flow

data class GeminiTurn(val role: String, val text: String)

data class GeminiImage(val bytes: ByteArray, val mimeType: String)

sealed class GeminiFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class MissingKey : GeminiFailure("Add your Gemini API key in Settings.")
    class MissingModel : GeminiFailure("Enter a text model ID in Settings.")
    class Unauthorized : GeminiFailure("Gemini rejected this API key. Check the key and API access.")
    class RateLimited : GeminiFailure("Gemini rate limit or quota reached. Try again later.")
    class Offline(cause: Throwable? = null) : GeminiFailure("No network connection. Check your internet and retry.", cause)
    class Http(message: String) : GeminiFailure(message)
    class EmptyResponse : GeminiFailure("Gemini returned no text. Try again.")
}

interface GeminiClient {
    fun streamReply(
        apiKey: String,
        modelId: String,
        systemPrompt: String,
        history: List<GeminiTurn>,
        userText: String,
        image: GeminiImage? = null
    ): Flow<String>

    suspend fun testConnection(apiKey: String, modelId: String, systemPrompt: String): String
}