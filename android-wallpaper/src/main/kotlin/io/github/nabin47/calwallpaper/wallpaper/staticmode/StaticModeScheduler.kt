package io.github.nabin47.calwallpaper.wallpaper.staticmode

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import io.github.nabin47.calwallpaper.wallpaper.system.InstalledWidgets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Keeps the background refresh triggers in line with what needs them: the static (battery mode) image and the
 * home-screen widgets. While either is in use it enables the refresh components, watches the Calendar Provider
 * with a content-trigger job (no polling) and sets an inexact, non-waking alarm for shortly after midnight.
 * Otherwise it turns all of that off, so a live-wallpaper user without widgets gets no wakeups at all.
 * The classes keep their static-mode names, under which Android stores their enabled state (ADR 0005).
 *
 * Content-trigger jobs cannot survive a reboot, so [StaticRefreshReceiver] calls [sync] after boot
 * (`RECEIVE_BOOT_COMPLETED`, ADR 0004). Idempotent; call from any thread.
 */
class StaticModeScheduler(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val widgets = InstalledWidgets(appContext)

    /** Enables or disables the refresh triggers for [settings] and the widgets currently placed. */
    fun sync(settings: Settings) {
        if (settings.batteryMode == BatteryMode.STATIC || widgets.any()) {
            setComponentsEnabled(true)
            scheduleCalendarJob()
            scheduleMidnightAlarm()
        } else {
            appContext.getSystemService(JobScheduler::class.java)?.cancel(CALENDAR_JOB_ID)
            appContext.getSystemService(AlarmManager::class.java)?.cancel(midnightIntent())
            setComponentsEnabled(false)
        }
    }

    /**
     * Re-syncs after a widget was added or the last one removed, reading the stored settings in the background.
     * For widget providers' `onEnabled` and `onDisabled`.
     */
    fun onWidgetsChanged() {
        val graph = WallpaperGraph.get(appContext)
        graph.processScope.launch { sync(graph.settings.settings.first()) }
    }

    /** (Re)schedules the job that runs after the Calendar Provider reports a change. */
    fun scheduleCalendarJob() {
        val job =
            JobInfo
                .Builder(CALENDAR_JOB_ID, ComponentName(appContext, StaticRefreshJobService::class.java))
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        CalendarContract.CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
                    ),
                ).setTriggerContentUpdateDelay(UPDATE_DELAY_MS)
                .setTriggerContentMaxDelay(MAX_DELAY_MS)
                .build()
        appContext.getSystemService(JobScheduler::class.java)?.schedule(job)
    }

    /** (Re)arms the inexact alarm for the next local midnight. Needs no exact-alarm permission. */
    fun scheduleMidnightAlarm() {
        val at = StaticPlan.nextMidnightRefresh(Clock.System.now(), TimeZone.currentSystemDefault())
        appContext
            .getSystemService(AlarmManager::class.java)
            ?.set(AlarmManager.RTC, at.toEpochMilliseconds(), midnightIntent())
    }

    private fun midnightIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(appContext, StaticRefreshReceiver::class.java).setAction(StaticRefreshReceiver.ACTION_MIDNIGHT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun setComponentsEnabled(enabled: Boolean) {
        val state =
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        listOf(StaticRefreshReceiver::class.java, StaticRefreshJobService::class.java).forEach { component ->
            appContext.packageManager.setComponentEnabledSetting(
                ComponentName(appContext, component),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    private companion object {
        const val CALENDAR_JOB_ID = 4701
        const val UPDATE_DELAY_MS = 5_000L
        const val MAX_DELAY_MS = 60_000L
    }
}
