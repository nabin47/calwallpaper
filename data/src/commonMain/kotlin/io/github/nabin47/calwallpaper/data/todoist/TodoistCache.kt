package io.github.nabin47.calwallpaper.data.todoist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The offline copy of the user's Todoist data, written by each successful sync and read by the wallpaper,
 * widgets and settings. Stored in credential-protected app storage and excluded from backups, because it holds
 * task titles (CLAUDE.md §7). Immutable; [toString] never prints titles.
 *
 * @property tasks open tasks that have a due date (Todoist returns only active tasks).
 * @property projects every project, for colors and the project picker in settings.
 */
@Serializable
data class TodoistCache(
    val tasks: List<CachedTask> = emptyList(),
    val projects: List<CachedProject> = emptyList(),
    val status: SyncStatus = SyncStatus(),
) {
    override fun toString(): String = "TodoistCache(tasks=${tasks.size}, projects=${projects.size}, status=$status)"
}

/**
 * One Todoist task, reduced to what the wallpaper shows.
 *
 * @property dueDate local due date, ISO `YYYY-MM-DD`.
 * @property dueTime local time of day (ISO `HH:MM[:SS]`) for tasks due at a time, else `null`.
 * @property dueInstant ISO instant for tasks fixed to a time zone; then [dueDate] and [dueTime] are only what
 *   Todoist reported and the wallpaper converts the instant to the device's zone instead.
 */
@Serializable
data class CachedTask(
    val id: String,
    val title: String,
    @SerialName("project") val projectId: String,
    val dueDate: String,
    val dueTime: String? = null,
    val dueInstant: String? = null,
    val order: Int = 0,
) {
    override fun toString(): String =
        "CachedTask(id=$id, project=$projectId, due=$dueDate, title=<${title.length} chars>)"
}

/** A Todoist project: its id, name and color as `0xAARRGGBB` (`null` for an unknown color name). */
@Serializable
data class CachedProject(
    val id: String,
    val name: String,
    val colorArgb: Int? = null,
) {
    override fun toString(): String = "CachedProject(id=$id, name=<${name.length} chars>)"
}

/**
 * How the last syncs went, for the settings screen. Times are epoch milliseconds.
 *
 * @property lastSuccessMs when data was last fetched, or `null` if never.
 * @property lastAttemptMs when a sync last ran, or `null` if never.
 * @property error why the last attempt failed, or `null` if it succeeded.
 */
@Serializable
data class SyncStatus(
    val lastSuccessMs: Long? = null,
    val lastAttemptMs: Long? = null,
    val error: SyncError? = null,
)

/** Why a sync failed. Each one maps to a message in settings; none contains user data. */
@Serializable
enum class SyncError {
    /** Todoist rejected the token (HTTP 401 or 403). Syncing stops until the user enters a new one. */
    @SerialName("auth")
    AUTH,

    /** The stored token can no longer be decrypted (for example after the screen lock was removed). */
    @SerialName("token_unreadable")
    TOKEN_UNREADABLE,

    /** No connection, or Todoist did not answer. Retried with backoff. */
    @SerialName("network")
    NETWORK,

    /** Too many requests (HTTP 429). Retried later. */
    @SerialName("rate_limited")
    RATE_LIMITED,

    /** Todoist had an error (HTTP 5xx). Retried with backoff. */
    @SerialName("server")
    SERVER,

    /** Todoist answered with something this app does not understand (e.g. an API change). */
    @SerialName("bad_response")
    BAD_RESPONSE,
}

/** True when syncing cannot continue until the user pastes a new token (rejected or unreadable token). */
val SyncStatus.needsNewToken: Boolean
    get() = error == SyncError.AUTH || error == SyncError.TOKEN_UNREADABLE
