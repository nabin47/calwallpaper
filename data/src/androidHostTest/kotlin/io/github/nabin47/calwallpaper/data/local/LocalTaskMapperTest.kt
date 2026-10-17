package io.github.nabin47.calwallpaper.data.local

import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalTaskMapperTest {
    private val today = LocalDate(2026, 10, 17)
    private val oct20 = LocalDate(2026, 10, 20)

    @Test
    fun datedTaskWithTime() {
        val item = localTaskToAgendaItem(5, "Call bank", oct20.toEpochDays(), 9 * 60 + 30, false, today)
        assertEquals(ItemTime.Due(oct20, LocalTime(9, 30)), item.time)
        assertEquals(SourceId.LOCAL_TASKS, item.key.source)
        assertEquals("5", item.key.sourceItemId)
        assertEquals(ItemKind.TASK, item.kind)
        assertEquals(OpenTarget.LocalItem(5), item.openTarget)
    }

    @Test
    fun undatedTaskShowsOnFirstDayWithoutTime() {
        val item = localTaskToAgendaItem(6, "Groceries", null, 600, false, today)
        assertEquals(ItemTime.Due(today, null), item.time)
    }

    @Test
    fun invalidMinuteIsIgnoredAndCompletionKept() {
        val item = localTaskToAgendaItem(7, "x", oct20.toEpochDays(), 5000, true, today)
        assertEquals(ItemTime.Due(oct20, null), item.time)
        assertTrue(item.isCompleted)
    }
}
