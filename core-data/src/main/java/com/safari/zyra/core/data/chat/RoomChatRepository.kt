package com.safari.zyra.core.data.chat

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomChatRepository @Inject constructor(
    private val dao: ChatMessageDao,
    private val conversationDao: ConversationDao
) : ChatRepository {
    override val messages: Flow<List<ChatMessage>> = dao.observeAll()
    override val conversations: Flow<List<Conversation>> = conversationDao.observeAll()
    override suspend fun recentForPrompt(limit: Int): List<ChatMessage> = dao.latest(limit).asReversed()
    override suspend fun add(message: ChatMessage): Long = dao.insert(message)
    override suspend fun createConversation(title: String): Long = conversationDao.insert(Conversation(title = title))
    override suspend fun renameConversation(id: Long, title: String) {
        require(title.isNotBlank()) { "Conversation title cannot be empty." }
        conversationDao.rename(id, title.trim().take(80))
    }
    override suspend fun deleteConversation(id: Long) {
        dao.deleteConversationMessages(id)
        conversationDao.delete(id)
    }
    override suspend fun searchMessages(query: String): List<ChatMessage> =
        if (query.isBlank()) emptyList() else dao.search(query.trim())
    override suspend fun delete(messageId: Long) = dao.deleteById(messageId)
    override suspend fun deleteAfter(messageId: Long) = dao.deleteAfter(messageId)
    override suspend fun clear() = dao.clear()
}
