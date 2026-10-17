# app

The Android application: onboarding, the calendar permission, the settings UI (Compose, Material 3) with a live
preview, and the "set as wallpaper" flow. It is also the composition root (manual constructor injection, ADR 0001).

**Screens** (ADR 0009)
- **Splash:** AndroidX SplashScreen until the settings are loaded.
- **Onboarding** (first launch only), in four steps with progress, Back and Skip:
  1. Welcome, on a live preview.
  2. Calendar access, with a rationale. It is asked here, in context, never at startup; if Android stops showing
     the dialog, the app links to system settings.
  3. Pick a layout and colors.
  4. Set the wallpaper, explaining Android's own preview. Returning with it set finishes onboarding.
- **Home** (`ui/home/`):
  - a large preview (home or lock screen) and a status card with the next step;
  - a calendar-access card while the preview shows sample events;
  - layout thumbnails and color swatches (one tap);
  - rows to every page with a summary of their values;
  - "Add task".
- **Pages** (`ui/pages/`, hosted by Navigation 3, keys in `ui/navigation/Route`):
  - Appearance, Layout, Lock screen & privacy (always previewed as the lock screen), Calendars & tasks, Todoist,
    Date & time, Wallpaper mode;
  - Widgets & shortcuts (pin widgets, add the tile), Backup & reset (export, import, reset with Undo), Help & about
    (answers, performance numbers, links).

  Visual pages pin the preview above the controls (`ui/components/PageScaffold`). The preview uses the same layout
  engine and Canvas renderer as the wallpaper, with the real agenda or a sample one until calendar access is
  granted. A contrast warning appears below WCAG AA (4.5:1).
- **Wallpaper:**
  - Live mode opens the system live-wallpaper preview. The system, not the app, asks home, lock or both.
  - Battery saver mode sets a still image on the chosen screens, and re-applies it about 1.5 s after the last change.

**Taps and widgets** (ADR 0005)
- **`open/OpenTargetActivity`:** the deep-link router. It has no UI and is not exported. It turns an
  `OpenTargetLink` into the best Intent an installed app handles.
- **`open/LaunchPlan`:** pure. It produces candidates with web fallbacks.
- **`open/LaunchIntents`:** two passes. First `resolveActivity` (with the manifest's `<queries>`), then
  `ActivityNotFoundException` as a fallback.
- **`widgets/`:** the Agenda and Week widgets with their receivers, and `WidgetFrames`, which renders frames with the
  wallpaper's pipeline.

**Todoist and quick-add** (ADR 0007)
- **`todoist/TodoistConnection`:** connect with a pasted token, disconnect, sync now, and the status.
- **`todoist/TodoistSyncScheduler`:** periodic work every 30 minutes, plus "sync now", with exponential backoff.
- **`todoist/TodoistSyncWorker`:** runs one sync; redraws widgets and the static image when data changed.
- **`ui/settings/TodoistSection`:** token entry, status and error messages, "Sync now", "Disconnect", projects.
- **`quickadd/`:** the quick-add dialog activity, the Quick Settings tile and the launcher shortcut
  (`res/xml/shortcuts.xml`; debug has its own copy).

**Key classes**
- `AppServices`: what the UI needs, sharing the process's `WallpaperGraph`.
- `SettingsEditor`: draft-while-dragging, one write per change.
- `ui/ScreenState`: what every screen gets (services, editor, settings, permission, preview agenda, snackbar, navigation).
- `backup/SettingsFiles`: reads and writes exported settings files through the system file picker.
- `Contrast`: the contrast warning.
- `SampleAgenda`: preview data.
- `rememberCalendarPermission`: permission state for Compose.

**Facts and rules**
- **Depends on:** every other module. Nothing may depend on `app`.
- **applicationId:** `io.github.nabin47.calwallpaper`. It is permanent and guarded by `ApplicationIdTest`. Debug builds add `.debug`.
- **Permissions:** `READ_CALENDAR`, plus `SET_WALLPAPER` and `RECEIVE_BOOT_COMPLETED` for battery saver mode (ADR 0004).
  Taps and widgets add none; `<queries>` lists only calendar, Todoist and browser intents. `INTERNET` is for Todoist,
  HTTPS only (`network_security_config.xml`). WorkManager merges its normal permissions (ADR 0007).
- **Release:** R8 full mode + resource shrinking via `optimization { enable = true }`; signing from CI secrets (docs/release.md).
