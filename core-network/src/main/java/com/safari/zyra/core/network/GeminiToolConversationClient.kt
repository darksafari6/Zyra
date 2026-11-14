package com.safari.zyra.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

@Singleton
class GeminiToolConversationClient @Inject constructor(
    private val apiKeyStore: com.safari.zyra.core.data.security.ApiKeyStore,
    private val settings: com.safari.zyra.core.data.SettingsRepository,
    private val model: GeminiRestToolModel,
    private val executor: GeminiToolExecutor
) {
    fun streamReply(history: List<GeminiTurn>, userText: String, declarations: List<GeminiFunctionDeclaration>): Flow<String> =
        kotlinx.coroutines.flow.flow {
            val config = settings.settings.first()
            val key = apiKeyStore.read()
            val turns = history.map { GeminiToolTurn(if (it.role == "assistant" || it.role == "model") "model" else "user", listOf(GeminiPart.Text(it.text))) }.toMutableList()
            turns += GeminiToolTurn("user", listOf(GeminiPart.Text(userText)))
            val loop = GeminiToolLoop(object : GeminiToolModel {
                override suspend fun generate(contents: List<GeminiToolTurn>, declarations: List<GeminiFunctionDeclaration>): List<GeminiPart> {
                    var result: List<GeminiPart> = emptyList()
                    model.streamGenerate(key, config.textModelId, config.systemPrompt, contents, declarations).collect { result = it }
                    return result
                }
            }, executor)
            loop.stream(turns, declarations).collect { emit(it) }
        }
}
