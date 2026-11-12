package com.safari.zyra.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.network.GeminiModel
import com.safari.zyra.core.network.GeminiModelCatalog
import com.safari.zyra.core.network.GeminiModelCapability
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val models: List<GeminiModel> = emptyList(),
    val selectedTextModel: String = "",
    val selectedLiveModel: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val keyStore: ApiKeyStore,
    private val catalog: GeminiModelCatalog
) : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

    fun validateAndLoadModels(key: String) {
        if (key.isBlank() || mutableState.value.loading) return
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val models = catalog.list(key)
                require(models.any { GeminiModelCapability.TEXT in it.capabilities }) {
                    "This key has no models supporting text generation."
                }
                keyStore.write(key)
                mutableState.update { current ->
                    val firstText = models.firstOrNull { GeminiModelCapability.TEXT in it.capabilities }?.id.orEmpty()
                    val firstLive = models.firstOrNull { GeminiModelCapability.LIVE in it.capabilities }?.id.orEmpty()
                    current.copy(models = models, selectedTextModel = current.selectedTextModel.ifBlank { firstText },
                        selectedLiveModel = current.selectedLiveModel.ifBlank { firstLive }, loading = false)
                }
            } catch (failure: Exception) {
                mutableState.update { it.copy(loading = false, error = failure.message ?: "Could not validate this Gemini key.") }
            }
        }
    }

    fun selectTextModel(id: String) = mutableState.update { it.copy(selectedTextModel = id) }
    fun selectLiveModel(id: String) = mutableState.update { it.copy(selectedLiveModel = id) }

    fun finish(language: String, onComplete: () -> Unit) {
        val current = mutableState.value
        if (current.selectedTextModel.isBlank()) {
            mutableState.update { it.copy(error = "Choose a text model before continuing.") }
            return
        }
        viewModelScope.launch {
            settings.updateTextModelId(current.selectedTextModel)
            settings.updateLiveModelId(current.selectedLiveModel)
            settings.updateVoiceSettings(language, 1f, 1f)
            settings.setOnboardingCompleted(true)
            onComplete()
        }
    }
}
