package io.github.nabin47.calwallpaper.core.time

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** True if this time touches any day of [range], judged in [zone] for timed items. Pure. */
fun ItemTime.overlaps(
    range: DateRange,
    zone: TimeZone,
): Boolean =
    when (this) {
        is ItemTime.AllDay -> start <= range.endInclusive && endInclusive >= range.start
        is ItemTime.Timed -> range.toInstantRange(zone).overlaps(start, end)
        is ItemTime.Due -> date in range
    }

/** The first local day this time falls on in [zone]. Pure. */
fun ItemTime.startDate(zone: TimeZone): LocalDate =
    when (this) {
        is ItemTime.AllDay -> start
        is ItemTime.Timed -> start.toLocalDateTime(zone).date
        is ItemTime.Due -> date
    }

/**
 * The instant used to order items in [zone]. Whole-day items (all-day events, tasks without a time) sort at
 * the start of their day. Pure.
 */
fun ItemTime.sortInstant(zone: TimeZone): Instant =
    when (this) {
        is ItemTime.AllDay -> start.atStartOfDayIn(zone)
        is ItemTime.Timed -> start
        is ItemTime.Due -> time?.let { LocalDateTime(date, it).toInstant(zone) } ?: date.atStartOfDayIn(zone)
    }

/** True for items that have no time of day: all-day events and tasks due on a date only. */
val ItemTime.isWholeDay: Boolean
    get() = this is ItemTime.AllDay || (this is ItemTime.Due && time == null)

/**
 * The agenda display order in [zone]: by start; on ties, whole-day items first, then events before tasks,
 * then by source and source id so the order is stable across refreshes. Never compares titles, so it
 * works the same in privacy mode.
 */
fun agendaOrder(zone: TimeZone): Comparator<AgendaItem> =
    compareBy<AgendaItem> { it.time.sortInstant(zone) }
        .thenBy { if (it.time.isWholeDay) 0 else 1 }
        .thenBy { it.kind }
        .thenBy { it.key.source }
        .thenBy { it.key.sourceItemId }
