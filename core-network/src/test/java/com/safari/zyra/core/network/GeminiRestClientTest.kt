package com.safari.zyra.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiRestClientTest {
    @Test
    fun parsesServerSentEventTextChunks() = runBlocking {
        val payload = """data: {"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}\n\ndata: {"candidates":[{"content":{"parts":[{"text":" world"}]}}]}\n\n"""
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(payload),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/event-stream")
            )
        }
        val http = HttpClient(engine)
        try {
            val chunks = GeminiRestClient(http).streamReply(
                apiKey = "unit-test-key",
                modelId = "test-model",
                systemPrompt = "Test",
                history = emptyList(),
                userText = "Hello?"
            ).toList()
            assertEquals(listOf("Hello", " world"), chunks)
        } finally {
            http.close()
        }
    }
}
