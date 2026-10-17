package io.github.nabin47.calwallpaper.data.todoist

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
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The cached tasks as agenda items, as of [today] in [zone]. Pure.
 *
 * - **Times.** A task fixed to a time zone is converted to [zone]. A floating task keeps its local date and time.
 * - **Overdue tasks** move to [today] without a time, as in Todoist's Today view. Todoist returns only open tasks,
 *   so these are still to do.
 * - **Links.** Each task opens itself in Todoist (or on the web).
 * - **Project.** The task's group is its project, so the project picker in settings filters it, and it takes the
 *   project's color.
 * - **Unparseable dates** drop the task rather than guess a day.
 */
fun TodoistCache.toAgendaItems(
    today: LocalDate,
    zone: TimeZone,
): List<AgendaItem> {
    val colors = projects.associate { it.id to it.colorArgb }
    return tasks.mapNotNull { task ->
        val due = task.dueIn(zone) ?: return@mapNotNull null
        AgendaItem(
            key = ItemKey(SourceId.TODOIST, task.id),
            kind = ItemKind.TASK,
            title = task.title,
            time = if (due.date < today) ItemTime.Due(today) else due,
            openTarget = OpenTarget.TodoistTask(task.id),
            groupId = task.projectId,
            colorArgb = colors[task.projectId],
        )
    }
}

/** The task's due date and time in [zone], or `null` if the stored values do not parse. */
private fun CachedTask.dueIn(zone: TimeZone): ItemTime.Due? =
    runCatching {
        val instant = dueInstant?.let(Instant::parse)
        if (instant != null) {
            val local: LocalDateTime = instant.toLocalDateTime(zone)
            ItemTime.Due(local.date, local.time)
        } else {
            ItemTime.Due(LocalDate.parse(dueDate), dueTime?.let(LocalTime::parse))
        }
    }.getOrNull()
