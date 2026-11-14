package com.safari.zyra.core.network

import kotlinx.coroutines.flow.Flow
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
    suspend fun generate(contents: List<GeminiToolTurn>, declarations: List<GeminiFunctionDeclaration>): List<GeminiPart>
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
        repeat(maxIterations + 1) { iteration ->
            val parts = withTimeout(callTimeoutMillis) { model.generate(history.toList(), declarations) }
            val calls = parts.filterIsInstance<GeminiPart.FunctionCall>()
            if (calls.isEmpty()) {
                parts.filterIsInstance<GeminiPart.Text>().forEach { emit(it.value) }
                return@flow
            }
            history += GeminiToolTurn("model", parts)
            val responses = calls.map { call ->
                val result = withTimeout(callTimeoutMillis) { executor.execute(call.value.name, call.value.arguments) }
                val payload = JSONObject().put("status", result.status).put("detail", result.detail)
                GeminiPart.Text(JSONObject().put("functionResponse", JSONObject()
                    .put("name", call.value.name)
                    .put("response", payload)
                    .apply { call.value.id?.let { put("id", it) } }).toString())
            }
            history += GeminiToolTurn("user", responses)
            if (iteration == maxIterations) emit("I stopped because the tool-call limit was reached. No further actions were run.")
        }
    }
}
