# Piece 9 — Manual verification checklist

## Build and automated tests

- [ ] Run ./gradlew :core-network:testDebugUnitTest and confirm GeminiToolLoopTest passes with the fake model/executor.
- [ ] Run ./gradlew :core-data:testDebugUnitTest.
- [ ] Run ./gradlew lintDebug testDebugUnitTest assembleDebug.
- [ ] Run connected Android tests on an emulator/device, especially the Room 3-to-4 migration.
- [ ] Confirm the CI debug APK artifact is generated; do not use a queued run as a pass.

## Tool safety

- [ ] Fresh install: global tool switch is off and all individual tool toggles are off.
- [ ] Enable one low-risk tool only; verify other tools are not declared to Gemini.
- [ ] Global kill switch off: verify all tool calls return Denied.
- [ ] High-risk call: exact number appears in confirmation sheet; cancel means dialer never opens; confirm opens dialer only and user must tap Call.
- [ ] High-risk SMS and WhatsApp: verify prefilled recipient/body; no message sends until user taps Send.
- [ ] Medium alarm, timer, calendar, flashlight, volume and media controls ask once per session; repeat after process restart.
- [ ] Flashlight permission denied, granted and permanently denied; torch must never be enabled before confirmation and permission.
- [ ] Brightness without WRITE_SETTINGS opens the special-access screen; retry after granting and verify brightness range.
- [ ] Wi-Fi and Bluetooth actions open system settings and do not silently change radio state.
- [ ] Unsupported intent handler: tool reports failure instead of claiming success.
- [ ] Stop the app process and verify the session-only action log is cleared.

## Memory and privacy

- [ ] Memory first-use screen is shown; Not now leaves memory off.
- [ ] Accept memory opt-in, then use remember_fact; verify persistence after app restart.
- [ ] Attempt to save password/API-key/card-like values; verify rejection.
- [ ] Disable memory; verify prompt injection stops and memory tools deny writes/deletes.
- [ ] Edit, delete and export saved memories; verify Room reflects the changes.
- [ ] forget_fact requires confirmation and deletes only the exact matching title.
- [ ] Verify memory context stays within the configured ~500-token text budget and is treated as context, not instructions.

## Gemini tool loop and grounding

- [ ] Select a model that supports function calling; ask for device information and inspect the functionCall → functionResponse → final response sequence.
- [ ] Trigger two sequential tool calls; verify the second request includes the prior functionResponse.
- [ ] Trigger malformed/unknown function names, API errors, timeout and max-iteration limit; verify no false success.
- [ ] Enable Google Search grounding only after reviewing current pricing and model support; verify citations/source URLs appear in chat.
- [ ] Verify models without grounding support show a clear API error and no invented sources.

## Device matrix

- [ ] Android 9 (API 28): Wi-Fi settings fallback, dialer, SMS, alarm, calendar and navigation intents.
- [ ] Android 10+ (API 29+): Wi-Fi Settings panel, torch permission, WRITE_SETTINGS and media key routing.
- [ ] Android 13+ notification/permission behavior; Android 14–16 target SDK 36 behavior.
- [ ] Test on OEM builds (including Vivo/Funtouch): app labels visibility, torch camera ID, special settings pages and installed default handlers.
- [ ] TalkBack, large text, dark theme and RTL Urdu in confirmation sheets and memory controls.

## Known deferred items

- Direct calling, notification-listener summaries, current-location reads and clipboard reads are not implemented.
- Persistent action log, robust permission-rationale history and background-safe notification service are not implemented.
- AccessibilityService automation is intentionally out of scope.
