package io.github.nabin47.calwallpaper.wallpaper.staticmode

import android.app.job.JobParameters
import android.app.job.JobService
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Runs when the Calendar Provider reports a change (a content-trigger job from [StaticModeScheduler]) and
 * refreshes the static wallpaper. Content-trigger jobs fire once, so it schedules itself again when done.
 * Enabled only in static mode.
 */
class StaticRefreshJobService : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work =
            WallpaperGraph.get(this).processScope.launch {
                try {
                    StaticWallpaperUpdater(this@StaticRefreshJobService).refresh()
                } finally {
                    jobFinished(params, false)
                    StaticModeScheduler(this@StaticRefreshJobService).scheduleCalendarJob()
                }
            }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return false
    }
}
