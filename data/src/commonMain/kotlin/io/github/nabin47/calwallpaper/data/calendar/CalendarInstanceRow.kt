package io.github.nabin47.calwallpaper.data.calendar

/**
 * One row of `CalendarContract.Instances`: a single occurrence of a calendar event. Plain values so the
 * mapping to agenda items is pure and testable on the JVM.
 *
 * @property beginMillis occurrence start, epoch milliseconds. For all-day events this is UTC midnight.
 * @property endMillis occurrence end, exclusive. For all-day events this is UTC midnight after the last day.
 * @property title may be `null` or blank in the provider.
 * @property displayColor the event or calendar color as `0xAARRGGBB`, if the provider has one.
 */
data class CalendarInstanceRow(
    val eventId: Long,
    val beginMillis: Long,
    val endMillis: Long,
    val title: String?,
    val allDay: Boolean,
    val calendarId: Long,
    val displayColor: Int?,
) {
    /** Redacts [title]; rows must be safe to log (CLAUDE.md §7). */
    override fun toString(): String =
        "CalendarInstanceRow(eventId=$eventId, begin=$beginMillis, end=$endMillis, allDay=$allDay, " +
            "calendarId=$calendarId)"
}
