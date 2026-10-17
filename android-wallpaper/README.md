# android-wallpaper

The live wallpaper, the static (battery saver) image, and the Canvas renderer for `Scene`. See ADR 0004.

**Public API**
- **`AgendaWallpaperService`:** the live wallpaper. It runs in the `:wallpaper` process and is `directBootAware`.
  - **When it draws:** on visibility, data, settings, date/time/time-zone, lock-state and configuration changes. Never on a timer.
  - **Privacy:** titles are hidden while `KeyguardManager.isKeyguardLocked`.
- **`WallpaperGraph`:** per-process dependencies (settings, sources, caches, request factory), created lazily.
- **`LiveWallpaper`:** intents for the system live-wallpaper preview and chooser; whether this wallpaper is active.
- `feed/`:
  - `AgendaFeed` merges the enabled sources. A failing source adds nothing instead of hiding the others.
  - `FeedKey` decides when observation must restart: new day, other preset or sources, first unlock, permission granted.
- `render/`:
  - `SceneCanvasRenderer` draws a `Scene` and allocates nothing while drawing.
  - `AndroidTextMeasurer` and `TextPaints` share font setup, so measured text equals drawn text.
  - `WallpaperFrame` keeps the one cached bitmap, repaints only on Scene changes, and records timings.
  - `RenderRequestFactory` resolves locale, week start, 12/24 h, font scale and Material You colors.
  - `AndroidRenderStrings` provides wallpaper words from `strings.xml`.
  - `SystemPalette` provides the dynamic colors.
- `cache/DirectBootCache`: the last agenda in device-protected storage. Titles are stored only when privacy mode is off.
- `diagnostics/Diagnostics`: measured performance numbers, shown in the app.
- `staticmode/`:
  - `StaticWallpaperUpdater` renders and sets the static image, skipping unchanged pictures.
  - `StaticModeScheduler` sets up the calendar content-trigger job and the midnight alarm, and toggles components.
  - `StaticRefreshReceiver` and `StaticRefreshJobService` are disabled in the manifest until static mode is chosen.
  - `StaticPlan` holds the pure planning.
- `system/`:
  - `DeviceState`: locked, unlocked, calendar permission.
  - `SystemEvents`: runtime-registered broadcasts.

**Tests**
- JVM: cache round trip and title stripping, static-mode planning (including DST midnights), feed keys and merging.
- Screenshots: `SceneScreenshotTest` (Roborazzi on Robolectric native graphics) covers every preset in light and dark,
  busy and empty, plus privacy mode. The goldens are in `src/test/screenshots/`; re-record them with the
  **Record screenshots** workflow (CONTRIBUTING.md).

**Depends on** `core`, `domain`, `render` and `data`.

**Rules**
- **No Compose and no DI framework** in this module: its code runs in the wallpaper process.
- **When to draw:** only on visibility, data, settings, time, lock-state or configuration changes.
- **Memory:** keep at most one screen-sized bitmap, and release it when the wallpaper is not visible.
- **Before the first unlock:** read only device-protected storage.
