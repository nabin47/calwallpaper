package io.github.nabin47.calwallpaper.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Builds the [LocalTasksDatabase]. Uses the platform SQLite ([AndroidSQLiteDriver]) instead of the bundled
 * one, so the APK carries no native SQLite libraries (CLAUDE.md §5: keep the APK small).
 *
 * Multi-instance invalidation is on: the app process writes (quick-add) while the wallpaper process observes, and
 * each process has its own instance, so Room must tell the other process about changes (ADR 0007).
 *
 * Call once per process and share the instance.
 */
object LocalTasksDatabaseFactory {
    private const val FILE_NAME = "local_tasks.db"

    /** Opens (creating if needed) the database in [context]'s app storage. */
    fun create(context: Context): LocalTasksDatabase {
        val appContext = context.applicationContext
        return Room
            .databaseBuilder<LocalTasksDatabase>(
                context = appContext,
                name = appContext.getDatabasePath(FILE_NAME).absolutePath,
            ).setQueryCoroutineContext(Dispatchers.IO)
            .setDriver(AndroidSQLiteDriver())
            .enableMultiInstanceInvalidation()
            .build()
    }
}
