# ZYRA — Android AI Assistant

ZYRA is Safari's Android AI assistant. It is built in phases as a modular Android application.

## Requirements
- Android 9 (API 28) or newer
- JDK 17
- Android SDK 36

## Build
Open the project in Android Studio and sync Gradle. This repository does not yet include the Gradle wrapper, so use an installed Gradle 8.13 distribution. Build the debug APK with:

```bash
gradle :app:assembleDebug
```

## Current implementation
- Piece 1: modular Android scaffold, Kotlin, Compose, Hilt and Gradle version catalog.
- Piece 2: ZYRA vector brand, adaptive launcher icon, Canvas logo, jelly orb and animated splash.
- Piece 3: Room-backed message history, encrypted API key storage, DataStore model/personality settings, Gemini REST SSE text/image requests, chat and settings UI, contact launchers.
- Piece 4: foreground tap-to-talk fast voice, runtime microphone rationale, SpeechRecognizer partial transcript and RMS callbacks (on-device recognizer preferred on Android 12+), streamed sentence-by-sentence Android TextToSpeech, spoken/tap stop, and persisted voice language/speed/pitch settings.

## Gemini setup
1. Create/manage an API key in [Google AI Studio](https://aistudio.google.com/app/apikey).
2. In ZYRA Settings, save the key, enter a supported **text model ID**, save settings, then use **Test Gemini connection**.
3. Check Google's live [Gemini API model catalog](https://ai.google.dev/gemini-api/docs/models) before choosing a model. Verify the exact endpoint ID and that it supports `generateContent`/streaming and image input if you plan to attach images. Do not rely on old tutorials or retired model IDs.
4. The Live model ID field is reserved for the later Live API phase; it is not used by the current text chat.

The API key is not compiled into the app. It is stored locally with Android Keystore-backed Jetpack Security Crypto `EncryptedSharedPreferences`, as requested for this phase. Note: AndroidX has deprecated that library and announced no further releases; this is a compatibility choice for this requested implementation, not a recommended long-term storage API. Keep the encrypted preferences excluded from backups.

## Notes
- Chat messages and image copies are stored in the app's private Room/files storage and survive normal app restarts.
- Clearing chat history deletes Room messages and the app's copied chat images.
- The app does not ship with a model ID or API key hardcoded.
- Voice capture starts only after the user taps the mic orb and grants RECORD_AUDIO. Android's speech recognition provider and language pack availability vary by device; online recognition may be used when on-device recognition is unavailable.
- Never commit API keys or signing credentials.
- Fast voice mode uses Android SpeechRecognizer and TextToSpeech; it is not Gemini Live duplex audio. Background wake-word listening is not part of this phase. Speech amplitude is read from SpeechRecognizer callbacks; the app no longer opens a second AudioRecord stream for the same microphone session.

## Piece 5 — Android hardening (implementation in progress)

Changes currently staged on the `piece-5-hardening` branch:

- Added safe drawing-area and IME padding to chat and settings; set the activity soft-input mode to `adjustResize`.
- Removed the fixed 2.5-second navigation timer; the app now waits for the splash composable's `onFinished` callback.
- Added bounded retry/backoff (two retries) for Gemini HTTP 429/500/503 responses before the first streamed token; surfaced prompt-block, safety and output-limit metadata.
- Converted attached images to JPEG on Dispatchers.IO, downscaled to a 1600 px maximum edge and compressed at quality 85 before storage/upload.
- Removed the second AudioRecord capture path; SpeechRecognizer now supplies RMS callbacks and silence timeout hints.
- Added TTS markdown/link/emoji cleanup, transient assistant audio focus, utterance completion tracking, and fallback to the device's default TTS language when a requested voice is unavailable.
- Made Keystore initialization failure non-fatal; Zyra never falls back to plaintext key storage and reports a friendly error when secure storage cannot save a key.
- Stop/destroy voice recognition and playback when the app moves to the background; normalized spoken stop commands include English and common Roman Urdu phrases.
- Removed the duplicate voice assistant message during speech playback by persisting the completed assistant response after TTS finishes. Thinking no longer repeats the already-saved user transcript; speaking uses the normal assistant message presentation.
- Text submission requests voice shutdown first when a voice session is active.
- Made Gemini connection testing read-only; settings are persisted only by Save settings, which now shows a confirmation snackbar.
- Enabled Room schema export, added a timestamp index, and added the version 1-to-2 migration scaffold.

### Piece 5 work still outstanding

- EXIF-aware image orientation handling is not implemented yet.
- Per-session SpeechRecognizer callback invalidation and a user-configurable spoken-stop toggle still need implementation.
- A dedicated TTS language-pack installation prompt is not implemented; unsupported requested voices fall back to the device default.
- Full single-active-request serialization between simultaneous text and voice operations still needs verification and hardening.
- No automated tests have been added yet.

### Verification

- No unit tests have been added for this partial update.
- `gradle :app:assembleDebug` has **not** been run in this environment. The repository has no Gradle wrapper and this session cannot clone the repository to run the build locally.
- CI must pass on the pull request before these changes can be considered build-verified.

### Real-device verification still required

- Status/navigation bar spacing and keyboard avoidance on Android 9–16, including gesture and three-button navigation.
- Splash-to-chat transition timing.
- Voice recognition, TTS playback and stop behavior across OEM speech services and installed language packs.
- Microphone behavior, audio focus and interruption handling with wired/Bluetooth headphones and speaker output.
- Image attachment size, EXIF orientation (still outstanding) and Gemini request limits.
- Keystore recovery on devices with invalidated or unavailable Android Keystore keys.
