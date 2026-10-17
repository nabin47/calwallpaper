package io.github.nabin47.calwallpaper.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import io.github.nabin47.calwallpaper.wallpaper.staticmode.StaticModeScheduler

/** The small agenda widget. */
class AgendaWidget : SceneGlanceWidget(WidgetKind.AGENDA) {
    override fun frameSource(context: Context): WidgetFrameSource = WidgetFrames(context)
}

/** The week strip widget. */
class WeekWidget : SceneGlanceWidget(WidgetKind.WEEK) {
    override fun frameSource(context: Context): WidgetFrameSource = WidgetFrames(context)
}

/**
 * Base receiver: when the first widget of a kind is added or the last removed, the background refresh triggers
 * are re-synced, so they run only while widgets (or static mode) need them.
 */
abstract class CalWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        StaticModeScheduler(context).onWidgetAdded()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        StaticModeScheduler(context).onWidgetRemoved()
    }
}

/** Receiver of [AgendaWidget], declared in the manifest. */
class AgendaWidgetReceiver : CalWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

/** Receiver of [WeekWidget], declared in the manifest. */
class WeekWidgetReceiver : CalWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekWidget()
}
