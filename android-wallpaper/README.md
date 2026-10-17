# android-wallpaper

`WallpaperService` and the Canvas renderer for `Scene`.

- **Public API (Phase 0):** `RedrawReason`, `RedrawPolicy` (draw only when visible; no timers).
- **Depends on:** `core`, `domain`, `render`, `data`.
- **Rules:** **no Compose and no DI framework** (keeps the wallpaper process small). Draw only on visibility, data, settings, or time change. Keep at most one screen-sized bitmap, released when invisible. Render from device-protected storage before first unlock.
