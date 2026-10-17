package io.github.nabin47.calwallpaper.render

import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.layout.LayoutContext
import io.github.nabin47.calwallpaper.render.layout.layoutAgenda
import io.github.nabin47.calwallpaper.render.layout.layoutMonthGrid
import io.github.nabin47.calwallpaper.render.layout.layoutNextThree
import io.github.nabin47.calwallpaper.render.layout.layoutWeekStrip
import io.github.nabin47.calwallpaper.render.layout.monthContaining
import io.github.nabin47.calwallpaper.render.layout.visibleIn
import io.github.nabin47.calwallpaper.render.layout.weekContaining
import io.github.nabin47.calwallpaper.render.text.TextMeasurer
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/**
 * Turns agenda items and settings into a [Scene] for one screen (CLAUDE.md §4: one Scene, many renderers).
 *
 * The layout honors the safe zone, system insets, side margin, vertical position, text scale (user and system),
 * spacing density, palette and opacities, and privacy mode. Content that does not fit is replaced by a "+N more"
 * line; nothing is drawn outside the safe area.
 *
 * Pure and deterministic for a given [TextMeasurer]: no I/O, no clock reads (time comes from the request).
 * Call it off the main thread when data or settings change, not on every frame.
 */
class LayoutEngine(
    private val measurer: TextMeasurer,
) {
    /** Lays out [request] with its preset. */
    fun layout(request: RenderRequest): Scene {
        val settings = request.settings.normalized()
        val context = LayoutContext(request, settings, measurer)
        val range = requiredRange(settings, context.today, request.firstDayOfWeek)
        val items = request.items.visibleIn(range, request.zone, settings.content)
        when (settings.layout.preset) {
            LayoutPreset.AGENDA -> context.layoutAgenda(items, range)
            LayoutPreset.WEEK_STRIP -> context.layoutWeekStrip(items, range)
            LayoutPreset.MONTH_GRID -> context.layoutMonthGrid(items, range, request.firstDayOfWeek)
            LayoutPreset.NEXT_THREE -> context.layoutNextThree(items)
        }
        return context.build()
    }

    /** Date-range helpers. */
    companion object {
        /**
         * The days whose items the preset in [settings] shows, so callers can ask sources for exactly that
         * range: the next `daysAhead` days for Agenda and Next-3, the current week for Week strip, and the
         * current month for Month grid.
         */
        fun requiredRange(
            settings: Settings,
            today: LocalDate,
            firstDayOfWeek: DayOfWeek,
        ): DateRange =
            when (settings.layout.preset) {
                LayoutPreset.AGENDA, LayoutPreset.NEXT_THREE -> {
                    DateRange.startingAt(
                        today,
                        settings.content.normalized().daysAhead,
                    )
                }

                LayoutPreset.WEEK_STRIP -> {
                    weekContaining(today, firstDayOfWeek)
                }

                LayoutPreset.MONTH_GRID -> {
                    monthContaining(today)
                }
            }

        /** Convenience: [requiredRange] for a [RenderRequest]. */
        fun requiredRange(request: RenderRequest): DateRange =
            requiredRange(request.settings, request.now.toLocalDateTime(request.zone).date, request.firstDayOfWeek)
    }
}
