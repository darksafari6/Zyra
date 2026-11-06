package com.safari.zyra.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.safari.zyra.core.data.settings.ZyraSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zyraPreferences: DataStore<Preferences> by preferencesDataStore(name = "zyra_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    private val reduceEffectsKey = booleanPreferencesKey("reduce_effects")
    private val textModelKey = stringPreferencesKey("text_model_id")
    private val liveModelKey = stringPreferencesKey("live_model_id")
    private val systemPromptKey = stringPreferencesKey("system_prompt")

    override val reduceEffects: Flow<Boolean> =
        context.zyraPreferences.data.map { it[reduceEffectsKey] ?: false }

    override val settings: Flow<ZyraSettings> = context.zyraPreferences.data.map { prefs ->
        ZyraSettings(
            textModelId = prefs[textModelKey].orEmpty(),
            liveModelId = prefs[liveModelKey].orEmpty(),
            systemPrompt = prefs[systemPromptKey] ?: ZyraSettings.DEFAULT_SYSTEM_PROMPT
        )
    }

    override suspend fun setReduceEffects(enabled: Boolean) {
        context.zyraPreferences.edit { it[reduceEffectsKey] = enabled }
    }

    override suspend fun updateTextModelId(value: String) {
        context.zyraPreferences.edit { it[textModelKey] = value.trim() }
    }

    override suspend fun updateLiveModelId(value: String) {
        context.zyraPreferences.edit { it[liveModelKey] = value.trim() }
    }

    override suspend fun updateSystemPrompt(value: String) {
        context.zyraPreferences.edit { it[systemPromptKey] = value }
    }
}