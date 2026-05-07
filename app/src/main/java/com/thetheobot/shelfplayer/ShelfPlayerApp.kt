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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private val demoItems = listOf(
    "Der Name des Windes",
    "Project Hail Mary",
    "Die Verwandlung",
    "Clean Architecture",
)

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
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Library) }
    var rememberedConnection by remember { mutableStateOf<ConnectionCredentials?>(null) }
    var connectionLoadFailed by remember { mutableStateOf(false) }
    var initializationAttempt by rememberSaveable { mutableStateOf(0) }

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
                        AppTab.Library -> LibraryScreen(padding, connectionSession)
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
                        AppTab.Player -> PlayerScreen(padding)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(padding: PaddingValues, connectionSession: ConnectionSession) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Deine Bibliothek",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            Card(colors = CardDefaults.elevatedCardColors()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Verbindung", style = MaterialTheme.typography.titleMedium)
                    Text(connectionSessionStatusText(connectionSession))
                    if (connectionSession.hasSavedServer) {
                        Text(
                            "Die Demo-Bibliothek nutzt aktuell diese vorgemerkte Server-URL.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            "Verbinde einen Server, damit wir später echte Inhalte laden können.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Die Liste bleibt vorerst mit Demo-Inhalten befüllt, bis der Audiobookshelf-Client live ist.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        items(demoItems) { item ->
            Card(colors = CardDefaults.elevatedCardColors()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item, style = MaterialTheme.typography.titleMedium)
                    Text("47%", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Now Playing", style = MaterialTheme.typography.labelLarge)
        Text(
            "Project Hail Mary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text("Kapitel 12 · 03:41:52 verbleibend", style = MaterialTheme.typography.bodyMedium)
    }
}
