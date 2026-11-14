package com.safari.zyra.core.data.settings

data class ZyraSettings(
    val textModelId: String = "",
    val liveModelId: String = "",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val persona: String = "Friendly",
    val customPersona: String = "",
    val voiceLanguage: String = "en-US",
    val voiceName: String = "",
    val voiceSpeed: Float = 1f,
    val voicePitch: Float = 1f,
    val pureBlack: Boolean = false,
    val autoTitle: Boolean = false,
    val toolsEnabled: Boolean = false,
    val toolToggles: Map<String, Boolean> = emptyMap(),
    val memoryEnabled: Boolean = false,
    val memoryConsentAsked: Boolean = false,
    val googleSearchGrounding: Boolean = false,
    val directCallsEnabled: Boolean = false,
    val listenForStopWhileSpeaking: Boolean = false
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are Zyra, Safari's AI assistant. Be fast, friendly, concise, and conversational. Speak naturally. Be honest about what you did and did not do. Never claim an action succeeded unless it actually did. Ask only when essential."
    }
}
