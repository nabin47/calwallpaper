package io.github.nabin47.calwallpaper.wallpaper.diagnostics

import android.content.Context
import android.os.Debug
import io.github.nabin47.calwallpaper.wallpaper.render.FrameStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Performance numbers measured on the device by the process that produced them (CLAUDE.md §5: measure, do not
 * guess). Contains no user content.
 *
 * @property layoutMicros time to lay out the last frame.
 * @property drawMicros time to paint it into the bitmap (0 when the cached bitmap was reused).
 * @property bitmapBytes memory held by the frame bitmap.
 * @property pssKb proportional set size of the reporting process when it was last idle, or `null` if not yet
 *   measured.
 * @property frames frames laid out since the process started.
 * @property measuredAtEpochMs when the snapshot was taken.
 */
@Serializable
data class PerfSnapshot(
    val layoutMicros: Long,
    val drawMicros: Long,
    val bitmapBytes: Int,
    val pssKb: Long? = null,
    val frames: Long = 0,
    val measuredAtEpochMs: Long = 0,
)

/** The latest snapshots, one per producer. */
@Serializable
data class PerfReport(
    val liveWallpaper: PerfSnapshot? = null,
    val staticImage: PerfSnapshot? = null,
)

/**
 * Stores [PerfReport]s in device-protected storage, written by the wallpaper process (or the static-image job)
 * and read by the settings screen. Writes happen only when the wallpaper becomes invisible, never per frame.
 * PSS is measured at most every [PSS_INTERVAL_MS] because reading it costs a few milliseconds.
 */
class Diagnostics(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val file = File(context.createDeviceProtectedStorageContext().filesDir, RELATIVE_PATH)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var lastPssAtMs = 0L

    @Volatile
    private var lastPssKb: Long? = null

    /** The stored report, or an empty one. */
    suspend fun read(): PerfReport =
        withContext(ioDispatcher) {
            runCatching {
                if (file.exists()) json.decodeFromString(PerfReport.serializer(), file.readText()) else null
            }.getOrNull() ?: PerfReport()
        }

    /** Records the live wallpaper's latest frame. Measures PSS if the last measurement is old. */
    suspend fun recordLive(
        stats: FrameStats,
        frames: Long,
    ) = update { it.copy(liveWallpaper = snapshot(stats, frames)) }

    /** Records the static image's latest render. */
    suspend fun recordStatic(stats: FrameStats) = update { it.copy(staticImage = snapshot(stats, frames = 1)) }

    private fun snapshot(
        stats: FrameStats,
        frames: Long,
    ): PerfSnapshot {
        val now = System.currentTimeMillis()
        if (now - lastPssAtMs >= PSS_INTERVAL_MS) {
            lastPssKb = Debug.getPss()
            lastPssAtMs = now
        }
        return PerfSnapshot(
            layoutMicros = stats.layoutNanos / NANOS_PER_MICRO,
            drawMicros = stats.drawNanos / NANOS_PER_MICRO,
            bitmapBytes = stats.bitmapBytes,
            pssKb = lastPssKb,
            frames = frames,
            measuredAtEpochMs = now,
        )
    }

    private suspend fun update(transform: (PerfReport) -> PerfReport) {
        val next = transform(read())
        withContext(ioDispatcher) {
            runCatching {
                file.parentFile?.mkdirs()
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(json.encodeToString(PerfReport.serializer(), next))
                check(temp.renameTo(file)) { "rename failed" }
            }
        }
    }

    private companion object {
        const val RELATIVE_PATH = "wallpaper/diagnostics.json"
        const val PSS_INTERVAL_MS = 10 * 60 * 1000L
        const val NANOS_PER_MICRO = 1_000L
    }
}
