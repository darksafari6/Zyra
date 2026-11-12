package com.safari.zyra

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.safari.zyra.core.ui.theme.ZyraColors
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safari.zyra.core.ui.theme.ZyraTheme
import com.safari.zyra.feature.chat.ChatScreen
import com.safari.zyra.feature.chat.SettingsScreen
import com.safari.zyra.ui.splash.ZyraSplash
import dagger.hilt.android.AndroidEntryPoint
import com.safari.zyra.feature.voice.VoiceSession
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var voiceSession: VoiceSession

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ZyraTheme { ZyraNavigation() } }
    }

    override fun onStop() {
        voiceSession.stop()
        super.onStop()
    }
}

private object ZyraRoutes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
private fun ZyraNavigation() {
    val navController = rememberNavController()
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
            NavHost(navController = navController, startDestination = ZyraRoutes.CHAT) {
                composable(ZyraRoutes.CHAT, enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = .96f, animationSpec = tween(260)) }, exitTransition = { fadeOut(tween(180)) + scaleOut(targetScale = .98f, animationSpec = tween(180)) }) {
                    ChatScreen(onOpenSettings = { navController.navigate(ZyraRoutes.SETTINGS) })
                }
                composable(ZyraRoutes.SETTINGS, enterTransition = { fadeIn(tween(260)) + scaleIn(initialScale = .96f, animationSpec = tween(260)) }, popExitTransition = { fadeOut(tween(180)) + scaleOut(targetScale = .98f, animationSpec = tween(180)) }) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
