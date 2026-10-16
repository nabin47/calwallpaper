# android-widgets

Jetpack Glance home-screen widgets, rendered from the same `Scene` pipeline. They are the guaranteed interactive surface.

- **Public API (Phase 0):** `WidgetKind`.
- **Depends on:** `core`, `domain`, `render`, `data`.
- **Rules:** Glance is added in Phase 5. Taps resolve `OpenTarget` to an Intent with a web fallback.
