package io.github.nabin47.calwallpaper.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code the release app runs on its hot paths, so Android compiles it ahead of time (CLAUDE.md §5):
 * app start, the first-run steps, scrolling the settings with the live preview, and the live wallpaper drawing in
 * the system's preview, which runs the wallpaper process's layout and renderer.
 *
 * Runs on an emulator only: `./gradlew :app:generateBaselineProfile` (see the module README). Never touches real
 * accounts or data; calendar access is not granted, so the preview shows the sample agenda.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    /** Runs the journey repeatedly until the recorded profile is stable, then writes it out. */
    @get:Rule
    val rule = BaselineProfileRule()

    /** The journey; it also feeds the startup profile, so the first screen's code is laid out first in the dex. */
    @Test
    fun generate() =
        rule.collect(packageName = APP_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            finishOnboardingIfShown()
            scrollSettings()
            previewLiveWallpaper()
        }

    /**
     * The first run shows onboarding: "Next", "Not now" (no calendar access), then "Open settings" at the end of
     * the last, scrollable step. Later iterations go straight to the settings and skip all of this.
     */
    private fun MacrobenchmarkScope.finishOnboardingIfShown() {
        val next = device.wait(Until.findObject(By.text(NEXT)), STEP_TIMEOUT_MS) ?: return
        next.click()
        device.wait(Until.findObject(By.text(NOT_NOW)), STEP_TIMEOUT_MS)?.click()
        flingScrollable(down = true)
        device.wait(Until.findObject(By.text(OPEN_SETTINGS)), STEP_TIMEOUT_MS)?.click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.scrollSettings() {
        repeat(FLINGS) { flingScrollable(down = true) }
        repeat(FLINGS) { flingScrollable(down = false) }
    }

    /** Flings the tallest scrollable (the settings list, not a sideways row of chips). */
    private fun MacrobenchmarkScope.flingScrollable(down: Boolean) {
        if (!device.wait(Until.hasObject(By.scrollable(true)), STEP_TIMEOUT_MS)) return
        val list = device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() } ?: return
        // Keep the gesture away from the system's edge-swipe areas.
        list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        list.fling(if (down) Direction.DOWN else Direction.UP)
        device.waitForIdle()
    }

    /**
     * Opens the system's live wallpaper preview for this app's wallpaper. The preview runs the real engine in
     * the `:wallpaper` process. If the image has no live wallpaper picker, `am` reports an error and this step
     * adds nothing.
     */
    private fun MacrobenchmarkScope.previewLiveWallpaper() {
        device.executeShellCommand(
            "am start -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER " +
                "--ecn android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT $APP_PACKAGE/$WALLPAPER_SERVICE",
        )
        device.wait(Until.gone(By.pkg(APP_PACKAGE).depth(0)), STEP_TIMEOUT_MS)
        device.waitForIdle()
        Thread.sleep(WALLPAPER_SETTLE_MS)
        pressHome()
    }

    private companion object {
        const val APP_PACKAGE = "io.github.nabin47.calwallpaper"
        const val WALLPAPER_SERVICE = "io.github.nabin47.calwallpaper.wallpaper.AgendaWallpaperService"

        // Button labels from app/src/main/res/values/strings.xml (the emulator runs in English).
        const val NEXT = "Next"
        const val NOT_NOW = "Not now"
        const val OPEN_SETTINGS = "Open settings"

        const val STEP_TIMEOUT_MS = 3_000L
        const val WALLPAPER_SETTLE_MS = 3_000L
        const val FLINGS = 3
        const val GESTURE_MARGIN_DIVISOR = 5
    }
}
