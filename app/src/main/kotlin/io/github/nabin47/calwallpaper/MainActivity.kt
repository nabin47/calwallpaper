package io.github.nabin47.calwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import io.github.nabin47.calwallpaper.ui.AppRoot
import io.github.nabin47.calwallpaper.ui.CalWallpaperTheme

/**
 * Launcher entry point and the live wallpaper's settings screen. Shows onboarding on first launch, then the
 * settings with a live preview. Runs in the main process; the wallpaper itself runs in `:wallpaper`.
 */
class MainActivity : ComponentActivity() {
    /** Bumped on every resume, so screens re-check things the user may have changed elsewhere (permissions). */
    private val resumeCount = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val services = AppServices(this)
        setContent {
            CalWallpaperTheme {
                AppRoot(services = services, resumeCount = resumeCount.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount.intValue++
    }
}
