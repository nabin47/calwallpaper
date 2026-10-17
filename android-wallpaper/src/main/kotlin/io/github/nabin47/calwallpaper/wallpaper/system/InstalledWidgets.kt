package io.github.nabin47.calwallpaper.wallpaper.system

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * This app's home-screen widgets that the user has placed, found without naming their classes: this module must
 * not depend on the widget or app modules (CLAUDE.md §4).
 *
 * The background triggers (calendar changes, midnight, time and time-zone changes; see
 * [io.github.nabin47.calwallpaper.wallpaper.staticmode.StaticModeScheduler]) use it to keep widgets current.
 * Call from any thread.
 */
class InstalledWidgets(
    context: Context,
) {
    private val appContext = context.applicationContext

    /** True if at least one of this app's widgets is on a home screen. */
    fun any(): Boolean = placed().isNotEmpty()

    /**
     * Asks every placed widget to redraw, with the same explicit `APPWIDGET_UPDATE` broadcast that the system
     * sends. Each provider re-renders from the current agenda and settings.
     */
    fun requestUpdate() {
        placed().forEach { (provider, ids) ->
            appContext.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }
    }

    /** Providers of this app with the ids of their placed widgets; providers without widgets are left out. */
    private fun placed(): List<Pair<ComponentName, IntArray>> {
        val manager = AppWidgetManager.getInstance(appContext) ?: return emptyList()
        return manager
            .getInstalledProvidersForPackage(appContext.packageName, null)
            .map { it.provider to manager.getAppWidgetIds(it.provider) }
            .filter { (_, ids) -> ids.isNotEmpty() }
    }
}
