package io.github.nabin47.calwallpaper.render

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SceneTest {
    @Test
    fun phoneSizeIsPortrait() {
        assertTrue(Scene(widthPx = 1080, heightPx = 2400).isPortrait)
    }

    @Test
    fun rejectsEmptySize() {
        assertFailsWith<IllegalArgumentException> { Scene(widthPx = 0, heightPx = 100) }
    }
}
