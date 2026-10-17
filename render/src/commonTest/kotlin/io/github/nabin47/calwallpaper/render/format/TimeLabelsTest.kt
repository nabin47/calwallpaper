package io.github.nabin47.calwallpaper.render.format

import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.render.Fixtures
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeLabelsTest {
    private val strings = EnglishRenderStrings

    @Test
    fun formatsTwelveAndTwentyFourHour() {
        assertEquals("09:05", formatTime(LocalTime(9, 5), use24Hour = true, strings = strings))
        assertEquals("9:05 AM", formatTime(LocalTime(9, 5), use24Hour = false, strings = strings))
        assertEquals("12:00 PM", formatTime(LocalTime(12, 0), use24Hour = false, strings = strings))
        assertEquals("12:30 AM", formatTime(LocalTime(0, 30), use24Hour = false, strings = strings))
    }

    @Test
    fun dayLabelsAreRelative() {
        val today = Fixtures.today
        assertEquals("Today", dayLabel(today, today, strings))
        assertEquals("Tomorrow", dayLabel(LocalDate(2026, 10, 18), today, strings))
        assertEquals("Mon, Oct 19", dayLabel(LocalDate(2026, 10, 19), today, strings))
    }

    @Test
    fun timeLabelsPerKind() {
        val day = Fixtures.today
        val zone = Fixtures.zone
        assertEquals("All day", timeLabel(ItemTime.AllDay(day, day), day, zone, true, strings))
        assertEquals("", timeLabel(ItemTime.Due(day), day, zone, true, strings))
        assertEquals("11:00", timeLabel(ItemTime.Due(day, LocalTime(11, 0)), day, zone, true, strings))
        val overnight = ItemTime.Timed(Fixtures.at(16, 22), Fixtures.at(17, 2))
        assertEquals("Cont.", timeLabel(overnight, day, zone, true, strings))
        assertEquals("22:00", timeLabel(overnight, LocalDate(2026, 10, 16), zone, true, strings))
    }
}
