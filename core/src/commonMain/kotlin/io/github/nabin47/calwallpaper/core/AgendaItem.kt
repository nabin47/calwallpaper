package io.github.nabin47.calwallpaper.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Instant

/** Stable identity of an item across refreshes: the source plus the source's own id for it. */
data class ItemKey(
    val source: SourceId,
    val sourceItemId: String,
)

/** What kind of thing an item is; renderers style events and tasks differently. */
enum class ItemKind {
    /** A calendar event. */
    EVENT,

    /** A task or to-do. */
    TASK,
}

/** When an item happens. Each variant keeps the precision the source actually has. */
sealed interface ItemTime {
    /**
     * Whole days, independent of time zone (calendar all-day events).
     * [endInclusive] is the last day the item covers; equal to [start] for single-day items.
     */
    data class AllDay(
        val start: LocalDate,
        val endInclusive: LocalDate,
    ) : ItemTime {
        init {
            require(endInclusive >= start) { "endInclusive must not be before start" }
        }
    }

    /** A span on the timeline. [end] may equal [start] for zero-length events. */
    data class Timed(
        val start: Instant,
        val end: Instant,
    ) : ItemTime {
        init {
            require(end >= start) { "end must not be before start" }
        }
    }

    /** A deadline (tasks): a local date with an optional local time, in whatever zone the user is in. */
    data class Due(
        val date: LocalDate,
        val time: LocalTime? = null,
    ) : ItemTime
}

/**
 * One thing to show on the wallpaper or a widget, from any [SourceId]. Immutable.
 *
 * [title] is user content. It must never be logged (CLAUDE.md §7), so [toString] redacts it.
 *
 * @property groupId the source's grouping (calendar id, task list or project id), used for per-calendar
 *   selection in settings; `null` when the source has no groups.
 * @property colorArgb the source's color for the item or its group, as `0xAARRGGBB`; `null` if none.
 */
data class AgendaItem(
    val key: ItemKey,
    val kind: ItemKind,
    val title: String,
    val time: ItemTime,
    val openTarget: OpenTarget,
    val isCompleted: Boolean = false,
    val groupId: String? = null,
    val colorArgb: Int? = null,
) {
    /** Redacts [title] so items can appear in logs and crash messages safely. */
    override fun toString(): String =
        "AgendaItem(key=$key, kind=$kind, time=$time, isCompleted=$isCompleted, title=<${title.length} chars>)"
}
