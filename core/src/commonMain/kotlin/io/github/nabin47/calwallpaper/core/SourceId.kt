package io.github.nabin47.calwallpaper.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Identifies an agenda source adapter. [key] is persisted in settings, exports and the database,
 * so it must never change once released; add new entries instead.
 */
@Serializable
enum class SourceId(
    val key: String,
) {
    /** Android Calendar Provider (covers Google Calendar synced on the device). */
    @SerialName("calendar_provider")
    CALENDAR_PROVIDER("calendar_provider"),

    /** The app's minimal local task list. */
    @SerialName("local_tasks")
    LOCAL_TASKS("local_tasks"),

    /** Todoist via the user's personal API token. */
    @SerialName("todoist")
    TODOIST("todoist"),
    ;

    /** Lookup by persisted key. */
    companion object {
        /** Returns the source for [key], or `null` for an unknown (e.g. newer) key. */
        fun fromKey(key: String): SourceId? = entries.firstOrNull { it.key == key }
    }
}
