# android-widgets

Jetpack Glance home-screen widgets, rendered from the same `Scene` pipeline. They are the guaranteed interactive surface.

See ADR 0005.

- **Public API:**
  - `WidgetKind`: `AGENDA` (small agenda) and `WEEK` (week strip), each with its layout preset.
  - `SceneGlanceWidget`: the Glance widget. It renders one frame per reported size, shows it as an image, and
    places a transparent tap area over each hit region. Each tap opens the app's router with an `OpenTargetLink`.
  - `WidgetFrameSource` and `WidgetFrame`: implemented by the app, which owns data and the Canvas renderer.
  - Pure helpers, unit-tested:
    - `widgetSettings`: the widget's layout (top, no safe zones or margins).
    - `WidgetTaps`: hit regions to dp tap areas, merged and capped at 40.
    - `WidgetCanvas`: bitmap size within the memory budget.
- **Depends on:** `core`, `domain`, `render`, and Glance (`api`, because the app declares the receivers).
- **Rules:**
  - No dependency on `android-wallpaper` or `app` (ModuleGraph).
  - Glance runs in the app's main process, never in `:wallpaper`.
  - The receivers, `AgendaWidget` and `WeekWidget` live in `app`, which wires in the renderer.
