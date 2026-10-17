package io.github.nabin47.calwallpaper.wallpaper

import android.content.Context
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.data.calendar.CalendarProviderSource
import io.github.nabin47.calwallpaper.data.local.LocalTasks
import io.github.nabin47.calwallpaper.data.local.LocalTasksDatabaseFactory
import io.github.nabin47.calwallpaper.data.local.LocalTasksSource
import io.github.nabin47.calwallpaper.data.settings.DataStoreSettingsRepository
import io.github.nabin47.calwallpaper.data.todoist.DataStoreTodoistCache
import io.github.nabin47.calwallpaper.data.todoist.TodoistSource
import io.github.nabin47.calwallpaper.domain.AgendaSource
import io.github.nabin47.calwallpaper.domain.settings.SettingsRepository
import io.github.nabin47.calwallpaper.wallpaper.cache.DirectBootCache
import io.github.nabin47.calwallpaper.wallpaper.diagnostics.Diagnostics
import io.github.nabin47.calwallpaper.wallpaper.feed.AgendaFeed
import io.github.nabin47.calwallpaper.wallpaper.render.RenderRequestFactory
import io.github.nabin47.calwallpaper.wallpaper.system.DeviceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide dependencies, built by hand (manual constructor injection, ADR 0001). One instance per process:
 * DataStore and Room must not be opened twice in the same process. Everything is created lazily, so the
 * wallpaper process only pays for what it uses.
 *
 * Direct Boot: [settings], [directBootCache], [deviceState] and [diagnostics] live in device-protected storage
 * and are safe before the first unlock. [feed] opens the local task database (credential-protected) on first
 * use, so callers must only observe it once [DeviceState.isUserUnlocked] is true.
 */
class WallpaperGraph private constructor(
    context: Context,
) {
    private val appContext = context.applicationContext

    /** Lives as long as the process; owns DataStore's background work. */
    val processScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** User settings, shared with the other process through multi-process DataStore. */
    val settings: SettingsRepository by lazy { DataStoreSettingsRepository.create(appContext, processScope) }

    /** Device state reads (locked, unlocked, permissions). */
    val deviceState: DeviceState by lazy { DeviceState(appContext) }

    /** Last agenda, readable before the first unlock. */
    val directBootCache: DirectBootCache by lazy { DirectBootCache(appContext) }

    /** Measured performance numbers, shown in the app. */
    val diagnostics: Diagnostics by lazy { Diagnostics(appContext) }

    /** Builds render requests with the device's locale, clock and palette choices. */
    val requestFactory: RenderRequestFactory by lazy { RenderRequestFactory(appContext) }

    /**
     * The Todoist offline cache (credential-protected; use only after unlock). Written by the app's sync worker,
     * read here; the wallpaper process never touches the network.
     */
    val todoistCache: DataStoreTodoistCache by lazy { DataStoreTodoistCache.create(appContext, processScope) }

    private val calendarSource by lazy { CalendarProviderSource(appContext) }
    private val localTasksDatabase by lazy { LocalTasksDatabaseFactory.create(appContext) }
    private val localTasksSource by lazy { LocalTasksSource(localTasksDatabase.taskDao()) }

    /** Writes to the local task list (quick-add). Credential-protected: use only after unlock. */
    val localTasks: LocalTasks by lazy { LocalTasks(localTasksDatabase.taskDao()) }

    /** All agenda sources merged. */
    val feed: AgendaFeed by lazy {
        AgendaFeed { id -> sourceFor(id) }
    }

    private fun sourceFor(id: SourceId): AgendaSource? =
        when (id) {
            SourceId.CALENDAR_PROVIDER -> calendarSource
            SourceId.LOCAL_TASKS -> localTasksSource
            SourceId.TODOIST -> TodoistSource(todoistCache.cache)
        }

    /** Access to the per-process instance. */
    companion object {
        @Volatile
        private var instance: WallpaperGraph? = null

        /** The graph for this process, created on first call. Thread-safe. */
        fun get(context: Context): WallpaperGraph =
            instance ?: synchronized(this) {
                instance ?: WallpaperGraph(context).also { instance = it }
            }
    }
}
