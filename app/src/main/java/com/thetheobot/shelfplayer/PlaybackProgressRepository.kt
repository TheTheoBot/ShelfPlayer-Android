package com.thetheobot.shelfplayer

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class PlaybackProgressSnapshot(
    val itemId: String,
    val positionMs: Int,
    val durationMs: Int,
    val syncedAtEpochMs: Long,
)

interface PlaybackProgressRepository {
    val latestProgress: StateFlow<PlaybackProgressSnapshot?>

    suspend fun syncProgress(itemId: String, positionMs: Int, durationMs: Int)
}

internal fun resolvePlaybackStartPositionSeconds(
    itemId: String,
    requestedStartPositionSeconds: Int?,
    latestProgress: PlaybackProgressSnapshot?,
): Int? {
    requestedStartPositionSeconds?.let { return it.coerceAtLeast(0) }

    val savedProgress = latestProgress ?: return null
    if (savedProgress.itemId != itemId || savedProgress.positionMs <= 0) {
        return null
    }

    if (savedProgress.durationMs > 0 && savedProgress.positionMs >= savedProgress.durationMs) {
        return null
    }

    return savedProgress.positionMs / 1000
}

class SharedPreferencesPlaybackProgressRepository(
    private val sharedPreferences: SharedPreferences,
    private val namespace: String? = null,
) : PlaybackProgressRepository {
    private val _latestProgress = MutableStateFlow(readSnapshot())
    override val latestProgress: StateFlow<PlaybackProgressSnapshot?> = _latestProgress

    fun recordProgress(itemId: String, positionMs: Int, durationMs: Int) {
        val snapshot = PlaybackProgressSnapshot(
            itemId = itemId,
            positionMs = positionMs.coerceAtLeast(0),
            durationMs = durationMs.coerceAtLeast(0),
            syncedAtEpochMs = System.currentTimeMillis(),
        )
        sharedPreferences.edit()
            .putString(key(KEY_ITEM_ID), snapshot.itemId)
            .putInt(key(KEY_POSITION_MS), snapshot.positionMs)
            .putInt(key(KEY_DURATION_MS), snapshot.durationMs)
            .putLong(key(KEY_SYNCED_AT_EPOCH_MS), snapshot.syncedAtEpochMs)
            .apply()
        _latestProgress.value = snapshot
    }

    override suspend fun syncProgress(itemId: String, positionMs: Int, durationMs: Int) {
        recordProgress(itemId, positionMs, durationMs)
    }

    private fun readSnapshot(): PlaybackProgressSnapshot? {
        val itemId = sharedPreferences.getString(key(KEY_ITEM_ID), null).orEmpty()
        if (itemId.isBlank()) return null

        val positionMs = sharedPreferences.getInt(key(KEY_POSITION_MS), 0)
        val durationMs = sharedPreferences.getInt(key(KEY_DURATION_MS), 0)
        val syncedAtEpochMs = sharedPreferences.getLong(key(KEY_SYNCED_AT_EPOCH_MS), 0L)
        return PlaybackProgressSnapshot(
            itemId = itemId,
            positionMs = positionMs,
            durationMs = durationMs,
            syncedAtEpochMs = syncedAtEpochMs,
        )
    }

    private fun key(baseKey: String): String {
        return namespace?.takeIf { it.isNotBlank() }?.let { "$it::$baseKey" } ?: baseKey
    }

    private companion object {
        const val KEY_ITEM_ID = "playback_progress_item_id"
        const val KEY_POSITION_MS = "playback_progress_position_ms"
        const val KEY_DURATION_MS = "playback_progress_duration_ms"
        const val KEY_SYNCED_AT_EPOCH_MS = "playback_progress_synced_at_epoch_ms"
    }
}

class AudiobookshelfPlaybackProgressRepository(
    private val connectionProvider: () -> ConnectionCredentials?,
    private val localRepository: SharedPreferencesPlaybackProgressRepository,
    private val connectionFactory: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection },
) : PlaybackProgressRepository {
    override val latestProgress: StateFlow<PlaybackProgressSnapshot?> = localRepository.latestProgress

    override suspend fun syncProgress(itemId: String, positionMs: Int, durationMs: Int) {
        localRepository.syncProgress(itemId, positionMs, durationMs)
        val credentials = connectionProvider() ?: return
        val token = credentials.accessToken.trim()
        if (token.isBlank()) return

        try {
            val normalizedServerUrl = normalizeServerUrl(credentials.serverUrl)
            val requestBody = buildString {
                append('{')
                append("\"currentTime\":")
                append(positionMs.coerceAtLeast(0) / 1000.0)
                append(',')
                append("\"duration\":")
                append(durationMs.coerceAtLeast(0) / 1000.0)
                append(',')
                append("\"isFinished\":")
                append(durationMs > 0 && positionMs >= durationMs)
                append('}')
            }

            val endpoint = URL("$normalizedServerUrl/api/me/progress/${encodeUrlPathSegment(itemId)}")
            if (!executeProgressUpdate(endpoint, token, requestBody, "POST")) {
                executeProgressUpdate(endpoint, token, requestBody, "PUT")
            }
        } catch (_: IOException) {
            // Keep the local progress snapshot even if the remote sync is temporarily unavailable.
        }
    }

    private fun executeProgressUpdate(url: URL, token: String, body: String, method: String): Boolean {
        val connection = connectionFactory(url).apply {
            requestMethod = method
            connectTimeout = 7_000
            readTimeout = 7_000
            doOutput = true
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }

        return try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            if (code in 200..299) {
                true
            } else if (method == "POST" && code in setOf(404, 405, 501)) {
                false
            } else {
                throw IOException("Audiobookshelf progress sync failed ($code)")
            }
        } finally {
            connection.disconnect()
        }
    }
}
