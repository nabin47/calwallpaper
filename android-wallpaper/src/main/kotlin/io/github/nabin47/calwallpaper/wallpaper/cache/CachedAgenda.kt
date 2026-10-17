package io.github.nabin47.calwallpaper.wallpaper.cache

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * The last agenda the wallpaper drew, in a form that can be written to device-protected storage and read
 * before the first unlock (Direct Boot, CLAUDE.md §2). Times are stored as plain numbers and ISO strings.
 *
 * Titles are user content and device-protected storage is readable before the user's credentials are entered,
 * so [fromItems] stores them only when privacy mode is off. Before the first unlock the device is always
 * locked, so with privacy on the wallpaper would hide titles anyway and nothing visible is lost.
 */
@Serializable
data class CachedAgenda(
    val items: List<CachedItem> = emptyList(),
) {
    /** Back to agenda items; entries that no longer parse are skipped. Pure. */
    fun toItems(): List<AgendaItem> = items.mapNotNull { it.toItem() }

    /** Conversion from live items. */
    companion object {
        /** Snapshot of [items]; titles are blanked unless [privacy] is [PrivacyMode.OFF]. Pure. */
        fun fromItems(
            items: List<AgendaItem>,
            privacy: PrivacyMode,
        ): CachedAgenda = CachedAgenda(items.map { CachedItem.from(it, keepTitle = privacy == PrivacyMode.OFF) })
    }
}

/** One cached item. Only what the layout needs; the open target is reduced to the item's day. */
@Serializable
data class CachedItem(
    val source: String,
    val id: String,
    val task: Boolean,
    val title: String,
    val completed: Boolean,
    val groupId: String? = null,
    val color: Int? = null,
    /** "allday", "timed" or "due". */
    val timeKind: String,
    /** All-day start or due date (ISO), or timed start (epoch ms as a string). */
    val start: String,
    /** All-day last day (ISO) or timed end (epoch ms as a string); `null` for due items. */
    val end: String? = null,
    /** Due time (ISO), for due items with a time. */
    val dueTime: String? = null,
) {
    /** Redacts the title so cache entries can appear in logs safely. */
    override fun toString(): String = "CachedItem(source=$source, id=$id, kind=$timeKind)"

    /** The agenda item, or `null` if a field is malformed (e.g. a newer format). */
    fun toItem(): AgendaItem? {
        val sourceId = SourceId.fromKey(source)
        val time = parseTime()
        return if (sourceId == null || time == null) {
            null
        } else {
            AgendaItem(
                key = ItemKey(sourceId, id),
                kind = if (task) ItemKind.TASK else ItemKind.EVENT,
                title = title,
                time = time,
                openTarget = OpenTarget.CalendarDay(startDate(time)),
                isCompleted = completed,
                groupId = groupId,
                colorArgb = color,
            )
        }
    }

    private fun parseTime(): ItemTime? =
        runCatching {
            when (timeKind) {
                ALL_DAY -> {
                    ItemTime.AllDay(LocalDate.parse(start), LocalDate.parse(end ?: start))
                }

                TIMED -> {
                    ItemTime.Timed(
                        Instant.fromEpochMilliseconds(start.toLong()),
                        Instant.fromEpochMilliseconds((end ?: start).toLong()),
                    )
                }

                DUE -> {
                    ItemTime.Due(LocalDate.parse(start), dueTime?.let(LocalTime::parse))
                }

                else -> {
                    null
                }
            }
        }.getOrNull()

    /** Factory and format constants. */
    companion object {
        private const val ALL_DAY = "allday"
        private const val TIMED = "timed"
        private const val DUE = "due"

        /** Snapshot of [item]; the title is kept only if [keepTitle]. */
        fun from(
            item: AgendaItem,
            keepTitle: Boolean,
        ): CachedItem {
            val base =
                CachedItem(
                    source = item.key.source.key,
                    id = item.key.sourceItemId,
                    task = item.kind == ItemKind.TASK,
                    title = if (keepTitle) item.title else "",
                    completed = item.isCompleted,
                    groupId = item.groupId,
                    color = item.colorArgb,
                    timeKind = DUE,
                    start = "",
                )
            return when (val t = item.time) {
                is ItemTime.AllDay -> {
                    base.copy(timeKind = ALL_DAY, start = t.start.toString(), end = t.endInclusive.toString())
                }

                is ItemTime.Timed -> {
                    base.copy(
                        timeKind = TIMED,
                        start = t.start.toEpochMilliseconds().toString(),
                        end = t.end.toEpochMilliseconds().toString(),
                    )
                }

                is ItemTime.Due -> {
                    base.copy(start = t.date.toString(), dueTime = t.time?.toString())
                }
            }
        }

        /** The day an item's tap should open: the first day it covers (UTC for timed items is close enough here). */
        private fun startDate(time: ItemTime): LocalDate =
            when (time) {
                is ItemTime.AllDay -> {
                    time.start
                }

                is ItemTime.Due -> {
                    time.date
                }

                is ItemTime.Timed -> {
                    LocalDate.fromEpochDays(
                        (time.start.toEpochMilliseconds() / MILLIS_PER_DAY).toInt(),
                    )
                }
            }

        private const val MILLIS_PER_DAY = 86_400_000L
    }
}
