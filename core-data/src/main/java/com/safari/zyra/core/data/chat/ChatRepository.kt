package com.safari.zyra.core.data.chat

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    val messages: Flow<List<ChatMessage>>
    val conversations: Flow<List<Conversation>>
    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>
    suspend fun recentForPrompt(limit: Int = 20, conversationId: Long = 1): List<ChatMessage>
    suspend fun getConversation(id: Long): Conversation?
    suspend fun updateSummary(id: Long, summary: String, throughMessageId: Long)
    suspend fun add(message: ChatMessage): Long
    suspend fun createConversation(title: String = "New chat"): Long
    suspend fun renameConversation(id: Long, title: String)
    suspend fun deleteConversation(id: Long)
    suspend fun searchMessages(query: String): List<ChatMessage>
    suspend fun delete(messageId: Long)
    suspend fun deleteAfter(messageId: Long, conversationId: Long = 1)
    suspend fun clear(conversationId: Long = 1)
}
