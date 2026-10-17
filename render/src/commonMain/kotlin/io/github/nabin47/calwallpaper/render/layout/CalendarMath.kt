package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.time.DateRange
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val DAYS_PER_WEEK = 7

/** The 7-day week containing [date], starting on [firstDayOfWeek]. Pure. */
internal fun weekContaining(
    date: LocalDate,
    firstDayOfWeek: DayOfWeek,
): DateRange {
    val offset = (date.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + DAYS_PER_WEEK) % DAYS_PER_WEEK
    return DateRange.startingAt(date.minus(offset, DateTimeUnit.DAY), DAYS_PER_WEEK)
}

/** All days of [date]'s month. Pure. */
internal fun monthContaining(date: LocalDate): DateRange {
    val first = LocalDate(date.year, date.month, 1)
    return DateRange(first, first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY))
}

/** Number of leading blank cells before the 1st in a month grid starting on [firstDayOfWeek]. */
internal fun leadingBlanks(
    firstOfMonth: LocalDate,
    firstDayOfWeek: DayOfWeek,
): Int = (firstOfMonth.dayOfWeek.isoDayNumber - firstDayOfWeek.isoDayNumber + DAYS_PER_WEEK) % DAYS_PER_WEEK

/** The seven weekdays in display order starting at [firstDayOfWeek]. */
internal fun weekdaysFrom(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    List(DAYS_PER_WEEK) { DayOfWeek((firstDayOfWeek.isoDayNumber - 1 + it) % DAYS_PER_WEEK + 1) }
