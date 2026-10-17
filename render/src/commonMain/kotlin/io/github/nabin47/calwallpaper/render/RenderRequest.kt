package io.github.nabin47.calwallpaper.render

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.domain.settings.ClockFormat
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.WeekStart
import io.github.nabin47.calwallpaper.render.format.EnglishRenderStrings
import io.github.nabin47.calwallpaper.render.format.RenderStrings
import io.github.nabin47.calwallpaper.render.theme.Palette
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** The surface to lay out on. [density] is pixels per dp; [fontScale] is the system font size setting. */
data class ScreenSpec(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val fontScale: Float = 1f,
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "Screen size must be positive" }
        require(density > 0f && fontScale > 0f) { "density and fontScale must be positive" }
    }
}

/** Areas covered by system UI (status bar, navigation bar, display cutouts), in pixels. */
data class Insets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
) {
    /** No insets. */
    companion object {
        /** No system UI overlap. */
        val NONE: Insets = Insets()
    }
}

/**
 * Everything the layout engine needs for one frame. Platform-dependent choices are resolved by the caller:
 * the locale's first day of week, the system 12/24-hour setting, localized strings and the dynamic palette.
 *
 * @property items candidates; the engine filters by settings and by [LayoutEngine.requiredRange].
 * @property isLocked whether the device is locked, for [Settings]' privacy mode.
 * @property now current time; "today" is its date in [zone].
 * @property firstDayOfWeek resolved week start (see [resolveFirstDayOfWeek]).
 * @property use24HourClock resolved clock format (see [resolveUse24Hour]).
 * @property dynamicPalette Material You colors, or `null` where unsupported.
 */
data class RenderRequest(
    val items: List<AgendaItem>,
    val settings: Settings,
    val screen: ScreenSpec,
    val now: Instant,
    val zone: TimeZone,
    val insets: Insets = Insets.NONE,
    val isLocked: Boolean = false,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val use24HourClock: Boolean = true,
    val strings: RenderStrings = EnglishRenderStrings,
    val dynamicPalette: Palette? = null,
)

/** [WeekStart] with [WeekStart.LOCALE] replaced by the platform's [localeDefault]. */
fun WeekStart.resolveFirstDayOfWeek(localeDefault: DayOfWeek): DayOfWeek =
    when (this) {
        WeekStart.LOCALE -> localeDefault
        WeekStart.MONDAY -> DayOfWeek.MONDAY
        WeekStart.SATURDAY -> DayOfWeek.SATURDAY
        WeekStart.SUNDAY -> DayOfWeek.SUNDAY
    }

/** [ClockFormat] with [ClockFormat.SYSTEM] replaced by the platform's [system24Hour] setting. */
fun ClockFormat.resolveUse24Hour(system24Hour: Boolean): Boolean =
    when (this) {
        ClockFormat.SYSTEM -> system24Hour
        ClockFormat.H12 -> false
        ClockFormat.H24 -> true
    }
