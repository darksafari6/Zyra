package com.safari.zyra.feature.settings

import android.content.Context
import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.chat.ChatRepository
import com.safari.zyra.core.data.security.ApiKeyStore
import com.safari.zyra.core.data.settings.ZyraSettings
import com.safari.zyra.core.network.GeminiClient
import com.safari.zyra.core.network.GeminiModelCatalog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun deletingKeyClearsSecureStoreAndConnectionState() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val settings = mockk<SettingsRepository>()
            every { settings.settings } returns flowOf(ZyraSettings())
            every { settings.reduceEffects } returns flowOf(false)

            val hasKey = MutableStateFlow(true)
            val keyStore = mockk<ApiKeyStore>(relaxed = true)
            every { keyStore.hasKey } returns hasKey

            val chats = mockk<ChatRepository>()
            every { chats.allMessages } returns flowOf(emptyList())

            val viewModel = SettingsViewModel(
                context = mockk<Context>(relaxed = true),
                settingsRepository = settings,
                keyStore = keyStore,
                catalog = mockk<GeminiModelCatalog>(),
                geminiClient = mockk<GeminiClient>(),
                chatRepository = chats
            )

            viewModel.deleteKey()

            verify(exactly = 1) { keyStore.clear() }
            assertFalse(viewModel.state.value.hasApiKey)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
