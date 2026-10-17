package io.github.nabin47.calwallpaper.wallpaper.staticmode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Refreshes the static wallpaper and the widgets after boot, at midnight, and when the time, time zone or
 * language changes. Enabled only while static mode or widgets are in use (see [StaticModeScheduler]). Not
 * exported: it receives system broadcasts and the app's own midnight alarm only.
 *
 * Work runs in the background with `goAsync`, bounded by [TIMEOUT] to stay inside the broadcast time limit.
 */
class StaticRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in HANDLED) return
        val pending = goAsync()
        val graph = WallpaperGraph.get(context)
        graph.processScope.launch {
            try {
                withTimeoutOrNull(TIMEOUT) {
                    // Re-arms the triggers: alarms and content-trigger jobs do not survive a reboot or a clock change.
                    StaticModeScheduler(context).sync(graph.settings.settings.first())
                    BackgroundRefresh.run(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** Actions this receiver handles. */
    companion object {
        /** The app's own alarm, shortly after local midnight. */
        const val ACTION_MIDNIGHT = "io.github.nabin47.calwallpaper.action.STATIC_MIDNIGHT"

        private val TIMEOUT = 9.seconds

        private val HANDLED =
            setOf(
                ACTION_MIDNIGHT,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
            )
    }
}
