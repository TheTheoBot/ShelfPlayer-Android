package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AppTab(val label: String) {
    Library("Library"),
    Connect("Connect"),
    Player("Player"),
}

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
    val libraryFeedState by libraryRepository.libraryFeedState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Library) }
    var connectionLoadFailed by remember { mutableStateOf(false) }
    var initializationAttempt by rememberSaveable { mutableStateOf(0) }
    var selectedLibraryItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLibraryItemDetailState by remember { mutableStateOf<ItemDetailState>(ItemDetailState.Loading) }
    var selectedLibraryItemDetailReloadKey by rememberSaveable { mutableStateOf(0) }
    var selectedChapterId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapterStartSeconds by rememberSaveable { mutableStateOf<Int?>(null) }
    var playbackRequestKey by rememberSaveable { mutableStateOf(0) }

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
                                icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                label = { Text(AppTab.Connect.label) },
                            )
                            NavigationBarItem(
                                selected = selectedTab == AppTab.Player,
                                onClick = { selectedTab = AppTab.Player },
                                icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                                label = { Text(AppTab.Player.label) },
                            )
                        }
                    }
                ) { padding ->
                    when (selectedTab) {
                        AppTab.Library -> {
                            if (selectedLibraryItemId != null) {
                                ItemDetailScreen(
                                    padding = padding,
                                    state = selectedLibraryItemDetailState,
                                    onBackClick = {
                                        selectedLibraryItemId = null
                                        selectedLibraryItemDetailState = ItemDetailState.Loading
                                        selectedLibraryItemDetailReloadKey++
                                        selectedChapterId = null
                                        selectedChapterStartSeconds = null
                                    },
                                    onPlayClick = { itemId ->
                                        selectedLibraryItemId = itemId
                                        selectedLibraryItemDetailState = ItemDetailState.Loading
                                        selectedChapterId = null
                                        selectedChapterStartSeconds = null
                                        selectedTab = AppTab.Player
                                        playbackRequestKey++
                                        selectedLibraryItemDetailReloadKey++
                                    },
                                    onChapterSelected = { chapterId ->
                                        selectedChapterId = chapterId
                                        selectedChapterStartSeconds = resolveSelectedChapterStartSeconds(chapterId)
                                        selectedTab = AppTab.Player
                                        playbackRequestKey++
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
                                        playbackRequestKey++
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
                        AppTab.Player -> PlayerScreen(
                            padding = padding,
                            activeLibraryItem = selectedLibraryItemId?.let { resolveLibraryItem(it) },
                            connectionCredentials = rememberedConnection,
                            selectedChapterId = selectedChapterId,
                            selectedChapterStartSeconds = selectedChapterStartSeconds,
                            playbackRequestKey = playbackRequestKey,
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
    connectionCredentials: ConnectionCredentials?,
    selectedChapterId: String?,
    selectedChapterStartSeconds: Int?,
    playbackRequestKey: Int,
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var durationMs by remember { mutableStateOf(0) }
    var positionMs by remember { mutableStateOf(0) }
    var lastHandledPlaybackRequestKey by rememberSaveable { mutableStateOf(0) }

    val itemId = activeLibraryItem?.id
    val streamUrl = remember(itemId, connectionCredentials) {
        val server = connectionCredentials?.serverUrl?.takeIf { it.isNotBlank() } ?: return@remember null
        val id = itemId ?: return@remember null
        "${normalizeServerUrl(server)}/api/items/$id/play"
    }

    fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPreparing = false
        isPlaying = false
        durationMs = 0
        positionMs = 0
    }

    fun startPlayback(startPositionSeconds: Int? = selectedChapterStartSeconds) {
        val url = streamUrl
        val token = connectionCredentials?.accessToken?.takeIf { it.isNotBlank() }
        if (url == null || token == null) {
            playbackError = "Fehlende Verbindung oder kein Titel ausgewählt"
            return
        }

        releasePlayer()
        playbackError = null

        val player = android.media.MediaPlayer()
        mediaPlayer = player
        isPreparing = true

        player.setOnPreparedListener {
            isPreparing = false
            durationMs = it.duration.coerceAtLeast(0)
            val startPositionMs = startPositionSeconds?.coerceAtLeast(0)?.times(1000)
            if (startPositionMs != null && startPositionMs in 0..durationMs) {
                it.seekTo(startPositionMs)
                positionMs = startPositionMs
            } else {
                positionMs = 0
            }
            it.start()
            isPlaying = true
        }
        player.setOnCompletionListener {
            isPlaying = false
            positionMs = durationMs
        }
        player.setOnErrorListener { _, _, _ ->
            playbackError = "Wiedergabe konnte nicht gestartet werden"
            isPreparing = false
            isPlaying = false
            true
        }

        player.setDataSource(
            context,
            android.net.Uri.parse(url),
            mapOf("Authorization" to "Bearer $token"),
        )
        player.prepareAsync()
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(mediaPlayer, isPlaying) {
        while (mediaPlayer != null && isPlaying) {
            positionMs = mediaPlayer?.currentPosition?.coerceAtLeast(0) ?: 0
            kotlinx.coroutines.delay(500)
        }
    }

    LaunchedEffect(playbackRequestKey) {
        if (playbackRequestKey > lastHandledPlaybackRequestKey && itemId != null && connectionCredentials != null) {
            lastHandledPlaybackRequestKey = playbackRequestKey
            startPlayback(selectedChapterStartSeconds)
        }
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
        selectedChapterId?.let {
            Text(
                "Kapitel-ID: $it",
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

        androidx.compose.material3.LinearProgressIndicator(
            progress = {
                if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(0.8f),
        )

        Text(
            "${formatDuration(positionMs)} / ${formatDuration(durationMs)}",
            style = MaterialTheme.typography.labelMedium,
        )

        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { startPlayback() }, enabled = !isPreparing) {
                Text(if (isPreparing) "Lädt…" else "Play")
            }
            Button(
                onClick = {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            it.pause()
                            isPlaying = false
                        } else {
                            it.start()
                            isPlaying = true
                        }
                    }
                },
                enabled = mediaPlayer != null && !isPreparing,
            ) {
                Text(if (isPlaying) "Pause" else "Resume")
            }
            Button(onClick = { releasePlayer() }, enabled = mediaPlayer != null) {
                Text("Stop")
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
