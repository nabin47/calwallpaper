# ADR 0004: Wallpaper process, Direct Boot cache and static battery mode

- Status: accepted
- Date: 2026-10-17

## Context
Phase 3 puts the Scene on the real wallpaper. The constraints:

- The wallpaper must stay light and must not load Compose (CLAUDE.md §3, §5).
- It must draw before the first unlock (Direct Boot), when the calendar provider and the Room database are unavailable (§2).
- Privacy mode depends on whether the lock screen is showing (§2).
- Battery mode needs a static image that stays current without a running service (§6).

## Decision
- **Separate process.** `AgendaWallpaperService` runs in `:wallpaper` (`android:process`), so the settings UI's Compose
  and Material code never load into the wallpaper's memory. The two processes share settings through
  `MultiProcessDataStoreFactory`: a change in the app reaches the wallpaper's `settings` flow. Each process builds its own
  `WallpaperGraph` (manual DI) with exactly one DataStore and one Room instance.
- **Event-driven drawing.** The engine draws when it becomes visible, when items or settings change, and on these
  broadcasts, registered at runtime: `DATE_CHANGED`, `TIME_SET`, `TIMEZONE_CHANGED`, `USER_PRESENT`, `SCREEN_ON`,
  `USER_UNLOCKED`. It also draws on configuration changes. It has no timers, no animation and no foreground service.
  Invisible engines draw nothing.
- **One bitmap.** `WallpaperFrame` keeps one screen-sized bitmap and repaints it only when the Scene changed. The bitmap
  is released when the wallpaper becomes invisible.
- **The wallpaper fills its whole surface.** A live wallpaper has nothing underneath it, so `Palette` and `Scene` gained an opaque
  `background` (presets, custom and Material You). It is a property, not a draw op, so widgets can stay transparent.
  The engine also reports this color to Android (`onComputeColors`), so the clock and status bar stay readable.
- **Privacy uses the lock state.** "Locked" means the device still needs the user's credentials:
  `KeyguardManager.isDeviceLocked` on secure devices, and keyguard showing (`isKeyguardLocked`) only on devices
  without a secure lock. It is read before every frame, and `USER_PRESENT` and `SCREEN_ON` trigger a redraw. The
  layout replaces titles itself, so no renderer can leak them.
  - *Why not `isKeyguardLocked` everywhere* (found on the owner's phone, 2026-10-17): with face unlock, Android sends
    `USER_PRESENT` at authentication while the lock screen is still showing. Nothing announces the keyguard
    actually going away, so the home screen kept the privacy view until the next redraw.
  - *Accepted trade-off:* after face unlock, the lock screen can briefly show the unlocked view until it is
    dismissed. Trusted places or devices (Smart Lock) also count as unlocked.
- **Separate lock-screen placement.** Lock-screen clocks are often far larger than home-screen ones; on the owner's
  phone the two-line clock and date cover the top ~55%. `LayoutSettings` therefore has its own lock position and
  safe zones: bottom by default, 55% clear at the top, 20% at the bottom, and up to 75% at the top. The layout uses
  them whenever the request is locked, including the static lock image and the preview's lock-screen switch.
- **Direct Boot.** The service is `directBootAware`. Before the first unlock it reads only device-protected storage: the
  settings and `DirectBootCache`, the last agenda as JSON. Live sources are observed only after `isUserUnlocked`. Because
  device-protected storage is readable before the user's credentials are entered, the cache stores titles only when
  privacy mode is **off**. With privacy on, the phone is always locked before the first unlock, so titles would be hidden
  anyway. Caches live under `wallpaper/` and are excluded from backups.
- **Static battery mode** runs in the main process:
  - **Rendering.** `StaticWallpaperUpdater` lays out the same Scene and calls `WallpaperManager.setBitmap`. Home and lock get
    separate images when privacy mode is "when locked", one shared image otherwise.
  - **No needless updates.** A SHA-256 digest of the scenes is stored, never the scenes, and an unchanged picture skips
    `setBitmap`.
  - **Triggers** (no polling, no exact alarms):
    - a content-trigger `JobScheduler` job on `CalendarContract.CONTENT_URI`;
    - an inexact, non-waking alarm shortly after local midnight;
    - the manifest-exempt broadcasts `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` and `LOCALE_CHANGED`;
    - setting changes made in the app, debounced.
  - **Off by default.** The static-mode receiver and job service are disabled in the manifest. `StaticModeScheduler`
    enables them only in static mode, so live-mode users get no wakeups at all.
- **New permissions (approved by the owner, 2026-10-17).** Both are "normal": granted at install with no prompt.
  - `SET_WALLPAPER`: needed to set the static image.
  - `RECEIVE_BOOT_COMPLETED`: content-trigger jobs cannot be persisted, so static mode re-arms its triggers after boot.
- **Setting the wallpaper.**
  - Live: Android lets no app pick home, lock or both for a live wallpaper. The app opens the system preview
    (`ACTION_CHANGE_LIVE_WALLPAPER`), and the system asks.
  - Static: the app sets the image itself on the screens chosen in settings (`Settings.staticTarget`).
- **Measured, not guessed.** The wallpaper records its layout and draw time, bitmap size and process PSS. It writes them
  only when it becomes invisible, and measures PSS at most every 10 minutes. The settings screen shows these numbers,
  so they can be read on a phone without adb. See `docs/performance.md`.

## Consequences
- **Two processes.** The wallpaper process pays its own runtime overhead. In exchange, the settings UI never inflates the
  wallpaper's memory, and its PSS can be measured on its own.
- **Local tasks across processes.** Room in the wallpaper process sees only its own writes. Local tasks are written in the
  app process, but no write UI exists yet. When quick-add arrives, enable multi-instance invalidation or another
  cross-process signal, and verify it.
- **Background.** The wallpaper background is a solid color from the palette. Using the user's own photo would need a photo
  picker (no permission needed) and is left for later.
- **No taps yet.** Touches are disabled until deep links (`OpenTarget` to Intent) arrive in Phase 5.
- **The midnight alarm is inexact.** The static image can update a little after midnight, at the latest when the phone wakes up.
- **Broadcast exemptions.** `DATE_CHANGED` is *not* exempt from the implicit-broadcast limits, so it is registered only at
  runtime, by the live engine. Static mode uses the midnight alarm instead.
