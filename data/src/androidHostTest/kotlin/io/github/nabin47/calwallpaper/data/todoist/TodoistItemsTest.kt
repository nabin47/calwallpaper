package io.github.nabin47.calwallpaper.data.todoist

import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Clock
import kotlin.time.Instant

class TodoistItemsTest {
    private val dhaka = TimeZone.of("Asia/Dhaka") // UTC+6.
    private val today = LocalDate(2026, 10, 17)
    private val project = CachedProject("p1", "Work", 0xFFDB4035.toInt())

    private fun cache(vararg tasks: CachedTask) = TodoistCache(tasks.toList(), listOf(project))

    @Test
    fun aDatedTaskBecomesATaskItemThatOpensInTodoist() {
        val item = cache(CachedTask("t1", "Write report", "p1", "2026-10-18")).toAgendaItems(today, dhaka).single()
        assertEquals(SourceId.TODOIST, item.key.source)
        assertEquals("t1", item.key.sourceItemId)
        assertEquals(ItemKind.TASK, item.kind)
        assertEquals("Write report", item.title)
        assertEquals(ItemTime.Due(LocalDate(2026, 10, 18)), item.time)
        assertEquals(OpenTarget.TodoistTask("t1"), item.openTarget)
        assertEquals("p1", item.groupId)
        assertEquals(0xFFDB4035.toInt(), item.colorArgb)
        assertFalse(item.isCompleted)
    }

    @Test
    fun floatingTimesStayLocalAndFixedTimesFollowTheDeviceZone() {
        val items =
            cache(
                CachedTask("float", "A", "p1", "2026-10-18", dueTime = "09:30:00"),
                CachedTask("fixed", "B", "p1", "2026-10-18", dueTime = "23:00:00", dueInstant = "2026-10-18T23:00:00Z"),
            ).toAgendaItems(today, dhaka)
        assertEquals(ItemTime.Due(LocalDate(2026, 10, 18), LocalTime(9, 30)), items[0].time)
        // 23:00 UTC is 05:00 the next day in Dhaka.
        assertEquals(ItemTime.Due(LocalDate(2026, 10, 19), LocalTime(5, 0)), items[1].time)
    }

    @Test
    fun overdueTasksMoveToTodayWithoutATime() {
        val item =
            cache(
                CachedTask("late", "Overdue", "p1", "2026-10-10", dueTime = "08:00"),
            ).toAgendaItems(today, dhaka).single()
        assertEquals(ItemTime.Due(today), item.time)
    }

    @Test
    fun unparseableDatesAndUnknownProjectsAreHandled() {
        val items =
            cache(
                CachedTask("bad", "Bad date", "p1", "not-a-date"),
                CachedTask("orphan", "No project", "gone", "2026-10-18"),
            ).toAgendaItems(today, dhaka)
        assertEquals(listOf("orphan"), items.map { it.key.sourceItemId })
        assertEquals(null, items.single().colorArgb)
    }

    @Test
    fun theSourceReturnsOnlyTasksInTheRange() =
        runBlocking {
            val clock =
                object : Clock {
                    override fun now(): Instant = Instant.parse("2026-10-17T06:00:00Z")
                }
            val source =
                TodoistSource(
                    flowOf(
                        cache(
                            CachedTask("in", "In range", "p1", "2026-10-19"),
                            CachedTask("out", "Later", "p1", "2026-11-30"),
                            CachedTask("late", "Overdue", "p1", "2026-10-01"),
                        ),
                    ),
                    clock,
                )
            val result = source.observe(DateRange(today, LocalDate(2026, 10, 23)), dhaka).first()
            val ids = (result as Outcome.Success).value.map { it.key.sourceItemId }
            assertEquals(listOf("in", "late"), ids)
        }

    @Test
    fun toStringNeverShowsTitles() {
        val task = CachedTask("t1", "Secret plan", "p1", "2026-10-18")
        assertFalse("Secret plan" in task.toString())
        assertFalse("Secret plan" in cache(task).toString())
        assertFalse("Work" in project.toString())
    }
}
