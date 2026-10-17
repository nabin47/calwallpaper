package io.github.nabin47.calwallpaper.wallpaper.render

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import io.github.nabin47.calwallpaper.render.theme.Palette

/**
 * Material You (dynamic color) palette from the system's tonal palettes, Android 12+. Follows the system's dark
 * theme: light text on a dark panel at night, dark text on a light panel by day. Reads colors on each call, so a
 * new system wallpaper or theme applies at the next layout.
 */
object SystemPalette {
    /** The current dynamic palette. */
    @TargetApi(Build.VERSION_CODES.S)
    fun read(context: Context): Palette {
        val uiMode = context.resources.configuration.uiMode
        val night = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (night) dark(context) else light(context)
    }

    @TargetApi(Build.VERSION_CODES.S)
    private fun dark(context: Context): Palette =
        Palette(
            panel = context.getColor(android.R.color.system_neutral1_900),
            text = context.getColor(android.R.color.system_neutral1_50),
            secondaryText = context.getColor(android.R.color.system_neutral2_200),
            accent = context.getColor(android.R.color.system_accent1_200),
            onAccent = context.getColor(android.R.color.system_accent1_800),
            divider = context.getColor(android.R.color.system_neutral2_700),
            background = context.getColor(android.R.color.system_accent2_800),
        )

    @TargetApi(Build.VERSION_CODES.S)
    private fun light(context: Context): Palette =
        Palette(
            panel = context.getColor(android.R.color.system_neutral1_10),
            text = context.getColor(android.R.color.system_neutral1_900),
            secondaryText = context.getColor(android.R.color.system_neutral2_600),
            accent = context.getColor(android.R.color.system_accent1_600),
            onAccent = context.getColor(android.R.color.system_accent1_0),
            divider = context.getColor(android.R.color.system_neutral2_100),
            background = context.getColor(android.R.color.system_accent2_100),
        )
}
