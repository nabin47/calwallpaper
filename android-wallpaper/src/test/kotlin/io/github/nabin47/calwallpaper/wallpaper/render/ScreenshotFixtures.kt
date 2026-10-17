package io.github.nabin47.calwallpaper.wallpaper.render

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

/** Deterministic agenda data for screenshots: Saturday 17 October 2026, 08:00 UTC. */
object ScreenshotFixtures {
    val zone: TimeZone = TimeZone.UTC
    val today: LocalDate = LocalDate(2026, 10, 17)
    val now = LocalDateTime(2026, 10, 17, 8, 0).toInstant(zone)

    private fun at(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ) = LocalDateTime(2026, 10, day, hour, minute).toInstant(zone)

    private fun event(
        id: String,
        title: String,
        time: ItemTime,
        color: Int,
    ) = AgendaItem(
        key = ItemKey(SourceId.CALENDAR_PROVIDER, id),
        kind = ItemKind.EVENT,
        title = title,
        time = time,
        openTarget = OpenTarget.CalendarDay(today),
        groupId = "1",
        colorArgb = color,
    )

    private fun task(
        id: String,
        title: String,
        date: LocalDate,
        time: LocalTime? = null,
        completed: Boolean = false,
    ) = AgendaItem(
        key = ItemKey(SourceId.LOCAL_TASKS, id),
        kind = ItemKind.TASK,
        title = title,
        time = ItemTime.Due(date, time),
        openTarget = OpenTarget.LocalItem(id.length.toLong()),
        isCompleted = completed,
    )

    private const val BLUE = 0xFF4C8BF5.toInt()
    private const val GREEN = 0xFF34A853.toInt()
    private const val RED = 0xFFEA4335.toInt()
    private const val AMBER = 0xFFF9AB00.toInt()

    /** A busy week with long titles, a completed task and multi-day events. */
    val items: List<AgendaItem> =
        listOf(
            event("standup", "Standup", ItemTime.Timed(at(17, 9, 30), at(17, 9, 45)), BLUE),
            event(
                "lunch",
                "Lunch with Rafi at the new place near Gulshan Avenue",
                ItemTime.Timed(at(17, 13), at(17, 14)),
                GREEN,
            ),
            event("trip", "Sylhet trip", ItemTime.AllDay(LocalDate(2026, 10, 18), LocalDate(2026, 10, 20)), AMBER),
            event(
                "review",
                "Quarterly planning review with the extended product, design and engineering leadership group",
                ItemTime.Timed(at(18, 15), at(18, 16, 30)),
                RED,
            ),
            event("gym", "Gym", ItemTime.Timed(at(19, 18), at(19, 19)), BLUE),
            event("dentist", "Dentist", ItemTime.Timed(at(22, 10), at(22, 11)), GREEN),
            event("party", "Nadia's birthday dinner", ItemTime.Timed(at(24, 19, 30), at(24, 22)), RED),
            task("t1", "Pay electricity bill", today),
            task("t2", "Call the bank about the card", today, LocalTime(11, 0)),
            task("t3", "Buy groceries", today, completed = true),
            task("t4", "Submit visa documents before the embassy closes for the long weekend", LocalDate(2026, 10, 21)),
        )
}
