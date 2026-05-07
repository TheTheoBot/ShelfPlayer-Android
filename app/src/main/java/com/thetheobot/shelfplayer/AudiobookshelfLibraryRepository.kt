package com.thetheobot.shelfplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AudiobookshelfLibraryRepository(
    private val connectionProvider: () -> ConnectionCredentials?,
    private val connectionFactory: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection },
) : LibraryRepository {
    private val _libraryFeedState = MutableStateFlow<LibraryFeedState>(LibraryFeedState.Loading)
    override val libraryFeedState: StateFlow<LibraryFeedState> = _libraryFeedState

    override suspend fun refresh() {
        val currentItems = _libraryFeedState.value.visibleItems()
        _libraryFeedState.value = LibraryFeedState.Refreshing(currentItems)

        val credentials = connectionProvider()
        if (credentials == null) {
            _libraryFeedState.value = LibraryFeedState.Error("Keine gespeicherte Verbindung vorhanden")
            return
        }

        val result = runSuspendCatchingPreservingCancellation {
            withContext(Dispatchers.IO) {
                loadLibraryItems(credentials)
            }
        }

        _libraryFeedState.value = result.fold(
            onSuccess = { items -> libraryFeedStateOf(items) },
            onFailure = { throwable ->
                LibraryFeedState.Error(
                    throwable.message ?: "Bibliothek konnte nicht geladen werden",
                )
            },
        )
    }

    private fun loadLibraryItems(credentials: ConnectionCredentials): List<LibraryItem> {
        val normalizedServerUrl = normalizeServerUrl(credentials.serverUrl)
        val token = credentials.accessToken.trim()

        val librariesUrl = URL("$normalizedServerUrl/api/libraries")
        val librariesJson = executeGet(librariesUrl, token)
        val libraries = JSONObject(librariesJson).optJSONArray("libraries") ?: JSONArray()

        if (libraries.length() == 0) {
            return emptyList()
        }

        val firstLibrary = libraries.optJSONObject(0)
            ?: throw IOException("Keine gültige Audiobookshelf-Library gefunden")
        val libraryId = firstLibrary.optString("id").ifBlank {
            throw IOException("Audiobookshelf-Library-ID fehlt")
        }

        val itemsUrl = URL("$normalizedServerUrl/api/libraries/$libraryId/items?limit=50")
        val itemsJson = executeGet(itemsUrl, token)
        return parseLibraryItems(itemsJson, normalizedServerUrl)
    }

    private fun executeGet(url: URL, token: String): String {
        val connection = connectionFactory(url).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 7_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("Audiobookshelf API Fehler ($code)${errorBody?.let { ": $it" } ?: ""}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun parseLibraryItems(payload: String, serverBaseUrl: String): List<LibraryItem> {
    val root = JSONObject(payload)
    val array = root.optJSONArray("results") ?: root.optJSONArray("items") ?: JSONArray()

    return buildList {
        for (index in 0 until array.length()) {
            val row = array.optJSONObject(index) ?: continue
            val id = row.optString("id").ifBlank { continue }
            val media = row.optJSONObject("media")

            val title = row.optString("title").ifBlank { "Unbenannter Titel" }
            val author = resolveAuthor(row, media)
            val progressPercent = resolveProgressPercent(row)
            val itemType = resolveItemType(row.optString("mediaType"))

            val coverPath = row.optString("coverPath")
            val coverUrl = if (coverPath.isBlank()) {
                "$serverBaseUrl/api/items/$id/cover"
            } else {
                normalizeServerUrl(serverBaseUrl) + "/" + coverPath.trimStart('/')
            }

            add(
                LibraryItem(
                    id = id,
                    title = title,
                    author = author,
                    progressPercent = progressPercent,
                    itemType = itemType,
                    coverUrl = coverUrl,
                )
            )
        }
    }
}

private fun resolveAuthor(row: JSONObject, media: JSONObject?): String {
    val directAuthor = row.optString("authorName")
    if (directAuthor.isNotBlank()) return directAuthor

    val mediaAuthor = media?.optString("authorName")
    if (!mediaAuthor.isNullOrBlank()) return mediaAuthor

    val metadataAuthor = media?.optJSONObject("metadata")?.optString("authorName")
    if (!metadataAuthor.isNullOrBlank()) return metadataAuthor

    return "Unbekannt"
}

private fun resolveProgressPercent(row: JSONObject): Int {
    val progress = row.optDouble("progress", Double.NaN)
    if (!progress.isNaN()) {
        return (progress * 100).toInt().coerceIn(0, 100)
    }

    val progressLastUpdate = row.optJSONObject("progressLastUpdate")?.optDouble("progress", Double.NaN)
    if (progressLastUpdate != null && !progressLastUpdate.isNaN()) {
        return (progressLastUpdate * 100).toInt().coerceIn(0, 100)
    }

    return 0
}

private fun resolveItemType(mediaType: String): LibraryItemType {
    return when (mediaType.lowercase()) {
        "podcast" -> LibraryItemType.Podcast
        "series" -> LibraryItemType.Series
        "book", "ebook" -> LibraryItemType.Book
        else -> LibraryItemType.Audiobook
    }
}
