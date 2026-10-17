package io.github.nabin47.calwallpaper.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SettingsCodec
import io.github.nabin47.calwallpaper.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * [SettingsRepository] on Jetpack DataStore.
 *
 * The file lives in **device-protected storage**, so the wallpaper can read settings before the first unlock
 * (Direct Boot, CLAUDE.md §2). Settings hold no secrets; the Todoist token is stored separately, encrypted.
 * A corrupt file (or one from a newer app version after a downgrade) is replaced with defaults.
 *
 * The live wallpaper runs in its own process (ADR 0004), so the file is opened with
 * [MultiProcessDataStoreFactory]: a change made in the settings screen reaches the wallpaper's [settings] flow.
 * DataStore allows one instance per file per process: create this once per process and share it.
 */
class DataStoreSettingsRepository internal constructor(
    private val store: DataStore<Settings>,
    private val codec: SettingsCodec,
) : SettingsRepository {
    override val settings: Flow<Settings> = store.data

    override suspend fun update(transform: (Settings) -> Settings) {
        store.updateData { transform(it).normalized() }
    }

    override suspend fun exportJson(): String = codec.encode(store.data.first())

    override suspend fun importJson(json: String): Outcome<Settings> {
        val result = codec.decode(json)
        if (result is Outcome.Success) store.updateData { result.value }
        return result
    }

    /** Construction. */
    companion object {
        private const val RELATIVE_PATH = "settings/settings.json"

        /**
         * Creates the repository for this process. [scope] owns DataStore's background work and should live as
         * long as the process (an application-level scope). Safe before the first unlock (Direct Boot).
         */
        fun create(
            context: Context,
            scope: CoroutineScope,
        ): DataStoreSettingsRepository {
            val codec = SettingsCodec()
            val deviceStorage = context.applicationContext.createDeviceProtectedStorageContext()
            val store =
                MultiProcessDataStoreFactory.create(
                    serializer = SettingsFileSerializer(codec),
                    corruptionHandler = ReplaceFileCorruptionHandler { Settings() },
                    scope = scope,
                    produceFile = { File(deviceStorage.filesDir, RELATIVE_PATH) },
                )
            return DataStoreSettingsRepository(store, codec)
        }
    }
}
