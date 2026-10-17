# app

The Android application: onboarding, the calendar permission, the settings UI (Compose, Material 3) with a live
preview, and the "set as wallpaper" flow. It is also the composition root (manual constructor injection, ADR 0001).

**Screens**
- **Onboarding:** first launch only.
  - A welcome page.
  - Calendar access, with a rationale. It is asked here, in context, and never at startup. If Android stops showing the
    dialog, the app links to system settings.
  - Choosing live or battery-saver mode.
- **Settings:** every option of CLAUDE.md §6, under a live preview that stays on screen. The preview uses the same
  layout engine and Canvas renderer as the wallpaper. It shows the real agenda, or a sample one until calendar access is
  granted, and can switch to the lock-screen look (privacy mode). A contrast warning appears below WCAG AA (4.5:1).
- **Wallpaper:**
  - Live mode opens the system live-wallpaper preview. The system, not the app, asks home, lock or both.
  - Battery saver mode sets a still image on the screens chosen here, and re-applies it about 1.5 s after the last change.
- **Performance:** layout and draw times, bitmap size and process memory, as measured by the wallpaper itself
  (`docs/performance.md`).

**Taps and widgets** (ADR 0005)
- **`open/OpenTargetActivity`:** the deep-link router. It has no UI and is not exported. It turns an
  `OpenTargetLink` into the best Intent an installed app handles.
- **`open/LaunchPlan`:** pure. It produces candidates with web fallbacks.
- **`open/LaunchIntents`:** two passes. First `resolveActivity` (with the manifest's `<queries>`), then
  `ActivityNotFoundException` as a fallback.
- **`widgets/`:** the Agenda and Week widgets with their receivers, and `WidgetFrames`, which renders frames with the
  wallpaper's pipeline.

**Key classes**
- `AppServices`: what the UI needs, sharing the process's `WallpaperGraph`.
- `SettingsEditor`: draft-while-dragging, one write per change.
- `Contrast`: the contrast warning.
- `SampleAgenda`: preview data.
- `rememberCalendarPermission`: permission state for Compose.

**Facts and rules**
- **Depends on:** every other module. Nothing may depend on `app`.
- **applicationId:** `io.github.nabin47.calwallpaper`. It is permanent and guarded by `ApplicationIdTest`. Debug builds add `.debug`.
- **Permissions:** `READ_CALENDAR`, plus `SET_WALLPAPER` and `RECEIVE_BOOT_COMPLETED` for battery saver mode (ADR 0004).
  Taps and widgets add none; `<queries>` lists only calendar, Todoist and browser intents.
- **Release:** R8 full mode + resource shrinking via `optimization { enable = true }`; signing from CI secrets (docs/release.md).
