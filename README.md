# ZYRA — Android AI Assistant

ZYRA is Safari's Android AI assistant. The app uses Kotlin, Jetpack Compose, Hilt, Room, DataStore, Ktor and the Gemini REST API.

## Requirements

- Android 9 (API 28) or newer
- JDK 17
- Android SDK 36
- Gradle 8.13

## Build

The repository includes the Gradle 8.13 wrapper scripts and distribution configuration. On the first same-repository pull-request run, CI generates the official wrapper JAR and commits it to the PR branch; it is also uploaded as the `gradle-wrapper-8.13` artifact. If you check out the branch before that bootstrap commit arrives, bootstrap locally with an installed Gradle 8.13 distribution:

```bash
gradle wrapper --gradle-version 8.13 --distribution-type bin
chmod +x ./gradlew
./gradlew lintDebug testDebugUnitTest assembleDebug
```

GitHub Actions bootstraps the wrapper, runs lint, unit tests and the debug build, then uploads `app-debug.apk` as the `zyra-debug-apk` artifact.

## Current implementation

- **Core app:** modular Kotlin/Compose app, Hilt dependency injection, Android 9+ support, Android SDK 36 target, dark-glass design system and responsive chat/settings surfaces.
- **Onboarding:** first-run welcome, Google AI Studio launch, Gemini key validation, model selection, microphone rationale and runtime permission, installed voice-language selection, and persisted completion state. Settings can launch onboarding again.
- **Gemini catalog:** paginated `v1beta/models` discovery using the saved key. The picker separates `generateContent` text models from `bidiGenerateContent` Live models and shows display name, ID and token limits. Manual model ID entry remains available. No production model ID or API key is hardcoded.
- **Settings:** dedicated `feature-settings` module and `SettingsViewModel`; connection test, model pickers, Friendly / Professional / Roman-Urdu buddy / Custom persona, installed recognizer and TTS locale union, installed TTS voice picker, speed/pitch and test speech, reduce effects, pure-black appearance, opt-in Gemini conversation auto-title, clear history, JSON/Markdown share export, API-key deletion, version, source/licence link and contact/issue link.
- **Conversations:** conversation list, new chat, rename, delete and cross-conversation message search. The current conversation switcher is a dialog rather than a persistent navigation drawer. Optional auto-title is off by default and runs only when a new text chat starts.
- **Long context:** chat and voice requests maintain a rolling extractive summary of older turns and pass recent history through a token-estimated budget. Earlier image attachments are represented as short text notes; only the current image is sent as image data.
- **Voice:** tap-to-talk SpeechRecognizer and streamed Android TextToSpeech, state-machine transitions, installed recognizer/TTS locale discovery, selectable TTS voice, stop and interruption handling. This is not Gemini Live duplex audio or background wake-word listening.
- **Security:** new API-key writes use Android Keystore AES-GCM; the wrapped ciphertext is stored in DataStore. On first read, a prior Jetpack Security Crypto value is migrated and then cleared from the legacy preference. The legacy dependency remains only for this migration path. Android backup is disabled.
- **CI/release:** Gradle 8.13 bootstrap, lint, unit tests, debug APK artifact, R8 minification and an opt-in signed release job for version tags. Signing keystore bytes and passwords are read only from repository secrets; nothing is committed.

## Database and key migration notes

The previous branch already used Room schema version 2 for the timestamp-index migration. Piece 7 therefore advances the database to **version 3** rather than reusing version 2:

1. Existing v1 databases run the existing v1→v2 timestamp-index migration.
2. v2→v3 creates the `conversations` table, inserts the default conversation (ID 1), adds `conversation_id` to existing messages with default 1, and creates a conversation/timestamp index. Existing messages remain in the default conversation.
3. Fresh installs create the default conversation when Room creates the database.
4. Existing `zyra_secrets/gemini_api_key` values are read through the legacy encrypted-preferences implementation, encrypted with a Keystore AES-GCM key, written to the `zyra_secure_values` DataStore, and removed from the legacy preferences only after the new write succeeds.

No destructive Room fallback is configured. If Android Keystore cannot decrypt an existing value, ZYRA reports that secure storage is unavailable rather than storing the key in plaintext.

## How to test each flow

### First-run setup

1. Install a fresh debug build or clear app data.
2. Start onboarding and open Google AI Studio.
3. Paste a valid Gemini API key and select **Validate key and load models**.
4. Search the returned model list; choose a text model and, if available, a Live model. Try manual ID entry as the fallback.
5. Allow or skip microphone permission.
6. Choose one of the installed recognizer/TTS languages and finish.
7. Restart the app; onboarding should not appear again unless requested from Settings.

### Settings

1. Open Settings and refresh the model catalog.
2. Test the Gemini connection.
3. Switch persona, language, TTS voice, speed and pitch; use **Test voice**.
4. Toggle reduce effects, pure black and auto-title, save, leave Settings and reopen it to check persistence.
5. Export JSON and Markdown through the Android share sheet.
6. Clear history and verify messages and copied image files are removed. Delete the API key and verify the connection status changes.
7. Use **Run onboarding again** to repeat setup.

