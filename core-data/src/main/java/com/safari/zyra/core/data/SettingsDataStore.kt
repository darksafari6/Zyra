package com.safari.zyra.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
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
    override val reduceEffects: Flow<Boolean> =
        context.zyraPreferences.data.map { it[reduceEffectsKey] ?: false }

    override suspend fun setReduceEffects(enabled: Boolean) {
        context.zyraPreferences.edit { it[reduceEffectsKey] = enabled }
    }
}
