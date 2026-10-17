package io.github.nabin47.calwallpaper.core.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus

/**
 * An inclusive range of calendar days, e.g. "today and the next 6 days". Zone-independent; convert with
 * [toInstantRange] when a source needs timestamps. Immutable.
 */
data class DateRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    init {
        require(endInclusive >= start) { "endInclusive must not be before start" }
    }

    /** Number of days in the range, at least 1. */
    val dayCount: Int get() = start.daysUntil(endInclusive) + 1

    /** True if [date] is one of the days in the range. */
    operator fun contains(date: LocalDate): Boolean = date >= start && date <= endInclusive

    /** All days in the range, in order. */
    fun days(): List<LocalDate> = List(dayCount) { start.plus(it, DateTimeUnit.DAY) }

    /**
     * The timeline span covered by these days in [zone]: from the start of [start] to the start of the day
     * after [endInclusive]. Handles days that are not 24 hours long (DST changes).
     */
    fun toInstantRange(zone: TimeZone): InstantRange =
        InstantRange(
            start = start.atStartOfDayIn(zone),
            endExclusive = endInclusive.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone),
        )

    /** Factory functions. */
    companion object {
        /** The [days]-day range beginning at [start]. [days] must be at least 1. */
        fun startingAt(
            start: LocalDate,
            days: Int,
        ): DateRange {
            require(days >= 1) { "days must be at least 1" }
            return DateRange(start, start.plus(days - 1, DateTimeUnit.DAY))
        }
    }
}
