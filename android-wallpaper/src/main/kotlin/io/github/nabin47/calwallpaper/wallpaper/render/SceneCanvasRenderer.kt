package io.github.nabin47.calwallpaper.wallpaper.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.github.nabin47.calwallpaper.render.DrawOp
import io.github.nabin47.calwallpaper.render.Scene

/**
 * Draws a [Scene] onto an Android [Canvas]. Used by the wallpaper engine, the static-wallpaper bitmap and the
 * screenshot tests. Paints and the rect are reused, so drawing allocates nothing (CLAUDE.md §5).
 * Call from the thread that owns the canvas; not thread-safe.
 */
class SceneCanvasRenderer(
    private val textPaints: TextPaints = TextPaints(),
) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val rect = RectF()

    /** Paints every op of [scene] in order. Does not clear the canvas: the wallpaper below stays visible. */
    fun draw(
        canvas: Canvas,
        scene: Scene,
    ) {
        val ops = scene.ops
        for (i in ops.indices) {
            when (val op = ops[i]) {
                is DrawOp.RoundRect -> drawRoundRect(canvas, op)
                is DrawOp.Text -> drawText(canvas, op)
                is DrawOp.Circle -> drawCircle(canvas, op)
            }
        }
    }

    private fun drawRoundRect(
        canvas: Canvas,
        op: DrawOp.RoundRect,
    ) {
        rect.set(op.bounds.left, op.bounds.top, op.bounds.right, op.bounds.bottom)
        fill.color = op.color
        canvas.drawRoundRect(rect, op.cornerRadius, op.cornerRadius, fill)
    }

    private fun drawText(
        canvas: Canvas,
        op: DrawOp.Text,
    ) {
        textPaints.apply(text, op.style)
        text.color = op.color
        canvas.drawText(op.text, op.x, op.baseline, text)
    }

    private fun drawCircle(
        canvas: Canvas,
        op: DrawOp.Circle,
    ) {
        val paint =
            if (op.strokeWidth > 0f) {
                stroke.strokeWidth = op.strokeWidth
                stroke
            } else {
                fill
            }
        paint.color = op.color
        canvas.drawCircle(op.centerX, op.centerY, op.radius, paint)
    }
}
