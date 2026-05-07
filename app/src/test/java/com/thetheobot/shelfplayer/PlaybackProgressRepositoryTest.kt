package com.thetheobot.shelfplayer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
