package com.safari.zyra.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.json.JSONObject
import androidx.datastore.preferences.preferencesDataStore
import com.safari.zyra.core.data.settings.ZyraSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zyraPreferences by preferencesDataStore(name = "zyra_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    private val reduceEffectsKey = booleanPreferencesKey("reduce_effects")
    private val onboardingKey = booleanPreferencesKey("onboarding_completed")
    private val textModelKey = stringPreferencesKey("text_model_id")
    private val liveModelKey = stringPreferencesKey("live_model_id")
    private val systemPromptKey = stringPreferencesKey("system_prompt")
    private val personaKey = stringPreferencesKey("persona")
    private val customPersonaKey = stringPreferencesKey("custom_persona")
    private val voiceLanguageKey = stringPreferencesKey("voice_language")
    private val voiceNameKey = stringPreferencesKey("voice_name")
    private val voiceSpeedKey = floatPreferencesKey("voice_speed")
    private val voicePitchKey = floatPreferencesKey("voice_pitch")
    private val pureBlackKey = booleanPreferencesKey("pure_black")
    private val autoTitleKey = booleanPreferencesKey("auto_title")
    private val toolsEnabledKey = booleanPreferencesKey("tools_enabled")
    private val toolTogglesKey = stringPreferencesKey("tool_toggles")
    private val memoryEnabledKey = booleanPreferencesKey("memory_enabled")
    private val memoryConsentKey = booleanPreferencesKey("memory_consent_asked")
    private val googleSearchKey = booleanPreferencesKey("google_search_grounding")
    private val directCallsKey = booleanPreferencesKey("direct_calls_enabled")
    private val listenForStopKey = booleanPreferencesKey("listen_for_stop_while_speaking")

    override val reduceEffects: Flow<Boolean> = context.zyraPreferences.data.map { it[reduceEffectsKey] ?: false }
    override val onboardingCompleted: Flow<Boolean> = context.zyraPreferences.data.map { it[onboardingKey] ?: false }

    override val settings: Flow<ZyraSettings> = context.zyraPreferences.data.map { prefs ->
        ZyraSettings(
            textModelId = prefs[textModelKey].orEmpty(),
            liveModelId = prefs[liveModelKey].orEmpty(),
            systemPrompt = prefs[systemPromptKey] ?: ZyraSettings.DEFAULT_SYSTEM_PROMPT,
            persona = prefs[personaKey] ?: "Friendly",
            customPersona = prefs[customPersonaKey].orEmpty(),
            voiceLanguage = prefs[voiceLanguageKey] ?: "en-US",
            voiceName = prefs[voiceNameKey].orEmpty(),
            voiceSpeed = prefs[voiceSpeedKey] ?: 1f,
            voicePitch = prefs[voicePitchKey] ?: 1f,
            pureBlack = prefs[pureBlackKey] ?: false,
            autoTitle = prefs[autoTitleKey] ?: false,
            toolsEnabled = prefs[toolsEnabledKey] ?: false,
            toolToggles = runCatching {
                val json = JSONObject(prefs[toolTogglesKey].orEmpty())
                buildMap { json.keys().forEach { key -> put(key, json.optBoolean(key, false)) } }
            }.getOrDefault(emptyMap()),
            memoryEnabled = prefs[memoryEnabledKey] ?: false,
            memoryConsentAsked = prefs[memoryConsentKey] ?: false,
            googleSearchGrounding = prefs[googleSearchKey] ?: false,
            directCallsEnabled = prefs[directCallsKey] ?: false,
            listenForStopWhileSpeaking = prefs[listenForStopKey] ?: false
        )
    }

    override suspend fun setReduceEffects(enabled: Boolean) { context.zyraPreferences.edit { it[reduceEffectsKey] = enabled } }
    override suspend fun setOnboardingCompleted(completed: Boolean) { context.zyraPreferences.edit { it[onboardingKey] = completed } }
    override suspend fun updateTextModelId(value: String) { context.zyraPreferences.edit { it[textModelKey] = value.trim() } }
    override suspend fun updateLiveModelId(value: String) { context.zyraPreferences.edit { it[liveModelKey] = value.trim() } }
    override suspend fun updateSystemPrompt(value: String) { context.zyraPreferences.edit { it[systemPromptKey] = value } }
    override suspend fun updatePersona(preset: String, custom: String) {
        context.zyraPreferences.edit { it[personaKey] = preset; it[customPersonaKey] = custom }
    }
    override suspend fun updateVoiceSettings(language: String, speed: Float, pitch: Float, voiceName: String) {
        context.zyraPreferences.edit {
            it[voiceLanguageKey] = language.trim().ifBlank { "en-US" }
            it[voiceNameKey] = voiceName
            it[voiceSpeedKey] = speed.coerceIn(0.65f, 1.5f)
            it[voicePitchKey] = pitch.coerceIn(0.7f, 1.4f)
        }
    }
    override suspend fun setPureBlack(enabled: Boolean) { context.zyraPreferences.edit { it[pureBlackKey] = enabled } }
    override suspend fun setAutoTitle(enabled: Boolean) { context.zyraPreferences.edit { it[autoTitleKey] = enabled } }
    override suspend fun setToolsEnabled(enabled: Boolean) { context.zyraPreferences.edit { it[toolsEnabledKey] = enabled } }
    override suspend fun setToolEnabled(name: String, enabled: Boolean) {
        context.zyraPreferences.edit { prefs ->
            val json = runCatching { JSONObject(prefs[toolTogglesKey].orEmpty()) }.getOrDefault(JSONObject())
            json.put(name, enabled)
            prefs[toolTogglesKey] = json.toString()
        }
    }
    override suspend fun setMemoryConsent(accepted: Boolean) {
        context.zyraPreferences.edit { prefs -> prefs[memoryConsentKey] = true; prefs[memoryEnabledKey] = accepted }
    }
    override suspend fun setMemoryEnabled(enabled: Boolean) {
        context.zyraPreferences.edit { prefs -> if (prefs[memoryConsentKey] == true) prefs[memoryEnabledKey] = enabled }
    }
    override suspend fun setGoogleSearchGrounding(enabled: Boolean) { context.zyraPreferences.edit { it[googleSearchKey] = enabled } }
    override suspend fun setDirectCallsEnabled(enabled: Boolean) { context.zyraPreferences.edit { it[directCallsKey] = enabled } }
    override suspend fun setListenForStopWhileSpeaking(enabled: Boolean) { context.zyraPreferences.edit { it[listenForStopKey] = enabled } }
}
