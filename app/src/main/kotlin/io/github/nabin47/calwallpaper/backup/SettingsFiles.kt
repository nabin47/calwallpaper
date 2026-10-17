package io.github.nabin47.calwallpaper.backup

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Reads and writes exported settings files chosen by the user through the system file picker (Storage Access
 * Framework: no storage permission). The content is the versioned JSON of `SettingsCodec`; it holds no tokens and
 * no event or task titles. All functions run on [Dispatchers.IO] and never throw; failures return null/false.
 */
class SettingsFiles(
    private val resolver: ContentResolver,
) {
    /** Writes [json] to [uri]. False if the file could not be written. */
    suspend fun write(
        uri: Uri,
        json: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                resolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) } != null
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }

    /** The text of [uri], or null if it cannot be read or is larger than any settings file could be. */
    suspend fun read(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                resolver.openInputStream(uri)?.use { readLimited(it) }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }

    /** File name and type of exports. */
    companion object {
        /** Suggested name for a new export. */
        const val DEFAULT_NAME: String = "calwallpaper-settings.json"

        /** Content type of exports. */
        const val MIME_TYPE: String = "application/json"
    }
}

/**
 * Reads [input] as UTF-8 text, or returns null once it exceeds [limitBytes], so picking a huge file by mistake
 * cannot exhaust memory. Pure apart from reading the stream; does not close it.
 */
fun readLimited(
    input: InputStream,
    limitBytes: Int = MAX_SETTINGS_BYTES,
): String? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_BYTES)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        out.write(buffer, 0, read)
        if (out.size() > limitBytes) return null
    }
    return out.toByteArray().decodeToString()
}

private const val BUFFER_BYTES = 8 * 1024

/** Settings files are a few kilobytes; anything above this is not one. */
const val MAX_SETTINGS_BYTES: Int = 256 * 1024
