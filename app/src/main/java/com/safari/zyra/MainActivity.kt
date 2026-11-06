package com.safari.zyra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safari.zyra.core.ui.theme.ZyraTheme
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
    const val SPLASH = "splash"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
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
        targetState = introComplete,
        transitionSpec = { fadeIn(tween(520)) togetherWith fadeOut(tween(300)) },
        label = "zyra-intro-to-chat"
    ) { ready ->
        if (!ready) {
            ZyraSplash(onFinished = { introComplete = true })
        } else {
            NavHost(navController = navController, startDestination = ZyraRoutes.CHAT) {
                composable(ZyraRoutes.CHAT) { EmptyRoute("Chat", navController) }
                composable(ZyraRoutes.SETTINGS) { EmptyRoute("Settings", navController) }
                composable(ZyraRoutes.ONBOARDING) { EmptyRoute("Onboarding", navController) }
            }
        }
    }
}

@Composable
private fun EmptyRoute(title: String, navController: NavHostController) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { navController.navigate(ZyraRoutes.CHAT) }) { Text("Chat") }
                TextButton(onClick = { navController.navigate(ZyraRoutes.SETTINGS) }) { Text("Settings") }
                TextButton(onClick = { navController.navigate(ZyraRoutes.ONBOARDING) }) { Text("Onboarding") }
            }
        }
    }
}
