package io.github.nabin47.calwallpaper.domain

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone

/**
 * A provider of agenda items (Calendar Provider, local tasks, Todoist, ...). Adding a source means adding an
 * implementation; render and wallpaper code only see [AgendaItem]s (CLAUDE.md §4).
 *
 * Implementations are read-only views: editing happens in the source app.
 */
interface AgendaSource {
    /** Which source this is; matches [AgendaItem.key]'s source for every emitted item. */
    val id: SourceId

    /**
     * Emits every item overlapping [range] (days in [zone]), then a new list whenever the underlying data
     * changes. The flow is cold; collection starts observation and cancellation stops it.
     *
     * Expected problems (missing permission, unreachable service) are emitted as [Outcome.Failure] with a
     * non-sensitive reason instead of being thrown. Completed items are included; filtering by settings is
     * the caller's job. Implementations do their own I/O off the main thread.
     */
    fun observe(
        range: DateRange,
        zone: TimeZone,
    ): Flow<Outcome<List<AgendaItem>>>
}
