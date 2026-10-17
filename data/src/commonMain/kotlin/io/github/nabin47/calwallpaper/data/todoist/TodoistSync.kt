package io.github.nabin47.calwallpaper.data.todoist

import io.github.nabin47.calwallpaper.data.secure.TokenRead
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/** The network side of a sync: fetches projects and tasks with a token. Implemented by [TodoistApi]. */
fun interface TodoistRemote {
    /** Everything the wallpaper needs, or why it could not be fetched. Never throws for expected failures. */
    suspend fun fetch(token: String): RemoteResult
}

/** What [TodoistRemote.fetch] got. */
sealed interface RemoteResult {
    /** The user's projects and dated open tasks. */
    data class Data(
        val projects: List<CachedProject>,
        val tasks: List<CachedTask>,
    ) : RemoteResult

    /** The fetch failed; [retryAfterSeconds] is Todoist's hint when it limited the rate. */
    data class Failed(
        val error: SyncError,
        val retryAfterSeconds: Long? = null,
    ) : RemoteResult
}

/** Where synced data lives: the offline cache, shared across processes. */
interface TodoistCacheStore {
    /** The current cache, then every change. */
    val cache: Flow<TodoistCache>

    /** Atomically replaces the cache with [transform] of it and returns the new value. */
    suspend fun update(transform: (TodoistCache) -> TodoistCache): TodoistCache
}

/** How a [TodoistSync.run] ended. */
sealed interface SyncResult {
    /** Fresh data is in the cache; [changed] tells whether anything visible differs from before. */
    data class Synced(
        val changed: Boolean,
    ) : SyncResult

    /** No token is stored: Todoist is not connected, and there is nothing to do. */
    data object NotConnected : SyncResult

    /**
     * The sync failed with [error]. [retryable] errors (network, server, rate limit) should be retried with
     * backoff; the others need the user (a new token) and must not be retried.
     */
    data class Failed(
        val error: SyncError,
        val retryable: Boolean,
        val retryAfterSeconds: Long? = null,
    ) : SyncResult
}

/**
 * One Todoist sync: read the token, fetch, and store the result and its status in the offline cache. Failures
 * keep the last good tasks, so the wallpaper keeps showing them offline. Holds no state; safe to run from any
 * coroutine. Never logs anything.
 */
class TodoistSync(
    private val remote: TodoistRemote,
    private val readToken: suspend () -> TokenRead,
    private val store: TodoistCacheStore,
    private val clock: Clock = Clock.System,
) {
    /** Runs one sync. */
    suspend fun run(): SyncResult {
        val now = clock.now().toEpochMilliseconds()
        return when (val token = readToken()) {
            TokenRead.Missing -> {
                SyncResult.NotConnected
            }

            TokenRead.Unreadable -> {
                fail(now, RemoteResult.Failed(SyncError.TOKEN_UNREADABLE))
            }

            is TokenRead.Present -> {
                when (val result = remote.fetch(token.token)) {
                    is RemoteResult.Data -> store(now, result)
                    is RemoteResult.Failed -> fail(now, result)
                }
            }
        }
    }

    /** Forgets everything synced (the user disconnected Todoist), so no titles stay behind. */
    suspend fun clear() {
        store.update { TodoistCache() }
    }

    private suspend fun store(
        now: Long,
        data: RemoteResult.Data,
    ): SyncResult {
        var changed = false
        store.update { old ->
            val tasks = data.tasks.sortedWith(compareBy({ it.dueDate }, { it.order }, { it.id }))
            changed = tasks != old.tasks || data.projects != old.projects
            TodoistCache(tasks, data.projects, SyncStatus(lastSuccessMs = now, lastAttemptMs = now, error = null))
        }
        return SyncResult.Synced(changed)
    }

    private suspend fun fail(
        now: Long,
        failure: RemoteResult.Failed,
    ): SyncResult {
        store.update { it.copy(status = it.status.copy(lastAttemptMs = now, error = failure.error)) }
        return SyncResult.Failed(failure.error, failure.error in RETRYABLE, failure.retryAfterSeconds)
    }

    private companion object {
        val RETRYABLE = setOf(SyncError.NETWORK, SyncError.SERVER, SyncError.RATE_LIMITED)
    }
}
