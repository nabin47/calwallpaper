package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.core.time.overlaps
import io.github.nabin47.calwallpaper.domain.settings.ContentSettings
import kotlinx.datetime.TimeZone

/**
 * The items the user asked to see: enabled sources, selected calendars and lists, completed tasks only if
 * enabled, and only those touching [range]. Pure.
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
    }

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
