package io.github.nabin47.calwallpaper.data.todoist

import io.github.nabin47.calwallpaper.data.secure.TokenRead
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class TodoistSyncTest {
    private val now = Instant.parse("2026-10-17T10:00:00Z")
    private val clock =
        object : Clock {
            override fun now(): Instant = now
        }
    private val project = CachedProject("p1", "Work", 0xFFDB4035.toInt())
    private val taskB = CachedTask("t2", "Second", "p1", "2026-10-18", order = 1)
    private val taskA = CachedTask("t1", "First", "p1", "2026-10-18", order = 0)

    private class FakeStore(
        initial: TodoistCache = TodoistCache(),
    ) : TodoistCacheStore {
        override val cache = MutableStateFlow(initial)

        override suspend fun update(transform: (TodoistCache) -> TodoistCache): TodoistCache =
            transform(cache.value).also { cache.value = it }
    }

    private fun sync(
        store: TodoistCacheStore,
        token: TokenRead = TokenRead.Present("secret-token"),
        remote: TodoistRemote,
    ) = TodoistSync(remote, { token }, store, clock)

    @Test
    fun successStoresSortedTasksAndClearsTheError() =
        runBlocking {
            val store = FakeStore(TodoistCache(status = SyncStatus(error = SyncError.NETWORK)))
            var usedToken: String? = null
            val result =
                sync(store) { token ->
                    usedToken = token
                    RemoteResult.Data(listOf(project), listOf(taskB, taskA))
                }.run()
            assertEquals(SyncResult.Synced(changed = true), result)
            assertEquals("secret-token", usedToken)
            val cache = store.cache.value
            assertEquals(listOf(taskA, taskB), cache.tasks)
            assertEquals(SyncStatus(now.toEpochMilliseconds(), now.toEpochMilliseconds(), null), cache.status)
        }

    @Test
    fun unchangedDataIsReportedAsUnchanged() =
        runBlocking {
            val store = FakeStore(TodoistCache(listOf(taskA, taskB), listOf(project)))
            val result = sync(store) { RemoteResult.Data(listOf(project), listOf(taskB, taskA)) }.run()
            assertEquals(SyncResult.Synced(changed = false), result)
        }

    @Test
    fun failuresKeepTheLastTasksAndRecordTheError() =
        runBlocking {
            val store = FakeStore(TodoistCache(listOf(taskA), listOf(project), SyncStatus(lastSuccessMs = 1)))
            val result = sync(store) { RemoteResult.Failed(SyncError.RATE_LIMITED, retryAfterSeconds = 30) }.run()
            assertEquals(SyncResult.Failed(SyncError.RATE_LIMITED, retryable = true, retryAfterSeconds = 30), result)
            val cache = store.cache.value
            assertEquals(listOf(taskA), cache.tasks)
            assertEquals(
                SyncStatus(lastSuccessMs = 1, lastAttemptMs = now.toEpochMilliseconds(), SyncError.RATE_LIMITED),
                cache.status,
            )
        }

    @Test
    fun onlyTransientErrorsAreRetried() =
        runBlocking {
            val retryable = setOf(SyncError.NETWORK, SyncError.SERVER, SyncError.RATE_LIMITED)
            SyncError.entries.filter { it != SyncError.TOKEN_UNREADABLE }.forEach { error ->
                val result = sync(FakeStore()) { RemoteResult.Failed(error) }.run() as SyncResult.Failed
                assertEquals(error in retryable, result.retryable, "$error")
            }
        }

    @Test
    fun withoutATokenNothingIsFetched() =
        runBlocking {
            val store = FakeStore()
            var called = false
            val result =
                sync(store, TokenRead.Missing) {
                    called = true
                    RemoteResult.Data(emptyList(), emptyList())
                }.run()
            assertEquals(SyncResult.NotConnected, result)
            assertFalse(called)
            assertNull(store.cache.value.status.lastAttemptMs)
        }

    @Test
    fun anUnreadableTokenStopsSyncingUntilTheUserActs() =
        runBlocking {
            val store = FakeStore()
            val result = sync(store, TokenRead.Unreadable) { error("must not fetch") }.run()
            assertEquals(SyncResult.Failed(SyncError.TOKEN_UNREADABLE, retryable = false), result)
            assertEquals(SyncError.TOKEN_UNREADABLE, store.cache.value.status.error)
        }

    @Test
    fun clearForgetsEverything() =
        runBlocking {
            val store = FakeStore(TodoistCache(listOf(taskA), listOf(project), SyncStatus(lastSuccessMs = 1)))
            sync(store) { RemoteResult.Data(emptyList(), emptyList()) }.clear()
            assertEquals(TodoistCache(), store.cache.value)
            assertTrue(
                store.cache.value.tasks
                    .isEmpty(),
            )
        }
}

class SyncStatusTest {
    @Test
    fun onlyTokenProblemsAskForANewToken() {
        val needsToken = SyncError.entries.filter { SyncStatus(error = it).needsNewToken }.toSet()
        assertEquals(setOf(SyncError.AUTH, SyncError.TOKEN_UNREADABLE), needsToken)
        assertFalse(SyncStatus().needsNewToken)
    }
}
