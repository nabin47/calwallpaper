package io.github.nabin47.calwallpaper.widgets

import io.github.nabin47.calwallpaper.core.OpenTargetLink
import io.github.nabin47.calwallpaper.render.HitRegion
import kotlin.math.abs

/**
 * One tappable area of a widget, in dp from the widget's top-left corner. [link] is the [OpenTargetLink] the
 * app's router opens; [label] is read out by screen readers (it can contain a title: never log it).
 */
data class WidgetTap(
    val leftDp: Float,
    val topDp: Float,
    val widthDp: Float,
    val heightDp: Float,
    val link: String,
    val label: String,
)

/** Turns a Scene's hit regions into widget tap areas. Pure. */
object WidgetTaps {
    /**
     * Glance allows at most 10 children per container and every tap costs views in the widget, so a widget
     * keeps at most this many tap areas (the first ones, which are the topmost rows).
     */
    const val MAX_TAPS: Int = 40

    /**
     * Tap areas for [regions] drawn at [pxPerDp] pixels per dp. Regions stacked directly on top of each other
     * with the same target and column (a week strip's weekday and date cells) become one area. Overlap order is
     * kept: later areas are on top, as in [io.github.nabin47.calwallpaper.render.Scene.hitTest].
     */
    fun from(
        regions: List<HitRegion>,
        pxPerDp: Float,
    ): List<WidgetTap> {
        require(pxPerDp > 0f) { "pxPerDp must be positive" }
        val merged = mutableListOf<HitRegion>()
        regions.forEach { region ->
            val index = merged.indexOfFirst { it.stacksOnto(region) }
            if (index >= 0) {
                val above = merged[index]
                merged[index] = above.copy(bounds = above.bounds.copy(bottom = region.bounds.bottom))
            } else {
                merged += region
            }
        }
        return merged
            .filter { it.bounds.width > 0f && it.bounds.height > 0f }
            .take(MAX_TAPS)
            .map { region ->
                WidgetTap(
                    leftDp = region.bounds.left / pxPerDp,
                    topDp = region.bounds.top / pxPerDp,
                    widthDp = region.bounds.width / pxPerDp,
                    heightDp = region.bounds.height / pxPerDp,
                    link = OpenTargetLink.encode(region.target),
                    label = region.label,
                )
            }
    }

    private fun HitRegion.stacksOnto(next: HitRegion): Boolean =
        target == next.target &&
            abs(bounds.left - next.bounds.left) < EPSILON &&
            abs(bounds.right - next.bounds.right) < EPSILON &&
            abs(bounds.bottom - next.bounds.top) < EPSILON

    private const val EPSILON = 0.5f
}
