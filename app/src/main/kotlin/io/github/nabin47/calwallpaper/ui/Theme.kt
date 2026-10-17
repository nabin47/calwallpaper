package io.github.nabin47.calwallpaper.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * App theme: Material 3 Expressive (ADR 0009), with its springy motion scheme. Colors are Material You (from the
 * user's wallpaper) on Android 12+, and a calm navy scheme built from the launcher icon's color below that.
 * Follows the system dark mode.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CalWallpaperTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            fallbackColors(dark)
        }
    MaterialExpressiveTheme(colorScheme = colors, motionScheme = MotionScheme.expressive(), content = content)
}

/** Navy, from the launcher icon (`#1F3A5F`), for Android 8–11 where Material You colors do not exist. */
private fun fallbackColors(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = Color(PALE_NAVY),
            onPrimary = Color(DEEP_NAVY),
            primaryContainer = Color(MID_NAVY),
            onPrimaryContainer = Color(PALE_BLUE),
        )
    } else {
        lightColorScheme(
            primary = Color(NAVY),
            onPrimary = Color(WHITE),
            primaryContainer = Color(PALE_BLUE),
            onPrimaryContainer = Color(DEEP_NAVY),
        )
    }

private const val NAVY = 0xFF1F3A5F
private const val DEEP_NAVY = 0xFF0E2F52
private const val MID_NAVY = 0xFF2B4A70
private const val PALE_NAVY = 0xFFA9C7F0
private const val PALE_BLUE = 0xFFD5E3FF
private const val WHITE = 0xFFFFFFFF
