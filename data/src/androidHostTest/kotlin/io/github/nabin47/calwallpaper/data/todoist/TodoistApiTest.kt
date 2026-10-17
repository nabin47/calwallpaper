package io.github.nabin47.calwallpaper.data.todoist

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodoistApiTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): TodoistApi =
        TodoistApi(HttpClient(MockEngine(handler)), baseUrl = "https://api.test/v1/")

    @Test
    fun fetchesEveryPageWithTheTokenAndMapsTheResult() =
        runBlocking {
            val requests = mutableListOf<HttpRequestData>()
            val result =
                api { request ->
                    requests += request
                    val path = request.url.encodedPath
                    val cursor = request.url.parameters["cursor"]
                    val body =
                        when {
                            path.endsWith("projects") -> {
                                """{"results":[{"id":"p1","name":"Work","color":"red","extra":1},""" +
                                    """{"id":"p2","name":"Old","is_archived":true}],"next_cursor":null}"""
                            }

                            cursor == null -> {
                                """{"results":[{"id":"t1","content":"A","project_id":"p1",""" +
                                    """"due":{"date":"2026-10-18","is_recurring":false}}],"next_cursor":"c2"}"""
                            }

                            else -> {
                                """{"results":[{"id":"t2","content":"B","project_id":"p1","checked":true,""" +
                                    """"due":{"date":"2026-10-18"}},{"id":"t3","content":"C","project_id":"p1"}],""" +
                                    """"next_cursor":null}"""
                            }
                        }
                    respond(body, HttpStatusCode.OK, json)
                }.fetch("secret-token")
            assertEquals(
                RemoteResult.Data(
                    projects = listOf(CachedProject("p1", "Work", 0xFFDB4035.toInt())),
                    tasks = listOf(CachedTask("t1", "A", "p1", "2026-10-18")),
                ),
                result,
            )
            assertEquals(3, requests.size)
            assertTrue(requests.all { it.headers[HttpHeaders.Authorization] == "Bearer secret-token" })
            assertTrue(requests.all { it.url.parameters["limit"] == "200" })
            assertEquals("c2", requests.last().url.parameters["cursor"])
            assertTrue(requests.all { it.url.toString().startsWith("https://api.test/v1/") })
        }

    @Test
    fun httpErrorsMapToSyncErrors() =
        runBlocking {
            val cases =
                mapOf(
                    HttpStatusCode.Unauthorized to SyncError.AUTH,
                    HttpStatusCode.Forbidden to SyncError.AUTH,
                    HttpStatusCode.TooManyRequests to SyncError.RATE_LIMITED,
                    HttpStatusCode.InternalServerError to SyncError.SERVER,
                    HttpStatusCode.ServiceUnavailable to SyncError.SERVER,
                    HttpStatusCode.Gone to SyncError.BAD_RESPONSE,
                    HttpStatusCode.NotFound to SyncError.BAD_RESPONSE,
                )
            cases.forEach { (status, error) ->
                val result = api { respond("", status) }.fetch("token")
                assertEquals(RemoteResult.Failed(error), result, "$status")
            }
        }

    @Test
    fun rateLimitsCarryRetryAfter() =
        runBlocking {
            val result =
                api { respond("", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "42")) }
                    .fetch("token")
            assertEquals(RemoteResult.Failed(SyncError.RATE_LIMITED, retryAfterSeconds = 42), result)
        }

    @Test
    fun brokenBodiesAndLostConnectionsAreReported() =
        runBlocking {
            assertEquals(
                RemoteResult.Failed(SyncError.BAD_RESPONSE),
                api { respond("not json", HttpStatusCode.OK, json) }.fetch("token"),
            )
            assertEquals(
                RemoteResult.Failed(SyncError.NETWORK),
                api { throw IOException("offline") }.fetch("token"),
            )
        }
}
