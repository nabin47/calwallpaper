package io.github.nabin47.calwallpaper.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import io.github.nabin47.calwallpaper.render.LayoutEngine
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import io.github.nabin47.calwallpaper.wallpaper.feed.currentAgenda
import io.github.nabin47.calwallpaper.wallpaper.render.AndroidTextMeasurer
import io.github.nabin47.calwallpaper.wallpaper.render.SceneCanvasRenderer
import io.github.nabin47.calwallpaper.wallpaper.render.TextPaints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Renders widget frames with the wallpaper's pipeline: the same agenda, settings, [LayoutEngine] and Canvas
 * renderer (CLAUDE.md §4), laid out with the widget's preset ([widgetSettings]). The panel is drawn without the
 * wallpaper background, so the launcher shows through around it. Widgets are on the home screen, which is
 * unlocked, so privacy mode "when locked" shows titles and "always" hides them, as on the wallpaper.
 *
 * Runs in the app's main process; work happens on [Dispatchers.Default].
 */
class WidgetFrames(
    context: Context,
) : WidgetFrameSource {
    private val appContext = context.applicationContext
    private val graph = WallpaperGraph.get(appContext)

    override suspend fun frame(
        kind: WidgetKind,
        widthDp: Float,
        heightDp: Float,
    ): WidgetFrame =
        withContext(Dispatchers.Default) {
            val settings = widgetSettings(graph.settings.settings.first(), kind)
            val items = graph.currentAgenda(settings)
            val metrics = appContext.resources.displayMetrics
            val canvas =
                WidgetCanvas.of(
                    widthDp = widthDp,
                    heightDp = heightDp,
                    density = metrics.density,
                    maxPixels = metrics.widthPixels.toLong() * metrics.heightPixels / 2,
                )
            val base = graph.requestFactory.create(items, settings, canvas.widthPx, canvas.heightPx, isLocked = false)
            val request = base.copy(screen = base.screen.copy(density = canvas.pxPerDp))
            val paints = TextPaints()
            val scene = LayoutEngine(AndroidTextMeasurer(paints)).layout(request)
            val bitmap = Bitmap.createBitmap(canvas.widthPx, canvas.heightPx, Bitmap.Config.ARGB_8888)
            SceneCanvasRenderer(paints).draw(Canvas(bitmap), scene)
            WidgetFrame(bitmap, WidgetTaps.from(scene.hitRegions, canvas.pxPerDp))
        }
}
