package com.safari.zyra.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding().background(Color(0xFF05060A)).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TextButton(onClick = onBack) { Text("←  Back to chat", color = Color(0xFF00E5FF)) }
        Text("Settings", color = Color(0xFFEAF0FF), fontSize = 27.sp)
        Text("Gemini connection", color = Color(0xFF00E5FF), fontSize = 13.sp)
        Surface(color = Color(0xCC0E1220), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (state.hasApiKey) "An encrypted API key is saved on this device." else "No API key saved yet.", color = Color(0xFF9BA7C2), fontSize = 12.sp)
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
                    TextButton(onClick = viewModel::clearApiKey, enabled = state.hasApiKey) { Text("Clear key", color = Color(0xFFFF8A8A)) }
                }
            }
        }

        Text("Model configuration", color = Color(0xFF00E5FF), fontSize = 13.sp)
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
        Text("Voice preferences", color = Color(0xFF00E5FF), fontSize = 13.sp)
        OutlinedTextField(
            value = voiceLanguage,
            onValueChange = { voiceLanguage = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Speech language (BCP-47)") },
            placeholder = { Text("en-US, ur-PK, hi-IN") },
            singleLine = true,
            colors = zyraFieldColors()
        )
        Text("Speech speed: " + String.format("%.2fx", voiceSpeed), color = Color(0xFF9BA7C2), fontSize = 12.sp)
        Slider(value = voiceSpeed, onValueChange = { voiceSpeed = it }, valueRange = 0.65f..1.5f)
        Text("Voice pitch: " + String.format("%.2fx", voicePitch), color = Color(0xFF9BA7C2), fontSize = 12.sp)
        Slider(value = voicePitch, onValueChange = { voicePitch = it }, valueRange = 0.7f..1.4f)

        Button(
            onClick = {
                viewModel.saveSettings(textModel, liveModel, prompt, voiceLanguage, voiceSpeed, voicePitch)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save settings") }

        Button(
            onClick = { viewModel.testConnection(textModel, liveModel, prompt) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.testingConnection && state.hasApiKey && textModel.isNotBlank()
        ) {
            if (state.testingConnection) Text("Testing…") else Text("Test Gemini connection")
        }
        state.testResult?.let { Text(it, color = if (it.startsWith("Connected")) Color(0xFF72E7BE) else Color(0xFFFFA0A0), fontSize = 12.sp) }

        Spacer(Modifier.height(5.dp))
        Text("Chat data", color = Color(0xFF00E5FF), fontSize = 13.sp)
        Text("Clear all saved messages and locally stored chat images from this device.", color = Color(0xFF9BA7C2), fontSize = 12.sp)
        TextButton(onClick = { confirmClear = true }) { Text("Clear chat history", color = Color(0xFFFF8A8A)) }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear chat history?") },
            text = { Text("This permanently deletes saved messages and chat image copies from this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text("Clear", color = Color(0xFFFF8A8A)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
            containerColor = Color(0xFF101522),
            titleContentColor = Color(0xFFEAF0FF),
            textContentColor = Color(0xFF9BA7C2)
        )
    }
}

@Composable
private fun zyraFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFFEAF0FF),
    unfocusedTextColor = Color(0xFFEAF0FF),
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color(0xFF35425B),
    focusedLabelColor = Color(0xFF00E5FF),
    unfocusedLabelColor = Color(0xFF9BA7C2),
    cursorColor = Color(0xFF00E5FF)
)
