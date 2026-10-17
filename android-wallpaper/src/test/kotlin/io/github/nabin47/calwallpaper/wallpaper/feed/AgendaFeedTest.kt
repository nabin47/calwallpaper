package io.github.nabin47.calwallpaper.wallpaper.feed

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.domain.AgendaSource
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.WeekStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class AgendaFeedTest {
    private val today = LocalDate(2026, 10, 17) // A Saturday.
    private val zone = TimeZone.UTC

    private fun task(id: String) =
        AgendaItem(
            key = ItemKey(SourceId.LOCAL_TASKS, id),
            kind = ItemKind.TASK,
            title = id,
            time = ItemTime.Due(today),
            openTarget = OpenTarget.LocalItem(1),
        )

    private class FakeSource(
        override val id: SourceId,
        private val outcome: Outcome<List<AgendaItem>>,
    ) : AgendaSource {
        var lastRange: DateRange? = null

        override fun observe(
            range: DateRange,
            zone: TimeZone,
        ): Flow<Outcome<List<AgendaItem>>> {
            lastRange = range
            return flowOf(outcome)
        }
    }

    private fun key(
        settings: Settings,
        localeFirstDay: DayOfWeek = DayOfWeek.MONDAY,
    ) = FeedKey.of(settings, today, zone, localeFirstDay, unlocked = true, calendarPermission = true)

    @Test
    fun keyRangeFollowsPresetAndWeekStart() {
        val agenda = key(Settings())
        assertEquals(DateRange(today, LocalDate(2026, 10, 23)), agenda.range, "agenda: today + 7 days")

        val week = Settings().let { it.copy(layout = it.layout.copy(preset = LayoutPreset.WEEK_STRIP)) }
        assertEquals(LocalDate(2026, 10, 12), key(week).range.start, "Monday start from the locale")
        assertEquals(LocalDate(2026, 10, 11), key(week, DayOfWeek.SUNDAY).range.start, "Sunday start from the locale")
        val saturday = week.copy(time = week.time.copy(weekStart = WeekStart.SATURDAY))
        assertEquals(today, key(saturday, DayOfWeek.SUNDAY).range.start, "explicit setting beats the locale")
    }

    @Test
    fun failedSourcesAddNothingInsteadOfHidingOthers() {
        val merged =
            AgendaFeed.mergeOutcomes(
                listOf(Outcome.Failure("no permission"), Outcome.Success(listOf(task("a"), task("b")))),
            )
        assertEquals(listOf("a", "b"), merged.map { it.title })
    }

    @Test
    fun observesOnlyEnabledSourcesForTheKeyRange() =
        runBlocking {
            val calendar = FakeSource(SourceId.CALENDAR_PROVIDER, Outcome.Success(listOf(task("cal"))))
            val local = FakeSource(SourceId.LOCAL_TASKS, Outcome.Success(listOf(task("local"))))
            val feed = AgendaFeed { id -> listOf(calendar, local).firstOrNull { it.id == id } }
            val onlyLocal =
                Settings().let { it.copy(content = it.content.copy(enabledSources = setOf(SourceId.LOCAL_TASKS))) }
            val k = key(onlyLocal)

            assertEquals(listOf("local"), feed.observe(k).first().map { it.title })
            assertEquals(null, calendar.lastRange, "disabled source is never observed")
            assertEquals(k.range, local.lastRange)
            assertEquals(emptyList(), feed.observe(k.copy(sources = setOf(SourceId.TODOIST))).first())
        }
}
