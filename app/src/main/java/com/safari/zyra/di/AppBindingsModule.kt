package com.safari.zyra.di

import android.content.Context
import androidx.room.Room
import com.safari.zyra.core.data.SettingsDataStore
import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.chat.ChatMessageDao
import com.safari.zyra.core.data.chat.ChatRepository
import com.safari.zyra.core.data.chat.ConversationDao
import com.safari.zyra.core.data.chat.RoomChatRepository
import com.safari.zyra.core.data.chat.ZyraDatabase
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.data.security.EncryptedApiKeyStore
import com.safari.zyra.core.network.GeminiClient
import com.safari.zyra.core.network.GeminiRestClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds @Singleton abstract fun bindSettingsRepository(implementation: SettingsDataStore): SettingsRepository
    @Binds @Singleton abstract fun bindChatRepository(implementation: RoomChatRepository): ChatRepository
    @Binds @Singleton abstract fun bindApiKeyStore(implementation: EncryptedApiKeyStore): ApiKeyStore
    @Binds @Singleton abstract fun bindGeminiClient(implementation: GeminiRestClient): GeminiClient

    companion object {
        @Provides @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ZyraDatabase =
            Room.databaseBuilder(context, ZyraDatabase::class.java, "zyra_chat.db")
                .addMigrations(ZyraDatabase.MIGRATION_1_2, ZyraDatabase.MIGRATION_2_3)
                .build()

        @Provides fun provideChatDao(database: ZyraDatabase): ChatMessageDao = database.chatMessageDao()
        @Provides fun provideConversationDao(database: ZyraDatabase): ConversationDao = database.conversationDao()

        @Provides @Singleton
        fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 120_000
            }
        }
    }
}
