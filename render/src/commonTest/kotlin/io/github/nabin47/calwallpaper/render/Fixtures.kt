package io.github.nabin47.calwallpaper.render

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.text.FontMetrics
import io.github.nabin47.calwallpaper.render.text.TextMeasurer
import io.github.nabin47.calwallpaper.render.text.TextStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Every character is half the font size wide; ascent 0.8, descent 0.2 of the size. Deterministic. */
object FakeMeasurer : TextMeasurer {
    override fun width(
        text: String,
        style: TextStyle,
    ): Float = text.length * style.sizePx * 0.5f

    override fun metrics(style: TextStyle): FontMetrics = FontMetrics(style.sizePx * 0.8f, style.sizePx * 0.2f)
}

object Fixtures {
    val zone: TimeZone = TimeZone.UTC
    val today: LocalDate = LocalDate(2026, 10, 17) // A Saturday.
    val now = LocalDateTime(2026, 10, 17, 8, 0).toInstant(zone)
    val screen = ScreenSpec(widthPx = 720, heightPx = 1600, density = 2f)
    const val LONG_TITLE =
        "Quarterly planning review with the extended product, design and engineering leadership group"

    fun at(
        day: Int,
        hour: Int,
        minute: Int = 0,
    ) = LocalDateTime(2026, 10, day, hour, minute).toInstant(zone)

    fun event(
        id: String,
        title: String,
        time: ItemTime,
        color: Int? = 0xFF4C8BF5.toInt(),
    ) = AgendaItem(
        key = ItemKey(SourceId.CALENDAR_PROVIDER, id),
        kind = ItemKind.EVENT,
        title = title,
        time = time,
        openTarget = OpenTarget.CalendarDay(today),
        groupId = "1",
        colorArgb = color,
    )

    fun task(
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
        openTarget = OpenTarget.LocalItem(id.hashCode().toLong()),
        isCompleted = completed,
    )

    /** A realistic week: timed and all-day events, tasks (one done), a multi-day event and a very long title. */
    val week: List<AgendaItem> =
        listOf(
            event("standup", "Standup", ItemTime.Timed(at(17, 9, 30), at(17, 9, 45))),
            event("lunch", "Lunch with Rafi", ItemTime.Timed(at(17, 13), at(17, 14)), color = 0xFF34A853.toInt()),
            event("trip", "Sylhet trip", ItemTime.AllDay(LocalDate(2026, 10, 18), LocalDate(2026, 10, 20))),
            event("review", LONG_TITLE, ItemTime.Timed(at(18, 15), at(18, 16, 30)), color = 0xFFEA4335.toInt()),
            event("gym", "Gym", ItemTime.Timed(at(19, 18), at(19, 19))),
            task("t1", "Pay electricity bill", today),
            task("t2", "Call the bank", today, LocalTime(11, 0)),
            task("t3", "Buy groceries", today, completed = true),
            task("t4", "Submit visa documents before the embassy closes for the long weekend", LocalDate(2026, 10, 21)),
        )

    fun settings(
        preset: LayoutPreset,
        privacy: PrivacyMode = PrivacyMode.OFF,
    ): Settings {
        val base = Settings()
        return base.copy(
            layout = base.layout.copy(preset = preset),
            content = base.content.copy(showCompleted = true),
            privacyMode = privacy,
        )
    }

    fun request(
        settings: Settings,
        items: List<AgendaItem> = week,
        isLocked: Boolean = false,
    ) = RenderRequest(
        items = items,
        settings = settings,
        screen = screen,
        now = now,
        zone = zone,
        isLocked = isLocked,
    )

    fun layout(
        settings: Settings,
        items: List<AgendaItem> = week,
        isLocked: Boolean = false,
    ): Scene = LayoutEngine(FakeMeasurer).layout(request(settings, items, isLocked))

    /** All text drawn in the scene. */
    fun Scene.texts(): List<String> = ops.filterIsInstance<DrawOp.Text>().map { it.text }
}
