package io.github.nabin47.calwallpaper.domain.settings

import io.github.nabin47.calwallpaper.core.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Persistent store for [Settings]. Implementations must be single-instance per process and safe to call from
 * any thread.
 */
interface SettingsRepository {
    /** Current settings, then every change. Emits [Settings] defaults if nothing was saved yet. */
    val settings: Flow<Settings>

    /** Atomically replaces the settings with `transform(current)`, normalized. */
    suspend fun update(transform: (Settings) -> Settings)

    /** The current settings as versioned JSON (see [SettingsCodec]), for export or backup. */
    suspend fun exportJson(): String

    /**
     * Replaces the settings with the ones in [json] (migrating older versions). On failure nothing changes
     * and the reason is safe to show to the user.
     */
    suspend fun importJson(json: String): Outcome<Settings>
}
