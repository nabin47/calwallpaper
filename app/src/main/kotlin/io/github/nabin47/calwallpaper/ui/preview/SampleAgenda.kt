package io.github.nabin47.calwallpaper.ui.preview

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

/** Localized titles for [SampleAgenda]. */
data class SampleTitles(
    val standup: String,
    val lunch: String,
    val bill: String,
    val dentist: String,
    val trip: String,
    val groceries: String,
)

/**
 * A realistic week shown in the preview before calendar access is granted, so every setting can still be
 * tried out. Relative to [today], so it never looks stale. Pure.
 */
object SampleAgenda {
    /** Sample items around [today] in [zone]. */
    fun items(
        today: LocalDate,
        zone: TimeZone,
        titles: SampleTitles,
    ): List<AgendaItem> {
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        return listOf(
            event("standup", titles.standup, today, LocalTime(hour = 9, minute = 30), durationMinutes = 15, zone, BLUE),
            event("lunch", titles.lunch, today, LocalTime(hour = 13, minute = 0), durationMinutes = 60, zone, GREEN),
            task("bill", titles.bill, today, null),
            task("groceries", titles.groceries, today, null, completed = true),
            event(
                "dentist",
                titles.dentist,
                tomorrow,
                LocalTime(hour = 16, minute = 0),
                durationMinutes = 45,
                zone,
                RED,
            ),
            AgendaItem(
                key = ItemKey(SourceId.CALENDAR_PROVIDER, "trip"),
                kind = ItemKind.EVENT,
                title = titles.trip,
                time =
                    ItemTime.AllDay(
                        start = today.plus(value = 2, unit = DateTimeUnit.DAY),
                        endInclusive = today.plus(value = 3, unit = DateTimeUnit.DAY),
                    ),
                openTarget = OpenTarget.CalendarDay(today),
                colorArgb = AMBER,
            ),
        )
    }

    @Suppress("LongParameterList") // A private test-data builder; named arguments would only add noise.
    private fun event(
        id: String,
        title: String,
        date: LocalDate,
        start: LocalTime,
        durationMinutes: Int,
        zone: TimeZone,
        color: Int,
    ): AgendaItem {
        val begin = date.atTime(start).toInstant(zone)
        return AgendaItem(
            key = ItemKey(SourceId.CALENDAR_PROVIDER, id),
            kind = ItemKind.EVENT,
            title = title,
            time = ItemTime.Timed(begin, begin + durationMinutes.minutes),
            openTarget = OpenTarget.CalendarDay(date),
            colorArgb = color,
        )
    }

    private fun task(
        id: String,
        title: String,
        date: LocalDate,
        time: LocalTime?,
        completed: Boolean = false,
    ) = AgendaItem(
        key = ItemKey(SourceId.LOCAL_TASKS, id),
        kind = ItemKind.TASK,
        title = title,
        time = ItemTime.Due(date, time),
        openTarget = OpenTarget.CalendarDay(date),
        isCompleted = completed,
    )

    private const val BLUE = 0xFF4C8BF5.toInt()
    private const val GREEN = 0xFF34A853.toInt()
    private const val RED = 0xFFEA4335.toInt()
    private const val AMBER = 0xFFFBBC04.toInt()
}
