# Offline wake-word engine evaluation

Evaluation date: 2026-11-13. This is a design decision, not a benchmark run on Zyra hardware.

| Engine | Licence / distribution | Footprint | Accuracy and battery | Custom phrase |
|---|---|---|---|---|
| Picovoice Porcupine | Proprietary SDK; commercial terms and an access key apply | Small native Android runtime plus a phrase model | Designed specifically for always-on KWS and low-power operation; actual accuracy/battery depends on device and model | Custom phrase models can be built through Picovoice Console; availability/terms depend on account |
| openWakeWord | Apache-2.0 project; core runtime is Python-oriented and Android-native integration is not turnkey | Model/runtime can be materially larger than a small embedded KWS engine | Good community experimentation; Android power and false-accept performance need a dedicated port and target-device validation | Custom models are possible, but training/data and Android packaging are additional work |
| Vosk | Apache-2.0 | Acoustic models are typically tens of MB or more | General offline ASR, not a dedicated wake-word detector; continuous decoding is usually a worse battery/false-trigger tradeoff | Grammar constraints can bias recognition, but this is not equivalent to a trained KWS phrase model |

## Selection

**Preferred production candidate: Porcupine**, conditional on obtaining an appropriate licence and a signed/custom `Hey Zyra` model. It has a native Android SDK and is purpose-built for wake-word detection. This repository does not include a proprietary access key or generated model, and therefore must not pretend that phrase detection is active.

Until those inputs are available, the safe implementation is a no-op `WakeWordEngine` provider with the feature off. Do not replace it with Android `SpeechRecognizer`: some providers can send audio to remote services, which would violate the requirement that audio remains local before wake detection.

## Acceptance gates before enabling

- Include the licence text and model provenance in the release.
- Verify inference is local with network disabled.
- Measure false accepts and false rejects over quiet rooms, TV/music, different distances, accents and normal conversation.
- Measure average and peak current over at least 8 hours with screen off.
- Pause during calls and while another app owns audio input.
- Verify that no PCM, transcript or debug recording is written before a match.
- Expose sensitivity only if the selected engine's model/runtime supports a meaningful threshold.
