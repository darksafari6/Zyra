package com.safari.zyra.feature.chat

import com.safari.zyra.core.ui.theme.ZyraColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var apiKey by remember { mutableStateOf("") }
    var textModel by remember(state.settings.textModelId) { mutableStateOf(state.settings.textModelId) }
    var liveModel by remember(state.settings.liveModelId) { mutableStateOf(state.settings.liveModelId) }
    var prompt by remember(state.settings.systemPrompt) { mutableStateOf(state.settings.systemPrompt) }
    var voiceLanguage by remember(state.settings.voiceLanguage) { mutableStateOf(state.settings.voiceLanguage) }
    var voiceSpeed by remember(state.settings.voiceSpeed) { mutableStateOf(state.settings.voiceSpeed) }
    var voicePitch by remember(state.settings.voicePitch) { mutableStateOf(state.settings.voicePitch) }
    var confirmClear by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().background(ZyraColors.background).verticalScroll(rememberScrollState()).widthIn(max = 720.dp).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back), color = ZyraColors.cyan) }
        Text(stringResource(R.string.settings_title), color = ZyraColors.text, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
        Text(stringResource(R.string.settings_gemini), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        Surface(color = ZyraColors.glass, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (state.hasApiKey) "An encrypted API key is saved on this device." else "No API key saved yet.", color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gemini API key") },
                    placeholder = { Text(if (state.hasApiKey) "Saved key — enter a replacement" else "Paste your API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = zyraFieldColors()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.saveApiKey(apiKey); apiKey = "" }, enabled = apiKey.isNotBlank()) { Text("Save key") }
                    TextButton(onClick = viewModel::clearApiKey, enabled = state.hasApiKey) { Text("Clear key", color = ZyraColors.error) }
                }
            }
        }

        Text(stringResource(R.string.settings_models), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        OutlinedTextField(
            value = textModel,
            onValueChange = { textModel = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text model ID") },
            placeholder = { Text("Enter an available Gemini model ID") },
            singleLine = true,
            colors = zyraFieldColors()
        )
        OutlinedTextField(
            value = liveModel,
            onValueChange = { liveModel = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Live model ID (for Piece 7)") },
            placeholder = { Text("Enter later when configuring Live API") },
            singleLine = true,
            colors = zyraFieldColors()
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Zyra system personality") },
            minLines = 4,
            colors = zyraFieldColors()
        )
        Text(stringResource(R.string.settings_accessibility), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_reduce_effects), color = ZyraColors.text, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
                Text(stringResource(R.string.settings_reduce_effects_desc), color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
            }
            Switch(checked = state.reduceEffects, onCheckedChange = viewModel::setReduceEffects)
        }
        Text(stringResource(R.string.settings_voice), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        OutlinedTextField(
            value = voiceLanguage,
            onValueChange = { voiceLanguage = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Speech language (BCP-47)") },
            placeholder = { Text("en-US, ur-PK, hi-IN") },
            singleLine = true,
            colors = zyraFieldColors()
        )
        Text("Speech speed: " + String.format("%.2fx", voiceSpeed), color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
        Slider(value = voiceSpeed, onValueChange = { voiceSpeed = it }, valueRange = 0.65f..1.5f)
        Text("Voice pitch: " + String.format("%.2fx", voicePitch), color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
        Slider(value = voicePitch, onValueChange = { voicePitch = it }, valueRange = 0.7f..1.4f)

        Button(
            onClick = {
                viewModel.saveSettings(textModel, liveModel, prompt, voiceLanguage, voiceSpeed, voicePitch)
                scope.launch { snackbarHostState.showSnackbar("Settings saved successfully.") }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.settings_save)) }
        SnackbarHost(hostState = snackbarHostState)

        Button(
            onClick = { viewModel.testConnection(textModel, liveModel, prompt) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.testingConnection && state.hasApiKey && textModel.isNotBlank()
        ) {
            if (state.testingConnection) Text("Testing…") else Text(stringResource(R.string.settings_test))
        }
        state.error?.let { Text(it, color = ZyraColors.error, fontSize = MaterialTheme.typography.labelSmall.fontSize) }
        state.testResult?.let { Text(it, color = if (it.startsWith("Connected")) ZyraColors.success else ZyraColors.error, fontSize = MaterialTheme.typography.labelSmall.fontSize) }

        Spacer(Modifier.height(5.dp))
        Text(stringResource(R.string.settings_chat_data), color = ZyraColors.cyan, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        Text("Clear all saved messages and locally stored chat images from this device.", color = ZyraColors.muted, fontSize = MaterialTheme.typography.labelSmall.fontSize)
        TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.settings_clear_history), color = ZyraColors.error) }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear chat history?") },
            text = { Text("This permanently deletes saved messages and chat image copies from this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text("Clear", color = ZyraColors.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
            containerColor = ZyraColors.surface,
            titleContentColor = ZyraColors.text,
            textContentColor = ZyraColors.muted
        )
    }
}

@Composable
private fun zyraFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ZyraColors.text,
    unfocusedTextColor = ZyraColors.text,
    focusedBorderColor = ZyraColors.cyan,
    unfocusedBorderColor = ZyraColors.border,
    focusedLabelColor = ZyraColors.cyan,
    unfocusedLabelColor = ZyraColors.muted,
    cursorColor = ZyraColors.cyan
)
