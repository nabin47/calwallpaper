package io.github.nabin47.calwallpaper.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class DateRangeTest {
    private val oct17 = LocalDate(2026, 10, 17)

    @Test
    fun startingAtCoversRequestedDays() {
        val range = DateRange.startingAt(oct17, 7)
        assertEquals(LocalDate(2026, 10, 23), range.endInclusive)
        assertEquals(7, range.dayCount)
        assertEquals(7, range.days().size)
        assertEquals(oct17, range.days().first())
    }

    @Test
    fun containsIsInclusive() {
        val range = DateRange.startingAt(oct17, 2)
        assertTrue(oct17 in range)
        assertTrue(LocalDate(2026, 10, 18) in range)
        assertFalse(LocalDate(2026, 10, 19) in range)
        assertFalse(LocalDate(2026, 10, 16) in range)
    }

    @Test
    fun rejectsInvalidRanges() {
        assertFailsWith<IllegalArgumentException> { DateRange.startingAt(oct17, 0) }
        assertFailsWith<IllegalArgumentException> { DateRange(oct17, LocalDate(2026, 10, 16)) }
    }

    @Test
    fun instantRangeHandlesDstShortDay() {
        // Europe/Berlin springs forward on 2026-03-29: that day is 23 hours long.
        val zone = TimeZone.of("Europe/Berlin")
        val window = DateRange.startingAt(LocalDate(2026, 3, 29), 1).toInstantRange(zone)
        assertEquals(23.hours, window.endExclusive - window.start)
    }
}
