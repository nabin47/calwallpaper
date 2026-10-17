package io.github.nabin47.calwallpaper.data.todoist

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.core.time.overlaps
import io.github.nabin47.calwallpaper.domain.AgendaSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Todoist tasks from the offline [cache] (CLAUDE.md §4). It never touches the network: the sync worker fills the
 * cache, so the wallpaper process needs no network code and shows the last known tasks when offline.
 * Emits again whenever a sync changes the cache, in any process.
 */
class TodoistSource(
    private val cache: Flow<TodoistCache>,
    private val clock: Clock = Clock.System,
) : AgendaSource {
    override val id: SourceId = SourceId.TODOIST

    override fun observe(
        range: DateRange,
        zone: TimeZone,
    ): Flow<Outcome<List<AgendaItem>>> =
        cache
            .map { it.copy(status = SyncStatus()) }
            .distinctUntilChanged()
            .map { snapshot ->
                // Today, not range.start: overdue tasks belong on today even when the range starts earlier
                // (month grid). A new day restarts observation (FeedKey), so this stays current.
                val today = clock.now().toLocalDateTime(zone).date
                val items = snapshot.toAgendaItems(today = today, zone = zone)
                Outcome.Success(items.filter { it.time.overlaps(range, zone) })
            }
}
