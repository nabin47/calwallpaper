# render

The layout engine. It turns agenda items, settings, screen size, insets and lock state into a platform-neutral
`Scene`: draw ops plus hit regions that carry an `OpenTarget`. Every renderer consumes the same scene (ADR 0003).

**Public API**
- `LayoutEngine(measurer).layout(RenderRequest)`.
- `LayoutEngine.requiredRange(...)`: the days to fetch for the current preset.
- `Scene`, `DrawOp`, `Bounds`, `HitRegion`, and `Scene.hitTest(x, y)`.
- `RenderRequest`, `ScreenSpec` and `Insets`, plus `resolveFirstDayOfWeek` and `resolveUse24Hour`.
- `text/`: `TextStyle`, `TextMeasurer` (implemented per platform), and `ellipsize` / `wrap`.
- `theme/`: `Palette` (presets, custom colors, dynamic color) and `applyOpacity`.
- `format/`: `RenderStrings` (localized words, with `EnglishRenderStrings` as the default) and the time and day labels.

**Presets**
- **Agenda list:** day headers, then one row per item.
- **Week strip:** seven columns of titles.
- **Month grid:** dots per day.
- **Next-3 minimal:** large titles, wrapped to two lines.

**Depends on** `core` and `domain`, which are exposed as `api`.

**Rules**
- No Android or `java.*` imports.
- No clock reads and no I/O.
- Pure and deterministic for a given measurer.
- Tests are JVM unit tests with a fake measurer; pixel tests live in `android-wallpaper`.
