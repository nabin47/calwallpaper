package io.github.nabin47.calwallpaper.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

/**
 * Room database for the local task list. Lives in credential-protected storage, so it is unavailable before
 * the first unlock (Direct Boot); the wallpaper renders from its own cache then (CLAUDE.md §2).
 *
 * Schema changes need a version bump, an exported schema under `data/schemas/` and a migration.
 */
@Database(entities = [LocalTaskEntity::class], version = 1, exportSchema = true)
@ConstructedBy(LocalTasksDatabaseConstructor::class)
abstract class LocalTasksDatabase : RoomDatabase() {
    /** Task queries. */
    abstract fun taskDao(): LocalTaskDao
}

/** Implemented by the Room compiler (KSP) for each target; see [LocalTasksDatabase]. */
expect object LocalTasksDatabaseConstructor : RoomDatabaseConstructor<LocalTasksDatabase> {
    override fun initialize(): LocalTasksDatabase
}
