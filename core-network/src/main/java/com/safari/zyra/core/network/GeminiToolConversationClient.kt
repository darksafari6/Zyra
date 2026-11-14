package com.safari.zyra.core.network

import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@Singleton
class GeminiToolConversationClient @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
    private val settings: SettingsRepository,
    private val model: GeminiRestToolModel,
    private val executor: GeminiToolExecutor
) {
    fun streamReply(
        history: List<GeminiTurn>,
        userText: String,
        declarations: List<GeminiFunctionDeclaration>,
        systemPromptOverride: String? = null
    ): Flow<String> = flow {
        val config = settings.settings.first()
        val key = apiKeyStore.read()
        val turns = history.map {
            GeminiToolTurn(
                if (it.role == "assistant" || it.role == "model") "model" else "user",
                listOf(GeminiPart.Text(it.text))
            )
        }.toMutableList()
        turns += GeminiToolTurn("user", listOf(GeminiPart.Text(userText)))
        val loop = GeminiToolLoop(object : GeminiToolModel {
            override fun generate(
                contents: List<GeminiToolTurn>,
                declarations: List<GeminiFunctionDeclaration>
            ): Flow<GeminiPart> = model.streamGenerate(
                key,
                config.textModelId,
                systemPromptOverride ?: config.systemPrompt,
                contents,
                declarations,
                config.googleSearchGrounding
            )
        }, executor)
        loop.stream(turns, declarations).collect { emit(it) }
    }
}
