package com.safari.zyra

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safari.zyra.core.data.SettingsRepository
import com.safari.zyra.core.data.settings.ZyraSettings
import com.safari.zyra.core.ui.theme.ZyraColors
import com.safari.zyra.core.ui.theme.ZyraTheme
import com.safari.zyra.feature.chat.ChatScreen
import com.safari.zyra.feature.onboarding.OnboardingScreen
import com.safari.zyra.feature.settings.SettingsScreen
import com.safari.zyra.feature.voice.VoiceSession
import com.safari.zyra.assistant.service.ZyraAssistantService
import com.safari.zyra.ui.splash.ZyraSplash
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private companion object { const val REQUEST_VOICE_PERMISSION = 8302; const val REQUEST_ASSISTANT_ROLE = 8304 }
    private var sharedText by mutableStateOf<String?>(null)
    private var sharedImage by mutableStateOf<Uri?>(null)
    @Inject lateinit var voiceSession: VoiceSession
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSharedContent(intent)
        ZyraShortcuts.publish(this)
        setContent {
            val appearance by settingsRepository.settings.collectAsState(initial = ZyraSettings())
            ZyraTheme(pureBlack = appearance.pureBlack) {
                ZyraNavigation(settingsRepository, sharedText, sharedImage, ::clearSharedContent, ::requestAssistantRole, ::stopAllAssistantActivity)
            }
        }
        handleVoiceLaunch(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleVoiceLaunch(intent)
        handleSharedContent(intent)
    }

    private fun handleSharedContent(source: Intent?) {
        val shared = parseSharedContent(source) ?: return
        sharedText = shared.text
        sharedImage = shared.image
    }

    private fun clearSharedContent() {
        sharedText = null
        sharedImage = null
    }

    private fun requestAssistantRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val manager = getSystemService(RoleManager::class.java)
            if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), REQUEST_ASSISTANT_ROLE)
                return
            }
        }
        runCatching { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
    }

    private fun stopAllAssistantActivity() {
        voiceSession.stop()
        stopService(Intent(this, com.safari.zyra.assistant.service.ZyraAssistantService::class.java))
    }

    private fun handleVoiceLaunch(source: android.content.Intent?) {
        if (source?.getBooleanExtra(ZyraAssistantService.EXTRA_OPEN_VOICE, false) != true) return
        source.removeExtra(ZyraAssistantService.EXTRA_OPEN_VOICE)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceSession.startListening()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_VOICE_PERMISSION)
        }
    }

    @Deprecated("Permission callback required for the notification Talk action on API 28+.")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_VOICE_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            voiceSession.startListening()
        }
    }

    override fun onStop() {
        voiceSession.stop()
        super.onStop()
    }
}

private object ZyraRoutes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
}

@Composable
private fun ZyraNavigation(
    settingsRepository: SettingsRepository,
    sharedText: String?,
    sharedImage: Uri?,
    onSharedContentConsumed: () -> Unit,
    onRequestAssistantRole: () -> Unit,
    onStopAssistant: () -> Unit
) {
    val navController = rememberNavController()
    val onboardingComplete by settingsRepository.onboardingCompleted.collectAsState(initial = false)
    var introComplete by remember { mutableStateOf(false) }
    AnimatedContent(
        modifier = Modifier.fillMaxSize().background(ZyraColors.background),
        targetState = introComplete,
        transitionSpec = { fadeIn(tween(520)) togetherWith fadeOut(tween(300)) },
        label = "zyra-intro-to-chat"
    ) { ready ->
        if (!ready) {
            ZyraSplash(onFinished = { introComplete = true })
        } else {
            val start = if (onboardingComplete) ZyraRoutes.CHAT else ZyraRoutes.ONBOARDING
            NavHost(navController = navController, startDestination = start) {
                composable(ZyraRoutes.CHAT, enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = .96f, animationSpec = tween(260)) }, exitTransition = { fadeOut(tween(180)) + scaleOut(targetScale = .98f, animationSpec = tween(180)) }) {
                    ChatScreen(
                        onOpenSettings = { navController.navigate(ZyraRoutes.SETTINGS) },
                        sharedText = sharedText,
                        sharedImage = sharedImage,
                        onSharedContentConsumed = onSharedContentConsumed
                    )
                }
                composable(ZyraRoutes.SETTINGS, enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = .96f, animationSpec = tween(260)) }, popExitTransition = { fadeOut(tween(180)) + scaleOut(targetScale = .98f, animationSpec = tween(180)) }) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onRerunOnboarding = {
                            navController.navigate(ZyraRoutes.ONBOARDING)
                        },
                        onRequestAssistantRole = onRequestAssistantRole,
                        onStopAssistant = onStopAssistant
                    )
                }
                composable(ZyraRoutes.ONBOARDING) {
                    OnboardingScreen(onFinished = {
                        navController.navigate(ZyraRoutes.CHAT) {
                            popUpTo(ZyraRoutes.CHAT) { inclusive = true }
                            launchSingleTop = true
                        }
                    })
                }
            }
        }
    }
}
