package com.safari.zyra.core.data.chat

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun recentForPrompt(limit: Int = 20): List<ChatMessage>
    suspend fun add(message: ChatMessage): Long
    suspend fun delete(messageId: Long)
    suspend fun deleteAfter(messageId: Long)
    suspend fun clear()
}