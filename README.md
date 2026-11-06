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
- Piece 4: foreground tap-to-talk fast voice, runtime microphone rationale, 16 kHz mono AudioRecord amplitude/VAD, partial SpeechRecognizer transcript (on-device recognizer preferred on Android 12+), streamed sentence-by-sentence Android TextToSpeech, spoken/tap stop, and persisted voice language/speed/pitch settings.

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
- Fast voice mode uses Android SpeechRecognizer and TextToSpeech; it is not Gemini Live duplex audio. Background wake-word listening is not part of this phase. A real-device test is required to confirm concurrent microphone amplitude capture behaves correctly with the device's speech-recognition service.
