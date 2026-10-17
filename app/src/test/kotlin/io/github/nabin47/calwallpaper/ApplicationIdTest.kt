package io.github.nabin47.calwallpaper

import kotlin.test.Test
import kotlin.test.assertEquals

/** Guards the permanent applicationId (CLAUDE.md §10) against accidental edits. */
class ApplicationIdTest {
    @Test
    fun applicationIdIsStable() {
        assertEquals("io.github.nabin47.calwallpaper", BuildConfig.APPLICATION_ID.removeSuffix(".debug"))
    }
}
