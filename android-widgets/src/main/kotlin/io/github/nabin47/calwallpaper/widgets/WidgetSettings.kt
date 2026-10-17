package io.github.nabin47.calwallpaper.widgets

import io.github.nabin47.calwallpaper.domain.settings.LayoutSettings
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.VerticalPosition

/**
 * The user's [settings] as a [kind] widget uses them: its own layout preset, panel at the top, and none of the
 * wallpaper's safe zones or side margins (the launcher already places and pads the widget). Appearance,
 * content, time and privacy settings apply unchanged. Pure.
 */
fun widgetSettings(
    settings: Settings,
    kind: WidgetKind,
): Settings =
    settings.copy(
        layout =
            LayoutSettings(
                preset = kind.preset,
                position = VerticalPosition.TOP,
                safeZoneTop = 0f,
                safeZoneBottom = 0f,
                horizontalInsetDp = 0,
                lockPosition = VerticalPosition.TOP,
                lockSafeZoneTop = 0f,
                lockSafeZoneBottom = 0f,
            ),
    )
