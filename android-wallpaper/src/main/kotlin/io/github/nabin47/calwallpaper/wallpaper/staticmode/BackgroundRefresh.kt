package io.github.nabin47.calwallpaper.wallpaper.staticmode

import android.content.Context
import io.github.nabin47.calwallpaper.wallpaper.system.InstalledWidgets

/** What a background trigger (calendar change, midnight, clock change) brings up to date. */
object BackgroundRefresh {
    /**
     * Re-renders the static wallpaper (a no-op unless battery mode is static) and asks placed widgets to redraw.
     * Call from a background coroutine.
     */
    suspend fun run(context: Context) {
        StaticWallpaperUpdater(context).refresh()
        InstalledWidgets(context).requestUpdate()
    }
}
