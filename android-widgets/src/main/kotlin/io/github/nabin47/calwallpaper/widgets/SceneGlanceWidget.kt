package io.github.nabin47.calwallpaper.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.Text
import io.github.nabin47.calwallpaper.core.OpenTargetLink
import kotlin.math.abs

/**
 * A home-screen widget that shows the same Scene as the wallpaper (CLAUDE.md §4): the [kind] layout is drawn
 * into a bitmap at the widget's exact size, and transparent tap areas are placed over the Scene's hit regions.
 * A tap opens the app's router with the region's [OpenTargetLink], which picks the source app at tap time.
 * Tapping anywhere else opens the app.
 *
 * Widgets are the always-interactive surface: unlike wallpaper taps, they do not depend on the launcher
 * (CLAUDE.md §2). Glance runs in the app's main process, never in the wallpaper process.
 */
abstract class SceneGlanceWidget(
    private val kind: WidgetKind,
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    /** Where frames come from; the app module provides it. */
    protected abstract fun frameSource(context: Context): WidgetFrameSource

    /**
     * Renders one frame per size the launcher reported, then shows the one for the size being composed.
     * Rendering happens before composition because it is a suspending, off-main-thread job.
     */
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val sizes = GlanceAppWidgetManager(context).getAppWidgetSizes(id).distinct().ifEmpty { listOf(FALLBACK_SIZE) }
        val source = frameSource(context)
        val frames = sizes.associateWith { source.frame(kind, it.width.value, it.height.value) }
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)
        provideContent {
            WidgetBody(frames.forSize(LocalSize.current), openApp, context.getString(kind.label))
        }
    }

    private companion object {
        /** Used when the launcher has not reported a size yet: about a 3 × 2 cell widget. */
        val FALLBACK_SIZE = DpSize(250.dp, 110.dp)
    }
}

/** The frame rendered for [size], or the closest one if the launcher has resized the widget since. */
private fun Map<DpSize, WidgetFrame?>.forSize(size: DpSize): WidgetFrame? =
    get(size) ?: entries
        .filter { it.value != null }
        .minByOrNull { (key, _) -> abs(key.width.value - size.width.value) + abs(key.height.value - size.height.value) }
        ?.value

@Composable
private fun WidgetBody(
    frame: WidgetFrame?,
    openApp: Intent?,
    description: String,
) {
    val root = GlanceModifier.fillMaxSize()
    Box(modifier = if (openApp != null) root.clickable(actionStartActivity(openApp)) else root) {
        if (frame == null) {
            Text(text = LocalContext.current.getString(R.string.widget_unavailable))
        } else {
            Image(
                provider = ImageProvider(frame.bitmap),
                contentDescription = description,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            // Glance keeps at most 10 children per container, so tap areas are grouped.
            frame.taps.chunked(TAPS_PER_GROUP).forEach { group ->
                Box(modifier = GlanceModifier.fillMaxSize()) { group.forEach { TapArea(it) } }
            }
        }
    }
}

/** A transparent tap area, positioned by padding inside a full-size box (Glance has no absolute offsets). */
@Composable
private fun TapArea(tap: WidgetTap) {
    val context = LocalContext.current
    // Explicit component: the router is not exported. The link as data keeps every PendingIntent distinct.
    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(tap.link)).setClassName(context.packageName, OpenTargetLink.ROUTER_CLASS)
    Box(modifier = GlanceModifier.fillMaxSize().padding(start = tap.leftDp.dp, top = tap.topDp.dp)) {
        Box(
            modifier =
                GlanceModifier
                    .size(tap.widthDp.dp, tap.heightDp.dp)
                    .clickable(actionStartActivity(intent))
                    .semantics { contentDescription = tap.label },
        ) {}
    }
}

private const val TAPS_PER_GROUP = 10
