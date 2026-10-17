package io.github.nabin47.calwallpaper.data.todoist

import android.content.Context
import io.github.nabin47.calwallpaper.data.secure.TokenStore

/** The encrypted store for the user's Todoist API token (CLAUDE.md §7). Not backed up; never logged. */
object TodoistToken {
    private const val KEY_ALIAS = "calwallpaper.todoist.token"
    private const val FILE_NAME = "todoist_token.bin"

    /** The token store. Cheap to create; the key and file are shared by every instance. */
    fun store(context: Context): TokenStore = TokenStore(context, KEY_ALIAS, FILE_NAME)
}
