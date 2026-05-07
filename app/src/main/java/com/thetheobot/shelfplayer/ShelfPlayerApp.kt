package com.thetheobot.shelfplayer

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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Link
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
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import java.util.Locale

private enum class AppTab(val label: String) {
    Library("Library"),
    Connect("Connect"),
    Player("Player"),
    Settings("Settings"),
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

private val playbackRateOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f)
private const val playbackPrepareTimeoutMs = 15_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfPlayerApp() {
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
    var selectedLibraryItemDetailReloadKey by rememberSaveable { mutableStateOf(0) }
    var selectedChapterId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapterStartSeconds by rememberSaveable { mutableStateOf<Int?>(null) }
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

    fun buildStreamUrl(itemId: String, accessToken: String): String? {
        val server = connectionCredentials?.serverUrl?.takeIf { it.isNotBlank() } ?: return null
        val encodedItemId = encodeUrlPathSegment(itemId)
        val encodedToken = encodeUrlPathSegment(accessToken)
        return "${normalizeServerUrl(server)}/api/items/$encodedItemId/play?token=$encodedToken"
    }

    fun applyPlaybackRate(player: android.media.MediaPlayer, rate: Float) {
        val playbackParams = player.playbackParams ?: android.media.PlaybackParams()
        player.playbackParams = playbackParams.setSpeed(rate)
    }

    fun syncPlaybackProgressNow(blocking: Boolean = false) {
        val itemId = playbackActiveItemId ?: return
        val durationMs = playbackDurationMs
        if (durationMs <= 0) return
        val positionMs = playbackPositionMs.coerceIn(0, durationMs)
        localProgressRepository.recordProgress(itemId, positionMs, durationMs)
        if (blocking) {
            runBlocking(Dispatchers.IO) {
                progressRepository.syncProgress(itemId, positionMs, durationMs)
            }
        } else {
            playbackProgressScope.launch {
                withContext(Dispatchers.IO) {
                    progressRepository.syncProgress(itemId, positionMs, durationMs)
                }
            }
        }
    }

    fun clearPlaybackStateWithoutSync() {
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

    fun releasePlayback(syncProgressBlocking: Boolean = false) {
        val activeItemId = playbackActiveItemId
        if (activeItemId != null && playbackDurationMs > 0) {
            syncPlaybackProgressNow(syncProgressBlocking)
        }
        clearPlaybackStateWithoutSync()
    }

    fun startPlayback(itemId: String, startPositionSeconds: Int? = selectedChapterStartSeconds) {
        val token = connectionCredentials?.accessToken?.takeIf { it.isNotBlank() }
        val streamUrl = token?.let { buildStreamUrl(itemId, it) }
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
        }
        player.setOnCompletionListener {
            isPlayingPlayback = false
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

    val selectedChapterLabel = (selectedLibraryItemDetailState as? ItemDetailState.Loaded)
        ?.detail
        ?.chapters
        ?.let { chapters -> selectedChapterId?.let { chapterId -> selectedChapterDisplayLabel(chapters, chapterId) } }

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

    LaunchedEffect(playbackActiveItemId, isPlayingPlayback, playbackDurationMs) {
        val itemId = playbackActiveItemId ?: return@LaunchedEffect
        if (!isPlayingPlayback || playbackDurationMs <= 0) return@LaunchedEffect
        while (playbackActiveItemId == itemId && isPlayingPlayback) {
            syncPlaybackProgressNow()
            delay(30_000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            releasePlayback(syncProgressBlocking = true)
            playbackProgressScope.cancel()
        }
    }

    MaterialTheme {
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
                                rememberedConnection = savedConnection
                                selectedTab = AppTab.Library
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
                            NavigationBarItem(
                                selected = selectedTab == AppTab.Library,
                                onClick = { selectedTab = AppTab.Library },
                                icon = { Icon(Icons.Rounded.Headphones, contentDescription = null) },
                                label = { Text(AppTab.Library.label) },
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.Connect,
                                onClick = { selectedTab = AppTab.Connect },
                                icon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                                label = { Text(AppTab.Connect.label) },
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.Player,
                                onClick = { selectedTab = AppTab.Player },
                                icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                                label = { Text(AppTab.Player.label) },
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.Settings,
                                onClick = { selectedTab = AppTab.Settings },
                                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                label = { Text(AppTab.Settings.label) },
                            )
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
                                        selectedLibraryItemId = null
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
                                        rememberedConnection = savedConnection
                                        selectedTab = AppTab.Library
                                    }
                                }
                            },
                        )
                        AppTab.Player -> {
                            val activePlaybackItem = (playbackActiveItemId ?: selectedLibraryItemId)?.let { resolveLibraryItem(it) }
                            val activePlaybackChapters = (selectedLibraryItemDetailState as? ItemDetailState.Loaded)
                                ?.detail
                                ?.chapters
                                .orEmpty()
                            PlayerScreen(
                                padding = padding,
                                activeLibraryItem = activePlaybackItem,
                                selectedChapterLabel = selectedChapterLabel,
                                selectedChapterStartSeconds = selectedChapterStartSeconds,
                                isPreparingPlayback = isPreparingPlayback,
                                isPlayingPlayback = isPlayingPlayback,
                                playbackRate = playbackRate,
                                skipIntervalSeconds = appSettings.playbackSkipIntervalSeconds,
                                playbackError = playbackError,
                                playbackDurationMs = playbackDurationMs,
                                playbackPositionMs = playbackPositionMs,
                                chapterQuickAccess = activePlaybackChapters,
                                onPlay = {
                                    activePlaybackItem?.id?.let { itemId ->
                                        startPlayback(itemId, selectedChapterStartSeconds)
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
    isPreparingPlayback: Boolean,
    isPlayingPlayback: Boolean,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Now Playing", style = MaterialTheme.typography.labelLarge)

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
                        OutlinedButton(
                            onClick = { onChapterSelected(chapter) },
                            enabled = !isPreparingPlayback,
                        ) {
                            Text(chapter.title)
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
