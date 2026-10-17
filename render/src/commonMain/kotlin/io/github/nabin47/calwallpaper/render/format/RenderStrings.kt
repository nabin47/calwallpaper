package io.github.nabin47.calwallpaper.render.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

/**
 * Every user-visible word the layout engine draws. Platforms supply localized strings (Android: from
 * `strings.xml`); [EnglishRenderStrings] is the built-in default used by tests and previews.
 */
interface RenderStrings {
    /** Label for the current day. */
    val today: String

    /** Label for the next day. */
    val tomorrow: String

    /** Time label for all-day events. */
    val allDay: String

    /** Time label for a timed event that started on an earlier day. */
    val continues: String

    /** Shown instead of a title while privacy mode hides titles. */
    val hiddenTitle: String

    /** Agenda empty state. */
    val emptyAgenda: String

    /** Week strip empty state. */
    val emptyWeek: String

    /** Month grid empty state. */
    val emptyMonth: String

    /** Next-3 empty state headline. */
    val allClear: String

    /** Next-3 empty state detail. */
    val nothingUpcoming: String

    /** Morning suffix for 12-hour times. */
    val am: String

    /** Afternoon suffix for 12-hour times. */
    val pm: String

    /** "+3 more" style label for items that did not fit. */
    fun more(count: Int): String

    /** "3 items" style count, used in privacy mode. */
    fun itemCount(count: Int): String

    /** Short weekday name, e.g. "Mon". */
    fun weekdayShort(day: DayOfWeek): String

    /** One- or two-letter weekday name for narrow columns, e.g. "M". */
    fun weekdayNarrow(day: DayOfWeek): String

    /** Short month name, e.g. "Oct". */
    fun monthShort(month: Month): String

    /** Full month name, e.g. "October". */
    fun monthLong(month: Month): String

    /** Day label for headers, e.g. "Sat, Oct 18". */
    fun dateLabel(date: LocalDate): String = "${weekdayShort(date.dayOfWeek)}, ${monthShort(date.month)} ${date.day}"

    /** Month grid title, e.g. "October 2026". */
    fun monthTitle(
        month: Month,
        year: Int,
    ): String = "${monthLong(month)} $year"
}

/** US-English strings. */
object EnglishRenderStrings : RenderStrings {
    override val today: String = "Today"
    override val tomorrow: String = "Tomorrow"
    override val allDay: String = "All day"
    override val continues: String = "Cont."
    override val hiddenTitle: String = "Busy"
    override val emptyAgenda: String = "Nothing scheduled"
    override val emptyWeek: String = "Nothing this week"
    override val emptyMonth: String = "Nothing this month"
    override val allClear: String = "All clear"
    override val nothingUpcoming: String = "Nothing coming up"
    override val am: String = "AM"
    override val pm: String = "PM"

    override fun more(count: Int): String = "+$count more"

    override fun itemCount(count: Int): String = if (count == 1) "1 item" else "$count items"

    override fun weekdayShort(day: DayOfWeek): String =
        day.name
            .take(SHORT)
            .lowercase()
            .replaceFirstChar { it.uppercase() }

    override fun weekdayNarrow(day: DayOfWeek): String = day.name.take(1)

    override fun monthShort(month: Month): String = monthLong(month).take(SHORT)

    override fun monthLong(month: Month): String = month.name.lowercase().replaceFirstChar { it.uppercase() }

    private const val SHORT = 3
}
