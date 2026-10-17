package io.github.nabin47.calwallpaper.data.settings

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SettingsCodec
import java.io.InputStream
import java.io.OutputStream

/**
 * Stores [Settings] in DataStore as the same versioned JSON used for export, so on-device data and backups
 * share one format and one migration path. Unreadable files raise [CorruptionException], which the repository
 * turns into defaults. DataStore calls this on its own I/O thread.
 */
internal class SettingsFileSerializer(
    private val codec: SettingsCodec,
) : Serializer<Settings> {
    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(input: InputStream): Settings =
        when (val result = codec.decode(input.readBytes().decodeToString())) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> throw CorruptionException(result.reason)
        }

    override suspend fun writeTo(
        t: Settings,
        output: OutputStream,
    ) {
        output.write(codec.encode(t).encodeToByteArray())
    }
}
