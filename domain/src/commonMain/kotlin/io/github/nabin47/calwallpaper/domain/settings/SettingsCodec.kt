package io.github.nabin47.calwallpaper.domain.settings

import io.github.nabin47.calwallpaper.core.Outcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Converts [Settings] to and from versioned JSON, used both for on-device storage and for user
 * import/export:
 *
 * ```json
 * { "schemaVersion": 1, "settings": { ... } }
 * ```
 *
 * Decoding migrates older versions step by step with [migrations], fills missing fields with defaults,
 * ignores unknown fields, maps unknown enum values to the field default, and [Settings.normalized] the
 * result. Files from a newer app version are rejected rather than half-read. Pure and thread-safe.
 *
 * @param currentVersion the schema version this app writes.
 * @param migrations steps from version 1 up to [currentVersion]; must be complete and in order.
 */
class SettingsCodec(
    private val currentVersion: Int = CURRENT_VERSION,
    private val migrations: List<SettingsMigration> = SettingsMigrations.ALL,
) {
    init {
        require(currentVersion >= 1) { "currentVersion must be at least 1" }
        require(migrations.map { it.fromVersion } == (1 until currentVersion).toList()) {
            "migrations must cover every version from 1 to $currentVersion in order"
        }
    }

    /** [settings] as pretty-printed, versioned JSON. */
    fun encode(settings: Settings): String {
        val envelope =
            buildJsonObject {
                put(KEY_VERSION, JsonPrimitive(currentVersion))
                put(KEY_SETTINGS, json.encodeToJsonElement(settings))
            }
        return json.encodeToString(JsonObject.serializer(), envelope)
    }

    /**
     * Parses versioned JSON. Never throws; failures carry a short reason that contains no user data and is
     * safe to show or log.
     */
    fun decode(text: String): Outcome<Settings> =
        try {
            decodeOrThrow(text)
        } catch (e: IllegalArgumentException) {
            // Covers SerializationException (a subclass) and malformed JSON structure.
            Outcome.Failure("Not a valid settings file (${e::class.simpleName})")
        }

    private fun decodeOrThrow(text: String): Outcome<Settings> {
        val envelope = json.parseToJsonElement(text).jsonObject
        val version = envelope[KEY_VERSION]?.jsonPrimitive?.intOrNull
        return when {
            version == null -> {
                Outcome.Failure("Not a settings file: missing $KEY_VERSION")
            }

            version > currentVersion -> {
                Outcome.Failure("Settings were saved by a newer app version ($version); please update the app")
            }

            version < 1 -> {
                Outcome.Failure("Unsupported settings version $version")
            }

            else -> {
                Outcome.Success(migrateAndDecode(envelope, version))
            }
        }
    }

    private fun migrateAndDecode(
        envelope: JsonObject,
        version: Int,
    ): Settings {
        val stored = envelope[KEY_SETTINGS]?.jsonObject ?: JsonObject(emptyMap())
        val migrated = migrations.drop(version - 1).fold(stored) { settings, step -> step.migrate(settings) }
        return json.decodeFromJsonElement<Settings>(migrated).normalized()
    }

    /** Envelope keys and the current schema version. */
    companion object {
        /** Schema version written by this app. Bump together with a new [SettingsMigration]. */
        const val CURRENT_VERSION: Int = 1

        /** Envelope key holding the schema version. */
        const val KEY_VERSION: String = "schemaVersion"

        /** Envelope key holding the settings object. */
        const val KEY_SETTINGS: String = "settings"

        private val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
                prettyPrint = true
            }
    }
}
