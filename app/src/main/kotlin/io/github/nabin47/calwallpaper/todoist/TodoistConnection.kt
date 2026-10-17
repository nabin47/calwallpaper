package io.github.nabin47.calwallpaper.todoist

import android.content.Context
import io.github.nabin47.calwallpaper.data.todoist.TodoistCache
import io.github.nabin47.calwallpaper.data.todoist.TodoistToken
import io.github.nabin47.calwallpaper.data.todoist.TodoistTokenFormat
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import io.github.nabin47.calwallpaper.wallpaper.system.InstalledWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * The settings screen's handle on Todoist: connect with a pasted token, disconnect, sync now, and the cached
 * data and sync status to show. The token goes straight into the encrypted [TodoistToken] store. It is never
 * kept in a field, shown again, or logged. Call the suspending functions from the UI's coroutine scope.
 */
class TodoistConnection(
    context: Context,
    private val graph: WallpaperGraph,
) {
    private val appContext = context.applicationContext
    private val tokens = TodoistToken.store(appContext)
    private val scheduler = TodoistSyncScheduler(appContext)
    private val connectedState = MutableStateFlow<Boolean?>(null)

    /** Whether a token is stored; `null` until [refresh] has checked. */
    val connected: StateFlow<Boolean?> = connectedState.asStateFlow()

    /** Synced projects and tasks plus the last sync's status. */
    val cache: Flow<TodoistCache> = graph.todoistCache.cache

    /** Re-reads whether a token is stored, and keeps the sync schedule in line with [enabled]. */
    suspend fun refresh(enabled: Boolean) {
        val stored = withContext(Dispatchers.IO) { tokens.exists() }
        connectedState.value = stored
        scheduler.update(active = enabled && stored)
    }

    /**
     * Stores the token in [input] and starts syncing. The previous account's data is forgotten first.
     * Returns false if the input cannot be a token or the Keystore refused to encrypt it.
     */
    suspend fun connect(input: String): Boolean {
        val token = TodoistTokenFormat.clean(input)
        val stored = token != null && tokens.write(token)
        if (stored) {
            graph.todoistCache.update { TodoistCache() }
            connectedState.value = true
            scheduler.update(active = true)
            scheduler.syncNow()
        }
        return stored
    }

    /** Deletes the token and every synced title, stops syncing, and redraws widgets without Todoist tasks. */
    suspend fun disconnect() {
        scheduler.update(active = false)
        tokens.clear()
        graph.todoistCache.update { TodoistCache() }
        connectedState.value = false
        withContext(Dispatchers.IO) { InstalledWidgets(appContext).requestUpdate() }
    }

    /** Syncs as soon as a network is available. */
    fun syncNow() = scheduler.syncNow()
}
