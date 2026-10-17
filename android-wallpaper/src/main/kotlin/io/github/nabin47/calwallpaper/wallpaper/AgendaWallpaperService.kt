package io.github.nabin47.calwallpaper.wallpaper

import android.app.WallpaperColors
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.wallpaper.feed.FeedKey
import io.github.nabin47.calwallpaper.wallpaper.render.WallpaperFrame
import io.github.nabin47.calwallpaper.wallpaper.system.SystemEvent
import io.github.nabin47.calwallpaper.wallpaper.system.SystemEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * The live wallpaper. Runs in the `:wallpaper` process, which never loads Compose (CLAUDE.md §3).
 *
 * Draws only when something changed (CLAUDE.md §5): becoming visible, new agenda data, new settings, a date,
 * time or time-zone change, a lock-state change (privacy mode) or a configuration change. There are no timers,
 * no animation and no foreground service; while invisible it draws nothing and holds no bitmap.
 */
class AgendaWallpaperService : WallpaperService() {
    private val engines = mutableSetOf<AgendaEngine>()

    override fun onCreateEngine(): Engine = AgendaEngine().also { engines += it }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        engines.forEach { it.onDeviceConfigurationChanged() }
    }

    /**
     * One wallpaper instance (home, lock, or the picker's preview). All callbacks and collection run on the main
     * thread; layout and painting are small enough to stay there, and data I/O runs on background dispatchers.
     */
    private inner class AgendaEngine : Engine() {
        private val graph = WallpaperGraph.get(this@AgendaWallpaperService)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val frame = WallpaperFrame()
        private val events = SystemEvents(this@AgendaWallpaperService, ::onSystemEvent)
        private val inputs = MutableStateFlow(readInputs())

        private var settings: Settings? = null
        private var items: List<AgendaItem>? = null
        private var visible = false
        private var width = 0
        private var height = 0
        private var frames = 0L
        private var colorsBackground: Int? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            // Taps open items in a later phase; until then the wallpaper ignores touches entirely.
            setTouchEventsEnabled(false)
            events.register()
            scope.launch {
                graph.settings.settings.collect { next ->
                    val previousPrivacy = settings?.privacyMode
                    settings = next
                    if (previousPrivacy != null && previousPrivacy != next.privacyMode) refreshCache(next)
                    onChange(RedrawReason.SETTINGS_CHANGED)
                }
            }
            scope.launch {
                observeItems().collect {
                    items = it
                    onChange(RedrawReason.DATA_CHANGED)
                }
            }
        }

        override fun onDestroy() {
            events.unregister()
            scope.cancel()
            frame.release()
            engines -= this
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                inputs.value = readInputs()
                onChange(RedrawReason.BECAME_VISIBLE)
            } else {
                recordDiagnostics()
                frame.release()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int,
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            if (visible) draw()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            frame.release()
            super.onSurfaceDestroyed(holder)
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? =
            colorsBackground?.let { WallpaperColors(Color.valueOf(it), null, null) }

        /** Font size, language or dark theme changed: text and dynamic colors may differ. */
        fun onDeviceConfigurationChanged() = onChange(RedrawReason.CONFIGURATION_CHANGED)

        private fun onSystemEvent(event: SystemEvent) {
            when (event) {
                SystemEvent.TIME -> {
                    inputs.value = readInputs()
                    onChange(RedrawReason.TIME_CHANGED)
                }

                SystemEvent.LOCK_STATE -> {
                    onChange(RedrawReason.LOCK_STATE_CHANGED)
                }

                SystemEvent.USER_UNLOCKED -> {
                    inputs.value = readInputs()
                }
            }
        }

        private fun onChange(reason: RedrawReason) {
            if (RedrawPolicy.shouldDrawNow(reason, visible)) draw()
        }

        /**
         * Items for the current settings and day. Before the first unlock, only the Direct Boot cache is
         * readable; afterwards the live sources are observed and every result refreshes the cache.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeItems(): Flow<List<AgendaItem>> =
            combine(graph.settings.settings, inputs) { settings, inputs ->
                FeedKey.of(
                    settings = settings,
                    today = inputs.today,
                    zone = inputs.zone,
                    localeFirstDayOfWeek = graph.requestFactory.localeFirstDayOfWeek(),
                    unlocked = inputs.unlocked,
                    calendarPermission = inputs.calendarPermission,
                )
            }.distinctUntilChanged()
                .flatMapLatest { key ->
                    if (key.unlocked) {
                        graph.feed.observe(key).onEach { list ->
                            settings?.let { graph.directBootCache.write(list, it.privacyMode) }
                        }
                    } else {
                        flow { emit(graph.directBootCache.read()) }
                    }
                }

        /**
         * Rewrites the Direct Boot cache under the new privacy mode, so titles appear or disappear there too
         * (a privacy change alone does not restart observation, which is what normally refreshes the cache).
         */
        private suspend fun refreshCache(next: Settings) {
            val current = items ?: return
            if (graph.deviceState.isUserUnlocked()) graph.directBootCache.write(current, next.privacyMode)
        }

        private fun readInputs(): EngineInputs {
            val zone = TimeZone.currentSystemDefault()
            return EngineInputs(
                today =
                    Clock.System
                        .now()
                        .toLocalDateTime(zone)
                        .date,
                zone = zone,
                unlocked = graph.deviceState.isUserUnlocked(),
                calendarPermission = graph.deviceState.hasCalendarPermission(),
            )
        }

        private fun draw() {
            val request = currentRequest() ?: return
            val bitmap = frame.update(request)
            frames++
            lockCanvas()?.let { canvas ->
                try {
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                } finally {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
            frame.currentScene?.background?.let(::updateColors)
        }

        /** The request for the current state, or `null` until settings, data and a surface are all present. */
        private fun currentRequest(): RenderRequest? {
            val currentSettings = settings
            val currentItems = items
            val hasSurface = width > 0 && height > 0
            return if (currentSettings != null && currentItems != null && hasSurface) {
                graph.requestFactory.create(
                    items = currentItems,
                    settings = currentSettings,
                    widthPx = width,
                    heightPx = height,
                    isLocked = graph.deviceState.isLocked(),
                )
            } else {
                null
            }
        }

        /** A hardware canvas for the surface, or `null` if the surface is gone (e.g. mid-teardown). */
        private fun lockCanvas(): Canvas? = runCatching { surfaceHolder.lockHardwareCanvas() }.getOrNull()

        /** Tells the system our dominant color, so it can pick readable status bar and clock colors. */
        private fun updateColors(background: Int) {
            if (background == colorsBackground) return
            colorsBackground = background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) notifyColorsChanged()
        }

        private fun recordDiagnostics() {
            val stats = frame.lastStats ?: return
            val count = frames
            graph.processScope.launch { graph.diagnostics.recordLive(stats, count) }
        }
    }

    /** What changes the observed data besides settings. */
    private data class EngineInputs(
        val today: LocalDate,
        val zone: TimeZone,
        val unlocked: Boolean,
        val calendarPermission: Boolean,
    )
}
