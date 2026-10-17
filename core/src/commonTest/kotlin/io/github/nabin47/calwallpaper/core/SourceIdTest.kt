package io.github.nabin47.calwallpaper.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SourceIdTest {
    @Test
    fun keysAreUnique() {
        assertEquals(
            SourceId.entries.size,
            SourceId.entries
                .map { it.key }
                .toSet()
                .size,
        )
    }

    @Test
    fun keysRoundTrip() {
        SourceId.entries.forEach { assertEquals(it, SourceId.fromKey(it.key)) }
        assertNull(SourceId.fromKey("unknown"))
    }
}
