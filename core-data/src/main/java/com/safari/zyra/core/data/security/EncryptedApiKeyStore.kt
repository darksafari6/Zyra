package com.safari.zyra.core.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private val Context.secretPreferences by preferencesDataStore(name = "zyra_secure_values")

@Singleton
class EncryptedApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) : ApiKeyStore {
    private val encryptedKey = stringPreferencesKey("gemini_api_key_wrapped")
    private val mutableHasKey = MutableStateFlow(runCatching { readStored().isNotBlank() }.getOrDefault(false))
    override val hasKey = mutableHasKey.asStateFlow()

    override fun read(): String = runCatching { readStored() }.getOrElse {
        mutableHasKey.value = false
        ""
    }

    override fun write(value: String) {
        val clean = value.trim()
        if (clean.isBlank()) return
        try {
            val encoded = encrypt(clean)
            runBlocking(Dispatchers.IO) { context.secretPreferences.edit { it[encryptedKey] = encoded } }
            mutableHasKey.value = true
        } catch (failure: Exception) {
            throw secureStorageUnavailable(failure)
        }
    }

    override fun clear() {
        runCatching {
            runBlocking(Dispatchers.IO) { context.secretPreferences.edit { it.remove(encryptedKey) } }
            legacyPreferences()?.edit()?.remove(KEY_GEMINI)?.commit()
        }
        mutableHasKey.value = false
    }

    private fun readStored(): String = runBlocking(Dispatchers.IO) {
        val stored = context.secretPreferences.data.let { flow ->
            kotlinx.coroutines.flow.first(flow)[encryptedKey].orEmpty()
        }
        if (stored.isNotBlank()) return@runBlocking decrypt(stored)
        val legacy = runCatching { legacyPreferences()?.getString(KEY_GEMINI, "").orEmpty() }.getOrDefault("")
        if (legacy.isBlank()) return@runBlocking ""
        val migrated = encrypt(legacy)
        context.secretPreferences.edit { it[encryptedKey] = migrated }
        legacyPreferences()?.edit()?.remove(KEY_GEMINI)?.commit()
        legacy
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        require(bytes.size > GCM_IV_BYTES) { "Encrypted key data is invalid." }
        val iv = bytes.copyOfRange(0, GCM_IV_BYTES)
        val payload = bytes.copyOfRange(GCM_IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(payload), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(256)
            .build())
        return generator.generateKey()
    }

    @Suppress("DEPRECATION")
    private fun legacyPreferences() = runCatching {
        EncryptedSharedPreferences.create(
            context,
            LEGACY_PREFS,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    private fun secureStorageUnavailable(cause: Throwable? = null) = IllegalStateException(
        "Android secure storage is unavailable. Zyra did not store your API key; check the device lock screen and try again.",
        cause
    )

    private companion object {
        const val KEY_GEMINI = "gemini_api_key"
        const val LEGACY_PREFS = "zyra_secrets"
        const val KEY_ALIAS = "zyra_gemini_key_v1"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
