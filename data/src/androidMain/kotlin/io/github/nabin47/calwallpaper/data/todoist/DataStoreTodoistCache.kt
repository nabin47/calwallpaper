package io.github.nabin47.calwallpaper.data.todoist

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * The Todoist offline cache on Jetpack DataStore, shared between the app (which syncs) and the wallpaper process
 * (which draws) through [MultiProcessDataStoreFactory].
 *
 * The file lives in **credential-protected** storage: it holds task titles, so it must not be readable before the
 * first unlock. The wallpaper reads it only after unlock, like the local task database. It is excluded from
 * backups (see the app's backup rules) and is rebuilt by the next sync. A corrupt file is replaced by an empty
 * cache. Create one per process.
 */
class DataStoreTodoistCache private constructor(
    private val store: DataStore<TodoistCache>,
) : TodoistCacheStore {
    override val cache: Flow<TodoistCache> = store.data

    override suspend fun update(transform: (TodoistCache) -> TodoistCache): TodoistCache =
        store.updateData { transform(it) }

    /** Construction. */
    companion object {
        /** Path under `filesDir`; the app's backup rules exclude it. */
        const val RELATIVE_PATH: String = "todoist/cache.json"

        /** The cache for this process. [scope] should live as long as the process. */
        fun create(
            context: Context,
            scope: CoroutineScope,
        ): DataStoreTodoistCache {
            val appContext = context.applicationContext
            val store =
                MultiProcessDataStoreFactory.create(
                    serializer = CacheSerializer,
                    corruptionHandler = ReplaceFileCorruptionHandler { TodoistCache() },
                    scope = scope,
                    produceFile = { File(appContext.filesDir, RELATIVE_PATH) },
                )
            return DataStoreTodoistCache(store)
        }
    }

    /** JSON on disk. Unknown fields are ignored, so an older app version still reads a newer file. */
    private object CacheSerializer : Serializer<TodoistCache> {
        private val json = Json { ignoreUnknownKeys = true }

        override val defaultValue: TodoistCache = TodoistCache()

        override suspend fun readFrom(input: InputStream): TodoistCache =
            try {
                json.decodeFromString(TodoistCache.serializer(), input.readBytes().decodeToString())
            } catch (e: SerializationException) {
                // The message names the failing field only, never a value.
                throw CorruptionException("Unreadable Todoist cache", e)
            } catch (e: IllegalArgumentException) {
                throw CorruptionException("Unreadable Todoist cache", e)
            }

        override suspend fun writeTo(
            t: TodoistCache,
            output: OutputStream,
        ) {
            output.write(json.encodeToString(TodoistCache.serializer(), t).encodeToByteArray())
        }
    }
}
