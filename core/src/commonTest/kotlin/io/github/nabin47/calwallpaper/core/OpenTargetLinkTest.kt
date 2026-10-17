package io.github.nabin47.calwallpaper.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class OpenTargetLinkTest {
    private val targets =
        listOf(
            OpenTarget.CalendarDay(LocalDate(2026, 10, 17)),
            OpenTarget.CalendarEvent(
                eventId = 42,
                begin = Instant.parse("2026-10-17T09:00:00Z"),
                end = Instant.parse("2026-10-17T09:30:00Z"),
            ),
            OpenTarget.TodoistTask("6X7rM8997g3RQmvh"),
            OpenTarget.TodoistProject("2203306141"),
            OpenTarget.LocalItem(7),
            OpenTarget.Url("https://example.com/a path?q=ä&x=1#frag"),
        )

    @Test
    fun everyTargetSurvivesARoundTrip() {
        targets.forEach { target ->
            assertEquals(target, OpenTargetLink.decode(OpenTargetLink.encode(target)), target.toString())
        }
    }

    @Test
    fun linksUseTheAppSchemeAndOnlyUriSafeCharacters() {
        targets.map(OpenTargetLink::encode).forEach { link ->
            assertTrue(link.startsWith("calwallpaper://open/"), link)
            assertTrue(link.all { it.code < 128 && it != ' ' && it != '#' }, link)
        }
        assertEquals("calwallpaper://open/day?date=2026-10-17", OpenTargetLink.encode(targets[0]))
    }

    @Test
    fun differentTargetsGiveDifferentLinks() {
        assertEquals(targets.size, targets.map(OpenTargetLink::encode).toSet().size)
    }

    @Test
    fun invalidLinksDecodeToNull() {
        listOf(
            "",
            "https://open/day?date=2026-10-17",
            "calwallpaper://open/unknown?id=1",
            "calwallpaper://open/day?date=not-a-date",
            "calwallpaper://open/event?id=1",
            "calwallpaper://open/local?id=abc",
            "calwallpaper://open/todoist-task?id=",
            "calwallpaper://open/url?u=http%3A%2F%2Fexample.com",
            "calwallpaper://open/url?u=%E",
            "calwallpaper://open/url?u=%ZZ",
        ).forEach { link -> assertNull(OpenTargetLink.decode(link), link) }
    }
}
