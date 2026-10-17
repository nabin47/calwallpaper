package io.github.nabin47.calwallpaper.wallpaper.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import io.github.nabin47.calwallpaper.render.LayoutEngine
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.render.Scene

/**
 * Lays out a [RenderRequest] and keeps the result in **one** screen-sized bitmap (CLAUDE.md §5). The bitmap is
 * redrawn only when the [Scene] differs from the one it holds, so becoming visible again with unchanged data
 * costs a layout and a copy, not a repaint. Call [release] when the wallpaper becomes invisible.
 *
 * Paints, fonts and the measurer are created once and reused. Not thread-safe: use from one thread (the
 * wallpaper engine's main thread, or one background job for the static image).
 */
class WallpaperFrame(
    textPaints: TextPaints = TextPaints(),
) {
    private val layoutEngine = LayoutEngine(AndroidTextMeasurer(textPaints))
    private val renderer = SceneCanvasRenderer(textPaints)
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var scene: Scene? = null

    /** Timing of the most recent [update], for diagnostics. */
    var lastStats: FrameStats? = null
        private set

    /** The scene currently in the bitmap, or `null` after [release]. Used for tap hit-testing later. */
    val currentScene: Scene? get() = scene

    /**
     * Brings the bitmap up to date with [request]. Returns the bitmap, which stays owned by this frame: do not
     * recycle it, and do not keep it after the next [update] or [release].
     */
    fun update(request: RenderRequest): Bitmap {
        val layoutStart = SystemClock.elapsedRealtimeNanos()
        val next = layoutEngine.layout(request)
        val layoutNanos = SystemClock.elapsedRealtimeNanos() - layoutStart
        val target = bitmapFor(next.widthPx, next.heightPx)
        var drawNanos = 0L
        if (next != scene) {
            val drawStart = SystemClock.elapsedRealtimeNanos()
            paint(target, next)
            drawNanos = SystemClock.elapsedRealtimeNanos() - drawStart
            scene = next
        }
        lastStats =
            FrameStats(layoutNanos = layoutNanos, drawNanos = drawNanos, bitmapBytes = target.allocationByteCount)
        return target
    }

    /** Frees the bitmap. The next [update] allocates a new one. */
    fun release() {
        bitmap?.recycle()
        bitmap = null
        canvas = null
        scene = null
    }

    /** Reuses the bitmap when the size matches; otherwise replaces it, so at most one exists at a time. */
    private fun bitmapFor(
        width: Int,
        height: Int,
    ): Bitmap {
        val current = bitmap
        if (current != null && current.fits(width, height)) return current
        current?.recycle()
        val created = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap = created
        canvas = Canvas(created)
        scene = null
        return created
    }

    private fun Bitmap.fits(
        width: Int,
        height: Int,
    ): Boolean = !isRecycled && this.width == width && this.height == height

    private fun paint(
        target: Bitmap,
        next: Scene,
    ) {
        val drawCanvas = canvas ?: Canvas(target).also { canvas = it }
        drawCanvas.drawColor(next.background)
        renderer.draw(drawCanvas, next)
    }
}

/** How long the last frame took and how much memory its bitmap holds. */
data class FrameStats(
    val layoutNanos: Long,
    val drawNanos: Long,
    val bitmapBytes: Int,
)
