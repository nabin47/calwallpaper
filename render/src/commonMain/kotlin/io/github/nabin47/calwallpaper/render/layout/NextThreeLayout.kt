package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.time.agendaOrder
import io.github.nabin47.calwallpaper.core.time.startDate
import io.github.nabin47.calwallpaper.render.Bounds
import io.github.nabin47.calwallpaper.render.format.dayLabel
import io.github.nabin47.calwallpaper.render.format.timeLabel
import io.github.nabin47.calwallpaper.render.text.ellipsize
import io.github.nabin47.calwallpaper.render.text.wrap
import kotlin.time.Instant

/**
 * Next-3 minimal: the next three unfinished items, each with a "when" line and a large title wrapped to two
 * lines. Completed tasks are skipped even when "show completed" is on. In privacy mode the title is replaced by
 * a neutral word.
 */
internal fun LayoutContext.layoutNextThree(items: List<AgendaItem>) {
    val upcoming =
        items
            .filter { !it.isCompleted && it.isUpcoming(request.now) }
            .sortedWith(agendaOrder(zone))
            .take(COUNT)
    val rows = mutableListOf<Row>()
    upcoming.forEach { item ->
        if (rows.isNotEmpty()) rows += spacerRow(groupGap * 2)
        rows += entryRow(item)
    }
    if (rows.isEmpty()) {
        rows += textRow(strings.allClear, large, palette.text)
        rows += textRow(strings.nothingUpcoming, caption, palette.secondaryText)
    }
    placeBlock(rows) { hidden -> moreRow(hidden) }
}

private fun LayoutContext.entryRow(item: AgendaItem): Row {
    val day = maxOf(item.time.startDate(zone), today)
    val whenText =
        listOf(dayLabel(day, today, strings), timeLabel(item.time, day, zone, request.use24HourClock, strings))
            .filter { it.isNotEmpty() }
            .joinToString(SEPARATOR)
    val whenLine = measurer.ellipsize(whenText, caption, innerWidth)
    val titleStyle = large.forItem(item)
    val title = if (hideTitles) strings.hiddenTitle else item.title
    val lines = measurer.wrap(title, titleStyle, innerWidth, MAX_TITLE_LINES)
    val height = lineHeight(caption) + rowGap / 2 + lines.size * lineHeight(titleStyle) + rowGap / 2
    return Row(height, itemCount = 1) { top ->
        text(whenLine, innerLeft, top + ascent(caption), caption, markerColor(item))
        var lineTop = top + lineHeight(caption) + rowGap / 2
        lines.forEach { line ->
            text(line, innerLeft, lineTop + ascent(titleStyle), titleStyle, titleColor(item))
            lineTop += lineHeight(titleStyle)
        }
        region(Bounds(content.left, top, content.right, top + height), item.openTarget)
    }
}

/** True if the item has not ended yet as of [now] (whole-day items count until the end of their last day). */
private fun AgendaItem.isUpcoming(now: Instant): Boolean =
    when (val t = time) {
        is ItemTime.Timed -> t.end > now || (t.end == t.start && t.start >= now)
        is ItemTime.AllDay, is ItemTime.Due -> true
    }

private const val COUNT = 3
private const val MAX_TITLE_LINES = 2
private const val SEPARATOR = " · "
