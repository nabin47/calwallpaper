# ADR 0009: App UX, navigation and Material 3 design

- Status: accepted
- Date: 2026-10-17

## Context
The owner's verdict after using the app on their phone: it was hard to know what to do or where to go next, and
choosing options seemed to do nothing. The app was one long settings list under a fixed preview. Specifically:

- **Hidden options.** Chip rows scrolled sideways, so some options were off-screen.
- **Changes with no visible effect.** Many changes only show on the lock screen (privacy, lock placement), or only
  in some layouts (week start), or only with certain data (completed tasks). The preview stayed on the home screen
  unless a switch was found and turned on.
- **No clear status.** Nothing said plainly whether CalWallpaper was actually the wallpaper, or what the next step
  was. Results of actions appeared as small text that was easy to miss.
- **Onboarding.** Its final step showed a mode choice but did not explain the system's own "Set wallpaper" dialog
  that follows.
- **No splash screen.**

## Decision
- **A home screen instead of one long list.** From top to bottom:
  - a large preview with a Home screen / Lock screen switch;
  - a status card that says whether the wallpaper is set and gives the one next step (set it, apply the battery
    saver image, or nothing);
  - a card asking for calendar access while the preview shows sample events;
  - one-tap layout thumbnails, drawn by the real layout engine, and color swatches;
  - grouped rows to every settings page, each summarizing its current values;
  - an "Add task" button for the local list.
- **One page per topic:** Appearance, Layout, Lock screen & privacy, Calendars & tasks, Todoist, Date & time,
  Wallpaper mode, Widgets & shortcuts, Backup & reset, Help & about.
  - Visual pages pin the preview above the scrolling controls.
  - The Lock screen page always previews the lock screen, which fixes "changing it does nothing".
  - Controls carry a one-line hint saying what they change.
  - Three or fewer short choices are segmented buttons; longer lists wrap as chips, so nothing hides off-screen.
  - Results show as snackbars. Reset offers Undo.
- **Navigation 3** (1.2.0, stable) hosts the screens.
  - Keys are `@Serializable` (`ui/navigation/Route`), so the back stack survives process death.
  - `NavDisplay` gives the system's predictive back gesture and transitions.
  - It replaces hand-rolled navigation and adds no network or tracking component.
- **Material 3 (Material You), styled the Expressive way.**
  - Material 3 1.4.0 is the latest stable release (Compose BOM 2026.09.00). In it, the Expressive APIs
    (`MaterialExpressiveTheme`, `MotionScheme.expressive()`, the flexible app bars) are **internal**, so an app
    cannot call them. Only the 1.5.0 pre-releases expose them, and CLAUDE.md §3 allows stable versions only. The
    first CI build failed on exactly this.
  - So the app uses the stable components and the Expressive visual language:
    - tonal icon containers;
    - grouped list surfaces with gaps instead of dividers;
    - large rounded cards;
    - large collapsing app bars;
    - segmented buttons.
  - Dynamic (Material You) colors on Android 12+, and a navy scheme from the launcher icon below that.
  - Switch to `MaterialExpressiveTheme` and its motion scheme once Material 3 1.5.0 is stable.
  - Icons are vector drawables of the Material icons. Material 3 1.4 no longer ships an icon library, and the large
    extended-icons artifact is not worth its size.
- **Splash screen.** AndroidX SplashScreen 1.2.0 shows the launcher icon on its own color. It stays up only until the
  settings are loaded (`setKeepOnScreenCondition`), so there is no artificial delay.
- **Onboarding in four steps**, with progress, Back and Skip:
  1. Welcome, on a live preview.
  2. Calendar access, with its rationale.
  3. Pick a look.
  4. Set the wallpaper: a numbered explanation of Android's own preview ("Set wallpaper", then "Home screen and lock
     screen"). Returning with the wallpaper set finishes onboarding by itself.
- **New features** that need no permission and no network:
  - **Widgets:** pin to the home screen in one tap (`requestPinAppWidget`). The Quick add tile can be added from the
    app on Android 13+ (`requestAddTileService`).
  - **Settings backup:** export and import through the system file picker (no storage permission). The file is the
    existing versioned `SettingsCodec` JSON, with no tokens or titles, and is capped at 256 KB.
  - **Reset to defaults**, with Undo.
  - **Help:** answers to the questions the platform's limits raise (taps, lock-screen clock, missing events,
    battery, privacy).

## Consequences
- **Not checked visually.** The app UI has no screenshot tests (the renderer does). Layout regressions in the UI are
  caught only on the phone. Adding Roborazzi Compose tests for the home screen is a follow-up.
- **Expressive is a follow-up.** When Material 3 1.5.0 is stable, adopt `MaterialExpressiveTheme`, the expressive
  motion scheme and the flexible app bars. Dependabot will offer the BOM that carries it.
- **Generator labels.** The Baseline Profile generator clicks onboarding buttons by their English labels. It was
  updated with them and must be updated again when they change.
