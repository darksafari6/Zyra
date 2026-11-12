package com.safari.zyra.core.data.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp ASC, id ASC")
    fun observeAll(conversationId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun latest(limit: Int, conversationId: Long): List<ChatMessage>

    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 100")
    suspend fun search(query: String): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE id > :messageId AND conversation_id = :conversationId")
    suspend fun deleteAfter(messageId: Long, conversationId: Long)

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun clear(conversationId: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()

    @Query("DELETE FROM chat_messages WHERE conversation_id = :conversationId")
    suspend fun deleteConversationMessages(conversationId: Long)
}
