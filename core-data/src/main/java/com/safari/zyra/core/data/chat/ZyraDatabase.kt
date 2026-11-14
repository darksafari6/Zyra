package com.safari.zyra.core.data.chat

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safari.zyra.core.data.memory.MemoryFact
import com.safari.zyra.core.data.memory.MemoryFactDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ChatMessage::class, Conversation::class, MemoryFact::class], version = 4, exportSchema = true)
abstract class ZyraDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun memoryFactDao(): MemoryFactDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_timestamp ON chat_messages(timestamp)")
            }
        }
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS memory_facts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, factKey TEXT NOT NULL, factValue TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_memory_facts_factKey ON memory_facts(factKey)")
            }
        }
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS conversations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, summary TEXT NOT NULL DEFAULT '', summaryThroughMessageId INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT OR IGNORE INTO conversations (id, title, createdAt, updatedAt) VALUES (1, 'New chat', 0, 0)")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN conversation_id INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversation_id_timestamp ON chat_messages(conversation_id, timestamp)")
            }
        }
    }
}
