package io.github.nabin47.calwallpaper.core.time

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ItemTimesTest {
    private val zone = TimeZone.of("Asia/Dhaka")
    private val oct17 = LocalDate(2026, 10, 17)
    private val range = DateRange.startingAt(oct17, 1)

    private fun at(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Instant = LocalDateTime(2026, 10, day, hour, minute).toInstant(zone)

    @Test
    fun timedOverlapUsesZoneDayBoundaries() {
        assertTrue(ItemTime.Timed(at(17, 23), at(18, 1)).overlaps(range, zone))
        assertFalse(ItemTime.Timed(at(16, 22), at(17, 0)).overlaps(range, zone), "ends exactly at midnight")
        assertFalse(ItemTime.Timed(at(18, 0), at(18, 1)).overlaps(range, zone))
        assertTrue(ItemTime.Timed(at(17, 0), at(17, 0)).overlaps(range, zone), "zero-length at start")
    }

    @Test
    fun allDayAndDueOverlap() {
        assertTrue(ItemTime.AllDay(LocalDate(2026, 10, 15), oct17).overlaps(range, zone))
        assertFalse(ItemTime.AllDay(LocalDate(2026, 10, 15), LocalDate(2026, 10, 16)).overlaps(range, zone))
        assertTrue(ItemTime.Due(oct17).overlaps(range, zone))
        assertFalse(ItemTime.Due(LocalDate(2026, 10, 18)).overlaps(range, zone))
    }

    @Test
    fun startDateConvertsTimedToZone() {
        // 20:00 UTC on the 16th is 02:00 on the 17th in Dhaka (UTC+6).
        val time = ItemTime.Timed(Instant.parse("2026-10-16T20:00:00Z"), Instant.parse("2026-10-16T21:00:00Z"))
        assertEquals(oct17, time.startDate(zone))
    }

    @Test
    fun agendaOrderPutsWholeDayFirstThenByTime() {
        fun item(
            id: String,
            kind: ItemKind,
            time: ItemTime,
        ) = AgendaItem(ItemKey(SourceId.CALENDAR_PROVIDER, id), kind, "t", time, OpenTarget.CalendarDay(oct17))

        val lunch = item("lunch", ItemKind.EVENT, ItemTime.Timed(at(17, 12), at(17, 13)))
        val holiday = item("holiday", ItemKind.EVENT, ItemTime.AllDay(oct17, oct17))
        val task = item("task", ItemKind.TASK, ItemTime.Due(oct17))
        val call = item("call", ItemKind.TASK, ItemTime.Due(oct17, LocalTime(9, 30)))

        val sorted = listOf(lunch, call, task, holiday).sortedWith(agendaOrder(zone))
        assertEquals(listOf("holiday", "task", "call", "lunch"), sorted.map { it.key.sourceItemId })
    }
}
