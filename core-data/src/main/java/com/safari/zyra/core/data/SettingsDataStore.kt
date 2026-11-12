package com.safari.zyra.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val voiceSpeedKey = floatPreferencesKey("voice_speed")
    private val voicePitchKey = floatPreferencesKey("voice_pitch")
    private val pureBlackKey = booleanPreferencesKey("pure_black")
    private val autoTitleKey = booleanPreferencesKey("auto_title")

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
            voiceSpeed = prefs[voiceSpeedKey] ?: 1f,
            voicePitch = prefs[voicePitchKey] ?: 1f,
            pureBlack = prefs[pureBlackKey] ?: false,
            autoTitle = prefs[autoTitleKey] ?: false
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
    override suspend fun updateVoiceSettings(language: String, speed: Float, pitch: Float) {
        context.zyraPreferences.edit {
            it[voiceLanguageKey] = language.trim().ifBlank { "en-US" }
            it[voiceSpeedKey] = speed.coerceIn(0.65f, 1.5f)
            it[voicePitchKey] = pitch.coerceIn(0.7f, 1.4f)
        }
    }
    override suspend fun setPureBlack(enabled: Boolean) { context.zyraPreferences.edit { it[pureBlackKey] = enabled } }
    override suspend fun setAutoTitle(enabled: Boolean) { context.zyraPreferences.edit { it[autoTitleKey] = enabled } }
}
