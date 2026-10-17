package io.github.nabin47.calwallpaper.data.todoist

/** What a pasted Todoist API token must look like before it is stored. Pure. */
object TodoistTokenFormat {
    private const val MIN_LENGTH = 20
    private const val MAX_LENGTH = 200

    /**
     * Trims surrounding whitespace (pasting often adds some) and returns the token, or `null` if it cannot be one.
     * Todoist tokens are 40 hexadecimal characters today. Only length and characters are checked, so a format
     * change on Todoist's side does not lock users out; the first sync is the real check.
     */
    fun clean(input: String): String? =
        input.trim().takeIf { token -> token.length in MIN_LENGTH..MAX_LENGTH && token.all { it.isLetterOrDigit() } }
}
