# Piece 10 — Close the gaps: implementation status

This document records only changes present on `piece-10-close-gaps`. It is intentionally explicit about unfinished work; this branch is not a complete Piece 10 delivery yet.

## Completed in this branch

- GitHub Actions now runs the Android verification job for pull requests targeting `main` and direct pushes to `main`. The workflow still runs the signed-release job only for version tags when release configuration is enabled.
- Fixed the Fast voice final-sentence TTS bug: `flushSentence` now passes the selected `voiceName`.
- Added the opt-in **Listen for stop while speaking (best with headphones)** setting. It defaults off, is persisted through DataStore, and barge-in recognition is armed only when enabled. English and Urdu strings are supplied for this new setting.
- Fast voice now uses the same Gemini tool conversation client, tool registry, action policy engine, and opted-in memory context as text chat. Pending MEDIUM/HIGH confirmations continue to use the shared confirmation state; voice speaks a prompt asking the user to confirm on screen.
- Tool-enabled text requests can include an image. The current image is sent as Gemini inline image data in the same user turn rather than silently bypassing tool calling.
- Settings offers Android's `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA` when the selected voice locale has no installed TTS voice. Android owns the installation flow; the app must re-check availability after the user returns.
- Removed the unused `AudioRecorder.kt` and both unused module marker classes.
- Added an Urdu translation for the new voice-stop setting.

## Live API authentication decision

The current app has no authenticated backend that can safely provision short-lived Live tokens. Google's current documentation recommends ephemeral tokens for client-to-server Live API applications, but token provisioning is a backend operation. Calling the token endpoint directly from this app with the user's long-lived API key would still expose that key to a client and would not meet the intended security boundary. Therefore this branch documents the requirement but does **not** pretend that ephemeral-token auth is implemented. The current Live WebSocket still uses the API key query parameter and should not be treated as production-hardened until a backend token broker is added.

Official references:
- [Gemini Live ephemeral tokens](https://ai.google.dev/gemini-api/docs/live-api/ephemeral-tokens)
- [Gemini Live WebSocket setup and ephemeral-token endpoint](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket)
- [Live API session resumption and GoAway](https://ai.google.dev/gemini-api/docs/live-api/session-management)
- [Live API WebSocket protocol, toolCall and toolResponse messages](https://ai.google.dev/api/live)
- [Android TTS install-data action](https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine#ACTION_INSTALL_TTS_DATA)
- [Android VoiceInteractionSession content view](https://developer.android.com/reference/android/service/voice/VoiceInteractionSession#onCreateContentView())

## Still open — do not mark Piece 10 complete

- Gemini Live function calling and safe MEDIUM/HIGH voice confirmations are not wired into the Live WebSocket session.
- Live session resumption is not implemented. Since the current Android transport does not retain resumption handles, GoAway, socket failure and capture/network failure now stop Live and start Fast voice in the active conversation with a visible status message. Google supports session resumption, so a future implementation should prefer that when a valid handle is available.
- The system assistant session now uses a compact rounded translucent-styled content surface, but its actual system-window sizing/translucency still needs on-device validation.
- Temperature and maxOutputTokens are not yet configurable in Settings or applied consistently to text, tool and Live requests.
- SettingsScreen and OnboardingScreen still contain hardcoded English strings; complete resource extraction and Urdu localization are not done.
- No OFL display/body fonts have been bundled yet; Typography still needs the font-family integration.
- The optional API-33+ AGSL background has not been added.
- The rolling conversation summary is still extractive/truncated text, not a Gemini-generated rolling summary.
- Dedicated ChatViewModel fake-client tests, conversation DAO tests and voice-plus-tool integration tests remain to be added.
- The Privacy screen still needs mic state, last voice-session timestamp and last tool actions surfaced together.

## Verification required

No local Gradle build or physical-device test was run while making these commits. GitHub Actions must pass for the current PR head before the branch can be called build-verified. The following still require a real Android device/emulator:

1. Fast voice tool invocation, spoken confirmation prompt, MEDIUM once-per-session and HIGH every-time confirmation, cancel/deny paths, and permission grants/denials.
2. Image-plus-tool requests with a real Gemini model that supports the selected function declarations and image input.
3. Barge-in disabled by default, enabled with headphones, false-positive handling, and microphone/audio-focus behavior across OEMs.
4. Missing TTS language installation, return from the system installer, and voice catalog refresh.
5. Android assistant role and translucent system surface.
6. Live GoAway, network loss, resumption/fallback behavior, Live function calls, token expiry and WebSocket authentication.
7. Urdu RTL, TalkBack, font rendering, low-memory devices and API 28–36.

Wake-word listening remains disabled. No wake-word model or licence asset was added.
