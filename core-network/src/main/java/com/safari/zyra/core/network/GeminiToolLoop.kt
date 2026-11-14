package com.safari.zyra.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeminiFunctionDeclaration(val name: String, val description: String, val parameters: JSONObject)
data class GeminiFunctionCall(val id: String?, val name: String, val arguments: JSONObject)
sealed interface GeminiPart {
    data class Text(val value: String) : GeminiPart
    data class FunctionCall(val value: GeminiFunctionCall) : GeminiPart
}
data class GeminiToolTurn(val role: String, val parts: List<GeminiPart>)
data class GeminiToolExecution(val status: String, val detail: String)
interface GeminiToolModel {
    fun generate(contents: List<GeminiToolTurn>, declarations: List<GeminiFunctionDeclaration>): Flow<GeminiPart>
}
fun interface GeminiToolExecutor {
    suspend fun execute(name: String, arguments: JSONObject): GeminiToolExecution
}

class GeminiToolLoop(
    private val model: GeminiToolModel,
    private val executor: GeminiToolExecutor,
    private val maxIterations: Int = 4,
    private val callTimeoutMillis: Long = 12_000
) {
    fun stream(initial: List<GeminiToolTurn>, declarations: List<GeminiFunctionDeclaration>): Flow<String> = flow {
        val history = initial.toMutableList()
        val limit = maxIterations.coerceIn(1, 8)
        repeat(limit) { iteration ->
            val parts = mutableListOf<GeminiPart>()
            withTimeout(callTimeoutMillis) {
                model.generate(history.toList(), declarations).collect { part ->
                    parts += part
                    if (part is GeminiPart.Text) emit(part.value)
                }
            }
            val calls = parts.filterIsInstance<GeminiPart.FunctionCall>()
            if (calls.isEmpty()) return@flow
            if (iteration == limit - 1) {
                emit("I stopped because the tool-call limit was reached. No additional actions were run.")
                return@flow
            }
            history += GeminiToolTurn("model", parts)
            val responses = calls.map { call ->
                val result = withTimeout(callTimeoutMillis) { executor.execute(call.value.name, call.value.arguments) }
                val response = JSONObject().put("status", result.status).put("detail", result.detail)
                val functionResponse = JSONObject().put("functionResponse", JSONObject()
                    .put("name", call.value.name)
                    .put("response", response)
                    .apply { call.value.id?.let { put("id", it) } })
                GeminiPart.Text(functionResponse.toString())
            }
            history += GeminiToolTurn("user", responses)
        }
    }
}
