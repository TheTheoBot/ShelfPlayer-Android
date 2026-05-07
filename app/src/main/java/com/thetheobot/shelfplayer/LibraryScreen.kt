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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    padding: PaddingValues,
    connectionSession: ConnectionSession,
    repository: LibraryRepository,
) {
    val libraryItems by repository.libraryItems.collectAsState()

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
                            "Die Bibliothek wird hier künftig mit echten Serverdaten verbunden.",
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
                "Bibliothekseinträge",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (libraryItems.isEmpty()) {
            item {
                Card(colors = CardDefaults.elevatedCardColors()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Noch keine Einträge vorhanden.")
                    }
                }
            }
        } else {
            items(libraryItems, key = { it.id }) { item ->
                LibraryItemCard(item = item)
            }
        }
    }
}

@Composable
private fun LibraryItemCard(item: LibraryItem) {
    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.82f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.author, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    formatProgress(item.progressPercent),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                formatItemType(item.itemType),
                style = MaterialTheme.typography.labelMedium,
            )
            if (!item.coverUrl.isNullOrBlank()) {
                Text(
                    item.coverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun formatProgress(progressPercent: Int): String {
    val clampedProgress = progressPercent.coerceIn(0, 100)
    return "$clampedProgress%"
}

internal fun formatItemType(itemType: LibraryItemType): String {
    return when (itemType) {
        LibraryItemType.Audiobook -> "Hörbuch"
        LibraryItemType.Book -> "Buch"
        LibraryItemType.Podcast -> "Podcast"
        LibraryItemType.Series -> "Serie"
    }
}
