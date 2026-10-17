package io.github.nabin47.calwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.nabin47.calwallpaper.ui.AppRoot
import io.github.nabin47.calwallpaper.ui.CalWallpaperTheme

/**
 * Launcher entry point and the live wallpaper's settings screen. Shows the splash screen until the settings are
 * loaded (usually a few milliseconds), then onboarding on first launch, otherwise the home screen. Runs in the
 * main process; the wallpaper itself runs in `:wallpaper`.
 */
class MainActivity : ComponentActivity() {
    /** Bumped on every resume, so screens re-check things the user may have changed elsewhere (permissions). */
    private val resumeCount = mutableIntStateOf(0)

    /** Set once the first screen can be drawn; the splash screen stays up until then. Main thread only. */
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !ready }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val services = AppServices(this)
        setContent {
            CalWallpaperTheme {
                AppRoot(services = services, resumeCount = resumeCount.intValue, onReady = { ready = true })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeCount.intValue++
    }
}
