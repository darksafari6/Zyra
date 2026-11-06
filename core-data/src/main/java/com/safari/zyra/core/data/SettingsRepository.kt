package com.safari.zyra.core.data
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val reduceEffects: Flow<Boolean>
    suspend fun setReduceEffects(enabled: Boolean)
}
