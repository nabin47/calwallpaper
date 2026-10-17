package io.github.nabin47.calwallpaper.wallpaper.render

import android.content.Context
import android.text.format.DateFormat
import io.github.nabin47.calwallpaper.render.format.RenderStrings
import io.github.nabin47.calwallpaper.wallpaper.R
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * [RenderStrings] from `strings.xml` and the current locale, so wallpaper words follow the device language
 * (CLAUDE.md §10). Reads resources on every call, so a locale change applies at the next layout without
 * rebuilding anything; layout reads only a few strings per frame.
 */
class AndroidRenderStrings(
    context: Context,
) : RenderStrings {
    private val context = context.applicationContext
    private val resources get() = context.resources
    private val locale: Locale get() = resources.configuration.locales[0]

    override val today: String get() = resources.getString(R.string.render_today)
    override val tomorrow: String get() = resources.getString(R.string.render_tomorrow)
    override val allDay: String get() = resources.getString(R.string.render_all_day)
    override val continues: String get() = resources.getString(R.string.render_continues)
    override val hiddenTitle: String get() = resources.getString(R.string.render_hidden_title)
    override val emptyAgenda: String get() = resources.getString(R.string.render_empty_agenda)
    override val emptyWeek: String get() = resources.getString(R.string.render_empty_week)
    override val emptyMonth: String get() = resources.getString(R.string.render_empty_month)
    override val allClear: String get() = resources.getString(R.string.render_all_clear)
    override val nothingUpcoming: String get() = resources.getString(R.string.render_nothing_upcoming)
    override val am: String get() = resources.getString(R.string.render_am)
    override val pm: String get() = resources.getString(R.string.render_pm)

    override fun more(count: Int): String = resources.getQuantityString(R.plurals.render_more, count, count)

    override fun itemCount(count: Int): String = resources.getQuantityString(R.plurals.render_item_count, count, count)

    override fun weekdayShort(day: DayOfWeek): String = day.toJava().getDisplayName(TextStyle.SHORT, locale)

    override fun weekdayNarrow(day: DayOfWeek): String = day.toJava().getDisplayName(TextStyle.NARROW, locale)

    override fun monthShort(month: Month): String = month.toJava().getDisplayName(TextStyle.SHORT, locale)

    override fun monthLong(month: Month): String = month.toJava().getDisplayName(TextStyle.FULL_STANDALONE, locale)

    override fun dateLabel(date: LocalDate): String = format(date, DATE_SKELETON)

    override fun monthTitle(
        month: Month,
        year: Int,
    ): String = format(LocalDate(year, month, 1), MONTH_SKELETON)

    /** Formats with the locale's preferred order for [skeleton] (e.g. "Sat, Oct 18" or "Sa., 18. Okt."). */
    private fun format(
        date: LocalDate,
        skeleton: String,
    ): String {
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return DateTimeFormatter.ofPattern(pattern, locale).format(date.toJavaLocalDate())
    }

    private fun DayOfWeek.toJava(): java.time.DayOfWeek = java.time.DayOfWeek.of(ordinal + 1)

    private fun Month.toJava(): java.time.Month = java.time.Month.of(ordinal + 1)

    private companion object {
        const val DATE_SKELETON = "EEEMMMd"
        const val MONTH_SKELETON = "MMMMyyyy"
    }
}
