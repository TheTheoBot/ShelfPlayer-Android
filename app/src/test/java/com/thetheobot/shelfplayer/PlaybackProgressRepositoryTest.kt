package com.thetheobot.shelfplayer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class PlaybackProgressRepositoryTest {
    @Test
    fun `audiobookshelf progress repository falls back from post to put and sends the expected payload`() = runBlocking {
        val connections = mutableListOf<RecordingHttpURLConnection>()
        val repository = AudiobookshelfPlaybackProgressRepository(
            connectionProvider = {
                ConnectionCredentials(
                    serverUrl = "https://books.example.com",
                    accessToken = " token-123 ",
                )
            },
            localRepository = SharedPreferencesPlaybackProgressRepository(FakeSharedPreferences()),
            connectionFactory = { url ->
                RecordingHttpURLConnection(
                    url = url,
                    responseCode = if (connections.isEmpty()) 404 else 204,
                ).also { connections += it }
            },
        )

        repository.syncProgress(itemId = "item-1", positionMs = 12_345, durationMs = 54_321)

        assertEquals(2, connections.size)
        assertEquals("POST", connections[0].requestMethod)
        assertEquals("PUT", connections[1].requestMethod)
        assertEquals("Bearer token-123", connections[0].getRequestProperty("Authorization"))
        assertEquals("application/json", connections[0].getRequestProperty("Content-Type"))
        assertTrue(connections[0].bodyAsString().contains("\"currentTime\":12.345"))
        assertTrue(connections[0].bodyAsString().contains("\"duration\":54.321"))
        assertTrue(connections[0].bodyAsString().contains("\"isFinished\":false"))
        assertEquals("item-1", repository.latestProgress.value?.itemId)
    }

    @Test
    fun `audiobookshelf progress repository encodes path segments safely`() = runBlocking {
        val urls = mutableListOf<URL>()
        val repository = AudiobookshelfPlaybackProgressRepository(
            connectionProvider = {
                ConnectionCredentials(
                    serverUrl = "https://books.example.com",
                    accessToken = "token-123",
                )
            },
            localRepository = SharedPreferencesPlaybackProgressRepository(FakeSharedPreferences()),
            connectionFactory = { url ->
                urls += url
                RecordingHttpURLConnection(url, responseCode = 204)
            },
        )

        repository.syncProgress(itemId = "item/with space", positionMs = 1_000, durationMs = 2_000)

        assertEquals(1, urls.size)
        assertTrue(urls.single().toString().contains("/api/me/progress/item%2Fwith%20space"))
    }

    @Test
    fun `audiobookshelf progress repository skips remote sync when credentials are unavailable`() = runBlocking {
        var connectionFactoryCalls = 0
        val repository = AudiobookshelfPlaybackProgressRepository(
            connectionProvider = { null },
            localRepository = SharedPreferencesPlaybackProgressRepository(FakeSharedPreferences()),
            connectionFactory = {
                connectionFactoryCalls += 1
                RecordingHttpURLConnection(it, responseCode = 204)
            },
        )

        repository.syncProgress(itemId = "item-1", positionMs = 1_000, durationMs = 2_000)

        assertEquals(0, connectionFactoryCalls)
        assertEquals("item-1", repository.latestProgress.value?.itemId)
    }

    @Test
    fun `shared preferences playback progress repository restores persisted snapshot across a fresh instance`() {
        val sharedPreferences = FakeSharedPreferences()

        val firstRepository = SharedPreferencesPlaybackProgressRepository(
            sharedPreferences = sharedPreferences,
            namespace = "https://books.example.com",
        )
        firstRepository.recordProgress(itemId = "item-1", positionMs = 12_345, durationMs = 54_321)

        val secondRepository = SharedPreferencesPlaybackProgressRepository(
            sharedPreferences = sharedPreferences,
            namespace = "https://books.example.com",
        )

        val snapshot = secondRepository.latestProgress.value
        assertEquals("item-1", snapshot?.itemId)
        assertEquals(12_345, snapshot?.positionMs)
        assertEquals(54_321, snapshot?.durationMs)
        assertTrue((snapshot?.recordedAtEpochMs ?: 0L) > 0L)
    }

    @Test
    fun `shared preferences playback progress repository discards invalid persisted snapshots`() {
        val sharedPreferences = FakeSharedPreferences()
        sharedPreferences.edit()
            .putString("https://books.example.com::playback_progress_item_id", "item-1")
            .putInt("https://books.example.com::playback_progress_position_ms", 0)
            .putInt("https://books.example.com::playback_progress_duration_ms", 3_272_000)
            .putLong("https://books.example.com::playback_progress_synced_at_epoch_ms", 123L)
            .apply()

        val repository = SharedPreferencesPlaybackProgressRepository(
            sharedPreferences = sharedPreferences,
            namespace = "https://books.example.com",
        )

        assertNull(repository.latestProgress.value)
    }

    @Test
    fun `shared preferences playback progress repository namespaces snapshots by server`() {
        val sharedPreferences = FakeSharedPreferences()

        SharedPreferencesPlaybackProgressRepository(
            sharedPreferences = sharedPreferences,
            namespace = "https://books-one.example.com",
        ).recordProgress(itemId = "item-1", positionMs = 1_000, durationMs = 2_000)

        val otherNamespaceRepository = SharedPreferencesPlaybackProgressRepository(
            sharedPreferences = sharedPreferences,
            namespace = "https://books-two.example.com",
        )

        assertNull(otherNamespaceRepository.latestProgress.value)
    }

    @Test
    fun `summarize latest playback progress returns a hint for the active item with a resumable snapshot`() {
        val hint = summarizeLatestPlaybackProgress(
            itemId = "item-1",
            latestProgress = PlaybackProgressSnapshot(
                itemId = "item-1",
                positionMs = 754_000,
                durationMs = 3_272_000,
                recordedAtEpochMs = 123L,
            ),
        )

        assertEquals("Zuletzt gespeichert bei 12:34 von 54:32", hint)
    }

    @Test
    fun `summarize latest playback progress formats hour-long durations clearly`() {
        val hint = summarizeLatestPlaybackProgress(
            itemId = "item-1",
            latestProgress = PlaybackProgressSnapshot(
                itemId = "item-1",
                positionMs = 3_723_000,
                durationMs = 7_265_000,
                recordedAtEpochMs = 123L,
            ),
        )

        assertEquals("Zuletzt gespeichert bei 1:02:03 von 2:01:05", hint)
    }

    @Test
    fun `summarize latest playback progress returns null for stale finished or zero progress`() {
        assertNull(
            summarizeLatestPlaybackProgress(
                itemId = "item-1",
                latestProgress = PlaybackProgressSnapshot(
                    itemId = "item-2",
                    positionMs = 754_000,
                    durationMs = 3_272_000,
                    recordedAtEpochMs = 123L,
                ),
            ),
        )
        assertNull(
            summarizeLatestPlaybackProgress(
                itemId = "item-1",
                latestProgress = PlaybackProgressSnapshot(
                    itemId = "item-1",
                    positionMs = 3_272_000,
                    durationMs = 3_272_000,
                    recordedAtEpochMs = 123L,
                ),
            ),
        )
        assertNull(
            summarizeLatestPlaybackProgress(
                itemId = "item-1",
                latestProgress = PlaybackProgressSnapshot(
                    itemId = "item-1",
                    positionMs = 0,
                    durationMs = 3_272_000,
                    recordedAtEpochMs = 123L,
                ),
            ),
        )
    }

    @Test
    fun `resolve playback start position honors explicit requested start over saved progress`() {
        assertEquals(
            42,
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = 42,
                latestProgress = PlaybackProgressSnapshot(
                    itemId = "item-1",
                    positionMs = 754_000,
                    durationMs = 3_272_000,
                    recordedAtEpochMs = 123L,
                ),
            ),
        )
    }

    @Test
    fun `resolve playback start position clamps negative requested positions to zero`() {
        assertEquals(0, resolvePlaybackStartPositionSeconds("item-1", -12, null))
    }

    @Test
    fun `resolve playback start position ignores stale or completed saved progress`() {
        assertNull(
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = null,
                latestProgress = PlaybackProgressSnapshot(
                    itemId = "item-2",
                    positionMs = 754_000,
                    durationMs = 3_272_000,
                    recordedAtEpochMs = 123L,
                ),
            ),
        )
        assertNull(
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = null,
                latestProgress = PlaybackProgressSnapshot(
                    itemId = "item-1",
                    positionMs = 3_272_000,
                    durationMs = 3_272_000,
                    recordedAtEpochMs = 123L,
                ),
            ),
        )
    }

    private class RecordingHttpURLConnection(
        url: URL,
        private val responseCode: Int,
    ) : HttpURLConnection(url) {
        private val bodyBuffer = ByteArrayOutputStream()

        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getOutputStream(): OutputStream = bodyBuffer

        override fun getResponseCode(): Int = responseCode

        fun bodyAsString(): String = bodyBuffer.toString(StandardCharsets.UTF_8.name())
    }
}
