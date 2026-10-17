package io.github.nabin47.calwallpaper.wallpaper.feed

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.getOrNull
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.domain.AgendaSource
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.LayoutEngine
import io.github.nabin47.calwallpaper.render.resolveFirstDayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/**
 * What the wallpaper must observe. When it changes (new day, other preset or sources, first unlock, permission
 * granted) the old observation is cancelled and a new one starts; otherwise sources keep their observers.
 *
 * @property calendarPermission part of the key because the calendar source ends its flow when the permission
 *   is missing, so granting it later must restart observation.
 */
data class FeedKey(
    val range: DateRange,
    val zone: TimeZone,
    val sources: Set<SourceId>,
    val unlocked: Boolean,
    val calendarPermission: Boolean,
) {
    /** Construction from settings. */
    companion object {
        /** The key for [settings] on [today]. [localeFirstDayOfWeek] resolves "follow locale" week starts. Pure. */
        fun of(
            settings: Settings,
            today: LocalDate,
            zone: TimeZone,
            localeFirstDayOfWeek: DayOfWeek,
            unlocked: Boolean,
            calendarPermission: Boolean,
        ): FeedKey {
            val firstDay = settings.time.weekStart.resolveFirstDayOfWeek(localeFirstDayOfWeek)
            return FeedKey(
                range = LayoutEngine.requiredRange(settings, today, firstDay),
                zone = zone,
                sources = settings.content.enabledSources,
                unlocked = unlocked,
                calendarPermission = calendarPermission,
            )
        }
    }
}

/**
 * Merges the enabled [AgendaSource]s into one list. A source that fails (no permission, unavailable) adds no
 * items instead of hiding the others. Filtering by calendar, completion and range happens later in the layout
 * engine, so one observation serves every setting except those in [FeedKey].
 *
 * @param sourceFor the adapter for a source id, or `null` if it is not available in this build or process.
 */
class AgendaFeed(
    private val sourceFor: (SourceId) -> AgendaSource?,
) {
    /** Items for [key], re-emitted whenever any observed source changes. */
    fun observe(key: FeedKey): Flow<List<AgendaItem>> {
        val sources = key.sources.sortedBy { it.ordinal }.mapNotNull(sourceFor)
        if (sources.isEmpty()) return flowOf(emptyList())
        return combine(sources.map { it.observe(key.range, key.zone) }) { outcomes -> mergeOutcomes(outcomes.asList()) }
    }

    /** Pure helpers. */
    companion object {
        /** All successful items, in source order; failures contribute nothing. */
        fun mergeOutcomes(outcomes: List<Outcome<List<AgendaItem>>>): List<AgendaItem> =
            outcomes.flatMap { it.getOrNull().orEmpty() }
    }
}
