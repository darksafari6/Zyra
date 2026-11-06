package com.safari.zyra.core.data.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)