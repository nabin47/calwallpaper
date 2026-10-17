package io.github.nabin47.calwallpaper.open

import io.github.nabin47.calwallpaper.core.OpenTarget
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/** One way to open a target, before it becomes an Android Intent (see [LaunchIntents]). */
sealed interface LaunchSpec {
    /** The calendar app's day view at [epochMillis] (the Calendar Provider's `time/` URI, any calendar app). */
    data class CalendarTime(
        val epochMillis: Long,
    ) : LaunchSpec

    /** One occurrence of a Calendar Provider event, in whichever calendar app handles it. */
    data class CalendarEvent(
        val eventId: Long,
        val beginMillis: Long,
        val endMillis: Long,
    ) : LaunchSpec

    /** An app's own deep link, e.g. `todoist://task?id=…`. */
    data class AppLink(
        val uri: String,
    ) : LaunchSpec

    /**
     * A web page. Always `https://`. With [appOnly], only an app that claims the link (not a browser) may open
     * it, so a source app's own web links open in that app when it is installed (Android 11+).
     */
    data class Web(
        val url: String,
        val appOnly: Boolean = false,
    ) : LaunchSpec

    /** This app's main screen (local items have no screen of their own yet). */
    data object OwnApp : LaunchSpec
}

/**
 * Turns an [OpenTarget] into ways to open it, best first. The router tries them in order and uses the first that
 * an installed app can handle, so every list ends with a fallback that always works: a web page or this app.
 * Pure and thread-safe.
 */
object LaunchPlan {
    /** Candidates for [target], best first. [zone] decides which instant "a day" starts at. */
    fun candidates(
        target: OpenTarget,
        zone: TimeZone,
    ): List<LaunchSpec> =
        when (target) {
            is OpenTarget.CalendarDay -> {
                dayCandidates(target.date, zone)
            }

            is OpenTarget.CalendarEvent -> {
                listOf(
                    LaunchSpec.CalendarEvent(
                        target.eventId,
                        target.begin.toEpochMilliseconds(),
                        target.end.toEpochMilliseconds(),
                    ),
                ) + dayCandidates(target.begin.toLocalDateTime(zone).date, zone)
            }

            is OpenTarget.TodoistTask -> {
                todoistCandidates("task", target.taskId)
            }

            is OpenTarget.TodoistProject -> {
                todoistCandidates("project", target.projectId)
            }

            is OpenTarget.LocalItem -> {
                listOf(LaunchSpec.OwnApp)
            }

            is OpenTarget.Url -> {
                listOf(LaunchSpec.Web(target.url))
            }
        }

    /**
     * A Todoist task or project: its web link in the Todoist app if the app claims such links, then the app's
     * `todoist://` link, then the web page in the browser. Only the web URL format is documented by Todoist
     * (its SDKs build `app.todoist.com/app/task/<id>`); the `todoist://` links are a best effort (ADR 0005).
     * An id that is not plain letters, digits, `-` or `_` is never put into a URL; Todoist's home is used instead.
     */
    private fun todoistCandidates(
        kind: String,
        id: String,
    ): List<LaunchSpec> =
        if (SAFE_ID.matches(id)) {
            val web = "https://app.todoist.com/app/$kind/$id"
            listOf(
                LaunchSpec.Web(web, appOnly = true),
                LaunchSpec.AppLink("todoist://$kind?id=$id"),
                LaunchSpec.Web(web),
            )
        } else {
            listOf(LaunchSpec.AppLink("todoist://today"), LaunchSpec.Web("https://app.todoist.com/app"))
        }

    /** The calendar app at the start of [date], then Google Calendar on the web for that day. */
    private fun dayCandidates(
        date: LocalDate,
        zone: TimeZone,
    ): List<LaunchSpec> =
        listOf(
            LaunchSpec.CalendarTime(date.atStartOfDayIn(zone).toEpochMilliseconds()),
            LaunchSpec.Web("https://calendar.google.com/calendar/r/day/${date.year}/${date.month.number}/${date.day}"),
        )

    /** Todoist ids are alphanumeric; anything else is never put into a URL. */
    private val SAFE_ID = Regex("[A-Za-z0-9_-]+")
}
