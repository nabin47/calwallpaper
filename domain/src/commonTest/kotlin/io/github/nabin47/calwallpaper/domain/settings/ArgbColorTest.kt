package io.github.nabin47.calwallpaper.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArgbColorTest {
    @Test
    fun hexRoundTrip() {
        val color = ArgbColor(0x80112233.toInt())
        assertEquals("#80112233", color.toHex())
        assertEquals(color, ArgbColor.parseOrNull(color.toHex()))
    }

    @Test
    fun sixDigitHexIsOpaque() {
        assertEquals(ArgbColor(0xFF112233.toInt()), ArgbColor.parseOrNull("#112233"))
    }

    @Test
    fun invalidHexIsNull() {
        assertNull(ArgbColor.parseOrNull("red"))
        assertNull(ArgbColor.parseOrNull("#12345"))
        assertNull(ArgbColor.parseOrNull("#GG112233"))
    }
}
