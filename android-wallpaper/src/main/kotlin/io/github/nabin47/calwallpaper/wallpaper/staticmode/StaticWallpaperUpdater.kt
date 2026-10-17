package io.github.nabin47.calwallpaper.wallpaper.staticmode

import android.app.WallpaperManager
import android.content.Context
import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.LayoutEngine
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import io.github.nabin47.calwallpaper.wallpaper.feed.FeedKey
import io.github.nabin47.calwallpaper.wallpaper.render.AndroidTextMeasurer
import io.github.nabin47.calwallpaper.wallpaper.render.WallpaperFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Renders the agenda to a bitmap and sets it as the home and/or lock wallpaper (static battery mode, CLAUDE.md
 * §6). Uses the same Scene pipeline as the live wallpaper. Skips `setBitmap` when the picture is unchanged, so a
 * calendar sync that changes nothing visible costs no wallpaper update.
 *
 * Runs in the app's main process (jobs, receivers and the settings screen), never in the wallpaper process.
 * Concurrent calls are serialized. Requires `SET_WALLPAPER` (ADR 0004).
 */
class StaticWallpaperUpdater(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val graph = WallpaperGraph.get(appContext)
    private val fingerprintFile = File(appContext.createDeviceProtectedStorageContext().filesDir, FINGERPRINT_PATH)

    /**
     * Re-renders and applies the static wallpaper if battery mode is static. [force] applies even an unchanged
     * picture (the user pressed "Apply", or another app may have replaced the wallpaper).
     */
    suspend fun refresh(force: Boolean = false): Outcome<Unit> =
        LOCK.withLock {
            val settings = graph.settings.settings.first()
            val manager = WallpaperManager.getInstance(appContext)
            when {
                settings.batteryMode != BatteryMode.STATIC -> {
                    Outcome.Failure("Battery mode is live")
                }

                !graph.deviceState.isUserUnlocked() -> {
                    Outcome.Failure("Device not unlocked yet")
                }

                !manager.isWallpaperSupported || !manager.isSetWallpaperAllowed -> {
                    Outcome.Failure("Setting the wallpaper is not allowed on this device")
                }

                else -> {
                    apply(manager, settings, loadItems(settings), force)
                }
            }
        }

    private suspend fun loadItems(settings: Settings): List<AgendaItem> {
        val zone = TimeZone.currentSystemDefault()
        val key =
            FeedKey.of(
                settings = settings,
                today =
                    Clock.System
                        .now()
                        .toLocalDateTime(zone)
                        .date,
                zone = zone,
                localeFirstDayOfWeek = graph.requestFactory.localeFirstDayOfWeek(),
                unlocked = true,
                calendarPermission = graph.deviceState.hasCalendarPermission(),
            )
        return withTimeoutOrNull(LOAD_TIMEOUT) { graph.feed.observe(key).first() }.orEmpty()
    }

    private suspend fun apply(
        manager: WallpaperManager,
        settings: Settings,
        items: List<AgendaItem>,
        force: Boolean,
    ): Outcome<Unit> =
        withContext(Dispatchers.IO) {
            val size = ScreenSize.portrait(appContext)
            val requests =
                StaticPlan.images(settings.staticTarget, settings.privacyMode, settings.layout).map { image ->
                    image to graph.requestFactory.create(items, settings, size.width, size.height, image.locked)
                }
            val fingerprint = fingerprintOf(requests)
            if (!force && fingerprint == readFingerprint()) return@withContext Outcome.Success(Unit)
            val frame = WallpaperFrame()
            try {
                requests.forEach { (image, request) ->
                    manager.setBitmap(frame.update(request), null, true, image.screens.toFlags())
                }
                frame.lastStats?.let { graph.diagnostics.recordStatic(it) }
                writeFingerprint(fingerprint)
                Outcome.Success(Unit)
            } catch (e: IOException) {
                Outcome.Failure("Could not set the wallpaper (${e::class.simpleName})")
            } finally {
                frame.release()
            }
        }

    /**
     * A digest of what would be drawn on which screens. Only the digest is stored, never the scene, so no titles
     * reach storage. Layout is cheap, so laying out twice beats keeping extra bitmaps around.
     */
    private fun fingerprintOf(requests: List<Pair<StaticImage, RenderRequest>>): String {
        val engine = LayoutEngine(AndroidTextMeasurer())
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        requests.forEach { (image, request) ->
            digest.update("${image.screens}|${engine.layout(request)}".toByteArray())
        }
        return BigInteger(1, digest.digest()).toString(HEX_RADIX)
    }

    private fun Set<Screen>.toFlags(): Int =
        fold(0) { flags, screen ->
            flags or
                when (screen) {
                    Screen.HOME -> WallpaperManager.FLAG_SYSTEM
                    Screen.LOCK -> WallpaperManager.FLAG_LOCK
                }
        }

    private fun readFingerprint(): String? = runCatching { fingerprintFile.readText() }.getOrNull()

    private fun writeFingerprint(value: String) {
        runCatching {
            fingerprintFile.parentFile?.mkdirs()
            fingerprintFile.writeText(value)
        }
    }

    /** Forgets what was applied, so the next refresh sets the wallpaper even if the picture is unchanged. */
    fun forgetApplied() {
        fingerprintFile.delete()
    }

    private companion object {
        val LOCK = Mutex()
        val LOAD_TIMEOUT = 10.seconds
        const val DIGEST_ALGORITHM = "SHA-256"
        const val HEX_RADIX = 16
        const val FINGERPRINT_PATH = "wallpaper/static_fingerprint.txt"
    }
}
