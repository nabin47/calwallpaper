package io.github.nabin47.calwallpaper.render

/**
 * Platform-neutral drawing of one frame: what to draw and where, in pixels.
 *
 * Every renderer (wallpaper Canvas, settings preview, widgets, later iOS) consumes a [Scene], so layout
 * logic is written once (CLAUDE.md §4). Phase 0 has no primitives yet; the layout engine arrives in Phase 1.
 * Immutable; build on any thread.
 */
data class Scene(
    val widthPx: Int,
    val heightPx: Int,
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "Scene size must be positive" }
    }

    /** True when the scene is taller than it is wide (the usual phone wallpaper). */
    val isPortrait: Boolean get() = heightPx > widthPx
}
