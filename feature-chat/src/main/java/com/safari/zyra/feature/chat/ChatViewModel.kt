package com.safari.zyra.feature.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.chat.ChatMessage
import com.safari.zyra.core.data.chat.ChatRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.network.GeminiClient
import com.safari.zyra.core.network.GeminiImage
import com.safari.zyra.core.network.GeminiTurn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val apiKeyStore: ApiKeyStore,
    private val geminiClient: GeminiClient
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.messages.collect { messages -> mutableState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings -> mutableState.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch {
            apiKeyStore.hasKey.collect { hasKey -> mutableState.update { it.copy(hasApiKey = hasKey) } }
        }
    }

    fun sendMessage(text: String, imageUri: Uri? = null) {
        val prompt = text.trim().ifBlank { if (imageUri != null) "Please describe and analyze this image." else "" }
        if (prompt.isBlank() || mutableState.value.isStreaming) return
        viewModelScope.launch {
            mutableState.update { it.copy(isStreaming = true, streamingText = "", error = null) }
            try {
                var image: GeminiImage? = null
                var storedImagePath: String? = null
                if (imageUri != null) {
                    val mime = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                    val directory = File(context.filesDir, "zyra_images").apply { mkdirs() }
                    val target = File(directory, "image_${System.currentTimeMillis()}.bin")
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Could not read the selected image.")
                    storedImagePath = target.absolutePath
                    image = GeminiImage(target.readBytes(), mime)
                }
                chatRepository.add(ChatMessage(role = "user", text = prompt, imagePath = storedImagePath))
                val history = chatRepository.recentForPrompt(21).dropLast(1).map { GeminiTurn(it.role, it.text) }
                val response = StringBuilder()
                geminiClient.streamReply(
                    apiKey = apiKeyStore.read(),
                    modelId = mutableState.value.settings.textModelId,
                    systemPrompt = mutableState.value.settings.systemPrompt,
                    history = history,
                    userText = prompt,
                    image = image
                ).collect { chunk ->
                    response.append(chunk)
                    mutableState.update { it.copy(streamingText = response.toString()) }
                }
                val finalText = response.toString().trim()
                if (finalText.isBlank()) error("Zyra received an empty response. Please retry.")
                chatRepository.add(ChatMessage(role = "assistant", text = finalText))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.update { it.copy(error = failure.message ?: "Something went wrong.") }
            } finally {
                mutableState.update { it.copy(isStreaming = false, streamingText = "") }
            }
        }
    }

    fun saveApiKey(value: String) {
        if (value.isNotBlank()) apiKeyStore.write(value)
        mutableState.update { it.copy(hasApiKey = apiKeyStore.hasKey.value, testResult = null) }
    }
    fun clearApiKey() {
        apiKeyStore.clear()
        mutableState.update { it.copy(hasApiKey = false, testResult = null) }
    }
    fun saveSettings(textModelId: String, liveModelId: String, systemPrompt: String) {
        viewModelScope.launch {
            settingsRepository.updateTextModelId(textModelId)
            settingsRepository.updateLiveModelId(liveModelId)
            settingsRepository.updateSystemPrompt(systemPrompt)
            val saved = settingsRepository.settings.first()
            mutableState.update { it.copy(settings = saved) }
        }
    }

    fun testConnection(textModelId: String, liveModelId: String, systemPrompt: String) {
        if (mutableState.value.testingConnection) return
        viewModelScope.launch {
            mutableState.update { it.copy(testingConnection = true, testResult = null, error = null) }
            try {
                settingsRepository.updateTextModelId(textModelId)
                settingsRepository.updateLiveModelId(liveModelId)
                settingsRepository.updateSystemPrompt(systemPrompt)
                val saved = settingsRepository.settings.first()
                mutableState.update { it.copy(settings = saved) }
                val response = geminiClient.testConnection(
                    apiKeyStore.read(),
                    saved.textModelId,
                    saved.systemPrompt
                )
                mutableState.update { it.copy(testResult = "Connected successfully: ${response.trim()}") }
            } catch (failure: Exception) {
                mutableState.update { it.copy(testResult = failure.message ?: "Connection test failed.") }
            } finally {
                mutableState.update { it.copy(testingConnection = false) }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clear()
            File(context.filesDir, "zyra_images").deleteRecursively()
        }
    }
    fun dismissError() = mutableState.update { it.copy(error = null) }
}