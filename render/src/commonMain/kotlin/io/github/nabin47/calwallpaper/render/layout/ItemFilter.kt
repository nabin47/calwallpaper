package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.core.time.overlaps
import io.github.nabin47.calwallpaper.domain.settings.ContentSettings
import kotlinx.datetime.TimeZone

/**
 * The items the user asked to see: enabled sources, selected calendars and lists, completed tasks only if
 * enabled, and only those touching [range]. Pure.
 *
 * Duplicates are shown once. The same event often arrives from several calendars, for example a holiday calendar
 * synced by more than one account. Items match when their title (ignoring case and surrounding spaces), kind,
 * time and completion match; the first one in list order wins. Items without a title (the Direct Boot cache with
 * privacy on) are never merged, because they cannot be told apart.
 */
internal fun List<AgendaItem>.visibleIn(
    range: DateRange,
    zone: TimeZone,
    content: ContentSettings,
): List<AgendaItem> =
    filter { item ->
        item.key.source in content.enabledSources &&
            item.isInSelectedGroup(content) &&
            (content.showCompleted || !item.isCompleted) &&
            item.time.overlaps(range, zone)
    }.distinctBy { it.duplicateKey() }

/** What makes two items look the same on screen; a blank title is unique to its item. */
private fun AgendaItem.duplicateKey(): Any {
    val title = title.trim().lowercase()
    return if (title.isEmpty()) key else DuplicateKey(title, kind, time, isCompleted)
}

private data class DuplicateKey(
    val title: String,
    val kind: ItemKind,
    val time: ItemTime,
    val completed: Boolean,
)

private fun AgendaItem.isInSelectedGroup(content: ContentSettings): Boolean {
    val group = groupId ?: return true
    val selection =
        when {
            key.source == SourceId.CALENDAR_PROVIDER -> content.selectedCalendarIds
            kind == ItemKind.TASK -> content.selectedTaskListIds
            else -> null
        }
    return selection == null || group in selection
}
