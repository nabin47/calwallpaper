package io.github.nabin47.calwallpaper.data.local

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Minutes in a day; valid minute-of-day values are `0 until MINUTES_PER_DAY`. */
internal const val MINUTES_PER_DAY: Int = 24 * 60

private const val MINUTES_PER_HOUR = 60

/**
 * Maps a stored local task to an [AgendaItem]. Pure.
 *
 * Tasks without a due date have no day of their own; they are shown on [undatedOn] (normally today, the first
 * day of the requested range) so they stay visible. A minute-of-day outside `0..1439` is ignored.
 */
fun localTaskToAgendaItem(
    id: Long,
    title: String,
    dueEpochDay: Long?,
    dueMinuteOfDay: Int?,
    completed: Boolean,
    undatedOn: LocalDate,
): AgendaItem {
    val date = dueEpochDay?.let { LocalDate.fromEpochDays(it) } ?: undatedOn
    val time =
        dueMinuteOfDay
            ?.takeIf { dueEpochDay != null && it in 0 until MINUTES_PER_DAY }
            ?.let { LocalTime(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }
    return AgendaItem(
        key = ItemKey(SourceId.LOCAL_TASKS, id.toString()),
        kind = ItemKind.TASK,
        title = title,
        time = ItemTime.Due(date, time),
        openTarget = OpenTarget.LocalItem(id),
        isCompleted = completed,
    )
}
