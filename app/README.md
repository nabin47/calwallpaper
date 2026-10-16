# app

The Android application: settings UI (Compose, Material 3), onboarding, permissions, and the deep-link router.
It is also the composition root (manual constructor injection, ADR 0001).

- **Phase 0:** `MainActivity` shows a hello screen with the app version.
- **Depends on:** every other module. Nothing may depend on `app`.
- **applicationId:** `io.github.nabin47.calwallpaper`. It is permanent and guarded by `ApplicationIdTest`. Debug builds add `.debug`.
- **Release:** R8 full mode + resource shrinking via `optimization { enable = true }`; signing from CI secrets (docs/release.md).
