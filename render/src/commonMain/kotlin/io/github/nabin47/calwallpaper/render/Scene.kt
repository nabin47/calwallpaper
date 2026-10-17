package io.github.nabin47.calwallpaper.render

import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.render.text.TextStyle

/**
 * Platform-neutral description of one frame: what to draw, in order, and which areas open what when tapped.
 *
 * Every renderer (wallpaper Canvas, settings preview, widgets, later iOS) draws the same [Scene], so layout
 * logic is written once (CLAUDE.md §4). Coordinates are pixels with the origin at the top left. The scene has
 * no background: the user's wallpaper shows through. Immutable; build on any thread, draw on the UI thread.
 *
 * @property ops draw operations, painted in list order (later ops on top).
 * @property hitRegions tappable areas. When regions overlap, the one added last wins (see [hitTest]).
 */
data class Scene(
    val widthPx: Int,
    val heightPx: Int,
    val ops: List<DrawOp> = emptyList(),
    val hitRegions: List<HitRegion> = emptyList(),
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "Scene size must be positive" }
    }

    /** True when the scene is taller than it is wide (the usual phone wallpaper). */
    val isPortrait: Boolean get() = heightPx > widthPx

    /** The target under the point ([x], [y]), or `null`. The most specific (last added) region wins. */
    fun hitTest(
        x: Float,
        y: Float,
    ): OpenTarget? = hitRegions.lastOrNull { it.bounds.contains(x, y) }?.target
}

/** An axis-aligned rectangle in pixels. [right] and [bottom] are exclusive. */
data class Bounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /** Horizontal size. */
    val width: Float get() = right - left

    /** Vertical size. */
    val height: Float get() = bottom - top

    /** True if the point lies inside (left/top inclusive, right/bottom exclusive). */
    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x >= left && x < right && y >= top && y < bottom
}

/** A tappable area that opens [target]. */
data class HitRegion(
    val bounds: Bounds,
    val target: OpenTarget,
)

/** One drawing instruction. Colors are `0xAARRGGBB` with any opacity setting already applied. */
sealed interface DrawOp {
    /** A filled rectangle with rounded corners ([cornerRadius] in pixels, 0 for square). */
    data class RoundRect(
        val bounds: Bounds,
        val cornerRadius: Float,
        val color: Int,
    ) : DrawOp

    /** A single line of text, already fitted to its width; [x] is the left edge, [baseline] the text baseline. */
    data class Text(
        val text: String,
        val x: Float,
        val baseline: Float,
        val style: TextStyle,
        val color: Int,
    ) : DrawOp

    /** A circle; filled when [strokeWidth] is 0, otherwise an outline of that width. */
    data class Circle(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val color: Int,
        val strokeWidth: Float = 0f,
    ) : DrawOp
}
