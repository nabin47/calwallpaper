package io.github.nabin47.calwallpaper.render.format

import io.github.nabin47.calwallpaper.core.ItemTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private const val NOON = 12
private const val TWO_DIGITS = 2

/** Formats [time] as `09:30` (24-hour) or `9:30 AM` (12-hour). Pure. */
fun formatTime(
    time: LocalTime,
    use24Hour: Boolean,
    strings: RenderStrings,
): String {
    val minutes = time.minute.toString().padStart(TWO_DIGITS, '0')
    if (use24Hour) return "${time.hour.toString().padStart(TWO_DIGITS, '0')}:$minutes"
    val hour12 = (time.hour % NOON).let { if (it == 0) NOON else it }
    val suffix = if (time.hour < NOON) strings.am else strings.pm
    return "$hour12:$minutes $suffix"
}

/** "Today", "Tomorrow" or a [RenderStrings.dateLabel] for [date], relative to [today]. Pure. */
fun dayLabel(
    date: LocalDate,
    today: LocalDate,
    strings: RenderStrings,
): String =
    when (date) {
        today -> strings.today
        today.plus(1, DateTimeUnit.DAY) -> strings.tomorrow
        else -> strings.dateLabel(date)
    }

/**
 * The time column text for an item shown on [day]: the start time, "All day", "Cont." for timed items that
 * began on an earlier day, or an empty string for tasks without a time. Pure.
 */
fun timeLabel(
    time: ItemTime,
    day: LocalDate,
    zone: TimeZone,
    use24Hour: Boolean,
    strings: RenderStrings,
): String =
    when (time) {
        is ItemTime.AllDay -> {
            strings.allDay
        }

        is ItemTime.Due -> {
            time.time?.let { formatTime(it, use24Hour, strings) }.orEmpty()
        }

        is ItemTime.Timed -> {
            val start = time.start.toLocalDateTime(zone)
            if (start.date < day) strings.continues else formatTime(start.time, use24Hour, strings)
        }
    }
