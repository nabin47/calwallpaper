package io.github.nabin47.calwallpaper.data.todoist

import io.github.nabin47.calwallpaper.data.secure.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

/**
 * Runs Todoist syncs over the network: Ktor on OkHttp (CLAUDE.md §3), with timeouts and no logging plugin
 * (requests carry the token, responses carry titles). Cleartext is refused by the app's network security config.
 * Callers never see the HTTP client, so only this module depends on Ktor.
 */
object TodoistHttp {
    private const val REQUEST_TIMEOUT_MS = 30_000L
    private const val CONNECT_TIMEOUT_MS = 15_000L

    /** One sync with a fresh client, closed afterwards. */
    suspend fun sync(
        tokens: TokenStore,
        cache: TodoistCacheStore,
    ): SyncResult = client().use { client -> TodoistSync(TodoistApi(client), tokens::read, cache).run() }

    private fun client(): HttpClient =
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
            }
        }
}
