package io.github.nabin47.calwallpaper.data.todoist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodoistTokenFormatTest {
    private val token = "0123456789abcdef0123456789abcdef01234567"

    @Test
    fun acceptsATokenAndTrimsPastedWhitespace() {
        assertEquals(token, TodoistTokenFormat.clean(token))
        assertEquals(token, TodoistTokenFormat.clean("  $token\n"))
    }

    @Test
    fun rejectsThingsThatCannotBeTokens() {
        listOf("", "short", "$token with spaces", "Bearer $token", "https://todoist.com/$token").forEach {
            assertNull(TodoistTokenFormat.clean(it), it)
        }
    }
}
