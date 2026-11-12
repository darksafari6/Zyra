package com.safari.zyra.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.safari.zyra.core.data.settings.ZyraSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDataStoreTest {
    @Test
    fun persistsOnboardingAndUserPreferences() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SettingsDataStore(context)
        repository.setOnboardingCompleted(true)
        repository.setReduceEffects(true)
        repository.updateTextModelId("chosen-text-model")
        repository.updateVoiceSettings("ur-PK", 1.1f, 0.9f, "installed-voice")

        assertTrue(repository.onboardingCompleted.first())
        assertTrue(repository.reduceEffects.first())
        val settings = repository.settings.first()
        assertEquals("chosen-text-model", settings.textModelId)
        assertEquals("ur-PK", settings.voiceLanguage)
        assertEquals("installed-voice", settings.voiceName)
        assertEquals(1.1f, settings.voiceSpeed)
        assertEquals(0.9f, settings.voicePitch)
        assertEquals(ZyraSettings.DEFAULT_SYSTEM_PROMPT, settings.systemPrompt)
    }
}
