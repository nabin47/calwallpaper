package io.github.nabin47.calwallpaper.todoist

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.nabin47.calwallpaper.data.todoist.SyncResult
import io.github.nabin47.calwallpaper.data.todoist.TodoistHttp
import io.github.nabin47.calwallpaper.data.todoist.TodoistToken
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import io.github.nabin47.calwallpaper.wallpaper.staticmode.BackgroundRefresh

/**
 * One Todoist sync, run by WorkManager (periodic or "sync now"; see [TodoistSyncScheduler]).
 *
 * - **Where it runs.** In the app's main process, the only process with network code. The wallpaper process
 *   reads the result from the shared cache.
 * - **After a sync.** When the visible data changed, the battery-saver image and the widgets redraw.
 * - **Failures.** Transient ones (network, server, rate limit) return [Result.retry], so WorkManager backs off
 *   exponentially. The others (rejected or unreadable token, unexpected response) need the user and fail
 *   without retrying. The settings screen shows them from the cache's sync status.
 *
 * Logs nothing.
 */
class TodoistSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = WallpaperGraph.get(applicationContext)
        val tokens = TodoistToken.store(applicationContext)
        val result = TodoistHttp.sync(tokens, graph.todoistCache)
        return when (result) {
            is SyncResult.Synced -> {
                if (result.changed) BackgroundRefresh.run(applicationContext)
                Result.success()
            }

            SyncResult.NotConnected -> {
                Result.success()
            }

            is SyncResult.Failed -> {
                if (result.retryable && runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        }
    }

    private companion object {
        /** After this many retries the run gives up; the periodic schedule tries again later. */
        const val MAX_RETRIES = 5
    }
}
