package io.github.nabin47.calwallpaper.domain.settings

import io.github.nabin47.calwallpaper.core.Outcome
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SettingsMigrationTest {
    /**
     * A hypothetical v1 -> v2 step: v1 stored a boolean "hideTitles"; v2 replaced it with "privacyMode".
     * Proves the chain runs before decoding, without touching the shipped schema.
     */
    private val v1ToV2 =
        SettingsMigration(fromVersion = 1) { settings ->
            val hide = (settings["hideTitles"] as? JsonPrimitive)?.content == "true"
            JsonObject(settings - "hideTitles" + ("privacyMode" to JsonPrimitive(if (hide) "always" else "off")))
        }

    private val codecV2 = SettingsCodec(currentVersion = 2, migrations = listOf(v1ToV2))

    @Test
    fun shippedChainIsConsistent() {
        // Constructing the production codec validates SettingsMigrations.ALL against CURRENT_VERSION.
        SettingsCodec()
        assertEquals(SettingsCodec.CURRENT_VERSION - 1, SettingsMigrations.ALL.size)
    }

    @Test
    fun olderFileIsMigrated() {
        val result = codecV2.decode("""{"schemaVersion":1,"settings":{"hideTitles":true}}""")
        assertEquals(PrivacyMode.ALWAYS, assertIs<Outcome.Success<Settings>>(result).value.privacyMode)
    }

    @Test
    fun currentFileIsNotMigratedAgain() {
        val result = codecV2.decode("""{"schemaVersion":2,"settings":{"privacyMode":"when_locked"}}""")
        assertEquals(PrivacyMode.WHEN_LOCKED, assertIs<Outcome.Success<Settings>>(result).value.privacyMode)
    }

    @Test
    fun encodeWritesCurrentVersion() {
        assertTrue("\"schemaVersion\": 2" in codecV2.encode(Settings()))
    }

    @Test
    fun incompleteChainIsRejected() {
        assertFailsWith<IllegalArgumentException> { SettingsCodec(currentVersion = 3, migrations = listOf(v1ToV2)) }
        assertFailsWith<IllegalArgumentException> { SettingsCodec(currentVersion = 2, migrations = emptyList()) }
    }
}
