package com.safari.zyra.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.chat.ChatMessage
import com.safari.zyra.core.data.chat.ChatRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.data.settings.ZyraSettings
import com.safari.zyra.core.network.GeminiModel
import com.safari.zyra.core.network.GeminiModelCatalog
import com.safari.zyra.core.network.GeminiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: ZyraSettings = ZyraSettings(),
    val hasApiKey: Boolean = false,
    val reduceEffects: Boolean = false,
    val models: List<GeminiModel> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val loadingModels: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val testResult: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val keyStore: ApiKeyStore,
    private val catalog: GeminiModelCatalog,
    private val geminiClient: GeminiClient,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { settingsRepository.settings.collect { value -> mutableState.update { it.copy(settings = value) } } }
        viewModelScope.launch { settingsRepository.reduceEffects.collect { value -> mutableState.update { it.copy(reduceEffects = value) } } }
        viewModelScope.launch { keyStore.hasKey.collect { value -> mutableState.update { it.copy(hasApiKey = value) } } }
        viewModelScope.launch { chatRepository.allMessages.collect { value -> mutableState.update { it.copy(messages = value) } } }
    }

    fun loadModels() {
        if (mutableState.value.loadingModels) return
        viewModelScope.launch {
            mutableState.update { it.copy(loadingModels = true, error = null) }
            try {
                val result = catalog.list(keyStore.read())
                mutableState.update { it.copy(models = result, loadingModels = false) }
            } catch (failure: Exception) {
                mutableState.update { it.copy(loadingModels = false, error = failure.message ?: "Could not load models.") }
            }
        }
    }

    fun testConnection() {
        val current = mutableState.value
        if (!current.hasApiKey || current.settings.textModelId.isBlank()) {
            mutableState.update { it.copy(error = "Save an API key and choose a text model first.") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(testResult = "Testing…", error = null) }
            val result = runCatching { geminiClient.testConnection(keyStore.read(), current.settings.textModelId, current.settings.systemPrompt) }
            mutableState.update { it.copy(testResult = result.fold({ "Connected: ${it.trim()}" }, { it.message ?: "Connection failed." })) }
        }
    }

    fun saveKey(value: String) {
        try { keyStore.write(value); mutableState.update { it.copy(error = null, saved = true) } }
        catch (failure: Exception) { mutableState.update { it.copy(error = failure.message, saved = false) } }
    }
    fun deleteKey() { keyStore.clear(); mutableState.update { it.copy(hasApiKey = false, saved = true) } }

    fun save(value: ZyraSettings, reduceEffects: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.updateTextModelId(value.textModelId)
            settingsRepository.updateLiveModelId(value.liveModelId)
            settingsRepository.updateSystemPrompt(value.systemPrompt)
            settingsRepository.updatePersona(value.persona, value.customPersona)
            settingsRepository.updateVoiceSettings(value.voiceLanguage, value.voiceSpeed, value.voicePitch, value.voiceName)
            settingsRepository.setReduceEffects(reduceEffects)
            settingsRepository.setPureBlack(value.pureBlack)
            settingsRepository.setAutoTitle(value.autoTitle)
            settingsRepository.setToolsEnabled(value.toolsEnabled)
            value.toolToggles.forEach { (name, enabled) -> settingsRepository.setToolEnabled(name, enabled) }
            settingsRepository.setMemoryEnabled(value.memoryEnabled)
            settingsRepository.setGoogleSearchGrounding(value.googleSearchGrounding)
            mutableState.update { it.copy(saved = true, error = null) }
            onComplete()
        }
    }

    fun clearHistory() { viewModelScope.launch { chatRepository.clearAll(); File(context.filesDir, "zyra_images").deleteRecursively(); mutableState.update { it.copy(messages = emptyList()) } } }
    fun completeOnboardingAgain() { viewModelScope.launch { settingsRepository.setOnboardingCompleted(false) } }
    fun setMemoryConsent(accepted: Boolean) { viewModelScope.launch { settingsRepository.setMemoryConsent(accepted) } }
    fun setMemoryEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.setMemoryEnabled(enabled) } }
    fun setToolEnabled(name: String, enabled: Boolean) { viewModelScope.launch { settingsRepository.setToolEnabled(name, enabled) } }
    fun setToolsEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.setToolsEnabled(enabled) } }
    fun setGoogleSearchGrounding(enabled: Boolean) { viewModelScope.launch { settingsRepository.setGoogleSearchGrounding(enabled) } }

    fun exportMarkdown(): String = buildString {
        appendLine("# Zyra chat export")
        mutableState.value.messages.forEach { message ->
            appendLine()
            appendLine("## ${message.role.replaceFirstChar(Char::uppercase)}")
            appendLine(message.text)
            if (message.imagePath != null) appendLine("[Image attachment stored locally]")
        }
    }

    fun exportJson(): String = org.json.JSONArray().apply {
        mutableState.value.messages.forEach { message ->
            put(org.json.JSONObject().put("id", message.id).put("role", message.role)
                .put("text", message.text).put("timestamp", message.timestamp)
                .put("imageAttached", message.imagePath != null))
        }
    }.toString(2)
}
