package io.github.nabin47.calwallpaper.data.todoist

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Fetches projects and open tasks from Todoist API v1 with the user's personal token (`Authorization: Bearer`).
 * Follows `next_cursor` pagination, up to [MAX_PAGES] pages per list.
 *
 * Failures become [RemoteResult.Failed] and are never thrown:
 * - 401 or 403 → [SyncError.AUTH];
 * - 429 → [SyncError.RATE_LIMITED], with `Retry-After` seconds;
 * - 5xx → [SyncError.SERVER];
 * - other statuses (including 410 for a retired API) and unparseable bodies → [SyncError.BAD_RESPONSE];
 * - no connection → [SyncError.NETWORK].
 *
 * Nothing is logged: requests carry the token and responses carry titles. The [client] must use HTTPS only
 * (CLAUDE.md §7); [baseUrl] exists for tests.
 */
class TodoistApi(
    private val client: HttpClient,
    private val baseUrl: String = BASE_URL,
) : TodoistRemote {
    override suspend fun fetch(token: String): RemoteResult =
        try {
            val projects = all("projects", ProjectDto.serializer(), token).mapNotNull(TodoistMapper::project)
            val tasks = all("tasks", TaskDto.serializer(), token).mapNotNull(TodoistMapper::task)
            RemoteResult.Data(projects, tasks)
        } catch (failure: HttpFailure) {
            failure.result
        } catch (_: SerializationException) {
            RemoteResult.Failed(SyncError.BAD_RESPONSE)
        } catch (_: IOException) {
            // Ktor reports connection problems and timeouts as IOException (OkHttp's java.io.IOException on
            // Android is the same type): Todoist could not be reached.
            RemoteResult.Failed(SyncError.NETWORK)
        }

    private suspend fun <T> all(
        path: String,
        item: KSerializer<T>,
        token: String,
    ): List<T> {
        val pageSerializer = PageDto.serializer(item)
        val results = mutableListOf<T>()
        var cursor: String? = null
        var pages = 0
        do {
            val page = JSON.decodeFromString(pageSerializer, get(path, token, cursor))
            results += page.results
            cursor = page.nextCursor
            pages++
        } while (cursor != null && pages < MAX_PAGES)
        return results
    }

    private suspend fun get(
        path: String,
        token: String,
        cursor: String?,
    ): String {
        val response =
            client.get(baseUrl + path) {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("limit", PAGE_SIZE)
                cursor?.let { parameter("cursor", it) }
            }
        val status = response.status.value
        if (!response.status.isSuccess()) {
            val error =
                when (status) {
                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> SyncError.AUTH
                    HTTP_TOO_MANY_REQUESTS -> SyncError.RATE_LIMITED
                    in HTTP_SERVER_ERRORS -> SyncError.SERVER
                    else -> SyncError.BAD_RESPONSE
                }
            val retryAfter = response.headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()
            throw HttpFailure(RemoteResult.Failed(error, retryAfter))
        }
        return response.bodyAsText()
    }

    /** A non-success HTTP status, carried out of the paging loop. Holds no request or response data. */
    private class HttpFailure(
        val result: RemoteResult.Failed,
    ) : Exception()

    /** Endpoint and limits. */
    companion object {
        /** Todoist API v1. REST v2 and Sync v9 are retired. */
        const val BASE_URL: String = "https://api.todoist.com/api/v1/"

        /** Largest page Todoist allows. */
        private const val PAGE_SIZE = 200

        /** Stops after 10,000 items per list, so a bad cursor can never loop forever. */
        private const val MAX_PAGES = 50

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_SERVER_ERRORS = 500..599

        private val JSON = Json { ignoreUnknownKeys = true }
    }
}
