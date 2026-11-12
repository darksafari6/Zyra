package com.safari.zyra.core.data.security

import android.content.Context
import android.content.SharedPreferences
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
    // Keystore can be unavailable or invalidated on some devices. Never crash app startup
    // and never silently fall back to storing the API key in plaintext.
    private val preferences: SharedPreferences? = runCatching {
        EncryptedSharedPreferences.create(
            context,
            "zyra_secrets",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    private val mutableHasKey = MutableStateFlow(
        runCatching { preferences?.getString(KEY_GEMINI, "").orEmpty().isNotBlank() }.getOrDefault(false)
    )
    override val hasKey = mutableHasKey.asStateFlow()

    override fun read(): String = runCatching {
        preferences?.getString(KEY_GEMINI, "").orEmpty()
    }.getOrElse {
        mutableHasKey.value = false
        ""
    }

    override fun write(value: String) {
        val clean = value.trim()
        if (clean.isBlank()) return
        val store = preferences ?: throw secureStorageUnavailable()
        try {
            check(store.edit().putString(KEY_GEMINI, clean).commit()) {
                "Android could not save the encrypted API key."
            }
            mutableHasKey.value = true
        } catch (failure: Exception) {
            mutableHasKey.value = false
            throw secureStorageUnavailable(failure)
        }
    }

    override fun clear() {
        runCatching { preferences?.edit()?.remove(KEY_GEMINI)?.commit() }
        mutableHasKey.value = false
    }

    private fun secureStorageUnavailable(cause: Throwable? = null) = IllegalStateException(
        "Android secure storage is unavailable on this device. Zyra did not save your key; check your device lock screen and try again.",
        cause
    )

    private companion object { const val KEY_GEMINI = "gemini_api_key" }
}
