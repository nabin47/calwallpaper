# ADR 0003: Scene layout engine and screenshot tests

- Status: accepted
- Date: 2026-10-17

## Context
The wallpaper, the settings preview, the widgets and (later) iOS must all show the same layout (CLAUDE.md §4).
Layout must be unit-testable on the JVM, and `render` may not depend on Android. Visual regressions should be
caught in CI without an emulator, and the owner has no local tools.

## Decision
- **`render` produces a `Scene`:** an ordered list of draw ops (rounded rects, single-line text, circles) plus hit regions
  carrying `OpenTarget`. Text arrives already fitted, so renderers only paint. Colors arrive with opacity already
  applied, so renderers do no styling logic.
- **Text is measured through an injected `TextMeasurer`.** The layout ellipsizes and wraps text with the platform's real font
  metrics (Android: `Paint`), yet stays pure and deterministic. JVM unit tests use a fake measurer with fixed
  glyph widths.
- **Everything the platform knows is resolved by the caller and passed in `RenderRequest`:** localized strings, first day of the week,
  12/24-hour clock, insets, lock state, and the dynamic (Material You) palette. The engine reads no clock and
  does no I/O.
- **The safe area is the stricter of the system insets and the user's safe zone, minus the side margin.** Each preset builds rows; one
  placement routine positions the block (top, center or bottom), draws the panel, and replaces rows that do not fit
  with "+N more". Content therefore never leaves the safe area.
- **Privacy mode is decided in the layout:** titles are replaced by counts, dots or a neutral word, so no renderer can
  leak them.
- **Screenshot tests live in `android-wallpaper`,** next to `SceneCanvasRenderer`, the real Android renderer. They use
  Roborazzi on Robolectric native graphics, so no emulator is needed. Goldens are PNGs under
  `android-wallpaper/src/test/screenshots/`. CI runs `verifyRoborazziDebug`; the **Record screenshots** workflow
  re-records the goldens and commits them, because the owner cannot run Gradle locally.

## Consequences
- **Font changes shift goldens.** Goldens depend on the fonts in Robolectric's Android image. A Robolectric or SDK upgrade can
  shift pixels; re-record and review the diff images in the CI artifact.
- **Measurer and renderer must match.** `AndroidTextMeasurer` and `SceneCanvasRenderer` share `TextPaints`, so a drawn width always equals
  the measured width.
- **No per-frame layout.** Scenes are built only when data, settings, time or lock state change (CLAUDE.md §5).
