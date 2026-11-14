package com.safari.zyra.feature.chat

import com.safari.zyra.core.data.chat.ChatMessage
import com.safari.zyra.core.data.chat.Conversation
import com.safari.zyra.core.data.settings.ZyraSettings
import com.safari.zyra.feature.voice.VoiceState
import com.safari.zyra.feature.voice.LiveVoiceUiState

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: Long = 1,
    val searchResults: List<ChatMessage> = emptyList(),
    val settings: ZyraSettings = ZyraSettings(),
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val error: String? = null,
    val hasApiKey: Boolean = false,
    val testingConnection: Boolean = false,
    val testResult: String? = null,
    val voiceState: VoiceState = VoiceState.Idle,
    val liveVoiceState: LiveVoiceUiState = LiveVoiceUiState(),
    val reduceEffects: Boolean = false
)
