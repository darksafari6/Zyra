package com.safari.zyra.core.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("DEPRECATION")
@Singleton
class EncryptedApiKeyStore @Inject constructor(
    @ApplicationContext context: Context
) : ApiKeyStore {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "zyra_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val mutableHasKey = MutableStateFlow(preferences.getString(KEY_GEMINI, "").orEmpty().isNotBlank())
    override val hasKey = mutableHasKey.asStateFlow()

    override fun read(): String = preferences.getString(KEY_GEMINI, "").orEmpty()
    override fun write(value: String) {
        val clean = value.trim()
        if (clean.isNotBlank()) {
            preferences.edit().putString(KEY_GEMINI, clean).apply()
            mutableHasKey.value = true
        }
    }
    override fun clear() {
        preferences.edit().remove(KEY_GEMINI).apply()
        mutableHasKey.value = false
    }

    private companion object { const val KEY_GEMINI = "gemini_api_key" }
}