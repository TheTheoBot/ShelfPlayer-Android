package com.thetheobot.shelfplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
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
        val librariesRoot = Json.parseToJsonElement(librariesJson).jsonObject
        val libraries = librariesRoot["libraries"]?.jsonArray ?: JsonArray(emptyList())

        if (libraries.isEmpty()) {
            return emptyList()
        }

        val firstLibrary = libraries.firstOrNull()?.jsonObject
            ?: throw IOException("Keine gültige Audiobookshelf-Library gefunden")
        val libraryId = firstLibrary.stringValue("id").ifBlank {
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
    val root = Json.parseToJsonElement(payload).jsonObject
    val array = root["results"]?.jsonArray ?: root["items"]?.jsonArray ?: JsonArray(emptyList())

    return buildList {
        for (rowElement in array) {
            val row = rowElement.jsonObject
            val id = row.stringValue("id")
            if (id.isBlank()) continue

            val media = row["media"]?.jsonObject
            val title = row.stringValue("title").ifBlank { "Unbenannter Titel" }
            val author = resolveAuthor(row, media)
            val progressPercent = resolveProgressPercent(row)
            val itemType = resolveItemType(row.stringValue("mediaType"))

            val coverPath = row.stringValue("coverPath")
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

private fun resolveAuthor(row: JsonObject, media: JsonObject?): String {
    val directAuthor = row.stringValue("authorName")
    if (directAuthor.isNotBlank()) return directAuthor

    val mediaAuthor = media?.stringValue("authorName").orEmpty()
    if (mediaAuthor.isNotBlank()) return mediaAuthor

    val metadataAuthor = media?.get("metadata")?.jsonObject?.stringValue("authorName").orEmpty()
    if (metadataAuthor.isNotBlank()) return metadataAuthor

    return "Unbekannt"
}

private fun resolveProgressPercent(row: JsonObject): Int {
    val progress = row.doubleValue("progress")
    if (progress != null) {
        return (progress * 100).toInt().coerceIn(0, 100)
    }

    val progressLastUpdate = row["progressLastUpdate"]?.jsonObject?.doubleValue("progress")
    if (progressLastUpdate != null) {
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

private fun JsonObject.stringValue(key: String): String {
    return (this[key] as? JsonPrimitive)?.content.orEmpty()
}

private fun JsonObject.doubleValue(key: String): Double? {
    return (this[key] as? JsonPrimitive)?.doubleOrNull
}
