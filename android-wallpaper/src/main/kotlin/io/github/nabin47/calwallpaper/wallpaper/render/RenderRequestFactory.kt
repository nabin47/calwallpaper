package io.github.nabin47.calwallpaper.wallpaper.render

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.render.ScreenSpec
import io.github.nabin47.calwallpaper.render.resolveFirstDayOfWeek
import io.github.nabin47.calwallpaper.render.resolveUse24Hour
import io.github.nabin47.calwallpaper.render.theme.Palette
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import java.util.Calendar
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Builds [RenderRequest]s with the device's choices resolved: locale week start, system 12/24-hour setting,
 * system font scale, localized strings and the Material You palette. One pipeline for the live wallpaper, the
 * static image and the settings preview (CLAUDE.md §4), so all three look the same.
 *
 * Reads the current configuration on every call, so locale and font-size changes apply at the next layout.
 * Call on any thread.
 */
class RenderRequestFactory(
    context: Context,
    private val clock: Clock = Clock.System,
) {
    private val context = context.applicationContext
    private val strings = AndroidRenderStrings(this.context)

    /**
     * A request for [items] laid out on a [widthPx] × [heightPx] surface. [isLocked] drives privacy mode;
     * [now] defaults to the current time.
     */
    fun create(
        items: List<AgendaItem>,
        settings: Settings,
        widthPx: Int,
        heightPx: Int,
        isLocked: Boolean,
        now: Instant = clock.now(),
    ): RenderRequest {
        val configuration = context.resources.configuration
        return RenderRequest(
            items = items,
            settings = settings,
            screen =
                ScreenSpec(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    density = context.resources.displayMetrics.density,
                    fontScale = configuration.fontScale,
                ),
            now = now,
            zone = TimeZone.currentSystemDefault(),
            isLocked = isLocked,
            firstDayOfWeek = settings.time.weekStart.resolveFirstDayOfWeek(localeFirstDayOfWeek()),
            use24HourClock = settings.time.clockFormat.resolveUse24Hour(DateFormat.is24HourFormat(context)),
            strings = strings,
            dynamicPalette = if (settings.appearance.useDynamicColor) dynamicPalette() else null,
        )
    }

    /** The locale's first day of the week (Monday in most of Europe, Sunday in the US, Saturday in some regions). */
    fun localeFirstDayOfWeek(): DayOfWeek {
        val locale = context.resources.configuration.locales[0]
        // java.util.Calendar numbers days Sunday = 1 ... Saturday = 7.
        val sundayFirst = Calendar.getInstance(locale).firstDayOfWeek
        return DayOfWeek.entries[(sundayFirst + DAYS_IN_WEEK - 2) % DAYS_IN_WEEK]
    }

    /** Material You colors on Android 12+, otherwise `null` (the preset palette is used). */
    fun dynamicPalette(): Palette? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SystemPalette.read(context) else null

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
