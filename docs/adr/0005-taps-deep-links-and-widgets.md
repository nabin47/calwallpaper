# ADR 0005: Taps, deep links and widgets

- Status: accepted
- Date: 2026-10-17

## Context
The Scene already carries hit regions with an `OpenTarget` (ADR 0003). This phase makes them do something on the
home-screen wallpaper and adds widgets, the interactive surface that always works (CLAUDE.md §2). Constraints:

- **Wallpaper taps depend on the launcher.** A launcher reports taps on empty home-screen space with
  `WallpaperManager.COMMAND_TAP`, in screen coordinates. Launcher3 does this; other launchers vary.
- **Background activity launch (BAL) limits.** A wallpaper service is not in the foreground. The documented exemptions
  do not mention wallpapers.
- **Package visibility.** Since Android 11, an app sees only the packages it declares in `<queries>`. Since Android
  14, implicit intents are not delivered to unexported components.
- **Module rules.** `android-widgets` and `android-wallpaper` must not depend on each other or on `app` (CLAUDE.md §4,
  enforced by `ModuleGraph`).
- **Widget limits.** Glance allows at most 10 children per container and has no absolute positioning. Widget bitmaps
  share a limit of 1.5 screens of memory, and Android 17 throws an error when it is exceeded.

## Decision
- **One link format, one router.**
  - `core/OpenTargetLink` encodes an `OpenTarget` as `calwallpaper://open/<kind>?…` and back. It is pure and tested.
  - The app's `OpenTargetActivity` is the deep-link router (CLAUDE.md §4). It has no UI (`Theme.NoDisplay`), is not
    exported, and finishes in `onCreate`.
  - Senders address the router by explicit component (`OpenTargetLink.ROUTER_CLASS`, a string, so neither Android
    module depends on `app`), with the link as Intent data. The data also keeps every widget `PendingIntent` distinct.
- **Choosing the app happens at tap time.**
  - `LaunchPlan` (pure) turns a target into candidates, best first, always ending in a fallback that needs no source
    app:
    - **A day:** the Calendar Provider's `time/<ms>` URI, which any calendar app handles, then Google Calendar on
      the web.
    - **An event:** `events/<id>` with begin and end extras, then its day.
    - **A Todoist task or project:** the `app.todoist.com` link, opened only by an app that claims it (non-browser,
      Android 11+); then `todoist://…`; then the same page in the browser. Only the web URL format is documented by
      Todoist (its SDKs build it); the `todoist://` links are a best effort. Ids that are not plain alphanumerics
      are never put into a URL.
    - **Local items:** this app.
    - **URLs:** the browser.
  - `LaunchIntents` tries the candidates in two passes. First it tries the ones that `resolveActivity` finds; that
    check is what the `<queries>` entries are for: calendar time and event URIs, the `todoist` scheme and
    `https` browsers, never `QUERY_ALL_PACKAGES`. If none of those start, it tries every candidate, catching
    `ActivityNotFoundException`. Starting an activity is not limited by package visibility, so a queries gap
    cannot leave a tap dead.
- **Wallpaper taps.**
  - `AgendaEngine.onCommand` handles `COMMAND_TAP` by hit-testing the drawn Scene (the surface fills the screen,
    so screen and surface coordinates match) and starting the router.
  - Taps are ignored in the picker's preview, while invisible, and while the device is locked.
  - Touch events stay disabled: taps arrive as commands.
- **Why the wallpaper may start the router.** AOSP's `BackgroundActivityStartController` allows a caller with a
  visible non-app window, and its comment names live wallpapers (`BAL_ALLOW_NON_APP_VISIBLE_WINDOW`):
  `WindowState` counts every system-type window except toasts, and `TYPE_WALLPAPER` is one. Once the router is in
  the foreground, starting the source app is an ordinary foreground start. The Android 12 trampoline restriction
  applies only to notifications. Muzei, a widely used live wallpaper, also starts activities from wallpaper taps.
- **Widgets reuse the Scene.**
  - `SceneGlanceWidget` (in `android-widgets`) asks the launcher for the widget's sizes (`SizeMode.Exact`), renders
    one frame per size, and shows it as an image.
  - Transparent tap areas are placed over the Scene's hit regions, positioned by padding inside full-size boxes and
    grouped in tens.
  - `WidgetTaps` merges stacked cells of the same target (a week strip's weekday and date) and keeps at most 40.
  - Tapping outside every area opens the app.
  - **Two widgets:** a small agenda (`AGENDA` preset) and a week strip (`WEEK_STRIP`).
  - **Widget layout:** `widgetSettings` replaces the wallpaper's placement with "top, no safe zones, no side
    margin". Appearance, content, time and privacy follow the user's settings. The background is not painted, so
    the launcher shows around the panel.
  - **Rendering:** `WidgetFrames`, in `app`, renders frames with the wallpaper's own pipeline, because `app` is the
    only module that sees both the widgets and the renderer. The receivers live in `app` for the same reason.
  - **Memory:** a frame is at most half a screen of pixels (`WidgetCanvas`). It is drawn at lower density if needed,
    so two sizes stay well under the limit.
- **Keeping widgets current.** There is no update period. The background triggers that static mode introduced now
  run while static mode or any widget is in use:
  - a calendar content-trigger job;
  - an inexact midnight alarm;
  - the time, time-zone and locale broadcasts, plus boot.

  `InstalledWidgets` finds this app's placed widgets through `AppWidgetManager` without naming their classes. It
  redraws them with the same explicit `APPWIDGET_UPDATE` broadcast the system sends. The app also redraws them after
  a settings change (debounced) and when calendar access changes. Widget providers re-sync the triggers in
  `onEnabled` and `onDisabled`. The static-mode class names stay, because Android stores a component's enabled
  state under its class name.
- **Accessibility.** `HitRegion` gained a `label` (the day name, or time and title), which widgets expose as each tap
  area's content description. With privacy mode hiding titles, labels hide them too (tested).

## Consequences
- **Needs a phone to confirm:**
  - wallpaper taps on the owner's launcher;
  - the BAL exemption in practice;
  - the `todoist://` links;
  - Google Calendar's handling of the time URI.

  See `docs/testing-taps.md`.
- **Resizing.** Glance composes the size it was given. If a launcher resizes a widget while its session is still
  alive, the closest pre-rendered frame is stretched until the next redraw.
- **No tap feedback on the wallpaper.** The wallpaper draws no ripple, because there are no animations
  (CLAUDE.md §5). The opened app is the feedback.
- **Taps cannot be turned off.** No setting exists for this yet. If users find accidental opens annoying, add one,
  with the preview and a settings test (CLAUDE.md §6).
- **Lock screen.** It stays read-only: Android never sends lock-screen taps to wallpapers, and the engine ignores taps
  while the device is locked.
