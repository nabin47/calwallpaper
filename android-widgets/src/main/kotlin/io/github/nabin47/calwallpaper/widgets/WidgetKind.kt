package io.github.nabin47.calwallpaper.widgets

import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset

/**
 * The home-screen widgets the app offers. Each renders one Scene layout [preset] with the user's appearance and
 * content settings, so widgets reuse the wallpaper's layout engine (CLAUDE.md §4). [label] is the string
 * resource that names it, for accessibility.
 */
enum class WidgetKind(
    val preset: LayoutPreset,
    val label: Int,
) {
    /** A small agenda: day headers and items. */
    AGENDA(LayoutPreset.AGENDA, R.string.widget_agenda_name),

    /** The current week as a strip of days with items beneath. */
    WEEK(LayoutPreset.WEEK_STRIP, R.string.widget_week_name),
}
