package io.github.nabin47.calwallpaper.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The app's screens, as Navigation 3 back-stack keys (ADR 0009). Serializable, so the back stack survives
 * configuration changes and process death. The home screen is always at the bottom of the stack.
 */
@Serializable
sealed interface Route : NavKey {
    /** Preview, wallpaper status, quick style picks and the list of settings pages. */
    @Serializable
    data object Home : Route

    /** Colors, font, text size, opacity, corners, spacing. */
    @Serializable
    data object Appearance : Route

    /** Layout preset and home-screen placement. */
    @Serializable
    data object Layout : Route

    /** Lock-screen placement and privacy mode, always previewed as the lock screen. */
    @Serializable
    data object LockScreen : Route

    /** Calendars, local tasks, date range and completed tasks. */
    @Serializable
    data object Sources : Route

    /** Todoist connection. */
    @Serializable
    data object Todoist : Route

    /** Week start and 12/24-hour time. */
    @Serializable
    data object DateTime : Route

    /** Live wallpaper or battery-saver image. */
    @Serializable
    data object WallpaperMode : Route

    /** The home-screen widgets, the Quick Settings tile and the shortcut. */
    @Serializable
    data object Widgets : Route

    /** Export, import and reset the settings. */
    @Serializable
    data object Backup : Route

    /** Answers to common questions, performance numbers, version and links. */
    @Serializable
    data object About : Route
}
