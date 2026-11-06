package com.safari.zyra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safari.zyra.core.ui.theme.ZyraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
    NavHost(navController = navController, startDestination = ZyraRoutes.CHAT) {
        composable(ZyraRoutes.SPLASH) { EmptyRoute("Splash", navController) }
        composable(ZyraRoutes.CHAT) { EmptyRoute("Chat", navController) }
        composable(ZyraRoutes.SETTINGS) { EmptyRoute("Settings", navController) }
        composable(ZyraRoutes.ONBOARDING) { EmptyRoute("Onboarding", navController) }
    }
}

@Composable
private fun EmptyRoute(title: String, navController: NavHostController) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { navController.navigate(ZyraRoutes.SPLASH) }) { Text("Splash") }
                TextButton(onClick = { navController.navigate(ZyraRoutes.CHAT) }) { Text("Chat") }
                TextButton(onClick = { navController.navigate(ZyraRoutes.SETTINGS) }) { Text("Settings") }
                TextButton(onClick = { navController.navigate(ZyraRoutes.ONBOARDING) }) { Text("Onboarding") }
            }
        }
    }
}
