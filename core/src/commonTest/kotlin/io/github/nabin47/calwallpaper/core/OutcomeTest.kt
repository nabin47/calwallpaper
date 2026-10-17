package io.github.nabin47.calwallpaper.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutcomeTest {
    @Test
    fun mapTransformsSuccess() {
        val outcome: Outcome<Int> = Outcome.Success(2)
        assertEquals(Outcome.Success(4), outcome.map { it * 2 })
    }

    @Test
    fun mapKeepsFailure() {
        val outcome: Outcome<Int> = Outcome.Failure("offline")
        assertEquals(Outcome.Failure("offline"), outcome.map { it * 2 })
    }

    @Test
    fun getOrNullReturnsNullForFailure() {
        assertNull(Outcome.Failure("offline").getOrNull())
        assertEquals(3, Outcome.Success(3).getOrNull())
    }
}
