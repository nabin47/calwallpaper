# android-wallpaper

`WallpaperService` and the Canvas renderer for `Scene`.

**Public API**
- `RedrawReason` and `RedrawPolicy`: draw only when visible, and never on a timer.
- `render/`:
  - `SceneCanvasRenderer`: draws a `Scene` onto a `Canvas` and allocates nothing while drawing.
  - `AndroidTextMeasurer`: the `TextMeasurer` used by the layout engine.
  - `TextPaints`: shared font setup, so measured text and drawn text always match.

**Screenshot tests**
- `SceneScreenshotTest` uses Roborazzi on Robolectric native graphics.
- It covers every preset in light and dark, with a busy week (long titles, a completed task) and with no items, plus privacy mode.
- The golden images are in `src/test/screenshots/`. CI fails on any pixel difference and uploads the diff images in the `build-reports` artifact.
- After an intentional visual change, re-record the goldens with the **Record screenshots** workflow (see CONTRIBUTING.md).

**Depends on** `core`, `domain` and `render`. It will add `data` when the wallpaper service reads agenda sources (Phase 3).

**Rules**
- **No Compose and no DI framework.** This keeps the wallpaper process small.
- **When to draw:** only on visibility, data, settings or time changes.
- **Memory:** keep at most one screen-sized bitmap, and release it when the wallpaper is not visible.
- **Before first unlock:** render from device-protected storage.
