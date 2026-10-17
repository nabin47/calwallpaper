package io.github.nabin47.calwallpaper.backup

import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SettingsCodec
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SettingsFilesTest {
    @Test
    fun anExportedFileReadsBackToTheSameSettings() {
        val codec = SettingsCodec()
        val settings = Settings().let { it.copy(layout = it.layout.copy(preset = LayoutPreset.MONTH_GRID)) }
        val text = readLimited(ByteArrayInputStream(codec.encode(settings).toByteArray()))
        val decoded = codec.decode(checkNotNull(text))
        assertIs<Outcome.Success<Settings>>(decoded)
        assertEquals(settings, decoded.value)
    }

    @Test
    fun aFileLargerThanTheLimitIsRejected() {
        assertNull(readLimited(ByteArrayInputStream(ByteArray(11)), limitBytes = 10))
        assertEquals("0123456789", readLimited(ByteArrayInputStream("0123456789".toByteArray()), limitBytes = 10))
    }

    @Test
    fun somethingThatIsNotASettingsFileFailsWithoutThrowing() {
        assertIs<Outcome.Failure>(SettingsCodec().decode("hello"))
    }
}
