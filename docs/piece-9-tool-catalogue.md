# Piece 9 — Tool catalogue and safety contract

Device tools are opt-in twice: the global Device tools kill switch must be on, and each individual tool must be enabled. All per-tool switches default off. Gemini only receives declarations for enabled tools.

| Tool | Risk | Android permission / user action | Behavior |
|---|---|---|---|
| open_app | LOW | Launcher app visibility query | Opens exact package or exact visible launcher label |
| open_url | LOW | Browser handler | Opens validated HTTP(S) URL |
| web_search | LOW | Search handler | Opens Android web-search UI; this is not Google Search grounding |
| call | HIGH | No CALL_PHONE permission; dialer handoff | Opens ACTION_DIAL. Zyra does not place calls directly |
| message | HIGH | SMS app; WhatsApp if selected | Prefills SMS or WhatsApp chat. User must tap Send |
| set_alarm | MEDIUM | Alarm app | Opens system alarm editor; user reviews/saves |
| set_timer | MEDIUM | Clock app | Opens system timer editor; user reviews/starts |
| calendar_event | MEDIUM | Calendar app | Opens event insert editor; user reviews/saves |
| flashlight | MEDIUM | CAMERA runtime permission | Uses CameraManager torch mode after permission and confirmation |
| volume | MEDIUM | AudioManager | Sets media stream volume |
| media_control | MEDIUM | Active media session / media app | Dispatches play, pause, play/pause, next or previous media key |
| navigation | LOW | Maps/deep-link handler | Opens a route in an installed navigation handler |
| device_info | LOW | None | Reads battery percentage, local time and network connectivity |
| wifi_settings | LOW | Android system settings | Opens Settings panel on API 29+ or Wi-Fi settings on API 28; user changes switch |
| bluetooth_settings | LOW | Android system settings | Opens Bluetooth settings; user changes switch |
| brightness | MEDIUM | Special WRITE_SETTINGS access | Opens Android's Modify system settings page if not granted; user must grant and retry |
| remember_fact | MEDIUM | Memory opt-in + tool toggle | Saves a short non-sensitive fact in Room |
| forget_fact | HIGH | Memory opt-in + tool toggle | Deletes by exact memory title after per-action confirmation |

## Confirmation rules

- LOW: executes when global and individual toggles are enabled.
- MEDIUM: one explicit confirmation per tool per app session. Permission prompts occur only after the user confirms the exact action.
- HIGH: exact arguments are displayed in the confirmation sheet and require confirmation every time.
- The app logs requested tool name, arguments summary, time and result. The current log is in-memory and resets when the app process ends.
- A global kill switch is checked again immediately before a pending action executes.
- Tool results distinguish success, denied, failed and waiting-for-confirmation. Intent-based handoffs say that the system app was opened and that the user still needs to complete the action.

## Deliberate limits in this pass

- Direct calling is disabled. Calls always open the dialer; no CALL_PHONE flow is shipped.
- No tool sends an SMS or WhatsApp message without the user pressing Send.
- Wi-Fi and Bluetooth are never silently toggled.
- Optional notification summaries, location reads and clipboard reads are not registered yet. If added later, each needs a separate explicit opt-in, a prominent explanation and a narrow request-time path.
- The tool log is session-only. Persistent action history is a follow-up.
- The tool framework exposes JSON parameter schemas, risk levels and required permissions. The permission UI currently covers runtime permissions declared by these tools; WRITE_SETTINGS uses Android's special-access screen.

## Gemini function calling

The client uses tools: [{ functionDeclarations: [...] }], reads candidates[].content.parts[].functionCall, executes only in the app, then appends a model turn and a user functionResponse turn before requesting the next model response. The loop is bounded to four tool-call rounds; individual tool execution is bounded to 12 seconds and Ktor's request timeout bounds the network request. Function responses contain explicit status and human-readable detail. The model is never told that an action succeeded unless the Android action returned successfully.

The REST wire shape follows Google's [Gemini function-calling guide](https://ai.google.dev/gemini-api/docs/function-calling). Validate exact support against the selected model because function calling is model-dependent.
