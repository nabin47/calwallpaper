package io.github.nabin47.calwallpaper.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Helpers for asking the system to use this app's live wallpaper. */
object LiveWallpaper {
    /** This app's live wallpaper component. */
    fun component(context: Context): ComponentName = ComponentName(context, AgendaWallpaperService::class.java)

    /**
     * Opens the system preview of this live wallpaper. The system screen, not the app, asks whether to use it for
     * the home screen, the lock screen or both: Android lets no app choose that for a live wallpaper.
     */
    fun previewIntent(context: Context): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component(context))

    /** The system's list of live wallpapers, a fallback when the direct preview is unavailable. */
    fun chooserIntent(): Intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)

    /** True if this app's live wallpaper is currently the home wallpaper. Best effort: false if unknown. */
    fun isActive(context: Context): Boolean =
        runCatching {
            WallpaperManager.getInstance(context).wallpaperInfo?.component == component(context)
        }.getOrDefault(false)
}
