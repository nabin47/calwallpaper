package io.github.nabin47.calwallpaper.todoist

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules Todoist syncs with WorkManager (CLAUDE.md §5): every [PERIOD_MINUTES] minutes on any network while
 * Todoist is enabled and connected, plus an immediate run when the user connects or taps "Sync now". Failed runs
 * back off exponentially from [BACKOFF_SECONDS]. Idempotent; call from any thread.
 */
class TodoistSyncScheduler(
    context: Context,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Starts periodic sync when [active], stops it otherwise. Keeps the existing schedule when nothing changed. */
    fun update(active: Boolean) {
        if (active) {
            val request =
                PeriodicWorkRequestBuilder<TodoistSyncWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(online)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                    .build()
            workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            workManager.cancelUniqueWork(PERIODIC)
            workManager.cancelUniqueWork(NOW)
        }
    }

    /** Syncs as soon as there is a network; a newer request replaces one that has not started yet. */
    fun syncNow() {
        val request =
            OneTimeWorkRequestBuilder<TodoistSyncWorker>()
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build()
        workManager.enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val PERIODIC = "todoist-sync"
        const val NOW = "todoist-sync-now"

        /** Twice the WorkManager minimum: fresh enough for a to-do list, light on battery. */
        const val PERIOD_MINUTES = 30L

        const val BACKOFF_SECONDS = 60L
    }
}
