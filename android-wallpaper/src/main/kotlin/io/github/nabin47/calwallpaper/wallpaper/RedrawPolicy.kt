package io.github.nabin47.calwallpaper.wallpaper

/** Events that may require the wallpaper to redraw. There are no timers (CLAUDE.md §5). */
enum class RedrawReason {
    /** The wallpaper became visible. */
    BECAME_VISIBLE,

    /** Agenda data changed. */
    DATA_CHANGED,

    /** The user changed a setting. */
    SETTINGS_CHANGED,

    /** Date, time or time zone changed. */
    TIME_CHANGED,
}

/**
 * Decides whether to draw now or defer until the wallpaper is visible again, so invisible wallpapers do no work.
 * Pure function; call from the engine's thread.
 */
object RedrawPolicy {
    /** Returns true if a frame should be drawn now for [reason] given current [isVisible]. */
    fun shouldDrawNow(
        reason: RedrawReason,
        isVisible: Boolean,
    ): Boolean = isVisible || reason == RedrawReason.BECAME_VISIBLE
}
