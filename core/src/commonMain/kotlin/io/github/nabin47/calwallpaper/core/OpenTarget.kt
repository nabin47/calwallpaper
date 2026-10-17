package io.github.nabin47.calwallpaper.core

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Where a tap on an item or a day should lead. Platform code resolves it to an Intent (Android) or a URL,
 * falling back to the web when the source app is missing (CLAUDE.md §4). Immutable.
 */
sealed interface OpenTarget {
    /** A day in the user's calendar app. */
    data class CalendarDay(
        val date: LocalDate,
    ) : OpenTarget

    /**
     * One occurrence of a calendar event. [begin] and [end] identify the occurrence of a recurring event,
     * which calendar apps need in addition to [eventId].
     */
    data class CalendarEvent(
        val eventId: Long,
        val begin: Instant,
        val end: Instant,
    ) : OpenTarget

    /** A Todoist task by its Todoist id. */
    data class TodoistTask(
        val taskId: String,
    ) : OpenTarget

    /** A Todoist project by its Todoist id. */
    data class TodoistProject(
        val projectId: String,
    ) : OpenTarget

    /** An item of the app's own local task list, by database id. */
    data class LocalItem(
        val id: Long,
    ) : OpenTarget

    /** A web page. Only `https://` URLs are allowed (CLAUDE.md §7: HTTPS only). */
    data class Url(
        val url: String,
    ) : OpenTarget {
        init {
            require(url.startsWith("https://")) { "Only https:// URLs are allowed" }
        }
    }
}
