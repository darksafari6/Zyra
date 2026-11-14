# Piece 9 — Privacy, grounding and policy notes

## Memory

Long-term memory is off until the user explicitly accepts the first-use prompt. Turning it off stops memory injection and memory tools. Facts are stored locally in Room. Values are capped at 500 characters and titles at 80. Secret filtering rejects obvious password/API-key/token labels, private-key blocks, bearer tokens and long card-like number sequences. This heuristic is not a guarantee; do not store secrets. The user can view, edit, delete and export saved facts.

Memory is injected only when enabled, using the latest 20 facts and a 2,000-character budget (roughly 500 tokens). Memory is quoted as user-approved context and must not override the system prompt or tool safety policy.

## Google Search grounding

Google Search grounding is an opt-in Gemini built-in tool. Availability depends on the selected model and API tier; enabling the switch can cause Google Search requests and additional charges. Google's current pricing page distinguishes model families and tiers. As of 13 November 2026, Gemini 3 and newer models show 5,000 free Search requests per month shared across those models, then USD $14 per 1,000 search requests, with a default daily cap; Gemini 2.5 pricing uses a different quota and USD $35 per 1,000 grounded prompts after its applicable free quota. A single user prompt can generate more than one Search query. Token charges for the Gemini request still apply. Review the [live Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing) and [grounding guide](https://ai.google.dev/gemini-api/docs/google-search) before enabling this for production.

The app appends only HTTPS source URLs returned in Gemini's grounding metadata. It must not invent citations when no grounding metadata is returned.

## AccessibilityService automation — future opt-in only

No AccessibilityService is registered or implemented in this phase. Before considering it:

- Prefer narrow Android intents, public APIs and explicit user actions wherever possible.
- Do not use the service to bypass platform security/privacy controls, silently change settings, record remote call audio, or autonomously plan and execute arbitrary actions.
- Google Play's policy says general-purpose assistants are not eligible to mark themselves as an accessibility tool using isAccessibilityTool=true; that designation is for apps whose primary purpose is disability support.
- A non-accessibility app using AccessibilityService would need the Play Console declaration, a prominent in-app disclosure during normal use describing exactly what data is accessed and why, affirmative consent, accurate Play listing disclosure, and continued policy compliance.
- Play policy prohibits Accessibility API use for autonomous agents that autonomously initiate, plan and execute actions or decisions. This is a major constraint for Zyra; a future design must be deterministic, user-scripted and tightly scoped, or not use AccessibilityService.
- Never add the service until legal/policy review, user research, clear consent UX, and a reviewable narrow feature specification are complete.

References:
- [Google Play AccessibilityService API policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Google Play Developer Program Policy — Accessibility API](https://support.google.com/googleplay/android-developer/answer/18265346)
- [Android Settings.Panel API](https://developer.android.com/reference/android/provider/Settings.Panel)
