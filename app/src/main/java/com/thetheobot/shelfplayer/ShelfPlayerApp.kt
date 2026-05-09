package com.thetheobot.shelfplayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt
import java.util.Locale

internal enum class AppTab(val label: String) {
    Library("Library"),
    Search("Search"),
    Connect("Connect"),
    Player("Player"),
    Settings("Settings"),
}

internal fun bottomNavigationTabs(): List<AppTab> {
    return AppTab.entries.filterNot { it == AppTab.Connect }
}

internal fun playbackActionLabel(
    playbackActiveItemId: String?,
    itemId: String,
    isPreparingPlayback: Boolean,
    isPlayingPlayback: Boolean,
): String {
    if (isPreparingPlayback && playbackActiveItemId == itemId) {
        return "Lädt…"
    }

    return when {
        playbackActiveItemId == itemId && isPlayingPlayback -> "Pause"
        playbackActiveItemId == itemId -> "Resume"
        else -> "Abspielen"
    }
}

internal fun playbackActionEnabled(
    playbackActiveItemId: String?,
    itemId: String,
    isPreparingPlayback: Boolean,
): Boolean {
    return !(isPreparingPlayback && playbackActiveItemId == itemId)
}

internal fun shouldSyncPlaybackProgress(
    playbackActiveItemId: String?,
    itemId: String,
    isPlayingPlayback: Boolean,
    isPreparingPlayback: Boolean,
): Boolean {
    return playbackActiveItemId == itemId && isPlayingPlayback && !isPreparingPlayback
}

