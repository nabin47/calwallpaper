package io.github.nabin47.calwallpaper.quickadd

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickDueTest {
    @Test
    fun choicesBecomeDates() {
        val today = LocalDate(2026, 12, 31)
        assertNull(QuickDue.NONE.dateFrom(today))
        assertEquals(today, QuickDue.TODAY.dateFrom(today))
        assertEquals(LocalDate(2027, 1, 1), QuickDue.TOMORROW.dateFrom(today))
    }
}
