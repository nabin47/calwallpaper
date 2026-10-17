package io.github.nabin47.calwallpaper.widgets

import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.OpenTargetLink
import io.github.nabin47.calwallpaper.render.Bounds
import io.github.nabin47.calwallpaper.render.HitRegion
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetTapsTest {
    private val monday = OpenTarget.CalendarDay(LocalDate(2026, 10, 19))
    private val tuesday = OpenTarget.CalendarDay(LocalDate(2026, 10, 20))

    @Test
    fun convertsPixelsToDpAndEncodesTheTarget() {
        val taps = WidgetTaps.from(listOf(HitRegion(Bounds(20f, 40f, 220f, 100f), monday, "Monday")), pxPerDp = 2f)
        assertEquals(listOf(WidgetTap(10f, 20f, 100f, 30f, OpenTargetLink.encode(monday), "Monday")), taps)
    }

    @Test
    fun stackedCellsOfTheSameDayBecomeOneTap() {
        // A week strip emits the weekday row for all days, then the date row.
        val regions =
            listOf(
                HitRegion(Bounds(0f, 0f, 50f, 20f), monday, "Mon"),
                HitRegion(Bounds(50f, 0f, 100f, 20f), tuesday, "Tue"),
                HitRegion(Bounds(0f, 20f, 50f, 60f), monday, "Mon"),
                HitRegion(Bounds(50f, 20f, 100f, 60f), tuesday, "Tue"),
            )
        val taps = WidgetTaps.from(regions, pxPerDp = 1f)
        assertEquals(2, taps.size)
        assertEquals(listOf(0f, 0f), taps.map { it.topDp })
        assertEquals(listOf(60f, 60f), taps.map { it.heightDp })
    }

    @Test
    fun separateRowsOfTheSameTargetStaySeparate() {
        val regions =
            listOf(
                HitRegion(Bounds(0f, 0f, 100f, 20f), monday, "Mon"),
                HitRegion(Bounds(0f, 40f, 100f, 60f), monday, "Mon"),
            )
        assertEquals(2, WidgetTaps.from(regions, pxPerDp = 1f).size)
    }

    @Test
    fun emptyRegionsAreDroppedAndTheCountIsCapped() {
        val regions =
            List(WidgetTaps.MAX_TAPS + 10) { i ->
                HitRegion(Bounds(0f, i * 10f, 100f, i * 10f + 5f), OpenTarget.LocalItem(i.toLong()), "$i")
            } + HitRegion(Bounds(0f, 0f, 0f, 10f), monday, "empty")
        val taps = WidgetTaps.from(regions, pxPerDp = 1f)
        assertEquals(WidgetTaps.MAX_TAPS, taps.size)
        assertEquals("0", taps.first().label)
    }
}
