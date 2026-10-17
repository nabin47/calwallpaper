package io.github.nabin47.calwallpaper.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import io.github.nabin47.calwallpaper.core.AgendaItem
import kotlinx.datetime.LocalDate

/**
 * A task in the app's minimal local list (CLAUDE.md §11: display, plus quick-add; no full editor).
 *
 * @property dueEpochDay due date as days since 1970-01-01; `null` for undated tasks.
 * @property dueMinuteOfDay optional due time as minutes after local midnight; ignored without a date.
 */
@Entity(tableName = "local_tasks", indices = [Index("due_epoch_day")])
data class LocalTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "due_epoch_day") val dueEpochDay: Long?,
    @ColumnInfo(name = "due_minute_of_day") val dueMinuteOfDay: Int?,
    val completed: Boolean = false,
    @ColumnInfo(name = "created_at_epoch_ms") val createdAtEpochMs: Long,
) {
    /** Redacts [title]; entities must be safe to log (CLAUDE.md §7). */
    override fun toString(): String = "LocalTaskEntity(id=$id, due=$dueEpochDay, completed=$completed)"
}

/** Maps to an agenda item; undated tasks are placed on [undatedOn]. See [localTaskToAgendaItem]. */
fun LocalTaskEntity.toAgendaItem(undatedOn: LocalDate): AgendaItem =
    localTaskToAgendaItem(id, title, dueEpochDay, dueMinuteOfDay, completed, undatedOn)
