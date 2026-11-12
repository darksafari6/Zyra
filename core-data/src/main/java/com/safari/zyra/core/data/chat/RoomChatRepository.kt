package com.safari.zyra.core.data.chat

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomChatRepository @Inject constructor(
    private val dao: ChatMessageDao
) : ChatRepository {
    override val messages: Flow<List<ChatMessage>> = dao.observeAll()
    override suspend fun recentForPrompt(limit: Int): List<ChatMessage> =
        dao.latest(limit).asReversed()
    override suspend fun add(message: ChatMessage): Long = dao.insert(message)
    override suspend fun delete(messageId: Long) = dao.deleteById(messageId)
    override suspend fun clear() = dao.clear()
}