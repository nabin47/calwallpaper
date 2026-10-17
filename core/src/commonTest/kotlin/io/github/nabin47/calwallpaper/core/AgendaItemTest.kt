package io.github.nabin47.calwallpaper.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class AgendaItemTest {
    private val item =
        AgendaItem(
            key = ItemKey(SourceId.LOCAL_TASKS, "7"),
            kind = ItemKind.TASK,
            title = "Secret dentist appointment",
            time = ItemTime.Due(LocalDate(2026, 10, 17)),
            openTarget = OpenTarget.LocalItem(7),
        )

    @Test
    fun toStringNeverContainsTitle() {
        val text = item.toString()
        assertFalse("dentist" in text)
        assertTrue("26 chars" in text)
    }

    @Test
    fun allDayRejectsEndBeforeStart() {
        assertFailsWith<IllegalArgumentException> {
            ItemTime.AllDay(LocalDate(2026, 10, 17), LocalDate(2026, 10, 16))
        }
    }

    @Test
    fun timedRejectsEndBeforeStart() {
        assertFailsWith<IllegalArgumentException> {
            ItemTime.Timed(Instant.fromEpochSeconds(10), Instant.fromEpochSeconds(5))
        }
    }

    @Test
    fun urlTargetMustBeHttps() {
        OpenTarget.Url("https://todoist.com/app")
        assertFailsWith<IllegalArgumentException> { OpenTarget.Url("http://example.com") }
    }
}
