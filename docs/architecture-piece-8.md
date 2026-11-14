# Piece 8 architecture — Zyra Everywhere

Status: foundations only. This document distinguishes implemented foundations from device/API work that remains.

## Component boundaries

- `AssistantServiceController` is the app-facing control boundary for starting/stopping a user-visible assistant session.
- `ZyraAssistantService` owns the ongoing notification and service lifetime. It must only be started while the app is visible or from a user-initiated surface, after the required runtime permissions are granted.
- `GeminiLiveClient` owns the Live API WebSocket protocol. It accepts the configured model ID and system instruction; it does not choose or embed a model.
- `LiveAudioEngine` is the intended platform boundary for microphone capture, effects, PCM framing and playback.
- `WakeWordEngine` is a separate, opt-in offline keyword-spotting boundary. It must never silently substitute cloud speech recognition.
- Settings are persisted through `SettingsRepository`; all new behavior must default off.

## Live protocol

The current official Live API uses a stateful WSS connection to `BidiGenerateContent`. The first client frame is `setup`; the client waits for `setupComplete`. Audio input is raw signed 16-bit little-endian PCM at 16 kHz with MIME type `audio/pcm;rate=16000`. Output is raw PCM at 24 kHz. Server events can include multiple model-turn parts, input/output transcription, interruption and turn-completion signals; clients must iterate all parts and clear queued output on interruption.

The current implementation uses the configured model and API key supplied at runtime. For production deployments, Google recommends ephemeral tokens rather than a long-lived API key in a client WebSocket URL. Do not log the WebSocket URL.

## Privacy invariants

1. No wake-word microphone session unless the user explicitly enables it.
2. Wake-word inference is on-device. No captured audio is uploaded or persisted before a positive local wake-word match.
3. A visible foreground notification and Android microphone indicator accompany an open microphone.
4. Stop-all must stop capture, playback, WebSocket activity and the foreground service.
5. Pause wake-word inference during calls, microphone privacy toggles, and competing audio-recording sessions.
6. Any Live API audio is transmitted only after the user has enabled Live mode and explicitly started a voice session or a locally detected wake phrase.

## Current implementation boundary

The Live WebSocket transport is added as a reusable protocol client. The Android foreground service is a lifecycle/notification shell. Production-grade audio routing, session resumption, wake-word model integration, the compact assistant UI, privacy dashboard, Glance widget, app shortcuts, share receiver, and full VoiceInteractionService session UX are not claimed complete by this foundation commit. Keep those features disabled until their end-to-end paths are implemented and tested.
