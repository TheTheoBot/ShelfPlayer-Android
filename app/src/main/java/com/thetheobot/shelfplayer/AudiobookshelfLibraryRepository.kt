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
import kotlinx.serialization.json.intOrNull
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

    override suspend fun getItemDetail(itemId: String): LibraryItemDetail? {
        val credentials = connectionProvider()
            ?: throw IOException("Keine gespeicherte Verbindung vorhanden")

        return withContext(Dispatchers.IO) {
            loadLibraryItemDetail(credentials, itemId)
        }
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

    private fun loadLibraryItemDetail(credentials: ConnectionCredentials, itemId: String): LibraryItemDetail? {
        val normalizedServerUrl = normalizeServerUrl(credentials.serverUrl)
        val token = credentials.accessToken.trim()
        val detailUrl = URL("$normalizedServerUrl/api/items/$itemId")
        val detailJson = executeGet(detailUrl, token)
        return parseLibraryItemDetail(detailJson, normalizedServerUrl)
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
            parseLibraryItemSummary(row, serverBaseUrl)?.let { add(it) }
        }
    }
}

internal fun parseLibraryItemDetail(payload: String, serverBaseUrl: String): LibraryItemDetail {
    val root = Json.parseToJsonElement(payload).jsonObject
    val detailObject = root["item"]?.jsonObject
        ?: root["result"]?.jsonObject
        ?: root["results"]?.jsonArray?.firstOrNull()?.jsonObject
        ?: root["media"]?.jsonObject
        ?: root

    val item = parseLibraryItemSummary(detailObject, serverBaseUrl)
        ?: throw IOException("Ungültige Audiobookshelf-Detaildaten")

    return LibraryItemDetail(
        item = item,
        progressPercent = item.progressPercent,
        description = resolveDescription(detailObject),
        chapters = resolveChapters(detailObject),
    )
}

private fun parseLibraryItemSummary(row: JsonObject, serverBaseUrl: String): LibraryItem? {
    val id = row.stringValue("id")
    if (id.isBlank()) return null

    val media = row["media"]?.jsonObject
    val title = row.stringValue("title").ifBlank { "Unbenannter Titel" }
    val author = resolveAuthor(row, media)
    val progressPercent = resolveProgressPercent(row)
    val itemType = resolveItemType(row.stringValue("mediaType").ifBlank { media?.stringValue("mediaType").orEmpty() })

    val coverPath = row.stringValue("coverPath")
    val coverUrl = if (coverPath.isBlank()) {
        "$serverBaseUrl/api/items/$id/cover"
    } else {
        normalizeServerUrl(serverBaseUrl) + "/" + coverPath.trimStart('/')
    }

    return LibraryItem(
        id = id,
        title = title,
        author = author,
        progressPercent = progressPercent,
        itemType = itemType,
        coverUrl = coverUrl,
    )
}

private fun resolveDescription(row: JsonObject): String {
    val metadata = row["metadata"]?.jsonObject
    val media = row["media"]?.jsonObject

    return listOf(
        row.stringValue("description"),
        row.stringValue("summary"),
        metadata?.stringValue("description").orEmpty(),
        metadata?.stringValue("summary").orEmpty(),
        media?.stringValue("description").orEmpty(),
        media?.stringValue("summary").orEmpty(),
    ).firstOrNull { it.isNotBlank() }.orEmpty()
}

private fun resolveChapters(row: JsonObject): List<LibraryChapter> {
    val media = row["media"]?.jsonObject
    val rawChapters = row["chapters"]?.jsonArray
        ?: media?.get("chapters")?.jsonArray
        ?: row["audioFiles"]?.jsonArray?.firstOrNull()?.jsonObject?.get("chapters")?.jsonArray
        ?: JsonArray(emptyList())

    return buildList {
        rawChapters.forEachIndexed { index, element ->
            val chapter = element.jsonObject
            val startSeconds = chapter.intValue("start")
                ?: chapter.intValue("startSeconds")
                ?: chapter.doubleValue("start")?.toInt()
                ?: chapter.doubleValue("startSeconds")?.toInt()
            val endSeconds = chapter.intValue("end")
                ?: chapter.intValue("endSeconds")
                ?: chapter.doubleValue("end")?.toInt()
                ?: chapter.doubleValue("endSeconds")?.toInt()

            add(
                LibraryChapter(
                    id = chapter.stringValue("id").ifBlank { "chapter-${index + 1}" },
                    title = chapter.stringValue("title").ifBlank { "Kapitel ${index + 1}" },
                    startSeconds = startSeconds,
                    endSeconds = endSeconds,
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
    return this[key]?.jsonPrimitive?.content.orEmpty()
}

private fun JsonObject.doubleValue(key: String): Double? {
    return this[key]?.jsonPrimitive?.doubleOrNull
}

private fun JsonObject.intValue(key: String): Int? {
    return this[key]?.jsonPrimitive?.intOrNull ?: this[key]?.jsonPrimitive?.doubleOrNull?.toInt()
}
