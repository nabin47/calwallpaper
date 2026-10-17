package io.github.nabin47.calwallpaper.ui.preview

import io.github.nabin47.calwallpaper.core.ItemKind
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertTrue

class SampleAgendaTest {
    @Test
    fun sampleCoversEventsTasksAndACompletedTask() {
        val titles = SampleTitles("a", "b", "c", "d", "e", "f")
        val items = SampleAgenda.items(LocalDate(2026, 10, 17), TimeZone.UTC, titles)
        assertTrue(items.any { it.kind == ItemKind.EVENT })
        assertTrue(items.any { it.kind == ItemKind.TASK })
        assertTrue(items.any { it.isCompleted }, "the preview must show what 'show completed' does")
        assertTrue(items.map { it.key }.toSet().size == items.size, "keys are unique")
    }
}
