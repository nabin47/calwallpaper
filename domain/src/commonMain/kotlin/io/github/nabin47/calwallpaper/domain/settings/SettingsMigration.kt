package io.github.nabin47.calwallpaper.domain.settings

import kotlinx.serialization.json.JsonObject

/**
 * Upgrades the stored settings JSON from schema version [fromVersion] to `fromVersion + 1`.
 *
 * Migrations work on raw JSON, not on [Settings], so they keep working after the Kotlin model has moved on.
 * They must be pure and must not drop data they do not understand.
 */
class SettingsMigration(
    val fromVersion: Int,
    private val transform: (JsonObject) -> JsonObject,
) {
    /** Applies this step to the `settings` object of an envelope at [fromVersion]. */
    fun migrate(settings: JsonObject): JsonObject = transform(settings)
}

/** The migrations shipped with the app, in order. Version 1 is the first released schema, so none yet. */
object SettingsMigrations {
    /** Every step from version 1 to [SettingsCodec.CURRENT_VERSION]. */
    val ALL: List<SettingsMigration> = emptyList()
}
