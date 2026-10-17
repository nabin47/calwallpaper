package io.github.nabin47.calwallpaper.widgets

import android.graphics.Bitmap

/**
 * One rendered widget: the Scene drawn into [bitmap] at the widget's size, and the tap areas on top of it.
 * The bitmap is handed to the launcher, so it must not be recycled or reused after it is returned.
 */
class WidgetFrame(
    val bitmap: Bitmap,
    val taps: List<WidgetTap>,
)

/**
 * Renders widget frames. The app module supplies it, because it owns the agenda data and the Canvas renderer;
 * this module only knows how to show a frame (CLAUDE.md §4: widgets do not depend on the wallpaper module).
 */
fun interface WidgetFrameSource {
    /** The [kind] widget at [widthDp] × [heightDp], or `null` if it cannot be rendered right now. */
    suspend fun frame(
        kind: WidgetKind,
        widthDp: Float,
        heightDp: Float,
    ): WidgetFrame?
}
