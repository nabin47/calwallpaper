package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.core.time.agendaOrder
import io.github.nabin47.calwallpaper.core.time.overlaps
import io.github.nabin47.calwallpaper.render.Bounds
import io.github.nabin47.calwallpaper.render.format.dayLabel
import io.github.nabin47.calwallpaper.render.format.timeLabel
import io.github.nabin47.calwallpaper.render.text.ellipsize
import kotlinx.datetime.LocalDate

/**
 * Agenda list: day headers ("Today", "Tomorrow", dates) followed by one row per item (marker, time, title).
 * Days without items are skipped. In privacy mode each header shows only an item count.
 */
internal fun LayoutContext.layoutAgenda(
    items: List<AgendaItem>,
    range: DateRange,
) {
    val order = agendaOrder(zone)
    val days =
        range.days().map { day ->
            day to
                items.filter { it.time.overlaps(DateRange(day, day), zone) }.sortedWith(order)
        }
    val timeColumn = timeColumnWidth(days)
    val rows = mutableListOf<Row>()
    days.filter { it.second.isNotEmpty() }.forEach { (day, dayItems) ->
        if (rows.isNotEmpty()) rows += spacerRow(groupGap)
        rows += dayHeaderRow(day, dayItems.size)
        if (!hideTitles) dayItems.forEach { rows += itemRow(it, day, timeColumn) }
    }
    if (rows.isEmpty()) {
        rows += dayHeaderRow(today, count = 0)
        rows += textRow(strings.emptyAgenda, body, palette.secondaryText)
    }
    placeBlock(rows) { hidden -> moreRow(hidden) }
}

private fun LayoutContext.dayHeaderRow(
    day: LocalDate,
    count: Int,
): Row {
    val height = lineHeight(header) + rowGap
    val label = measurer.ellipsize(dayLabel(day, today, strings), header, innerWidth)
    val countLabel = if (hideTitles && count > 0) strings.itemCount(count) else ""
    return Row(height, itemCount = if (hideTitles) count else 0) { top ->
        val baseline = top + rowGap / 2 + ascent(header)
        text(label, innerLeft, baseline, header, if (day == today) palette.accent else palette.text)
        text(countLabel, innerRight - width(countLabel, caption), baseline, caption, palette.secondaryText)
        dayRegion(Bounds(content.left, top, content.right, top + height), day)
    }
}

private fun LayoutContext.itemRow(
    item: AgendaItem,
    day: LocalDate,
    timeColumn: Float,
): Row {
    val height = maxOf(lineHeight(body), lineHeight(caption)) + rowGap
    val markerWidth = dp(MARKER_COLUMN_DP)
    val titleLeft = innerLeft + markerWidth + timeColumn
    val titleStyle = body.forItem(item)
    val title = measurer.ellipsize(item.title, titleStyle, innerRight - titleLeft)
    val time = timeLabel(item.time, day, zone, request.use24HourClock, strings)
    return Row(height, itemCount = 1) { top ->
        val baseline = top + rowGap / 2 + ascent(body)
        marker(item, innerLeft + markerWidth / 2 - dp(MARKER_INSET_DP), top + height / 2)
        text(time, innerLeft + markerWidth, baseline, caption, palette.secondaryText)
        text(title, titleLeft, baseline, titleStyle, titleColor(item))
        region(Bounds(content.left, top, content.right, top + height), item.openTarget, "$time ${item.title}".trim())
    }
}

/** Widest time label plus a gap, capped so titles keep most of the row. */
private fun LayoutContext.timeColumnWidth(days: List<Pair<LocalDate, List<AgendaItem>>>): Float {
    val widest =
        days.maxOfOrNull { (day, dayItems) ->
            dayItems.maxOfOrNull { width(timeLabel(it.time, day, zone, request.use24HourClock, strings), caption) }
                ?: 0f
        } ?: 0f
    return if (widest == 0f) 0f else minOf(widest + dp(TIME_GAP_DP), innerWidth * MAX_TIME_FRACTION)
}

private const val MARKER_COLUMN_DP = 18f
private const val MARKER_INSET_DP = 4f
private const val TIME_GAP_DP = 10f
private const val MAX_TIME_FRACTION = 0.4f
