package io.github.nabin47.calwallpaper.wallpaper.render

import android.graphics.Bitmap
import android.graphics.Canvas
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.LayoutEngine
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.render.ScreenSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every layout preset in light and dark, with a busy week (long titles, completed task) and empty,
 * through the real Android Canvas renderer, and compares against goldens in `src/test/screenshots/`.
 *
 * Record goldens with `./gradlew recordRoborazziDebug` (CI: "Record screenshots" workflow); verify with
 * `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SceneScreenshotTest(
    private val case: Case,
) {
    /** One screenshot. */
    data class Case(
        val preset: LayoutPreset,
        val dark: Boolean,
        val empty: Boolean,
        val locked: Boolean = false,
    ) {
        /** File name, e.g. `agenda_dark_empty`. */
        val name: String =
            listOfNotNull(
                preset.name.lowercase(),
                if (dark) "dark" else "light",
                "empty".takeIf { empty },
                "locked".takeIf { locked },
            ).joinToString("_")

        override fun toString(): String = name
    }

    @Test
    fun screenshot() {
        val settings =
            Settings().let { base ->
                base.copy(
                    appearance =
                        base.appearance.copy(
                            palette = if (case.dark) PalettePreset.CALM_DARK else PalettePreset.CALM_LIGHT,
                        ),
                    layout = base.layout.copy(preset = case.preset),
                    content = base.content.copy(showCompleted = true),
                    privacyMode = PrivacyMode.WHEN_LOCKED,
                )
            }
        val request =
            RenderRequest(
                items = if (case.empty) emptyList() else ScreenshotFixtures.items,
                settings = settings,
                screen = ScreenSpec(WIDTH, HEIGHT, DENSITY),
                now = ScreenshotFixtures.now,
                zone = ScreenshotFixtures.zone,
                isLocked = case.locked,
            )
        val scene = LayoutEngine(AndroidTextMeasurer()).layout(request)
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Stand-in for the user's wallpaper, so panel translucency is visible.
        canvas.drawColor(if (case.dark) DARK_BACKDROP else LIGHT_BACKDROP)
        SceneCanvasRenderer().draw(canvas, scene)
        bitmap.captureRoboImage("src/test/screenshots/${case.name}.png")
    }

    companion object {
        private const val WIDTH = 720
        private const val HEIGHT = 1600
        private const val DENSITY = 2f
        private const val DARK_BACKDROP = 0xFF2B3440.toInt()
        private const val LIGHT_BACKDROP = 0xFFC9D6E3.toInt()

        /** Every preset x light/dark x busy/empty, plus privacy mode on the locked agenda. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val matrix =
                LayoutPreset.entries.flatMap { preset ->
                    listOf(false, true).flatMap { dark ->
                        listOf(false, true).map { empty -> Case(preset, dark, empty) }
                    }
                }
            val extra = listOf(Case(LayoutPreset.AGENDA, dark = true, empty = false, locked = true))
            return (matrix + extra).map { arrayOf<Any>(it) }
        }
    }
}
