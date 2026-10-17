package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.core.time.agendaOrder
import io.github.nabin47.calwallpaper.core.time.overlaps
import io.github.nabin47.calwallpaper.render.Bounds
import io.github.nabin47.calwallpaper.render.text.ellipsize
import kotlinx.datetime.LocalDate

/**
 * Week strip: seven columns (weekday, date, then up to [MAX_LINES] item titles each). Today's date sits in an
 * accent circle. In privacy mode, columns show colored dots instead of titles.
 */
internal fun LayoutContext.layoutWeekStrip(
    items: List<AgendaItem>,
    week: DateRange,
) {
    val order = agendaOrder(zone)
    val columns =
        week.days().map { day ->
            day to
                items.filter { it.time.overlaps(DateRange(day, day), zone) }.sortedWith(order)
        }
    val columnWidth = innerWidth / columns.size
    val rows =
        mutableListOf(weekdayRow(columns.map { it.first }, columnWidth), dateRow(columns.map { it.first }, columnWidth))
    val busiest = columns.maxOf { it.second.size }
    when {
        busiest == 0 -> {
            rows += textRow(strings.emptyWeek, caption, palette.secondaryText)
        }

        hideTitles -> {
            rows += dotsRow(columns, columnWidth)
        }

        else -> {
            repeat(minOf(busiest, MAX_LINES)) { line -> rows += itemLineRow(columns, columnWidth, line) }
            if (busiest > MAX_LINES) rows += overflowRow(columns, columnWidth)
        }
    }
    placeBlock(rows) { hidden -> moreRow(hidden) }
}

private fun LayoutContext.columnLeft(
    index: Int,
    columnWidth: Float,
): Float = innerLeft + index * columnWidth

private fun LayoutContext.weekdayRow(
    days: List<LocalDate>,
    columnWidth: Float,
): Row {
    val height = lineHeight(caption) + rowGap
    return Row(height) { top ->
        days.forEachIndexed { i, day ->
            val label = strings.weekdayNarrow(day.dayOfWeek)
            val x = columnLeft(i, columnWidth) + (columnWidth - width(label, caption)) / 2
            text(label, x, top + rowGap / 2 + ascent(caption), caption, palette.secondaryText)
            region(
                Bounds(columnLeft(i, columnWidth), top, columnLeft(i + 1, columnWidth), top + height),
                OpenTarget.CalendarDay(day),
            )
        }
    }
}

private fun LayoutContext.dateRow(
    days: List<LocalDate>,
    columnWidth: Float,
): Row {
    val height = lineHeight(body) * TODAY_CIRCLE_FACTOR + rowGap
    return Row(height) { top ->
        val centerY = top + height / 2
        days.forEachIndexed { i, day ->
            val label = day.day.toString()
            val centerX = columnLeft(i, columnWidth) + columnWidth / 2
            val isToday = day == today
            if (isToday) {
                circle(
                    centerX,
                    centerY,
                    minOf(lineHeight(body) * TODAY_CIRCLE_FACTOR, columnWidth) / 2,
                    palette.accent,
                )
            }
            val baseline = centerY - lineHeight(body) / 2 + ascent(body)
            text(
                label,
                centerX - width(label, body) / 2,
                baseline,
                body,
                if (isToday) palette.onAccent else palette.text,
            )
            region(
                Bounds(columnLeft(i, columnWidth), top, columnLeft(i + 1, columnWidth), top + height),
                OpenTarget.CalendarDay(day),
            )
        }
    }
}

private fun LayoutContext.itemLineRow(
    columns: List<Pair<LocalDate, List<AgendaItem>>>,
    columnWidth: Float,
    line: Int,
): Row {
    val height = lineHeight(small) + rowGap / 2
    val maxText = columnWidth - dp(COLUMN_PADDING_DP) * 2
    return Row(height, itemCount = columns.count { it.second.size > line }) { top ->
        columns.forEachIndexed { i, (_, dayItems) ->
            val item = dayItems.getOrNull(line) ?: return@forEachIndexed
            val style = small.forItem(item)
            val left = columnLeft(i, columnWidth) + dp(COLUMN_PADDING_DP)
            roundRect(Bounds(left, top + dp(1f), left + dp(BAR_WIDTH_DP), top + height - dp(1f)), 0f, markerColor(item))
            val label = measurer.ellipsize(item.title, style, maxText - dp(BAR_WIDTH_DP + BAR_GAP_DP))
            text(
                label,
                left + dp(BAR_WIDTH_DP + BAR_GAP_DP),
                top + lineGap(rowGap) + ascent(small),
                style,
                titleColor(item),
            )
            region(
                Bounds(columnLeft(i, columnWidth), top, columnLeft(i + 1, columnWidth), top + height),
                item.openTarget,
            )
        }
    }
}

private fun LayoutContext.overflowRow(
    columns: List<Pair<LocalDate, List<AgendaItem>>>,
    columnWidth: Float,
): Row {
    val height = lineHeight(small) + rowGap / 2
    val hidden = columns.sumOf { (it.second.size - MAX_LINES).coerceAtLeast(0) }
    return Row(height, itemCount = hidden) { top ->
        columns.forEachIndexed { i, (_, dayItems) ->
            val extra = dayItems.size - MAX_LINES
            if (extra <= 0) return@forEachIndexed
            val label = "+$extra"
            val x = columnLeft(i, columnWidth) + (columnWidth - width(label, small)) / 2
            text(label, x, top + lineGap(rowGap) + ascent(small), small, palette.secondaryText)
        }
    }
}

private fun LayoutContext.dotsRow(
    columns: List<Pair<LocalDate, List<AgendaItem>>>,
    columnWidth: Float,
): Row {
    val radius = dp(DOT_DP)
    val height = radius * 2 + rowGap
    return Row(height, itemCount = columns.sumOf { it.second.size }) { top ->
        columns.forEachIndexed { i, (_, dayItems) ->
            dotRun(dayItems.take(MAX_DOTS), columnLeft(i, columnWidth) + columnWidth / 2, top + height / 2, radius)
        }
    }
}

/** Up to [MAX_DOTS] colored dots centered on ([centerX], [centerY]). */
internal fun LayoutContext.dotRun(
    items: List<AgendaItem>,
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    val step = radius * DOT_STEP_FACTOR
    val start = centerX - step * (items.size - 1) / 2
    items.forEachIndexed { i, item -> circle(start + i * step, centerY, radius, markerColor(item)) }
}

/** Top offset of a compact line whose row adds half a row gap: a quarter gap above the text. */
private fun lineGap(rowGap: Float): Float = rowGap / LINE_GAP_DIVISOR

private const val LINE_GAP_DIVISOR = 4f
private const val MAX_LINES = 3
internal const val MAX_DOTS = 3
private const val TODAY_CIRCLE_FACTOR = 1.6f
private const val COLUMN_PADDING_DP = 2f
private const val BAR_WIDTH_DP = 2.5f
private const val BAR_GAP_DP = 3f
private const val DOT_DP = 2.5f
private const val DOT_STEP_FACTOR = 3f
