package io.github.nabin47.calwallpaper.data.local

import io.github.nabin47.calwallpaper.core.Outcome
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Clock

/**
 * Write side of the local task list: quick-add, complete, delete. Read through [LocalTasksSource].
 * Suspending; Room moves the work off the caller's thread.
 */
class LocalTasks(
    private val dao: LocalTaskDao,
    private val clock: Clock = Clock.System,
) {
    /**
     * Adds a task. [title] is trimmed; blank titles and titles longer than [MAX_TITLE_LENGTH] are rejected.
     * [time] is ignored when [due] is `null`. Returns the new id.
     */
    suspend fun add(
        title: String,
        due: LocalDate? = null,
        time: LocalTime? = null,
    ): Outcome<Long> {
        val clean = title.trim()
        return when {
            clean.isEmpty() -> Outcome.Failure("Title is empty")
            clean.length > MAX_TITLE_LENGTH -> Outcome.Failure("Title is too long")
            else -> Outcome.Success(dao.insert(newEntity(clean, due, time)))
        }
    }

    private fun newEntity(
        title: String,
        due: LocalDate?,
        time: LocalTime?,
    ) = LocalTaskEntity(
        title = title,
        dueEpochDay = due?.toEpochDays(),
        dueMinuteOfDay = if (due != null) time?.let { it.hour * MINUTES_PER_HOUR + it.minute } else null,
        createdAtEpochMs = clock.now().toEpochMilliseconds(),
    )

    /** Marks task [id] done or not done. Returns false if no such task exists. */
    suspend fun setCompleted(
        id: Long,
        completed: Boolean,
    ): Boolean = dao.setCompleted(id, completed) > 0

    /** Deletes task [id]. Returns false if no such task exists. */
    suspend fun delete(id: Long): Boolean = dao.delete(id) > 0

    /** Limits. */
    companion object {
        /** Longest accepted title; keeps wallpaper layout and storage bounded. */
        const val MAX_TITLE_LENGTH: Int = 500

        private const val MINUTES_PER_HOUR = 60
    }
}