### Conversations and context

1. Create multiple chats, rename one, switch between them and delete one.
2. Search for a phrase from a different conversation and open its result.
3. Enable auto-title, create a new text chat and send its first message; disable the setting to keep manually named chats.
4. In a long conversation, continue beyond the recent-history budget. Older turns should be represented by the rolling summary; previous images should appear as text notes in history, not be re-uploaded.

### Automated tests

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

The test sources cover Gemini SSE parsing with Ktor MockEngine, voice state transitions, speech-text sanitization, DataStore settings persistence and Room v2→v3 migration. The DataStore and migration tests are Android instrumentation tests and require an emulator/device:

```bash
./gradlew connectedDebugAndroidTest
```

### Signed release setup

Set repository variable `ENABLE_SIGNED_RELEASE=true` and these repository secrets:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Push a version tag such as `v0.2.0`. The release job decodes the keystore into the runner's temporary directory, uses the GitHub run number as `versionCode`, builds the minified signed APK and attaches it to the GitHub Release.

## Validation status and real-device checks

Do not treat this branch as verified until the latest GitHub Actions run is green. Instrumentation tests are not part of the hosted unit-test step unless an emulator is provisioned.

Still required on physical devices/emulators:

- First-run and rerun onboarding on Android 9–16; permission denial, permanent denial and grant flows.
- Keystore migration from a real previous installation, lock-screen changes, Keystore invalidation and app reinstall behavior.
- Gemini API key validity, pagination, quota errors, Live-capable model availability and actual model token limits.
- Installed speech-recognizer/TTS locale availability across OEMs; voice selection, Urdu/Roman Urdu, speed/pitch, audio focus, Bluetooth and interruption behavior.
- Conversation migration using a copy of a real v2 database; long-context quality and auto-title quality.
- Camera/gallery attachments, image cleanup, RTL Urdu, landscape, tablet/foldable layouts, TalkBack, reduce-effects and pure-black appearance.
- Signed release only after repository secrets and the release variable are configured.

Never commit API keys, signing credentials or keystore files.


## Piece 8 — Zyra Everywhere

This branch now includes the usable integration surfaces and a foreground-only Live voice path:

- Gemini Live WebSocket transport and LiveVoiceSession orchestration: explicit 16 kHz PCM microphone capture, 24 kHz PCM playback, input/output captions, interruption handling and a visible Live/Stop Live control. Fast voice remains available as a manual fallback.
- Android VoiceInteractionService and VoiceInteractionSessionService, with a compact system assistant surface. Settings includes the Set as default assistant action, which asks Android to grant the assistant role.
- Quick Settings tile that opens Zyra's visible voice flow, a Glance Ask Zyra home-screen widget, dynamic Ask Zyra and Voice chat shortcuts, and Android Sharesheet support for shared text or a single image.
- A Stop all assistant activity privacy action. Voice sessions are also stopped when the app leaves the foreground.
- The foreground microphone-service shell is registered but is not started automatically. Background wake-word listening remains disabled.

### External requirement: Hey Zyra wake word

The offline WakeWordEngine contract remains fail-closed. A real custom Hey Zyra detector requires a licensed wake-word SDK account and the generated phrase model (for example, a Picovoice .ppn model). Those account-specific assets cannot be generated or safely embedded without the owner's licence and model. No microphone is opened for wake-word detection, and the app must not claim that Hey Zyra is listening.

### Remaining verification and hardening

- Validate the Gemini Live wire protocol against the exact model available to the user's API key; implement session resumption and automatic Fast fallback on GoAway/network loss.
- Package and test the licensed local wake-word model, sensitivity control, call/audio-focus pause behavior and OEM battery guidance after the owner supplies the model/licence.
- Test the VoiceInteraction role, notification behavior, share intents, widget and Quick Settings tile on Android 9–16 and OEM devices.
- Run lint, unit tests, assembleDebug and physical-device microphone/route/Doze tests. The GitHub Actions run for this branch is the build gate; a queued/running job is not a pass.

### Piece 8 references

- [Architecture and privacy invariants](docs/architecture-piece-8.md)
- [Wake-word engine comparison and external asset requirement](docs/wake-word.md)
- [Permissions, battery method and device test plan](docs/permissions-piece-8.md)
- [Official Gemini Live API overview](https://aistudio.google.com/docs/live-api)
- [Official Gemini Live API capabilities](https://aistudio.google.com/docs/live-api/capabilities)
- [Official Gemini Live WebSocket guide](https://aistudio.google.com/docs/live-api/get-started-websocket)

No local Gradle build or physical-device test has been run from this editing session. Do not treat Piece 8 as release-ready until GitHub Actions is green and the device matrix above passes.

Never commit API keys, SDK licence keys, custom wake-word model assets or signing credentials.