internal fun playbackProgressFraction(positionMs: Int, durationMs: Int): Float {
    if (durationMs <= 0) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

internal fun seekPlaybackPosition(positionMs: Int, deltaMs: Int, durationMs: Int): Int {
    val boundedDuration = durationMs.coerceAtLeast(0)
    if (boundedDuration == 0) {
        return (positionMs + deltaMs).coerceAtLeast(0)
    }

    return (positionMs + deltaMs).coerceIn(0, boundedDuration)
}

internal fun formatPlaybackRate(rate: Float): String {
    return if (rate == rate.toInt().toFloat()) {
        "${rate.toInt()}.0x"
    } else {
        String.format(Locale.US, "%.2fx", rate)
    }
}

internal fun buildDirectPlaybackStreamUrl(serverUrl: String, itemId: String): String {
    val encodedItemId = encodeUrlPathSegment(itemId)
    return "${normalizeServerUrl(serverUrl)}/api/items/$encodedItemId/play"
}

internal fun resolvePlaybackFlushPositionMs(
    playbackPositionMs: Int,
    mediaPlayerPositionMs: Int?,
): Int {
    return (mediaPlayerPositionMs ?: playbackPositionMs).coerceAtLeast(0)
}

private data class PlayerStatePresentation(
    val statusText: String,
    val accessibilityStateDescription: String,
)

private fun resolvePlayerStatePresentation(
    isPreparingPlayback: Boolean,
    isPlayingPlayback: Boolean,
    hasActivePlaybackItem: Boolean,
    playbackError: String?,
): PlayerStatePresentation {
    return when {
        !playbackError.isNullOrBlank() -> PlayerStatePresentation(
            statusText = "Fehler",
            accessibilityStateDescription = "Fehler",
        )
        isPreparingPlayback -> PlayerStatePresentation(
            statusText = "Lädt…",
            accessibilityStateDescription = "Lädt",
        )
        isPlayingPlayback -> PlayerStatePresentation(
            statusText = "Wiedergabe läuft",
            accessibilityStateDescription = "Wiedergabe läuft",
        )
        hasActivePlaybackItem -> PlayerStatePresentation(
            statusText = "Pausiert",
            accessibilityStateDescription = "Pausiert",
        )
        else -> PlayerStatePresentation(
            statusText = "Bereit",
            accessibilityStateDescription = "Bereit",
        )
    }
}

internal fun chapterQuickAccessStateDescription(isActiveChapter: Boolean): String {
    return if (isActiveChapter) {
        "Aktuelles Kapitel"
    } else {
        "Kapitel verfügbar"
    }
}

internal fun playerStateStatusText(
    isPreparingPlayback: Boolean,
    isPlayingPlayback: Boolean,
    hasActivePlaybackItem: Boolean,
    playbackError: String?,
): String {
    return resolvePlayerStatePresentation(
        isPreparingPlayback = isPreparingPlayback,
        isPlayingPlayback = isPlayingPlayback,
        hasActivePlaybackItem = hasActivePlaybackItem,
        playbackError = playbackError,
    ).statusText
}

internal fun playerStateAccessibilityDescription(
    isPreparingPlayback: Boolean,
    isPlayingPlayback: Boolean,
    hasActivePlaybackItem: Boolean,
    playbackError: String?,
): String {
    return resolvePlayerStatePresentation(
        isPreparingPlayback = isPreparingPlayback,
        isPlayingPlayback = isPlayingPlayback,
        hasActivePlaybackItem = hasActivePlaybackItem,
        playbackError = playbackError,
    ).accessibilityStateDescription
}

private val playbackRateOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f)
private const val playbackPrepareTimeoutMs = 15_000L
private const val playbackProgressSyncIntervalMs = 30_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShelfPlayerApp(
    launchState: AppLaunchState = AppLaunchState(route = null, eventId = 0, isDeepLink = false),
) {
    val context = LocalContext.current.applicationContext
    var connectionStore by remember { mutableStateOf<ConnectionCredentialsStore?>(null) }
    var connectionStoreReady by remember { mutableStateOf(false) }
    var connectionInitFailed by remember { mutableStateOf(false) }
    var rememberedConnection by remember { mutableStateOf<ConnectionCredentials?>(null) }
    val libraryRepository = remember {
        AudiobookshelfLibraryRepository(
            connectionProvider = { rememberedConnection },
        )
    }
    val appSettingsRepository = remember {
        SharedPreferencesAppSettingsRepository(
            context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE),
        )
    }
    val appSettings by appSettingsRepository.settings.collectAsState()
    val libraryFeedState by libraryRepository.libraryFeedState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Library) }
    var connectionLoadFailed by remember { mutableStateOf(false) }
    var initializationAttempt by rememberSaveable { mutableStateOf(0) }
    var selectedLibraryItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibraryItemDetailState by remember { mutableStateOf<ItemDetailState>(ItemDetailState.Loading) }
    var playbackLibraryItemDetailState by remember { mutableStateOf<ItemDetailState>(ItemDetailState.Loading) }
    var selectedLibraryItemDetailReloadKey by rememberSaveable { mutableStateOf(0) }
    var selectedChapterId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapterStartSeconds by rememberSaveable { mutableStateOf<Int?>(null) }
    var lastPlaybackActiveItemId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    val connectionCredentials = rememberedConnection
    var playbackActiveItemId by remember { mutableStateOf<String?>(null) }
    var isPreparingPlayback by remember { mutableStateOf(false) }
    var isPlayingPlayback by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var playbackDurationMs by remember { mutableStateOf(0) }
    var playbackPositionMs by remember { mutableStateOf(0) }
    var playbackRate by rememberSaveable { mutableStateOf(appSettings.defaultPlaybackRate) }
    var playbackPrepareWatchdogToken by remember { mutableStateOf(0) }
    var playbackProgressSyncJob by remember { mutableStateOf<Job?>(null) }
    val playbackProgressNamespace = rememberedConnection
        ?.serverUrl
        ?.takeIf { it.isNotBlank() }
        ?.let(::normalizeServerUrl)
    val localProgressRepository = remember(playbackProgressNamespace) {
        SharedPreferencesPlaybackProgressRepository(
            context.getSharedPreferences("playback_progress", android.content.Context.MODE_PRIVATE),
            namespace = playbackProgressNamespace,
        )
    }
    val latestPlaybackProgress by localProgressRepository.latestProgress.collectAsState()
    val progressRepository = remember(playbackProgressNamespace) {
        AudiobookshelfPlaybackProgressRepository(
            connectionProvider = { rememberedConnection },
            localRepository = localProgressRepository,
        )
    }
    val playbackProgressScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }

    LaunchedEffect(appSettings.defaultPlaybackRate, playbackActiveItemId, isPlayingPlayback, isPreparingPlayback) {
        if (playbackActiveItemId == null && !isPlayingPlayback && !isPreparingPlayback) {
            playbackRate = appSettings.defaultPlaybackRate
        }
    }

    fun resolveLibraryItem(itemId: String): LibraryItem? {
        val currentItems = when (val state = libraryFeedState) {
            is LibraryFeedState.Loaded -> state.items
            is LibraryFeedState.Refreshing -> state.items
            else -> emptyList()
        }
        return currentItems.firstOrNull { it.id == itemId }
    }

    fun resolveSelectedChapterStartSeconds(chapterId: String): Int? {
        val detail = (selectedLibraryItemDetailState as? ItemDetailState.Loaded)?.detail ?: return null
        return detail.chapters.firstOrNull { it.id == chapterId }?.startSeconds
    }

    fun buildDirectStreamUrl(itemId: String): String? {
        val server = connectionCredentials?.serverUrl?.takeIf { it.isNotBlank() } ?: return null
        return buildDirectPlaybackStreamUrl(server, itemId)
    }

    fun requestPlaybackStreamUrl(itemId: String, accessToken: String): String? {
        val server = connectionCredentials?.serverUrl?.takeIf { it.isNotBlank() } ?: return null
        val normalizedServer = normalizeServerUrl(server)
        val endpoint = java.net.URL("$normalizedServer/api/items/${encodeUrlPathSegment(itemId)}/play")
        val body = "{\"deviceInfo\":{\"clientName\":\"ShelfPlayer Android\",\"clientVersion\":\"0.1.0\",\"sdkVersion\":${android.os.Build.VERSION.SDK_INT}},\"supportedMimeTypes\":[\"audio/mpeg\",\"audio/mp4\",\"audio/aac\",\"audio/flac\",\"audio/ogg\",\"audio/webm\"]}"

        return try {
            val connection = (endpoint.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7_000
                readTimeout = 12_000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $accessToken")
            }

            val responseBody = try {
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                if (code !in 200..299) {
                    null
                } else {
                    connection.inputStream.bufferedReader().use { it.readText() }
                }
            } finally {
                connection.disconnect()
            }

            val relativeContentUrl = responseBody
                ?.let { kotlinx.serialization.json.Json.parseToJsonElement(it).jsonObject }
                ?.get("audioTracks")
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("contentUrl")
                ?.jsonPrimitive
                ?.content
                ?.takeIf { it.isNotBlank() }

            if (relativeContentUrl != null) {
                "$normalizedServer${if (relativeContentUrl.startsWith('/')) "" else "/"}$relativeContentUrl"
            } else {
                buildDirectStreamUrl(itemId)
            }
        } catch (_: Throwable) {
            buildDirectStreamUrl(itemId)
        }
    }

    fun applyPlaybackRate(player: android.media.MediaPlayer, rate: Float) {
        val playbackParams = player.playbackParams ?: android.media.PlaybackParams()
        player.playbackParams = playbackParams.setSpeed(rate)
    }

    fun syncPlaybackProgressNow() {
        val itemId = playbackActiveItemId ?: return
        val durationMs = playbackDurationMs
        if (durationMs <= 0) return
        val positionMs = playbackPositionMs.coerceIn(0, durationMs)
        playbackProgressScope.launch {
            progressRepository.syncProgress(itemId, positionMs, durationMs)
        }
    }

    fun startPlaybackProgressSyncLoop(itemId: String) {
        playbackProgressSyncJob?.cancel()
        playbackProgressSyncJob = playbackProgressScope.launch {
            while (shouldSyncPlaybackProgress(playbackActiveItemId, itemId, isPlayingPlayback, isPreparingPlayback)) {
                delay(playbackProgressSyncIntervalMs)
                if (shouldSyncPlaybackProgress(playbackActiveItemId, itemId, isPlayingPlayback, isPreparingPlayback)) {
                    syncPlaybackProgressNow()
                }
            }
        }
    }

    fun stopPlaybackProgressSyncLoop() {
        playbackProgressSyncJob?.cancel()
        playbackProgressSyncJob = null
    }

    fun clearPlaybackStateWithoutSync() {
        stopPlaybackProgressSyncLoop()
        playbackPrepareWatchdogToken += 1
        mediaPlayer?.release()
        mediaPlayer = null
        playbackActiveItemId = null
        isPreparingPlayback = false
        isPlayingPlayback = false
        playbackDurationMs = 0
        playbackPositionMs = 0
        playbackError = null
    }

    fun releasePlayback() {
        val activeItemId = playbackActiveItemId
        if (activeItemId != null && playbackDurationMs > 0) {
            syncPlaybackProgressNow()
        }
        clearPlaybackStateWithoutSync()
    }

    fun resetToLibraryRootForConnectionChange() {
        releasePlayback()
        selectedTab = AppTab.Library
        selectedLibraryItemId = null
        selectedLibraryItemDetailState = ItemDetailState.Loading
        selectedLibraryItemDetailReloadKey++
        selectedChapterId = null
        selectedChapterStartSeconds = null
        playbackError = null
    }

    fun resetSelectedLibraryItemDetail() {
        val resetState = defaultItemDetailResetState()
        selectedTab = resetState.selectedTab
        selectedLibraryItemDetailState = resetState.selectedLibraryItemDetailState
        selectedLibraryItemId = resetState.selectedLibraryItemId
        selectedChapterId = resetState.selectedChapterId
        selectedChapterStartSeconds = resetState.selectedChapterStartSeconds
    }

    fun flushPlaybackProgressOnDispose() {
        val activeItemId = playbackActiveItemId
        if (activeItemId != null && playbackDurationMs > 0) {
            val durationMs = playbackDurationMs
            val positionMs = resolvePlaybackFlushPositionMs(playbackPositionMs, mediaPlayer?.currentPosition)
            val flushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            flushScope.launch {
                localProgressRepository.recordProgress(
                    itemId = activeItemId,
                    positionMs = positionMs.coerceAtMost(durationMs),
                    durationMs = durationMs,
                    persistSynchronously = true,
                )
                progressRepository.syncProgress(
                    itemId = activeItemId,
                    positionMs = positionMs.coerceAtMost(durationMs),
                    durationMs = durationMs,
                    persistLocalSnapshot = false,
                )
            }.invokeOnCompletion { flushScope.cancel() }
        }
        clearPlaybackStateWithoutSync()
    }

    fun startPlayback(itemId: String, startPositionSeconds: Int? = selectedChapterStartSeconds) {
        val token = connectionCredentials?.accessToken?.takeIf { it.isNotBlank() }
        val streamUrl = token?.let { requestPlaybackStreamUrl(itemId, it) }
        if (streamUrl == null || token == null) {
            playbackError = "Fehlende Verbindung oder kein Titel ausgewählt"
            return
        }

        releasePlayback()
        playbackError = null
        playbackRate = appSettings.defaultPlaybackRate

        val watchdogToken = playbackPrepareWatchdogToken + 1
        playbackPrepareWatchdogToken = watchdogToken

        val player = android.media.MediaPlayer()
        mediaPlayer = player
        playbackActiveItemId = itemId
        isPreparingPlayback = true

        player.setOnPreparedListener {
            playbackPrepareWatchdogToken += 1
            isPreparingPlayback = false
            playbackDurationMs = it.duration.coerceAtLeast(0)
            val effectiveStartPositionSeconds = resolvePlaybackStartPositionSeconds(
                itemId = itemId,
                requestedStartPositionSeconds = startPositionSeconds,
                latestProgress = latestPlaybackProgress,
            )
            val startPositionMs = effectiveStartPositionSeconds?.coerceAtLeast(0)?.times(1000)
            if (startPositionMs != null && startPositionMs in 0..playbackDurationMs) {
                it.seekTo(startPositionMs)
                playbackPositionMs = startPositionMs
            } else {
                playbackPositionMs = 0
            }
            applyPlaybackRate(it, playbackRate)
            it.start()
            isPlayingPlayback = true
            startPlaybackProgressSyncLoop(itemId)
        }
        player.setOnCompletionListener {
            isPlayingPlayback = false
            stopPlaybackProgressSyncLoop()
            playbackPositionMs = playbackDurationMs
            syncPlaybackProgressNow()
        }
        player.setOnErrorListener { _, _, _ ->
            playbackPrepareWatchdogToken += 1
            releasePlayback()
            playbackError = "Wiedergabe konnte nicht gestartet werden"
            true
        }

        try {
            player.setDataSource(
                context,
                android.net.Uri.parse(streamUrl),
                mapOf("Authorization" to "Bearer $token"),
            )
            player.prepareAsync()
        } catch (throwable: Throwable) {
            playbackPrepareWatchdogToken += 1
            clearPlaybackStateWithoutSync()
            playbackError = throwable.message?.takeIf { it.isNotBlank() }
                ?: "Wiedergabe konnte nicht gestartet werden"
            return
        }
    }

    fun pausePlayback() {
        mediaPlayer?.takeIf { it.isPlaying }?.pause()
        playbackPositionMs = mediaPlayer?.currentPosition?.coerceAtLeast(0) ?: playbackPositionMs
        syncPlaybackProgressNow()
        stopPlaybackProgressSyncLoop()
        isPlayingPlayback = false
    }

    fun resumePlayback() {
        val player = mediaPlayer ?: return
        if (isPreparingPlayback) return
        if (playbackDurationMs > 0 && playbackPositionMs >= playbackDurationMs) {
            player.seekTo(0)
            playbackPositionMs = 0
        }
        applyPlaybackRate(player, playbackRate)
        player.start()
        isPlayingPlayback = true
        playbackActiveItemId?.let { startPlaybackProgressSyncLoop(it) }
    }

    fun seekPlayback(positionMs: Int) {
        val player = mediaPlayer ?: return
        val targetPositionMs = seekPlaybackPosition(
            positionMs = positionMs,
            deltaMs = 0,
            durationMs = playbackDurationMs,
        )
        player.seekTo(targetPositionMs)
        playbackPositionMs = targetPositionMs
    }

    fun skipPlayback(deltaSeconds: Int) {
        if (playbackDurationMs <= 0) return
        seekPlayback(
            seekPlaybackPosition(
                positionMs = playbackPositionMs,
                deltaMs = deltaSeconds * 1000,
                durationMs = playbackDurationMs,
            ),
        )
    }

    fun changePlaybackRate(rate: Float) {
        playbackRate = rate
        mediaPlayer?.let { applyPlaybackRate(it, rate) }
    }

    fun togglePlayback(itemId: String, startPositionSeconds: Int? = selectedChapterStartSeconds) {
        if (playbackActiveItemId != itemId || mediaPlayer == null) {
            startPlayback(itemId, startPositionSeconds)
            return
        }

        if (isPreparingPlayback) {
            return
        }

        if (isPlayingPlayback) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun playbackActionLabelFor(itemId: String): String {
        return playbackActionLabel(
            playbackActiveItemId = playbackActiveItemId,
            itemId = itemId,
            isPreparingPlayback = isPreparingPlayback,
            isPlayingPlayback = isPlayingPlayback,
        )
    }

    fun playbackActionEnabledFor(itemId: String): Boolean {
        return playbackActionEnabled(
            playbackActiveItemId = playbackActiveItemId,
            itemId = itemId,
            isPreparingPlayback = isPreparingPlayback,
        )
    }

    LaunchedEffect(context, initializationAttempt) {
        val initializedStoreResult = runSuspendCatchingPreservingCancellation {
            withContext(Dispatchers.IO) {
                EncryptedConnectionCredentialsStore.create(context)
            }
        }

        val initializedStore = initializedStoreResult.getOrNull()
        if (initializedStore == null) {
            connectionStoreReady = true
            connectionInitFailed = true
            return@LaunchedEffect
        }

        val loadedConnection = runSuspendCatchingPreservingCancellation {
            withContext(Dispatchers.IO) {
                initializedStore.load()
            }
        }
        connectionStore = initializedStore
        connectionLoadFailed = loadedConnection.isFailure
        rememberedConnection = loadedConnection.getOrNull()
        connectionStoreReady = true
    }

    fun retryConnectionStoreInitialization() {
        connectionStore = null
        rememberedConnection = null
        connectionLoadFailed = false
        connectionInitFailed = false
        connectionStoreReady = false
        initializationAttempt++
    }

    val connectionSession = ConnectionSession(savedConnection = rememberedConnection)
    val rootState = resolveAppRootState(
        connectionStoreReady = connectionStoreReady,
        connectionInitFailed = connectionInitFailed,
        connectionLoadFailed = connectionLoadFailed,
        connectionSession = connectionSession,
    )

    LaunchedEffect(rootState, launchState) {
        if (rootState != AppRootState.Ready) return@LaunchedEffect
        if (shouldIgnoreDeepLinkLaunch(launchState.route, launchState.isDeepLink)) return@LaunchedEffect

        if (launchState.route == null) {
            if (!shouldResetToDefaultLibraryState(selectedTab, selectedLibraryItemId)) return@LaunchedEffect
            selectedTab = AppTab.Library
            selectedLibraryItemId = null
            selectedLibraryItemDetailState = ItemDetailState.Loading
            selectedLibraryItemDetailReloadKey++
            selectedChapterId = null
            selectedChapterStartSeconds = null
            return@LaunchedEffect
        }

        val launchSelection = appLaunchSelectionForRoute(launchState.route) ?: return@LaunchedEffect
        if (!shouldApplyAppLaunchSelection(selectedTab, selectedLibraryItemId, launchSelection)) return@LaunchedEffect

        selectedTab = launchSelection.tab
        selectedLibraryItemId = launchSelection.itemId
        selectedLibraryItemDetailState = ItemDetailState.Loading
        selectedLibraryItemDetailReloadKey++
        selectedChapterId = null
        selectedChapterStartSeconds = null
    }

    val selectedLibraryItemDetail = selectedLibraryItemDetailState as? ItemDetailState.Loaded
    val playbackLibraryItemDetail = playbackLibraryItemDetailState as? ItemDetailState.Loaded
    val playerSelectedChapterContext = resolvePlayerSelectedChapterContext(
        playbackActiveItemId = playbackActiveItemId,
        playbackLibraryItemDetail = playbackLibraryItemDetail?.detail,
        selectedChapterId = selectedChapterId,
        selectedChapterStartSeconds = selectedChapterStartSeconds,
    )

    LaunchedEffect(selectedLibraryItemId, selectedLibraryItemDetailReloadKey) {
        val itemId = selectedLibraryItemId ?: return@LaunchedEffect
        selectedLibraryItemDetailState = ItemDetailState.Loading
        selectedLibraryItemDetailState = runSuspendCatchingPreservingCancellation {
            withContext(Dispatchers.IO) {
                libraryRepository.getItemDetail(itemId)
            }
        }.fold(
            onSuccess = { detail ->
                detail?.let { ItemDetailState.Loaded(it) }
                    ?: ItemDetailState.Error("Detaildaten konnten nicht gefunden werden")
            },
            onFailure = { throwable ->
                ItemDetailState.Error(throwable.message ?: "Detaildaten konnten nicht geladen werden")
            },
        )
    }

    LaunchedEffect(playbackActiveItemId) {
        if (shouldResetSelectedChapterContext(lastPlaybackActiveItemId, playbackActiveItemId)) {
            selectedChapterId = null
            selectedChapterStartSeconds = null
        }
        lastPlaybackActiveItemId = playbackActiveItemId
        val itemId = playbackActiveItemId ?: run {
            playbackLibraryItemDetailState = ItemDetailState.Loading
            return@LaunchedEffect
        }
        playbackLibraryItemDetailState = ItemDetailState.Loading
        playbackLibraryItemDetailState = runSuspendCatchingPreservingCancellation {
            withContext(Dispatchers.IO) {
                libraryRepository.getItemDetail(itemId)
            }
        }.fold(
            onSuccess = { detail ->
                detail?.let { ItemDetailState.Loaded(it) }
                    ?: ItemDetailState.Error("Detaildaten konnten nicht gefunden werden")
            },
            onFailure = { throwable ->
                ItemDetailState.Error(throwable.message ?: "Detaildaten konnten nicht geladen werden")
            },
        )
    }

    LaunchedEffect(playbackPrepareWatchdogToken, isPreparingPlayback, playbackActiveItemId) {
        val itemId = playbackActiveItemId ?: return@LaunchedEffect
        if (!isPreparingPlayback) return@LaunchedEffect
        val tokenAtStart = playbackPrepareWatchdogToken
        delay(playbackPrepareTimeoutMs)
        if (
            isPreparingPlayback &&
            playbackActiveItemId == itemId &&
            playbackPrepareWatchdogToken == tokenAtStart
        ) {
            releasePlayback()
            playbackError = "Wiedergabe-Start timeout nach 15s. Bitte Verbindung/Server prüfen."
        }
    }

    LaunchedEffect(mediaPlayer, isPlayingPlayback) {
        while (mediaPlayer != null && isPlayingPlayback) {
            playbackPositionMs = mediaPlayer?.currentPosition?.coerceAtLeast(0) ?: 0
            kotlinx.coroutines.delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            flushPlaybackProgressOnDispose()
            playbackProgressScope.cancel()
        }
    }

    val darkTheme = shouldUseDarkTheme(appSettings.themeMode, isSystemInDarkTheme())

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        when (rootState) {
            AppRootState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("ShelfPlayer wird vorbereitet…")
                }
            }

            AppRootState.NoConnection -> {
                ConnectionScreen(
                    padding = PaddingValues(0.dp),
                    connectionSession = connectionSession,
                    onConnectionSaved = { savedConnection ->
                        runSuspendCatchingPreservingCancellation {
                            withContext(Dispatchers.IO) {
                                connectionStore?.save(savedConnection) ?: false
                            }
                        }.getOrDefault(false).also { saved ->
                            if (saved) {
                                connectionLoadFailed = false
                                resetToLibraryRootForConnectionChange()
                                rememberedConnection = savedConnection
                            }
                        }
                    },
                )
            }

            AppRootState.LoadError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("ShelfPlayer konnte die gespeicherte Verbindung nicht laden.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { retryConnectionStoreInitialization() }) {
                        Text("Nochmal versuchen")
                    }
                }
            }

            AppRootState.FatalError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("ShelfPlayer konnte den Verbindungsspeicher nicht initialisieren.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { retryConnectionStoreInitialization() }) {
                        Text("Nochmal versuchen")
                    }
                }
            }

            AppRootState.Ready -> {
                val backNavigation = resolveAppBackNavigation(selectedTab, selectedLibraryItemId)
                BackHandler(enabled = backNavigation != AppBackNavigation.Unhandled) {
                    when (backNavigation) {
                        AppBackNavigation.CloseItemDetail -> {
                            resetSelectedLibraryItemDetail()
                        }

                        AppBackNavigation.SwitchToLibrary -> {
                            selectedTab = AppTab.Library
                        }

                        AppBackNavigation.Unhandled -> Unit
                    }
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ShelfPlayer · ${selectedTab.label}")
                                    Text(
                                        connectionSessionStatusText(connectionSession),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    if (connectionLoadFailed) {
                                        Text(
                                            "Gespeicherte Verbindung konnte nicht geladen werden",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            },
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            bottomNavigationTabs().forEach { tab ->
                                val icon = when (tab) {
                                    AppTab.Library -> Icons.Rounded.Headphones
                                    AppTab.Search -> Icons.Rounded.Search
                                    AppTab.Player -> Icons.Rounded.PlayArrow
                                    AppTab.Settings -> Icons.Rounded.Settings
                                    AppTab.Connect -> Icons.Rounded.Link
                                }
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = { Icon(icon, contentDescription = null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    }
                ) { padding ->
                    when (selectedTab) {
                        AppTab.Library -> {
                            if (selectedLibraryItemId != null) {
                                val currentItemId = selectedLibraryItemId ?: ""
                                ItemDetailScreen(
                                    padding = padding,
                                    state = selectedLibraryItemDetailState,
                                    selectedChapterId = selectedChapterId,
                                    playbackActionLabel = playbackActionLabelFor(currentItemId),
                                    playbackActionEnabled = playbackActionEnabledFor(currentItemId),
                                    onBackClick = {
                                        resetSelectedLibraryItemDetail()
                                    },
                                    onPlaybackAction = {
                                        selectedTab = AppTab.Player
                                        selectedLibraryItemId?.let { itemId ->
                                            togglePlayback(itemId, selectedChapterStartSeconds)
                                        }
                                    },
                                    onChapterSelected = { chapterId ->
                                        val startSeconds = resolveSelectedChapterStartSeconds(chapterId)
                                        selectedChapterId = chapterId
                                        selectedChapterStartSeconds = startSeconds
                                        selectedTab = AppTab.Player
                                        selectedLibraryItemId?.let { itemId ->
                                            startPlayback(itemId, startSeconds)
                                        }
                                    },
                                    onRetry = {
                                        selectedLibraryItemDetailReloadKey++
                                    },
                                )
                            } else {
                                LibraryScreen(
                                    padding = padding,
                                    repository = libraryRepository,
                                    onAppear = { libraryRepository.refresh() },
                                    onRefresh = { libraryRepository.refresh() },
                                    onItemClick = { itemId ->
                                        selectedLibraryItemId = itemId
                                        selectedLibraryItemDetailState = ItemDetailState.Loading
                                        selectedChapterId = null
                                        selectedChapterStartSeconds = null
                                        selectedLibraryItemDetailReloadKey++
                                    },
                                    onPlayClick = { itemId ->
                                        selectedLibraryItemId = itemId
                                        selectedLibraryItemDetailState = ItemDetailState.Loading
                                        selectedChapterId = null
                                        selectedChapterStartSeconds = null
                                        selectedTab = AppTab.Player
                                        startPlayback(itemId)
                                        selectedLibraryItemDetailReloadKey++
                                    },
                                )
                            }
                        }
                        AppTab.Search -> SearchScreen(
                            padding = padding,
                            repository = libraryRepository,
                            onResultClick = { itemId ->
                                selectedLibraryItemId = itemId
                                selectedLibraryItemDetailState = ItemDetailState.Loading
                                selectedChapterId = null
                                selectedChapterStartSeconds = null
                                selectedLibraryItemDetailReloadKey++
                                selectedTab = AppTab.Library
                            },
                        )
                        AppTab.Connect -> ConnectionScreen(
                            padding = padding,
                            connectionSession = connectionSession,
                            onConnectionSaved = { savedConnection ->
                                runSuspendCatchingPreservingCancellation {
                                    withContext(Dispatchers.IO) {
                                        connectionStore?.save(savedConnection) ?: false
                                    }
                                }.getOrDefault(false).also { saved ->
                                    if (saved) {
                                        connectionLoadFailed = false
                                        resetToLibraryRootForConnectionChange()
                                        rememberedConnection = savedConnection
                                    }
                                }
                            },
                        )
                        AppTab.Player -> {
                            val activePlaybackItem = playbackActiveItemId?.let { resolveLibraryItem(it) }
                            val playerSelectedChapterLabel = playerSelectedChapterContext.label
                            val playerSelectedChapterStartSeconds = playerSelectedChapterContext.startSeconds
                            val playbackResumeHint = activePlaybackItem?.id?.let { summarizeLatestPlaybackProgress(it, latestPlaybackProgress) }
                            PlayerScreen(
                                padding = padding,
                                activeLibraryItem = activePlaybackItem,
                                selectedChapterLabel = playerSelectedChapterLabel,
                                selectedChapterStartSeconds = playerSelectedChapterStartSeconds,
                                playbackResumeHint = playbackResumeHint,
                                isPreparingPlayback = isPreparingPlayback,
                                isPlayingPlayback = isPlayingPlayback,
                                hasActivePlaybackItem = playbackActiveItemId != null,
                                playbackRate = playbackRate,
                                skipIntervalSeconds = appSettings.playbackSkipIntervalSeconds,
                                playbackError = playbackError,
                                playbackDurationMs = playbackDurationMs,
                                playbackPositionMs = playbackPositionMs,
                                chapterQuickAccess = playbackLibraryItemDetail?.detail?.chapters.orEmpty(),
                                onPlay = {
                                    activePlaybackItem?.id?.let { itemId ->
                                        startPlayback(itemId, playerSelectedChapterStartSeconds)
                                    }
                                },
                                onPauseResume = {
                                    if (isPlayingPlayback) {
                                        pausePlayback()
                                    } else {
                                        resumePlayback()
                                    }
                                },
                                onSeekTo = { seekPlayback(it) },
                                onSkipBackward = { skipPlayback(-appSettings.playbackSkipIntervalSeconds) },
                                onSkipForward = { skipPlayback(appSettings.playbackSkipIntervalSeconds) },
                                onRateChanged = { changePlaybackRate(it) },
                                onChapterSelected = { chapter ->
                                    selectedChapterId = chapter.id
                                    selectedChapterStartSeconds = chapter.startSeconds
                                    seekPlayback((chapter.startSeconds ?: 0) * 1000)
                                },
                                onStop = { releasePlayback() },
                            )
                        }
                        AppTab.Settings -> SettingsScreen(
                            padding = padding,
                            appSettingsRepository = appSettingsRepository,
                            connectionSession = connectionSession,
                            onOpenConnectTab = { selectedTab = AppTab.Connect },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(
    padding: PaddingValues,
    activeLibraryItem: LibraryItem?,
    selectedChapterLabel: String?,
    selectedChapterStartSeconds: Int?,
    playbackResumeHint: String?,
    isPreparingPlayback: Boolean,
    isPlayingPlayback: Boolean,
    hasActivePlaybackItem: Boolean,
    playbackRate: Float,
    skipIntervalSeconds: Int,
    playbackError: String?,
    playbackDurationMs: Int,
    playbackPositionMs: Int,
    chapterQuickAccess: List<LibraryChapter>,
    onPlay: () -> Unit,
    onPauseResume: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onRateChanged: (Float) -> Unit,
    onChapterSelected: (LibraryChapter) -> Unit,
    onStop: () -> Unit,
) {
    val progressFraction = playbackProgressFraction(playbackPositionMs, playbackDurationMs)
    var seekFraction by rememberSaveable { mutableStateOf(progressFraction) }
    var isSeeking by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(progressFraction, isSeeking) {
        if (!isSeeking) {
            seekFraction = progressFraction
        }
    }

    val displayedPositionMs = if (isSeeking && playbackDurationMs > 0) {
        (seekFraction * playbackDurationMs).roundToInt()
    } else {
        playbackPositionMs
    }
    val activeQuickAccessChapter = resolveActiveChapterForPlaybackPosition(chapterQuickAccess, displayedPositionMs)
    val playerStatusText = playerStateStatusText(
        isPreparingPlayback = isPreparingPlayback,
        isPlayingPlayback = isPlayingPlayback,
        hasActivePlaybackItem = hasActivePlaybackItem,
        playbackError = playbackError,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Now Playing", style = MaterialTheme.typography.labelLarge)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .semantics {
                    stateDescription = playerStateAccessibilityDescription(
                        isPreparingPlayback = isPreparingPlayback,
                        isPlayingPlayback = isPlayingPlayback,
                        hasActivePlaybackItem = hasActivePlaybackItem,
                        playbackError = playbackError,
                    )
                },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
        ) {
            Text(
                playerStatusText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (activeLibraryItem == null) {
            Text(
                "Kein Titel ausgewählt",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Wähle einen Eintrag aus der Bibliothek.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        Text(
            activeLibraryItem.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(activeLibraryItem.author, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${formatItemType(activeLibraryItem.itemType)} · Fortschritt ${formatProgress(activeLibraryItem.progressPercent)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        selectedChapterLabel?.let { label ->
            Text(
                "Ausgewähltes Kapitel: $label",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        selectedChapterStartSeconds?.let {
            Text(
                "Kapitelstart ab ${formatDuration(it * 1000)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Kapitel-Kontext",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    playerChapterContextDisplayText(activeQuickAccessChapter, selectedChapterLabel),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        playbackResumeHint?.let { hint ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Fortsetzen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${formatDuration(displayedPositionMs)} / ${formatDuration(playbackDurationMs)}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "Verbleibend ${formatDuration((playbackDurationMs - displayedPositionMs).coerceAtLeast(0))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = seekFraction,
                onValueChange = {
                    isSeeking = true
                    seekFraction = it
                },
                onValueChangeFinished = {
                    val targetPositionMs = if (playbackDurationMs > 0) {
                        (seekFraction * playbackDurationMs).roundToInt().coerceIn(0, playbackDurationMs)
                    } else {
                        0
                    }
                    onSeekTo(targetPositionMs)
                    isSeeking = false
                },
                enabled = playbackDurationMs > 0 && !isPreparingPlayback,
                modifier = Modifier.fillMaxWidth(0.84f),
            )
        }

        if (chapterQuickAccess.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kapitel-Quick-Access", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chapterQuickAccess.take(4).forEach { chapter ->
                        val isActiveChapter = chapter.id == activeQuickAccessChapter?.id
                        val chapterButtonModifier = Modifier.semantics {
                            stateDescription = chapterQuickAccessStateDescription(isActiveChapter)
                        }
                        if (isActiveChapter) {
                            Button(
                                modifier = chapterButtonModifier,
                                onClick = { onChapterSelected(chapter) },
                                enabled = !isPreparingPlayback,
                            ) {
                                Text(chapter.title)
                            }
                        } else {
                            OutlinedButton(
                                modifier = chapterButtonModifier,
                                onClick = { onChapterSelected(chapter) },
                                enabled = !isPreparingPlayback,
                            ) {
                                Text(chapter.title)
                            }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSkipBackward, enabled = playbackDurationMs > 0 && !isPreparingPlayback) {
                Icon(Icons.Rounded.FastRewind, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${skipIntervalSeconds}s")
            }
            Button(onClick = onPlay, enabled = !isPreparingPlayback) {
                Text(if (isPreparingPlayback) "Lädt…" else "Play")
            }
            Button(
                onClick = onPauseResume,
                enabled = !isPreparingPlayback && playbackDurationMs > 0,
            ) {
                Text(if (isPlayingPlayback) "Pause" else "Resume")
            }
            Button(onClick = onSkipForward, enabled = playbackDurationMs > 0 && !isPreparingPlayback) {
                Text("${skipIntervalSeconds}s")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Rounded.FastForward, contentDescription = null)
            }
            Button(onClick = onStop, enabled = playbackDurationMs > 0 || isPreparingPlayback) {
                Text("Stop")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Wiedergabegeschwindigkeit", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                playbackRateOptions.forEach { rate ->
                    val selected = rate == playbackRate
                    val rateButtonLabel = formatPlaybackRate(rate)
                    if (selected) {
                        Button(onClick = { onRateChanged(rate) }, enabled = !isPreparingPlayback) {
                            Text(rateButtonLabel)
                        }
                    } else {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { onRateChanged(rate) },
                            enabled = !isPreparingPlayback,
                        ) {
                            Text(rateButtonLabel)
                        }
                    }
                }
            }
        }

        playbackError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatDuration(milliseconds: Int): String {
    if (milliseconds <= 0) return "00:00"
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
