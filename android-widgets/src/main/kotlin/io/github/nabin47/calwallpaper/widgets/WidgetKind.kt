package io.github.nabin47.calwallpaper.widgets

/**
 * Home-screen widgets the app will offer (Glance, Phase 5). [layoutKey] names the Scene layout preset
 * each widget renders, so widgets reuse the wallpaper's layout engine.
 */
enum class WidgetKind(
    val layoutKey: String,
) {
    /** Scrollable agenda list. */
    AGENDA("agenda"),

    /** Next three items, minimal. */
    NEXT_THREE("next3"),
}
