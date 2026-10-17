package io.github.nabin47.calwallpaper.wallpaper.cache

import android.content.Context
import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Stores the last agenda in **device-protected storage**, so the wallpaper has something to draw before the
 * first unlock, when the calendar provider and the local task database are unavailable (CLAUDE.md §2).
 *
 * Writes are atomic (temp file + rename) and skipped when nothing changed. A missing or corrupt file reads as
 * an empty agenda. The file is excluded from backups (see the app's backup rules): it is a cache.
 * I/O runs on [ioDispatcher].
 */
class DirectBootCache(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val file = File(context.createDeviceProtectedStorageContext().filesDir, RELATIVE_PATH)
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var lastWritten: CachedAgenda? = null

    /** The cached items, or an empty list when there is no usable cache (missing, unreadable or corrupt). */
    suspend fun read(): List<AgendaItem> = withContext(ioDispatcher) { readSnapshot()?.toItems().orEmpty() }

    /**
     * Removes titles from the stored cache if [privacy] no longer allows them. Call when the user turns privacy
     * mode on, so no title stays in device-protected storage even if the live wallpaper is not running.
     */
    suspend fun restrictTo(privacy: PrivacyMode) {
        withContext(ioDispatcher) {
            val stored = readSnapshot() ?: return@withContext
            val restricted = stored.restrictedTo(privacy)
            if (restricted != stored) store(restricted)
        }
    }

    /**
     * Replaces the cache with [items], with titles kept only when [privacy] is off. Best effort: a failed write
     * only means an older (or no) agenda is shown before the next unlock.
     */
    suspend fun write(
        items: List<AgendaItem>,
        privacy: PrivacyMode,
    ) {
        val snapshot = CachedAgenda.fromItems(items, privacy)
        if (snapshot == lastWritten) return
        withContext(ioDispatcher) { store(snapshot) }
    }

    private fun readSnapshot(): CachedAgenda? =
        runCatching {
            if (file.exists()) json.decodeFromString(CachedAgenda.serializer(), file.readText()) else null
        }.getOrNull()

    /** Atomic replace (temp file + rename). Best effort. */
    private fun store(snapshot: CachedAgenda) {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(CachedAgenda.serializer(), snapshot))
            check(temp.renameTo(file)) { "rename failed" }
        }.onSuccess { lastWritten = snapshot }
    }

    private companion object {
        const val RELATIVE_PATH = "wallpaper/agenda_cache.json"
    }
}
