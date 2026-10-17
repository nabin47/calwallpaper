package io.github.nabin47.calwallpaper.data.calendar

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Maps a Calendar Provider instance to an [AgendaItem]. Pure.
 *
 * All-day events are stored by the provider as UTC midnights with an exclusive end, so their dates are read in
 * UTC, not in the device zone; otherwise they would shift a day west of Greenwich. The key includes the
 * occurrence start so each occurrence of a recurring event is distinct.
 */
fun CalendarInstanceRow.toAgendaItem(): AgendaItem {
    val end = maxOf(endMillis, beginMillis)
    val begin = Instant.fromEpochMilliseconds(beginMillis)
    val endInstant = Instant.fromEpochMilliseconds(end)
    return AgendaItem(
        key = ItemKey(SourceId.CALENDAR_PROVIDER, "$eventId@$beginMillis"),
        kind = ItemKind.EVENT,
        title = title.orEmpty(),
        time = if (allDay) allDayTime(begin, endInstant) else ItemTime.Timed(begin, endInstant),
        openTarget = OpenTarget.CalendarEvent(eventId = eventId, begin = begin, end = endInstant),
        groupId = calendarId.toString(),
        colorArgb = displayColor,
    )
}

private fun allDayTime(
    begin: Instant,
    endExclusive: Instant,
): ItemTime.AllDay {
    val startDate = begin.toLocalDateTime(TimeZone.UTC).date
    val endDateExclusive = endExclusive.toLocalDateTime(TimeZone.UTC).date
    val endInclusive = if (endDateExclusive > startDate) endDateExclusive.minus(1, DateTimeUnit.DAY) else startDate
    return ItemTime.AllDay(startDate, endInclusive)
}
