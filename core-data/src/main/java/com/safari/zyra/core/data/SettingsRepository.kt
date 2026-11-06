package com.safari.zyra.core.data

import com.safari.zyra.core.data.settings.ZyraSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val reduceEffects: Flow<Boolean>
    val settings: Flow<ZyraSettings>
    suspend fun setReduceEffects(enabled: Boolean)
    suspend fun updateTextModelId(value: String)
    suspend fun updateLiveModelId(value: String)
    suspend fun updateSystemPrompt(value: String)
}