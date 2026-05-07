package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Library) }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("ShelfPlayer · ${selectedTab.label}") },
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
                AppTab.Library -> LibraryScreen(padding)
                AppTab.Connect -> ConnectionScreen(padding)
                AppTab.Player -> PlayerScreen(padding)
            }
        }
    }
}

@Composable
private fun LibraryScreen(padding: PaddingValues) {
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
