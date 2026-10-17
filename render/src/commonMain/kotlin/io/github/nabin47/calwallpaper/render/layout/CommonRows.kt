package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.render.text.TextStyle
import io.github.nabin47.calwallpaper.render.text.ellipsize
import io.github.nabin47.calwallpaper.render.theme.withAlpha

/** A single line of [text] in [style], ellipsized to the inner width. */
internal fun LayoutContext.textRow(
    text: String,
    style: TextStyle,
    color: Int,
): Row {
    val line = measurer.ellipsize(text, style, innerWidth)
    val height = lineHeight(style) + rowGap
    return Row(height) { top -> text(line, innerLeft, top + rowGap / 2 + ascent(style), style, color) }
}

/** Empty vertical space. */
internal fun spacerRow(height: Float): Row = Row(height) {}

/** The "+N more" row used when items do not fit. */
internal fun LayoutContext.moreRow(hidden: Int): Row = textRow(strings.more(hidden), caption, palette.secondaryText)

/** The item's own color (with text opacity), or the accent. */
internal fun LayoutContext.markerColor(item: AgendaItem): Int =
    item.colorArgb?.let { withAlpha(it, settings.appearance.textOpacity) } ?: palette.accent

/**
 * Draws an item marker centered at ([centerX], [centerY]): a dot for events, a ring for open tasks and a filled
 * ring for completed tasks.
 */
internal fun LayoutContext.marker(
    item: AgendaItem,
    centerX: Float,
    centerY: Float,
) {
    when {
        item.kind == ItemKind.EVENT -> circle(centerX, centerY, dp(DOT_RADIUS_DP), markerColor(item))
        item.isCompleted -> circle(centerX, centerY, dp(RING_RADIUS_DP), palette.secondaryText)
        else -> circle(centerX, centerY, dp(RING_RADIUS_DP), palette.secondaryText, dp(RING_STROKE_DP))
    }
}

/** Text color for an item title; completed items are muted. */
internal fun LayoutContext.titleColor(item: AgendaItem): Int =
    if (item.isCompleted) palette.secondaryText else palette.text

/** [style] with strikethrough for completed items. */
internal fun TextStyle.forItem(item: AgendaItem): TextStyle = if (item.isCompleted) copy(strikethrough = true) else this

private const val DOT_RADIUS_DP = 3.5f
private const val RING_RADIUS_DP = 4.5f
private const val RING_STROKE_DP = 1.5f
