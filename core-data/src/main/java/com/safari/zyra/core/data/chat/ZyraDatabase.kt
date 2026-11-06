package com.safari.zyra.core.data.chat

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class ZyraDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}