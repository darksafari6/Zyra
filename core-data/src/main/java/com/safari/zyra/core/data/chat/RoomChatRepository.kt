package com.safari.zyra.core.data.chat

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomChatRepository @Inject constructor(
    private val dao: ChatMessageDao,
    private val conversationDao: ConversationDao
) : ChatRepository {
    override val messages: Flow<List<ChatMessage>> = dao.observeAll(1)
    override val allMessages: Flow<List<ChatMessage>> = dao.observeAllMessages()
    override val conversations: Flow<List<Conversation>> = conversationDao.observeAll()
    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> = dao.observeAll(conversationId)
    override suspend fun recentForPrompt(limit: Int, conversationId: Long): List<ChatMessage> = dao.latest(limit, conversationId).asReversed()
    override suspend fun getConversation(id: Long): Conversation? = conversationDao.getById(id)
    override suspend fun updateSummary(id: Long, summary: String, throughMessageId: Long) = conversationDao.updateSummary(id, summary, throughMessageId)
    override suspend fun add(message: ChatMessage): Long {
        val id = dao.insert(message)
        conversationDao.touch(message.conversation_id)
        return id
    }
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
    override suspend fun deleteAfter(messageId: Long, conversationId: Long) = dao.deleteAfter(messageId, conversationId)
    override suspend fun clear(conversationId: Long) = dao.clear(conversationId)
    override suspend fun clearAll() = dao.clearAll()
}
