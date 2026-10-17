package io.github.nabin47.calwallpaper.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.render.LayoutEngine
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.wallpaper.render.AndroidTextMeasurer
import io.github.nabin47.calwallpaper.wallpaper.render.SceneCanvasRenderer
import io.github.nabin47.calwallpaper.wallpaper.render.TextPaints

/**
 * The wallpaper as it will look, drawn by the same layout engine and Canvas renderer as the live wallpaper
 * (CLAUDE.md §6: every setting shows in the live preview). [request] is laid out at full screen size and scaled
 * down, so text wraps and overflows exactly as on the real wallpaper. Re-lays out only when [request] changes.
 */
@Composable
fun LivePreview(
    request: RenderRequest,
    description: String,
    modifier: Modifier = Modifier,
) {
    val textPaints = remember { TextPaints() }
    val engine = remember { LayoutEngine(AndroidTextMeasurer(textPaints)) }
    val renderer = remember { SceneCanvasRenderer(textPaints) }
    val scene = remember(request) { engine.layout(request) }
    Canvas(
        modifier =
            modifier
                .aspectRatio(scene.widthPx.toFloat() / scene.heightPx, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(16.dp))
                .semantics { contentDescription = description },
    ) {
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val scale = size.width / scene.widthPx
            native.save()
            native.scale(scale, scale)
            native.drawColor(scene.background)
            renderer.draw(native, scene)
            native.restore()
        }
    }
}
