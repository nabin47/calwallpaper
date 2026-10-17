package io.github.nabin47.calwallpaper.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Database access for [LocalTaskEntity]. Room runs the queries on its own query context. */
@Dao
interface LocalTaskDao {
    /**
     * Tasks due between [startEpochDay] and [endEpochDay] (inclusive), plus all undated tasks, re-emitted on
     * every change to the table.
     */
    @Query(
        "SELECT * FROM local_tasks " +
            "WHERE due_epoch_day IS NULL OR due_epoch_day BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY due_epoch_day, due_minute_of_day, id",
    )
    fun observeDueBetween(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<LocalTaskEntity>>

    /** Inserts [task] and returns its new id. */
    @Insert
    suspend fun insert(task: LocalTaskEntity): Long

    /** Marks a task done or not done; returns the number of rows changed (0 if [id] is unknown). */
    @Query("UPDATE local_tasks SET completed = :completed WHERE id = :id")
    suspend fun setCompleted(
        id: Long,
        completed: Boolean,
    ): Int

    /** Deletes a task; returns the number of rows removed. */
    @Query("DELETE FROM local_tasks WHERE id = :id")
    suspend fun delete(id: Long): Int
}
