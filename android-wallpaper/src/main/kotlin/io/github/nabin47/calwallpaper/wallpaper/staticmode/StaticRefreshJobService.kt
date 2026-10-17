package io.github.nabin47.calwallpaper.wallpaper.staticmode

import android.app.job.JobParameters
import android.app.job.JobService
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs when the Calendar Provider reports a change (a content-trigger job from [StaticModeScheduler]) and
 * refreshes the static wallpaper and the widgets. Content-trigger jobs fire once, so it re-syncs the triggers
 * when done, which schedules it again while static mode or widgets still need it.
 */
class StaticRefreshJobService : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val graph = WallpaperGraph.get(this)
        work =
            graph.processScope.launch {
                try {
                    BackgroundRefresh.run(this@StaticRefreshJobService)
                } finally {
                    // Re-arms the job if static mode or widgets still need it, and switches everything off if not.
                    withContext(NonCancellable) {
                        StaticModeScheduler(this@StaticRefreshJobService).sync(graph.settings.settings.first())
                    }
                    jobFinished(params, false)
                }
            }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return false
    }
}
