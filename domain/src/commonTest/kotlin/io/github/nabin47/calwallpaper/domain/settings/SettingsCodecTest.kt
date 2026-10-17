package io.github.nabin47.calwallpaper.domain.settings

import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SettingsCodecTest {
    private val codec = SettingsCodec()

    /** Every field set to a non-default value, so a round trip proves each one is persisted. */
    private val everythingChanged =
        Settings(
            appearance =
                AppearanceSettings(
                    palette = PalettePreset.CUSTOM,
                    customColors =
                        CustomColors(
                            panel = ArgbColor(0x80112233.toInt()),
                            text = ArgbColor(0xFFFFFFFF.toInt()),
                            accent = ArgbColor(0xFF4C8BF5.toInt()),
                            background = ArgbColor(0xFF203040.toInt()),
                        ),
                    useDynamicColor = true,
                    font = FontChoice.SERIF,
                    textScale = 1.25f,
                    textOpacity = 0.9f,
                    panelOpacity = 0.5f,
                    cornerRadiusDp = 8,
                    density = SpacingDensity.COMPACT,
                ),
            layout =
                LayoutSettings(
                    preset = LayoutPreset.WEEK_STRIP,
                    position = VerticalPosition.BOTTOM,
                    safeZoneTop = 0.3f,
                    safeZoneBottom = 0.1f,
                    horizontalInsetDp = 32,
                ),
            content =
                ContentSettings(
                    enabledSources = setOf(SourceId.TODOIST),
                    selectedCalendarIds = setOf("3", "7"),
                    selectedTaskListIds = setOf("inbox"),
                    daysAhead = 14,
                    showCompleted = true,
                ),
            time = TimeSettings(weekStart = WeekStart.SATURDAY, clockFormat = ClockFormat.H24),
            privacyMode = PrivacyMode.ALWAYS,
            batteryMode = BatteryMode.STATIC,
            staticTarget = WallpaperTarget.LOCK,
        )

    private fun decodeOk(text: String): Settings = assertIs<Outcome.Success<Settings>>(codec.decode(text)).value

    @Test
    fun defaultsRoundTrip() {
        assertEquals(Settings(), decodeOk(codec.encode(Settings())))
    }

    @Test
    fun everyFieldRoundTrips() {
        assertEquals(everythingChanged, decodeOk(codec.encode(everythingChanged)))
    }

    @Test
    fun exportIsVersionedAndReadable() {
        val text = codec.encode(everythingChanged)
        assertTrue("\"schemaVersion\": 1" in text)
        assertTrue("\"#80112233\"" in text, "colors are hex strings")
        assertTrue("\"week_strip\"" in text, "enums use stable serial names")
    }

    @Test
    fun phaseThreeFieldsDecodeFromOlderFiles() {
        // Files written before the static target and custom background existed keep working.
        val old =
            decodeOk(
                """{"schemaVersion":1,"settings":{"appearance":{"customColors":""" +
                    """{"panel":"#FF000000","text":"#FFFFFFFF","accent":"#FF4C8BF5"}}}}""",
            )
        assertEquals(WallpaperTarget.HOME_AND_LOCK, old.staticTarget)
        assertEquals(null, old.appearance.customColors?.background)
        assertTrue("\"home_and_lock\"" in codec.encode(Settings()), "target uses a stable serial name")
    }

    @Test
    fun missingFieldsUseDefaults() {
        val decoded = decodeOk("""{"schemaVersion":1,"settings":{"privacyMode":"off"}}""")
        assertEquals(Settings(privacyMode = PrivacyMode.OFF), decoded)
        assertEquals(Settings(), decodeOk("""{"schemaVersion":1}"""))
    }

    @Test
    fun unknownKeysAndEnumValuesAreTolerated() {
        val decoded =
            decodeOk(
                """{"schemaVersion":1,"futureField":true,
                   "settings":{"batteryMode":"quantum","layout":{"preset":"spiral","extra":1}}}""",
            )
        assertEquals(BatteryMode.LIVE, decoded.batteryMode)
        assertEquals(LayoutPreset.AGENDA, decoded.layout.preset)
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        val decoded =
            decodeOk(
                """{"schemaVersion":1,"settings":{
                   "appearance":{"textScale":9.0,"textOpacity":-1,"panelOpacity":2,"cornerRadiusDp":500},
                   "layout":{"safeZoneTop":0.5,"safeZoneBottom":0.5,"horizontalInsetDp":-4},
                   "content":{"daysAhead":400}}}""",
            )
        with(decoded.appearance) {
            assertEquals(AppearanceSettings.TEXT_SCALE.endInclusive, textScale)
            assertEquals(0f, textOpacity)
            assertEquals(1f, panelOpacity)
            assertEquals(AppearanceSettings.CORNER_RADIUS_DP.last, cornerRadiusDp)
        }
        assertEquals(LayoutSettings().safeZone, decoded.layout.safeZone, "0.5 + 0.5 leaves no room: defaults")
        assertEquals(0, decoded.layout.horizontalInsetDp)
        assertEquals(ContentSettings.DAYS_AHEAD.last, decoded.content.daysAhead)
    }

    @Test
    fun rejectsNewerVersionMalformedJsonAndBadColors() {
        assertIs<Outcome.Failure>(codec.decode("""{"schemaVersion":99,"settings":{}}"""))
        assertIs<Outcome.Failure>(codec.decode("""{"settings":{}}"""))
        assertIs<Outcome.Failure>(codec.decode("""{"schemaVersion":0}"""))
        assertIs<Outcome.Failure>(codec.decode("not json"))
        assertIs<Outcome.Failure>(codec.decode("[1,2]"))
        assertIs<Outcome.Failure>(
            codec.decode(
                """{"schemaVersion":1,"settings":{"appearance":{"customColors":
                   {"panel":"red","text":"#FFFFFFFF","accent":"#FF000000"}}}}""",
            ),
        )
    }

    @Test
    fun failureReasonNeverEchoesInput() {
        val failure = assertIs<Outcome.Failure>(codec.decode("""{"schemaVersion":1,"settings":"my secret"}"""))
        assertTrue("secret" !in failure.reason)
    }
}
