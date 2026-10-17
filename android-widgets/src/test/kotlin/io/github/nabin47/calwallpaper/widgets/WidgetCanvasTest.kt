package io.github.nabin47.calwallpaper.widgets

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WidgetCanvasTest {
    @Test
    fun smallWidgetsUseFullDensity() {
        assertEquals(WidgetCanvas(700, 385, 2.8f), WidgetCanvas.of(250f, 137.5f, 2.8f, maxPixels = 1_000_000))
    }

    @Test
    fun hugeWidgetsAreScaledDownToTheBudget() {
        val canvas = WidgetCanvas.of(400f, 900f, 3f, maxPixels = 1_000_000)
        assertTrue(canvas.widthPx.toLong() * canvas.heightPx <= 1_002_000, "$canvas")
        assertEquals(canvas.widthPx / 400f, canvas.pxPerDp, 0.01f)
        assertEquals(canvas.heightPx / 900f, canvas.pxPerDp, 0.01f)
    }

    @Test
    fun degenerateSizesStillGiveAPixel() {
        val canvas = WidgetCanvas.of(0f, 0f, 2f, maxPixels = 0)
        assertTrue(canvas.widthPx >= 1 && canvas.heightPx >= 1 && canvas.pxPerDp > 0f)
    }
}
