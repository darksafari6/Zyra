# Piece 8 permissions and device test plan

## Permission inventory

| Permission / capability | Why it is needed | When requested |
|---|---|---|
| `RECORD_AUDIO` | User-approved microphone capture for Fast/Live voice and optional local wake-word inference | At the point the user enables or starts a microphone feature |
| `FOREGROUND_SERVICE` | Keep an explicitly started assistant service alive with a persistent notification | Manifest declaration; no runtime dialog |
| `FOREGROUND_SERVICE_MICROPHONE` (Android 14+) | Declare microphone foreground-service use on Android 14+ | Manifest declaration; no runtime dialog |
| `POST_NOTIFICATIONS` (Android 13+) | Show the persistent service notification and controls | Runtime request before starting the service |
| `INTERNET` | Gemini Live WebSocket traffic after an explicitly started Live session | Manifest declaration; no runtime dialog |
| `READ_PHONE_STATE` | Not requested by this design. Prefer audio-focus/recording-configuration signals and avoid collecting call details. | Not applicable |
| Battery-optimization exemption | Not required and should not be requested automatically. Guide the user to OEM settings only if wake-word reliability needs it. | User-initiated system settings screen, with clear explanation |

Android system role/assistant selection is user-mediated through system settings; Zyra must not attempt to grant itself the role.

## Battery impact method

Use a repeatable 8-hour screen-off test on the same device, battery state, network and ambient conditions. Compare (A) app force-stopped, (B) service notification only, (C) wake-word engine enabled, and (D) repeated Live sessions. Record Android Battery Historian / batterystats deltas, elapsed time, battery percentage, CPU time, wakeups, mobile/Wi-Fi bytes, false detections and temperature. Repeat each condition at least three times; report median and range. Use an external USB power monitor where available. Do not infer battery life from CPU usage alone.

## Real-device matrix

- Android 9–10, Android 11–12, Android 13, Android 14–16.
- Screen on/off, Doze, app standby, battery saver and reboot recovery.
- Pixel/AOSP, Samsung One UI, Xiaomi/Redmi HyperOS/MIUI, Oppo/Realme ColorOS, Vivo Funtouch OS and Tecno/Infinix HiOS.
- Wired headset, Bluetooth headset (SCO and media routes), speaker, Bluetooth route changes mid-session.
- Incoming/outgoing phone call, VoIP call, competing recorder, microphone privacy toggle and permission revocation.
- Notification denied, microphone denied/permanently denied, no network, invalid Live model, expired API credentials, WebSocket timeout and server interruption.
- Quick Settings tile start/stop, assistant role selection, app shortcut, home widget and text/image share target.

## Release gate

No wake-word option may be enabled by default. Do not describe background wake-word, default-assistant UX, Glance, share target, battery behavior or OEM compatibility as working until the complete path has passed the matrix above on physical devices.
