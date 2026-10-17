package io.github.nabin47.calwallpaper.data.local

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.domain.AgendaSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [AgendaSource] for the local task list. Undated tasks appear on the first day of the requested range.
 * Emits again whenever the table changes; database errors become [Outcome.Failure].
 */
class LocalTasksSource(
    private val dao: LocalTaskDao,
) : AgendaSource {
    override val id: SourceId = SourceId.LOCAL_TASKS

    override fun observe(
        range: DateRange,
        zone: TimeZone,
    ): Flow<Outcome<List<AgendaItem>>> =
        dao
            .observeDueBetween(range.start.toEpochDays(), range.endInclusive.toEpochDays())
            .map<List<LocalTaskEntity>, Outcome<List<AgendaItem>>> { rows ->
                Outcome.Success(rows.map { it.toAgendaItem(undatedOn = range.start) })
            }.catch { emit(Outcome.Failure("Local tasks are unavailable")) }
}
