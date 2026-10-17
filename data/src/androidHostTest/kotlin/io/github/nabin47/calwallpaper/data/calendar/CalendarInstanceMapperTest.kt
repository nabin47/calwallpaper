package io.github.nabin47.calwallpaper.data.calendar

import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class CalendarInstanceMapperTest {
    private val oct17Utc = Instant.parse("2026-10-17T00:00:00Z").toEpochMilliseconds()
    private val dayMs = 24L * 60 * 60 * 1000

    private fun row(
        begin: Long,
        end: Long,
        allDay: Boolean,
        title: String? = "Standup",
    ) = CalendarInstanceRow(
        eventId = 42,
        beginMillis = begin,
        endMillis = end,
        title = title,
        allDay = allDay,
        calendarId = 3,
        displayColor = 0xFF4C8BF5.toInt(),
    )

    @Test
    fun singleAllDayEventUsesUtcDates() {
        val item = row(oct17Utc, oct17Utc + dayMs, allDay = true).toAgendaItem()
        val date = LocalDate(2026, 10, 17)
        assertEquals(ItemTime.AllDay(date, date), item.time)
    }

    @Test
    fun multiDayAllDayEventHasInclusiveEnd() {
        val item = row(oct17Utc, oct17Utc + 3 * dayMs, allDay = true).toAgendaItem()
        assertEquals(ItemTime.AllDay(LocalDate(2026, 10, 17), LocalDate(2026, 10, 19)), item.time)
    }

    @Test
    fun zeroLengthAllDayEventCoversItsStartDay() {
        val item = row(oct17Utc, oct17Utc, allDay = true).toAgendaItem()
        assertEquals(ItemTime.AllDay(LocalDate(2026, 10, 17), LocalDate(2026, 10, 17)), item.time)
    }

    @Test
    fun timedEventKeepsInstantsAndIdentity() {
        val begin = oct17Utc + 9 * 60 * 60 * 1000
        val item = row(begin, begin + 30 * 60 * 1000, allDay = false).toAgendaItem()
        val beginInstant = Instant.fromEpochMilliseconds(begin)
        assertEquals(ItemTime.Timed(beginInstant, Instant.fromEpochMilliseconds(begin + 30 * 60 * 1000)), item.time)
        assertEquals(SourceId.CALENDAR_PROVIDER, item.key.source)
        assertEquals("42@$begin", item.key.sourceItemId, "each occurrence of a recurring event is distinct")
        assertEquals(ItemKind.EVENT, item.kind)
        assertEquals("3", item.groupId)
        assertEquals(0xFF4C8BF5.toInt(), item.colorArgb)
        assertEquals(
            OpenTarget.CalendarEvent(42, beginInstant, Instant.fromEpochMilliseconds(begin + 1_800_000)),
            item.openTarget,
        )
    }

    @Test
    fun endBeforeBeginIsRepaired() {
        val item = row(oct17Utc, oct17Utc - 1000, allDay = false).toAgendaItem()
        val time = item.time as ItemTime.Timed
        assertEquals(time.start, time.end)
    }

    @Test
    fun missingTitleBecomesEmptyAndRowsRedactTitles() {
        assertEquals("", row(oct17Utc, oct17Utc + 1, allDay = false, title = null).toAgendaItem().title)
        assertFalse("Standup" in row(oct17Utc, oct17Utc + 1, allDay = false).toString())
    }
}
