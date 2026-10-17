package io.github.nabin47.calwallpaper

import android.content.Context
import io.github.nabin47.calwallpaper.data.calendar.DeviceCalendars
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import io.github.nabin47.calwallpaper.wallpaper.staticmode.StaticModeScheduler
import io.github.nabin47.calwallpaper.wallpaper.staticmode.StaticWallpaperUpdater
import io.github.nabin47.calwallpaper.wallpaper.system.InstalledWidgets
import java.io.File

/**
 * What the settings UI needs, built by hand (ADR 0001). Shares the process-wide [WallpaperGraph], so the
 * settings DataStore is opened once per process. Create one per activity; everything inside is cheap or lazy.
 */
class AppServices(
    context: Context,
) {
    private val appContext = context.applicationContext

    /** Settings, sources, request factory and diagnostics shared with the wallpaper code. */
    val graph: WallpaperGraph = WallpaperGraph.get(appContext)

    /** The device's calendars, for per-calendar selection. */
    val calendars: DeviceCalendars by lazy { DeviceCalendars(appContext) }

    /** Applies the static (battery mode) wallpaper. */
    val staticUpdater: StaticWallpaperUpdater by lazy { StaticWallpaperUpdater(appContext) }

    /** Turns the background refresh triggers (static mode, widgets) on or off. */
    val staticScheduler: StaticModeScheduler by lazy { StaticModeScheduler(appContext) }

    /** The home-screen widgets the user placed, to redraw them after a settings change. */
    val widgets: InstalledWidgets by lazy { InstalledWidgets(appContext) }

    /** Whether onboarding was completed. */
    val onboarding: OnboardingStore = OnboardingStore(File(appContext.filesDir, "onboarding_done"))
}

/**
 * Remembers that onboarding finished, as the existence of an empty marker file. A file avoids a second
 * settings store and stays out of exported settings; it is not needed before the first unlock.
 */
class OnboardingStore(
    private val marker: File,
) {
    /** True once [markDone] was called on this installation. */
    fun isDone(): Boolean = marker.exists()

    /** Records completion. Best effort: if it fails, onboarding simply shows again next time. */
    fun markDone() {
        runCatching { marker.createNewFile() }
    }
}
