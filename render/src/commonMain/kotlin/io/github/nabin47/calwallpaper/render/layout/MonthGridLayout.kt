package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.core.time.agendaOrder
import io.github.nabin47.calwallpaper.core.time.overlaps
import io.github.nabin47.calwallpaper.render.Bounds
import io.github.nabin47.calwallpaper.render.text.TextStyle
import io.github.nabin47.calwallpaper.render.text.shrinkToFit
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Month grid: month title, weekday initials and one cell per day with up to three colored dots for its items.
 * Never shows titles, so it looks the same in privacy mode. Cells shrink to fit the safe area.
 */
internal fun LayoutContext.layoutMonthGrid(
    items: List<AgendaItem>,
    month: DateRange,
    firstDayOfWeek: kotlinx.datetime.DayOfWeek,
) {
    val order = agendaOrder(zone)
    val blanks = leadingBlanks(month.start, firstDayOfWeek)
    val weeks = (blanks + month.dayCount + DAYS - 1) / DAYS
    val columnWidth = innerWidth / DAYS
    val title = textRow(strings.monthTitle(month.start.month, month.start.year), header, palette.text)
    val weekdays = weekdayInitialsRow(firstDayOfWeek, columnWidth)
    val dayLabels = (1..month.dayCount).map { it.toString() }
    val numbers = measurer.shrinkToFit(caption, dayLabels, columnWidth - dp(CELL_PADDING_DP) * 2)
    val empty = items.isEmpty()
    val message = if (empty) textRow(strings.emptyMonth, caption, palette.secondaryText) else null
    val fixed = title.height + weekdays.height + (message?.height ?: 0f)
    val available = content.height - 2 * padding - fixed
    val cellHeight = minOf(columnWidth, available / weeks).coerceAtLeast(lineHeight(numbers) + dp(MIN_CELL_EXTRA_DP))
    val cell = Cell(columnWidth, cellHeight, numbers)
    val dayItems: (LocalDate) -> List<AgendaItem> = { day ->
        items.filter { it.time.overlaps(DateRange(day, day), zone) }.sortedWith(order)
    }
    val rows = mutableListOf(title, weekdays)
    repeat(weeks) { week -> rows += weekRow(month, blanks, week, cell, dayItems) }
    message?.let { rows += it }
    placeBlock(rows) { hidden -> moreRow(hidden) }
}

private fun LayoutContext.weekdayInitialsRow(
    firstDayOfWeek: kotlinx.datetime.DayOfWeek,
    columnWidth: Float,
): Row {
    val labels = weekdaysFrom(firstDayOfWeek).map { strings.weekdayNarrow(it) }
    val style = measurer.shrinkToFit(caption, labels, columnWidth - dp(CELL_PADDING_DP) * 2)
    val height = lineHeight(style) + rowGap
    return Row(height) { top ->
        labels.forEachIndexed { i, label ->
            val x = innerLeft + i * columnWidth + (columnWidth - width(label, style)) / 2
            text(label, x, top + rowGap / 2 + ascent(style), style, palette.secondaryText)
        }
    }
}

private fun LayoutContext.weekRow(
    month: DateRange,
    blanks: Int,
    week: Int,
    cell: Cell,
    dayItems: (LocalDate) -> List<AgendaItem>,
): Row =
    Row(cell.height) { top ->
        repeat(DAYS) { column ->
            val index = week * DAYS + column - blanks
            if (index < 0 || index >= month.dayCount) return@repeat
            dayCell(month.start.plus(index, DateTimeUnit.DAY), innerLeft + column * cell.width, top, cell, dayItems)
        }
    }

/** Size of every day cell, and the style of its number (shrunk to fit the cell width). */
private class Cell(
    val width: Float,
    val height: Float,
    val number: TextStyle,
)

private fun LayoutContext.dayCell(
    day: LocalDate,
    left: Float,
    top: Float,
    cell: Cell,
    dayItems: (LocalDate) -> List<AgendaItem>,
) {
    val width = cell.width
    val height = cell.height
    val label = day.day.toString()
    val centerX = left + width / 2
    val numberCenterY = top + height * NUMBER_POSITION
    val isToday = day == today
    if (isToday) circle(centerX, numberCenterY, minOf(width, height) * TODAY_RADIUS_FACTOR, palette.accent)
    val number = cell.number
    val baseline = numberCenterY - lineHeight(number) / 2 + ascent(number)
    text(label, centerX - width(label, number) / 2, baseline, number, if (isToday) palette.onAccent else palette.text)
    dotRun(dayItems(day).take(MAX_DOTS), centerX, top + height * DOTS_POSITION, dp(DOT_DP))
    dayRegion(Bounds(left, top, left + width, top + height), day)
}

private const val DAYS = 7
private const val MIN_CELL_EXTRA_DP = 8f
private const val CELL_PADDING_DP = 2f
private const val NUMBER_POSITION = 0.38f
private const val DOTS_POSITION = 0.8f
private const val TODAY_RADIUS_FACTOR = 0.33f
private const val DOT_DP = 2f
