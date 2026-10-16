package io.github.nabin47.calwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.nabin47.calwallpaper.ui.CalWallpaperTheme
import io.github.nabin47.calwallpaper.ui.HelloScreen

/** Launcher entry point. Phase 0 shows a static hello screen; settings and onboarding replace it later. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CalWallpaperTheme {
                HelloScreen(versionName = BuildConfig.VERSION_NAME)
            }
        }
    }
}
