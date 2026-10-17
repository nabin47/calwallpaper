package io.github.nabin47.calwallpaper.widgets

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The bitmap size a widget is drawn at: [pxPerDp] pixels per dp, so [widthPx] × [heightPx] covers the widget.
 */
data class WidgetCanvas(
    val widthPx: Int,
    val heightPx: Int,
    val pxPerDp: Float,
) {
    /** Sizing. */
    companion object {
        /**
         * Full density for a [widthDp] × [heightDp] widget, scaled down if the bitmap would exceed
         * [maxPixels]. Launchers limit widget bitmap memory to 1.5 screens in total (Android 17 throws above
         * it), and a widget may be drawn for two sizes (portrait and landscape), so callers pass half a screen.
         * Pure.
         */
        fun of(
            widthDp: Float,
            heightDp: Float,
            density: Float,
            maxPixels: Long,
        ): WidgetCanvas {
            val fullWidth = (widthDp * density).coerceAtLeast(1f)
            val fullHeight = (heightDp * density).coerceAtLeast(1f)
            val scale = minOf(1f, sqrt(maxPixels.coerceAtLeast(1).toFloat() / (fullWidth * fullHeight)))
            return WidgetCanvas(
                widthPx = (fullWidth * scale).roundToInt().coerceAtLeast(1),
                heightPx = (fullHeight * scale).roundToInt().coerceAtLeast(1),
                pxPerDp = density * scale,
            )
        }
    }
}
