package com.safari.zyra.core.data.chat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class ZyraDatabaseMigrationTest {
    private var database: ZyraDatabase? = null

    @After
    fun closeDatabase() { database?.close() }

    @Test
    fun migratesVersionTwoMessagesIntoDefaultConversation() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "zyra-v2-migration-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val old = SQLiteDatabase.openOrCreateDatabase(file, null)
        old.execSQL("CREATE TABLE chat_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, role TEXT NOT NULL, text TEXT NOT NULL, imagePath TEXT, timestamp INTEGER NOT NULL)")
        old.execSQL("CREATE INDEX index_chat_messages_timestamp ON chat_messages(timestamp)")
        old.execSQL("INSERT INTO chat_messages (id, role, text, imagePath, timestamp) VALUES (7, 'user', 'keep this message', NULL, 1234)")
        old.version = 2
        old.close()

        database = Room.databaseBuilder(context, ZyraDatabase::class.java, name)
            .addMigrations(ZyraDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        val migrated = database!!.chatMessageDao().latest(5, 1)
        assertEquals(1, migrated.size)
        assertEquals(7L, migrated.single().id)
        assertEquals(1L, migrated.single().conversation_id)
        assertEquals("keep this message", migrated.single().text)
        assertNotNull(database!!.conversationDao().getById(1))
    }
}
