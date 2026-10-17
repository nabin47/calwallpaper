package io.github.nabin47.calwallpaper.open

import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.OpenTargetLink
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class LaunchPlanTest {
    private val dhaka = TimeZone.of("Asia/Dhaka") // UTC+6.

    @Test
    fun aDayOpensTheCalendarAtLocalMidnightThenTheWeb() {
        assertEquals(
            listOf(
                LaunchSpec.CalendarTime(Instant.parse("2026-10-16T18:00:00Z").toEpochMilliseconds()),
                LaunchSpec.Web("https://calendar.google.com/calendar/r/day/2026/10/17"),
            ),
            LaunchPlan.candidates(OpenTarget.CalendarDay(LocalDate(2026, 10, 17)), dhaka),
        )
    }

    @Test
    fun anEventFallsBackToItsLocalDay() {
        val begin = Instant.parse("2026-10-17T20:00:00Z") // Already Oct 18 in Dhaka.
        val end = Instant.parse("2026-10-17T21:00:00Z")
        val candidates = LaunchPlan.candidates(OpenTarget.CalendarEvent(42, begin, end), dhaka)
        assertEquals(
            LaunchSpec.CalendarEvent(42, begin.toEpochMilliseconds(), end.toEpochMilliseconds()),
            candidates[0],
        )
        assertEquals(LaunchPlan.candidates(OpenTarget.CalendarDay(LocalDate(2026, 10, 18)), dhaka), candidates.drop(1))
    }

    @Test
    fun todoistPrefersTheAppThenTheWeb() {
        assertEquals(
            listOf(
                LaunchSpec.Web("https://app.todoist.com/app/task/6X7rM8997g3RQmvh", appOnly = true),
                LaunchSpec.AppLink("todoist://task?id=6X7rM8997g3RQmvh"),
                LaunchSpec.Web("https://app.todoist.com/app/task/6X7rM8997g3RQmvh"),
            ),
            LaunchPlan.candidates(OpenTarget.TodoistTask("6X7rM8997g3RQmvh"), dhaka),
        )
        assertEquals(
            LaunchSpec.Web("https://app.todoist.com/app/project/2203306141"),
            LaunchPlan.candidates(OpenTarget.TodoistProject("2203306141"), dhaka).last(),
        )
    }

    @Test
    fun unsafeTodoistIdsNeverReachAUrl() {
        listOf("", "a/b", "1?x=2", "ä1", "1 2").forEach { id ->
            val candidates = LaunchPlan.candidates(OpenTarget.TodoistTask(id), dhaka)
            assertEquals(LaunchSpec.Web("https://app.todoist.com/app"), candidates.last(), id)
            assertTrue(candidates.none { it is LaunchSpec.AppLink && it.uri.contains("id=") }, id)
        }
    }

    @Test
    fun localItemsAndUrls() {
        assertEquals(listOf(LaunchSpec.OwnApp), LaunchPlan.candidates(OpenTarget.LocalItem(3), dhaka))
        assertEquals(
            listOf(LaunchSpec.Web("https://example.com/x")),
            LaunchPlan.candidates(OpenTarget.Url("https://example.com/x"), dhaka),
        )
    }

    @Test
    fun everyTargetEndsWithAFallbackThatNeedsNoSourceApp() {
        val targets =
            listOf(
                OpenTarget.CalendarDay(LocalDate(2026, 1, 1)),
                OpenTarget.CalendarEvent(1, Instant.fromEpochMilliseconds(0), Instant.fromEpochMilliseconds(1)),
                OpenTarget.TodoistTask("1"),
                OpenTarget.TodoistProject("1"),
                OpenTarget.LocalItem(1),
                OpenTarget.Url("https://example.com"),
            )
        targets.forEach { target ->
            val last = LaunchPlan.candidates(target, dhaka).last()
            assertTrue(last == LaunchSpec.OwnApp || (last is LaunchSpec.Web && !last.appOnly), "$target")
        }
    }

    @Test
    fun routerClassNameMatchesTheActivity() {
        assertEquals(OpenTargetActivity::class.java.name, OpenTargetLink.ROUTER_CLASS)
    }
}
