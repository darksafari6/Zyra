package com.safari.zyra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safari.zyra.core.ui.theme.ZyraTheme
import com.safari.zyra.feature.chat.ChatScreen
import com.safari.zyra.feature.chat.SettingsScreen
import com.safari.zyra.ui.splash.ZyraSplash
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ZyraTheme { ZyraNavigation() } }
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
    LaunchedEffect(Unit) {
        delay(2_500)
        introComplete = true
    }
    AnimatedContent(
        modifier = Modifier.fillMaxSize().background(Color(0xFF05060A)),
        targetState = introComplete,
        transitionSpec = { fadeIn(tween(520)) togetherWith fadeOut(tween(300)) },
        label = "zyra-intro-to-chat"
    ) { ready ->
        if (!ready) {
            ZyraSplash(onFinished = { introComplete = true })
        } else {
            NavHost(navController = navController, startDestination = ZyraRoutes.CHAT) {
                composable(ZyraRoutes.CHAT) {
                    ChatScreen(onOpenSettings = { navController.navigate(ZyraRoutes.SETTINGS) })
                }
                composable(ZyraRoutes.SETTINGS) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
