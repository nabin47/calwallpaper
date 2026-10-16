package io.github.nabin47.calwallpaper.widgets

import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetKindTest {
    @Test
    fun layoutKeysAreUnique() {
        assertEquals(
            WidgetKind.entries.size,
            WidgetKind.entries
                .map { it.layoutKey }
                .toSet()
                .size,
        )
    }
}
