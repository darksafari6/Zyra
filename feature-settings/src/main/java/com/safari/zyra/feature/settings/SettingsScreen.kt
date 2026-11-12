package com.safari.zyra.feature.settings

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import com.safari.zyra.core.data.settings.ZyraSettings
import com.safari.zyra.core.network.GeminiModelCapability
import com.safari.zyra.feature.voice.InstalledVoiceLocaleCatalog
import java.util.Locale

@Composable
fun SettingsScreen(onBack: () -> Unit, onRerunOnboarding: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var settings by remember(state.settings) { mutableStateOf(state.settings) }
    var reduceEffects by remember(state.reduceEffects) { mutableStateOf(state.reduceEffects) }
    var key by remember { mutableStateOf("") }
    var modelSearch by remember { mutableStateOf("") }
    var textExpanded by remember { mutableStateOf(false) }
    var liveExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var voiceExpanded by remember { mutableStateOf(false) }
    var personaExpanded by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val installedLocales = remember { mutableStateOf(listOf(Locale.getDefault())) }
    val ttsVoices = remember { mutableStateOf(emptySet<Voice>()) }
    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsRef.value?.let { engine ->
                    ttsVoices.value = engine.voices ?: emptySet()
                }
            }
        }
    }
    SideEffect { ttsRef.value = tts }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }
    DisposableEffect(context) {
        val catalog = InstalledVoiceLocaleCatalog(context) { installedLocales.value = it }
        onDispose { catalog.close() }
    }
    val languages = installedLocales.value
    val voices = ttsVoices.value.sortedBy { it.name }
    val personas = listOf("Friendly", "Professional", "Roman-Urdu buddy", "Custom")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        SettingsSection("Connection") {
            Text(if (state.hasApiKey) "Gemini API key saved securely on this device." else "No API key saved.")
            OutlinedTextField(key, { key = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Replace Gemini API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            Button(onClick = { viewModel.saveKey(key); key = "" }, enabled = key.isNotBlank()) { Text("Save key") }
            Button(onClick = viewModel::testConnection, enabled = state.hasApiKey && settings.textModelId.isNotBlank()) { Text("Test Gemini connection") }
            state.testResult?.let { Text(it, color = if (it.startsWith("Connected")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        SettingsSection("Models") {
            Button(onClick = viewModel::loadModels, enabled = state.hasApiKey && !state.loadingModels) { Text(if (state.loadingModels) "Loading…" else "Refresh model catalog") }
            OutlinedTextField(modelSearch, { modelSearch = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search models") })
            Text("Text model")
            ModelPicker(settings.textModelId, state.models.filter { GeminiModelCapability.TEXT in it.capabilities }.filter { it.id.contains(modelSearch, true) || it.displayName.contains(modelSearch, true) }, textExpanded, { textExpanded = it }) { settings = settings.copy(textModelId = it) }
            OutlinedTextField(settings.textModelId, { settings = settings.copy(textModelId = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Manual text model ID") })
            Text("Live model")
            ModelPicker(settings.liveModelId, state.models.filter { GeminiModelCapability.LIVE in it.capabilities }.filter { it.id.contains(modelSearch, true) || it.displayName.contains(modelSearch, true) }, liveExpanded, { liveExpanded = it }) { settings = settings.copy(liveModelId = it) }
            OutlinedTextField(settings.liveModelId, { settings = settings.copy(liveModelId = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Manual Live model ID") })
        }
        SettingsSection("Persona") {
            OutlinedButton(onClick = { personaExpanded = true }) { Text(settings.persona) }
            DropdownMenu(personaExpanded, { personaExpanded = false }) {
                personas.forEach { DropdownMenuItem(text = { Text(it) }, onClick = {
                    val prompt = when (it) {
                        "Professional" -> "You are Zyra, a concise and professional assistant. Be accurate, structured and respectful."
                        "Roman-Urdu buddy" -> "You are Zyra, a friendly Roman Urdu speaking buddy. Reply naturally in the user's language."
                        "Custom" -> settings.customPersona
                        else -> ZyraSettings.DEFAULT_SYSTEM_PROMPT
                    }
                    settings = settings.copy(persona = it, systemPrompt = prompt)
                    personaExpanded = false
                }) }
            }
            if (settings.persona == "Custom") OutlinedTextField(settings.customPersona, { settings = settings.copy(customPersona = it, systemPrompt = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Custom persona instructions") }, minLines = 3)
        }
        SettingsSection("Voice") {
            OutlinedButton(onClick = { languageExpanded = true }) { Text("Language: ${languages.firstOrNull { it.toLanguageTag() == settings.voiceLanguage }?.displayName ?: settings.voiceLanguage}") }
            DropdownMenu(languageExpanded, { languageExpanded = false }) {
                languages.forEach { locale -> DropdownMenuItem(text = { Text("${locale.displayName} (${locale.toLanguageTag()})") }, onClick = { settings = settings.copy(voiceLanguage = locale.toLanguageTag()); languageExpanded = false }) }
            }
            OutlinedButton(onClick = { voiceExpanded = true }, enabled = voices.isNotEmpty()) { Text("Voice: ${voices.firstOrNull { it.name == settings.voiceName }?.name ?: settings.voiceName.ifBlank { "System default" }}") }
            DropdownMenu(voiceExpanded, { voiceExpanded = false }) {
                voices.forEach { voice -> DropdownMenuItem(text = { Text("${voice.name} — ${voice.locale.displayName}") }, onClick = { settings = settings.copy(voiceName = voice.name); voiceExpanded = false }) }
            }
            Text("Speech speed: ${"%.2f".format(settings.voiceSpeed)}×")
            Slider(settings.voiceSpeed, { settings = settings.copy(voiceSpeed = it) }, valueRange = .65f..1.5f)
            Text("Pitch: ${"%.2f".format(settings.voicePitch)}×")
            Slider(settings.voicePitch, { settings = settings.copy(voicePitch = it) }, valueRange = .7f..1.4f)
            Button(onClick = {
                tts.language = Locale.forLanguageTag(settings.voiceLanguage)
                voices.firstOrNull { it.name == settings.voiceName }?.let(tts::setVoice)
                tts.setSpeechRate(settings.voiceSpeed)
                tts.setPitch(settings.voicePitch)
                tts.speak("Hello, I am Zyra. Your voice settings are ready.", TextToSpeech.QUEUE_FLUSH, null, "zyra-voice-test")
            }) { Text("Test voice") }
        }
        SettingsSection("Appearance") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Reduce effects"); Switch(reduceEffects, { reduceEffects = it }) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Pure black background"); Switch(settings.pureBlack, { settings = settings.copy(pureBlack = it) }) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Auto-title conversations (experimental)"); Switch(settings.autoTitle, { settings = settings.copy(autoTitle = it) }) }
        }
        SettingsSection("Privacy and data") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { shareText(context, viewModel.exportJson(), "application/json") }) { Text("Export JSON") }
                OutlinedButton(onClick = { shareText(context, viewModel.exportMarkdown(), "text/markdown") }) { Text("Export Markdown") }
            }
            OutlinedButton(onClick = { confirmClear = true }) { Text("Clear chat history") }
            OutlinedButton(onClick = viewModel::deleteKey, enabled = state.hasApiKey) { Text("Delete API key") }
            OutlinedButton(onClick = { viewModel.completeOnboardingAgain(); onRerunOnboarding() }) { Text("Run onboarding again") }
            if (confirmClear) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text("Confirm clear") }
                    OutlinedButton(onClick = { confirmClear = false }) { Text("Cancel") }
                }
            }
        }
        SettingsSection("About") {
            Text("ZYRA by Safari")
            val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "Unknown" }
            Text("Version $version")
            OutlinedButton(onClick = { openUrl(context, "https://github.com/darksafari6/Zyra") }) { Text("Source and licences") }
            OutlinedButton(onClick = { openUrl(context, "https://github.com/darksafari6/Zyra/issues/new") }) { Text("Contact / report an issue") }
        }
        Button(onClick = { viewModel.save(settings, reduceEffects, onBack) }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
        Spacer(Modifier.padding(bottom = 16.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun ModelPicker(selected: String, models: List<com.safari.zyra.core.network.GeminiModel>, expanded: Boolean, onExpanded: (Boolean) -> Unit, onSelect: (String) -> Unit) {
    Column {
        OutlinedButton(onClick = { onExpanded(true) }, modifier = Modifier.fillMaxWidth()) { Text(selected.ifBlank { "Choose from catalog" }) }
        DropdownMenu(expanded, { onExpanded(false) }) {
            models.forEach { model ->
                DropdownMenuItem(text = { Text("${model.displayName} — ${model.id}\nTokens: ${model.inputTokenLimit ?: "—"} in / ${model.outputTokenLimit ?: "—"} out") }, onClick = { onSelect(model.id); onExpanded(false) })
            }
        }
    }
}

private fun shareText(context: android.content.Context, value: String, mime: String) {
    val intent = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_TEXT, value)
    context.startActivity(Intent.createChooser(intent, "Export Zyra chat"))
}
private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
}
