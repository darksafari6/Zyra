package com.safari.zyra.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.safari.zyra.core.network.GeminiModelCapability
import com.safari.zyra.feature.voice.InstalledVoiceLocaleCatalog
import java.util.Locale

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var page by remember { mutableStateOf(0) }
    var apiKey by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en-US") }
    var search by remember { mutableStateOf("") }
    var languageExpanded by remember { mutableStateOf(false) }
    val installedLocales = remember { mutableStateOf(listOf(Locale.getDefault())) }
    DisposableEffect(context) {
        val catalog = InstalledVoiceLocaleCatalog(context) { installedLocales.value = it }
        onDispose { catalog.close() }
    }
    var microphoneGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        microphoneGranted = it
    }
    val pages = listOf("Welcome", "Gemini API key", "Choose models", "Microphone", "Voice language", "Ready")

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ZYRA  •  SETUP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text(pages[page], style = MaterialTheme.typography.headlineMedium)
        when (page) {
            0 -> {
                Text("Your personal AI assistant, configured with your own Gemini API key. Setup takes only a minute.")
                Button(onClick = { page++ }) { Text("Get started") }
            }
            1 -> {
                Text("Create a key in Google AI Studio. Your key stays encrypted on this device.")
                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))) }) { Text("Open Google AI Studio") }
                OutlinedTextField(apiKey, { apiKey = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Gemini API key") }, singleLine = true)
                Button(onClick = { viewModel.validateAndLoadModels(apiKey) }, enabled = apiKey.isNotBlank() && !state.loading) { Text(if (state.loading) "Validating…" else "Validate key and load models") }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.models.isNotEmpty()) Button(onClick = { page++ }) { Text("Continue") }
            }
            2 -> {
                Text("Models are loaded from Google's API using your saved key. You can still enter an ID manually in Settings.")
                OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search models") })
                Text("Text model")
                ModelDropdown(
                    selected = state.selectedTextModel,
                    options = state.models.filter { GeminiModelCapability.TEXT in it.capabilities }.filter { it.displayName.contains(search, true) || it.id.contains(search, true) },
                    onSelect = viewModel::selectTextModel
                )
                Text("Live model (optional)")
                ModelDropdown(
                    selected = state.selectedLiveModel,
                    options = state.models.filter { GeminiModelCapability.LIVE in it.capabilities }.filter { it.displayName.contains(search, true) || it.id.contains(search, true) },
                    onSelect = viewModel::selectLiveModel
                )
                OutlinedTextField(state.selectedTextModel, viewModel::selectTextModel, modifier = Modifier.fillMaxWidth(), label = { Text("Manual text model ID") })
                OutlinedTextField(state.selectedLiveModel, viewModel::selectLiveModel, modifier = Modifier.fillMaxWidth(), label = { Text("Manual Live model ID") })
            }
            3 -> {
                Text("ZYRA uses the microphone only after you tap the voice control. Android will ask for permission before recording.")
                Text(if (microphoneGranted) "Microphone permission granted." else "Microphone permission not granted.")
                OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Allow microphone") }
                Text("You can skip this and enable it later.")
            }
            4 -> {
                Text("Choose the language Zyra should use for speech recognition and spoken replies.")
                val languages = installedLocales.value
                OutlinedButton(onClick = { languageExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    val selected = languages.firstOrNull { it.toLanguageTag() == language }
                    Text(selected?.displayName ?: language)
                }
                DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                    languages.forEach { locale ->
                        DropdownMenuItem(
                            text = { Text("${locale.displayName} (${locale.toLanguageTag()})") },
                            onClick = { language = locale.toLanguageTag(); languageExpanded = false }
                        )
                    }
                }
            }
            5 -> {
                Text("Your setup is ready. You can change your key, models, voice and appearance any time in Settings.")
                Button(onClick = { viewModel.finish(language, onFinished) }, enabled = state.selectedTextModel.isNotBlank()) { Text("Finish setup") }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (page > 0) OutlinedButton(onClick = { page-- }) { Text("Back") }
            if (page in 2..4) Button(onClick = { page++ }) { Text(if (page == 4) "Review" else "Continue") }
            if (page == 3) OutlinedButton(onClick = { page++ }) { Text("Skip") }
        }
    }
}

@Composable
private fun ModelDropdown(selected: String, options: List<com.safari.zyra.core.network.GeminiModel>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(options.firstOrNull { it.id == selected }?.let { "${it.displayName}  •  ${it.id}" } ?: selected.ifBlank { "Select a model" })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { model ->
                DropdownMenuItem(
                    text = { Text("${model.displayName}\n${model.id}\nInput: ${model.inputTokenLimit ?: "—"} • Output: ${model.outputTokenLimit ?: "—"}") },
                    onClick = { onSelect(model.id); expanded = false }
                )
            }
        }
    }
}
