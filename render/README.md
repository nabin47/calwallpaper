# render

Layout engine. Turns agenda items, settings, screen size, insets and privacy state into a platform-neutral `Scene`
(text runs, rounded rects, hit regions carrying an `OpenTarget`). Every renderer consumes it.

- **Public API (Phase 0):** `Scene` (size only; primitives arrive in Phase 1).
- **Depends on:** `core`, `domain`.
- **Rules:** pure functions, no platform APIs, unit-tested on the JVM.
