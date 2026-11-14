package com.safari.zyra.core.data.chat

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages", indices = [Index(value = ["timestamp"]), Index(value = ["conversation_id", "timestamp"])])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "conversation_id", defaultValue = "1") val conversation_id: Long = 1
)
