package io.github.nabin47.calwallpaper.render

import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.VerticalPosition
import io.github.nabin47.calwallpaper.render.Fixtures.LONG_TITLE
import io.github.nabin47.calwallpaper.render.Fixtures.layout
import io.github.nabin47.calwallpaper.render.Fixtures.settings
import io.github.nabin47.calwallpaper.render.Fixtures.texts
import io.github.nabin47.calwallpaper.render.Fixtures.today
import io.github.nabin47.calwallpaper.render.Fixtures.week
import io.github.nabin47.calwallpaper.render.format.EnglishRenderStrings
import io.github.nabin47.calwallpaper.render.text.ELLIPSIS
import io.github.nabin47.calwallpaper.render.theme.Palette
import io.github.nabin47.calwallpaper.render.theme.alphaOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayoutEngineTest {
    private val presets = LayoutPreset.entries

    /** Vertical extent of every op, using the fake measurer's metrics. */
    private fun DrawOp.verticalExtent(): ClosedFloatingPointRange<Float> =
        when (this) {
            is DrawOp.RoundRect -> bounds.top..bounds.bottom
            is DrawOp.Circle -> (centerY - radius)..(centerY + radius)
            is DrawOp.Text -> FakeMeasurer.metrics(style).let { (baseline - it.ascent)..(baseline + it.descent) }
        }

    @Test
    fun everyPresetStaysInsideTheSafeZone() {
        val many = week + (1..40).map { Fixtures.event("extra$it", "Extra $it", week[0].time) }
        for (preset in presets) {
            for (position in VerticalPosition.entries) {
                val base = settings(preset)
                val s =
                    base.copy(
                        layout = base.layout.copy(position = position, safeZoneTop = 0.3f, safeZoneBottom = 0.2f),
                    )
                val scene = layout(s, many)
                val top = Fixtures.screen.heightPx * 0.3f
                val bottom = Fixtures.screen.heightPx * 0.8f
                scene.ops.forEach { op ->
                    val extent = op.verticalExtent()
                    assertTrue(
                        extent.start >= top - 0.01f && extent.endInclusive <= bottom + 0.01f,
                        "$preset/$position: $op",
                    )
                }
                scene.hitRegions.forEach {
                    assertTrue(
                        it.bounds.top >= top - 0.01f && it.bounds.bottom <= bottom + 0.01f,
                    )
                }
            }
        }
    }

    @Test
    fun tinySafeAreaDrawsNothingOutsideIt() {
        for (preset in presets) {
            val base = settings(preset)
            val s = base.copy(layout = base.layout.copy(safeZoneTop = 0.5f, safeZoneBottom = 0.49f))
            val scene = layout(s)
            val top = Fixtures.screen.heightPx * 0.5f
            val bottom = Fixtures.screen.heightPx * 0.51f
            scene.ops.forEach { op ->
                val extent = op.verticalExtent()
                assertTrue(extent.start >= top - 0.01f && extent.endInclusive <= bottom + 0.01f, "$preset: $op")
            }
        }
    }

    @Test
    fun agendaOverflowShowsMoreLine() {
        val many = (1..60).map { Fixtures.event("e$it", "Event $it", week[0].time) }
        val scene = layout(settings(LayoutPreset.AGENDA), many)
        assertTrue(scene.texts().any { it.startsWith("+") && it.endsWith("more") })
    }

    @Test
    fun longTitlesAreEllipsizedWithinTheSafeWidth() {
        for (preset in presets) {
            val scene = layout(settings(preset))
            val right = Fixtures.screen.widthPx - Fixtures.screen.density * 24f
            scene.ops.filterIsInstance<DrawOp.Text>().forEach { op ->
                assertTrue(
                    op.x + FakeMeasurer.width(op.text, op.style) <= right + 0.01f,
                    "$preset: '${op.text}' overflows",
                )
            }
            assertFalse(scene.texts().contains(LONG_TITLE), "$preset drew the full long title")
        }
        assertTrue(layout(settings(LayoutPreset.AGENDA)).texts().any { it.endsWith(ELLIPSIS) })
    }

    @Test
    fun privacyModeHidesTitlesWhenLocked() {
        val whenLocked = { preset: LayoutPreset -> settings(preset, PrivacyMode.WHEN_LOCKED) }
        for (preset in presets) {
            val locked = layout(whenLocked(preset), isLocked = true).texts()
            assertFalse(locked.any { "Standup" in it || "Lunch" in it || "bill" in it }, "$preset leaked a title")
        }
        assertTrue(layout(whenLocked(LayoutPreset.AGENDA), isLocked = false).texts().contains("Standup"))
        assertTrue(layout(whenLocked(LayoutPreset.AGENDA), isLocked = true).texts().any { it.endsWith("items") })
        assertFalse(layout(settings(LayoutPreset.NEXT_THREE, PrivacyMode.ALWAYS)).texts().contains("Standup"))
    }

    @Test
    fun opacitySettingsApplyToPanelAndText() {
        val base = settings(LayoutPreset.AGENDA)
        val translucent = base.copy(appearance = base.appearance.copy(panelOpacity = 0.5f, textOpacity = 0.8f))
        val scene = layout(translucent)
        val panel = scene.ops.first() as DrawOp.RoundRect
        assertEquals(128, alphaOf(panel.color))
        val text = scene.ops.filterIsInstance<DrawOp.Text>().first()
        assertEquals(204, alphaOf(text.color))

        val noPanel = base.copy(appearance = base.appearance.copy(panelOpacity = 0f))
        assertTrue(layout(noPanel).ops.none { it is DrawOp.RoundRect && it.bounds.height > 200f })
    }

    @Test
    fun textScaleAndSystemFontScaleEnlargeText() {
        val base = settings(LayoutPreset.AGENDA)

        fun bodySize(scene: Scene) = scene.ops.filterIsInstance<DrawOp.Text>().maxOf { it.style.sizePx }
        val normal = bodySize(layout(base))
        val userScaled = bodySize(layout(base.copy(appearance = base.appearance.copy(textScale = 1.5f))))
        val systemScaled =
            bodySize(
                LayoutEngine(FakeMeasurer).layout(
                    Fixtures.request(base).copy(screen = Fixtures.screen.copy(fontScale = 1.3f)),
                ),
            )
        assertEquals(normal * 1.5f, userScaled, 0.01f)
        assertEquals(normal * 1.3f, systemScaled, 0.01f)
    }

    @Test
    fun hitTestFindsItemAndDayTargets() {
        val scene = layout(settings(LayoutPreset.AGENDA))
        val standup = scene.ops.filterIsInstance<DrawOp.Text>().first { it.text == "Standup" }
        assertEquals(OpenTarget.CalendarDay(today), scene.hitTest(standup.x + 2f, standup.baseline - 2f))
        val bill = scene.ops.filterIsInstance<DrawOp.Text>().first { it.text == "Pay electricity bill" }
        assertEquals(
            week.first { it.title == "Pay electricity bill" }.openTarget,
            scene.hitTest(
                bill.x + 2f,
                bill.baseline - 2f,
            ),
        )
        val header = scene.ops.filterIsInstance<DrawOp.Text>().first { it.text == "Today" }
        assertEquals(OpenTarget.CalendarDay(today), scene.hitTest(header.x + 2f, header.baseline - 2f))
    }

    @Test
    fun everyTapTargetHasALabel() {
        for (preset in presets) {
            val scene = layout(settings(preset))
            assertTrue(scene.hitRegions.isNotEmpty(), "$preset")
            scene.hitRegions.forEach { assertTrue(it.label.isNotBlank(), "$preset: ${it.target}") }
        }
        val agenda = layout(settings(LayoutPreset.AGENDA))
        assertTrue(agenda.hitRegions.any { it.label.endsWith("Pay electricity bill") })
        assertTrue(agenda.hitRegions.any { it.label == "Today" })
    }

    @Test
    fun hiddenTitlesStayOutOfTapLabels() {
        for (preset in presets) {
            val hidden = settings(preset, PrivacyMode.ALWAYS)
            val labels = layout(hidden).hitRegions.map { it.label }
            week.forEach { item ->
                assertTrue(labels.none { it.contains(item.title) }, "$preset leaks a title")
            }
        }
    }

    @Test
    fun emptyStatesShowAMessage() {
        val strings = EnglishRenderStrings
        assertTrue(layout(settings(LayoutPreset.AGENDA), emptyList()).texts().contains(strings.emptyAgenda))
        assertTrue(layout(settings(LayoutPreset.WEEK_STRIP), emptyList()).texts().contains(strings.emptyWeek))
        assertTrue(layout(settings(LayoutPreset.MONTH_GRID), emptyList()).texts().contains(strings.emptyMonth))
        assertTrue(layout(settings(LayoutPreset.NEXT_THREE), emptyList()).texts().contains(strings.allClear))
    }

    @Test
    fun filtersBySourceCompletionAndCalendar() {
        val base = settings(LayoutPreset.AGENDA)
        val noTasks = base.copy(content = base.content.copy(enabledSources = setOf(SourceId.CALENDAR_PROVIDER)))
        assertFalse(layout(noTasks).texts().contains("Call the bank"))
        val hideDone = base.copy(content = base.content.copy(showCompleted = false))
        assertFalse(layout(hideDone).texts().contains("Buy groceries"))
        assertTrue(layout(base).texts().contains("Buy groceries"))
        val otherCalendar = base.copy(content = base.content.copy(selectedCalendarIds = setOf("2")))
        assertFalse(layout(otherCalendar).texts().contains("Standup"))
    }

    @Test
    fun nextThreeShowsOnlyUpcomingItems() {
        val past =
            Fixtures.event(
                "early",
                "Early call",
                io.github.nabin47.calwallpaper.core.ItemTime
                    .Timed(Fixtures.at(17, 6), Fixtures.at(17, 7)),
            )
        val texts = layout(settings(LayoutPreset.NEXT_THREE), week + past).texts()
        assertFalse(texts.contains("Early call"))
        assertFalse(texts.contains("Buy groceries"), "completed tasks are not upcoming")
        assertEquals(3, texts.count { it.startsWith("Today") || it.startsWith("Tomorrow") || it.contains(", Oct") })
    }

    @Test
    fun monthGridHasOneTargetPerDay() {
        val scene = layout(settings(LayoutPreset.MONTH_GRID))
        val days =
            scene.hitRegions
                .map { it.target }
                .filterIsInstance<OpenTarget.CalendarDay>()
                .map { it.date }
                .toSet()
        assertEquals(31, days.size)
        assertTrue(scene.texts().contains("October 2026"))
    }

    @Test
    fun requiredRangeMatchesPreset() {
        fun range(preset: LayoutPreset) = LayoutEngine.requiredRange(settings(preset), today, DayOfWeek.MONDAY)
        assertEquals(DateRange.startingAt(today, 7), range(LayoutPreset.AGENDA))
        assertEquals(DateRange.startingAt(today, 7), range(LayoutPreset.NEXT_THREE))
        assertEquals(DateRange(LocalDate(2026, 10, 12), LocalDate(2026, 10, 18)), range(LayoutPreset.WEEK_STRIP))
        assertEquals(DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)), range(LayoutPreset.MONTH_GRID))
        assertEquals(
            LocalDate(2026, 10, 17),
            LayoutEngine.requiredRange(settings(LayoutPreset.WEEK_STRIP), today, DayOfWeek.SATURDAY).start,
        )
    }

    @Test
    fun lockScreenUsesItsOwnPlacement() {
        val base = settings(LayoutPreset.AGENDA, PrivacyMode.WHEN_LOCKED)
        val s =
            base.copy(
                layout =
                    base.layout.copy(
                        lockPosition = VerticalPosition.BOTTOM,
                        lockSafeZoneTop = 0.6f,
                        lockSafeZoneBottom = 0.2f,
                    ),
            )
        val locked = layout(s, isLocked = true)
        val height = Fixtures.screen.heightPx
        locked.ops.forEach { op ->
            val extent = op.verticalExtent()
            assertTrue(extent.start >= height * 0.6f - 0.01f && extent.endInclusive <= height * 0.8f + 0.01f, "$op")
        }
        val panel = locked.ops.filterIsInstance<DrawOp.RoundRect>().first()
        assertEquals(height * 0.8f, panel.bounds.bottom, 0.01f, "bottom position hugs the lock safe zone")
    }

    @Test
    fun sceneBackgroundComesFromThePalette() {
        val base = settings(LayoutPreset.AGENDA)
        val light = base.copy(appearance = base.appearance.copy(palette = PalettePreset.CALM_LIGHT))
        assertEquals(Palette.CALM_LIGHT.background, layout(light).background)
    }

    @Test
    fun sceneSizeMatchesScreen() {
        val scene = layout(settings(LayoutPreset.AGENDA))
        assertEquals(720, scene.widthPx)
        assertTrue(scene.isPortrait)
    }
}
