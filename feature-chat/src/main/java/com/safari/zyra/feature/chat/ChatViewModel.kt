package com.safari.zyra.feature.chat

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.chat.ChatMessage
import com.safari.zyra.core.data.chat.ChatRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.network.GeminiClient
import com.safari.zyra.core.network.GeminiImage
import com.safari.zyra.core.network.GeminiTurn
import com.safari.zyra.feature.voice.VoiceSession
import com.safari.zyra.feature.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val apiKeyStore: ApiKeyStore,
    private val geminiClient: GeminiClient,
    private val voiceSession: VoiceSession
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { chatRepository.messages.collect { messages -> mutableState.update { it.copy(messages = messages) } } }
        viewModelScope.launch { settingsRepository.settings.collect { settings -> mutableState.update { it.copy(settings = settings) } } }
        viewModelScope.launch { apiKeyStore.hasKey.collect { hasKey -> mutableState.update { it.copy(hasApiKey = hasKey) } } }
        viewModelScope.launch { voiceSession.state.collect { voice -> mutableState.update { it.copy(voiceState = voice) } } }
    }

    fun startVoice() = voiceSession.startListening()
    fun stopVoice() = voiceSession.stop()
    fun stopSpeaking() = voiceSession.stopSpeaking()
    fun voicePermissionDenied() {
        mutableState.update { it.copy(voiceState = VoiceState.Error("Microphone access was denied. Enable it in Android Settings to use voice chat.")) }
    }

    fun sendMessage(text: String, imageUri: Uri? = null) {
        val prompt = text.trim().ifBlank { if (imageUri != null) "Please describe and analyze this image." else "" }
        if (prompt.isBlank() || mutableState.value.isStreaming) return
        if (mutableState.value.voiceState !is VoiceState.Idle) voiceSession.stop()
        viewModelScope.launch {
            mutableState.update { it.copy(isStreaming = true, streamingText = "", error = null) }
            try {
                val preparedImage = imageUri?.let { prepareImage(it) }
                val image = preparedImage?.first
                val storedImagePath = preparedImage?.second
                chatRepository.add(ChatMessage(role = "user", text = prompt, imagePath = storedImagePath))
                val history = chatRepository.recentForPrompt(21).dropLast(1).map { GeminiTurn(it.role, it.text) }
                val response = StringBuilder()
                geminiClient.streamReply(
                    apiKey = apiKeyStore.read(),
                    modelId = mutableState.value.settings.textModelId,
                    systemPrompt = mutableState.value.settings.systemPrompt,
                    history = history,
                    userText = prompt,
                    image = image
                ).collect { chunk ->
                    response.append(chunk)
                    mutableState.update { it.copy(streamingText = response.toString()) }
                }
                val finalText = response.toString().trim()
                if (finalText.isBlank()) error("Zyra received an empty response. Please retry.")
                chatRepository.add(ChatMessage(role = "assistant", text = finalText))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.update { it.copy(error = failure.message ?: "Something went wrong.") }
            } finally { mutableState.update { it.copy(isStreaming = false, streamingText = "") } }
        }
    }

    private suspend fun prepareImage(uri: Uri): Pair<GeminiImage, String> = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        require(mime.startsWith("image/")) { "The selected file is not a supported image." }
        val sourceBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the selected image.")
        require(sourceBytes.size <= MAX_IMAGE_BYTES) { "Image is too large. Choose an image under 14 MB." }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected image could not be decoded." }

        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_SIDE)
        val decoded = BitmapFactory.decodeByteArray(
            sourceBytes, 0, sourceBytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: error("The selected image could not be decoded.")
        val scale = minOf(1f, MAX_IMAGE_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val outputBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } else decoded
        try {
            val output = ByteArrayOutputStream()
            check(outputBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                "Could not prepare the selected image."
            }
            val jpegBytes = output.toByteArray()
            val directory = File(context.filesDir, "zyra_images").apply { mkdirs() }
            val target = File(directory, "image_${System.currentTimeMillis()}.jpg")
            target.writeBytes(jpegBytes)
            GeminiImage(jpegBytes, "image/jpeg") to target.absolutePath
        } finally {
            outputBitmap.recycle()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (maxOf(width / sample, height / sample) > maxSide * 2) sample *= 2
        return sample
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 14 * 1024 * 1024
        const val MAX_IMAGE_SIDE = 1_600
        const val JPEG_QUALITY = 85
    }

    fun saveApiKey(value: String) {
        if (value.isNotBlank()) apiKeyStore.write(value)
        mutableState.update { it.copy(hasApiKey = apiKeyStore.hasKey.value, testResult = null) }
    }
    fun clearApiKey() {
        apiKeyStore.clear()
        mutableState.update { it.copy(hasApiKey = false, testResult = null) }
    }
    fun saveSettings(textModelId: String, liveModelId: String, systemPrompt: String, voiceLanguage: String, voiceSpeed: Float, voicePitch: Float) {
        viewModelScope.launch {
            settingsRepository.updateTextModelId(textModelId)
            settingsRepository.updateLiveModelId(liveModelId)
            settingsRepository.updateSystemPrompt(systemPrompt)
            settingsRepository.updateVoiceSettings(voiceLanguage, voiceSpeed, voicePitch)
            mutableState.update { it.copy(settings = settingsRepository.settings.first()) }
        }
    }

    fun testConnection(textModelId: String, liveModelId: String, systemPrompt: String) {
        if (mutableState.value.testingConnection) return
        viewModelScope.launch {
            mutableState.update { it.copy(testingConnection = true, testResult = null, error = null) }
            try {
                val response = geminiClient.testConnection(apiKeyStore.read(), textModelId, systemPrompt)
                mutableState.update { it.copy(testResult = "Connected successfully: ${response.trim()}") }
            } catch (failure: Exception) {
                mutableState.update { it.copy(testResult = failure.message ?: "Connection test failed.") }
            } finally { mutableState.update { it.copy(testingConnection = false) } }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { chatRepository.clear(); File(context.filesDir, "zyra_images").deleteRecursively() }
    }
    fun dismissError() = mutableState.update { it.copy(error = null) }

    override fun onCleared() {
        voiceSession.stop()
        super.onCleared()
    }
}