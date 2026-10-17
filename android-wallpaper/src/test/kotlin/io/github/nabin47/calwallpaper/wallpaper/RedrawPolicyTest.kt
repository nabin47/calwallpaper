package io.github.nabin47.calwallpaper.wallpaper

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedrawPolicyTest {
    @Test
    fun defersWhileInvisible() {
        assertFalse(RedrawPolicy.shouldDrawNow(RedrawReason.DATA_CHANGED, isVisible = false))
    }

    @Test
    fun drawsWhenVisible() {
        RedrawReason.entries.forEach { assertTrue(RedrawPolicy.shouldDrawNow(it, isVisible = true)) }
        assertTrue(RedrawPolicy.shouldDrawNow(RedrawReason.BECAME_VISIBLE, isVisible = false))
    }
}
