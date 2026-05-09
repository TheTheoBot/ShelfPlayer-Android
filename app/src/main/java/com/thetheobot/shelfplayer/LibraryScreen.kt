package com.thetheobot.shelfplayer

import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LibraryScreen(
    padding: PaddingValues,
    repository: LibraryRepository,
    onAppear: suspend () -> Unit = {},
    onRefresh: suspend () -> Unit = onAppear,
    onItemClick: (String) -> Unit = {},
    onPlayClick: (String) -> Unit = {},
) {
    val libraryFeedState by repository.libraryFeedState.collectAsState()
    var isLocallyRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var refreshErrorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerRefresh() {
        scope.launch {
            isLocallyRefreshing = true
            refreshErrorMessage = null
            try {
                val refreshResult = runSuspendCatchingPreservingCancellation {
                    onRefresh()
                }
                refreshErrorMessage = refreshResult.exceptionOrNull()?.message?.let { message ->
                    "Aktualisierung fehlgeschlagen: ${message.ifBlank { "unbekannter Fehler" }}"
                }
            } finally {
                isLocallyRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val appearResult = runSuspendCatchingPreservingCancellation {
            onAppear()
        }
        refreshErrorMessage = appearResult.exceptionOrNull()?.message?.let { message ->
            "Initiales Laden fehlgeschlagen: ${message.ifBlank { "unbekannter Fehler" }}"
        }
    }

    val displayState = when {
        isLocallyRefreshing && libraryFeedState is LibraryFeedState.Loaded -> {
            LibraryFeedState.Refreshing((libraryFeedState as LibraryFeedState.Loaded).items)
        }
        isLocallyRefreshing && libraryFeedState is LibraryFeedState.Empty -> {
            LibraryFeedState.Refreshing(emptyList())
        }
        else -> libraryFeedState
    }
    val visibleItems = displayState.visibleItems()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLocallyRefreshing,
        onRefresh = {
            triggerRefresh()
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .pullRefresh(pullRefreshState),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                LibraryFeedStateCard(
                    state = displayState,
                    onRetry = { triggerRefresh() },
                )
            }

            refreshErrorMessage?.let { message ->
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (visibleItems.isNotEmpty()) {
                if (displayState is LibraryFeedState.Refreshing) {
                    item {
                        Text(
                            "Vorhandene Einträge bleiben sichtbar, während neue Daten geladen werden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(visibleItems, key = { it.id }) { item ->
                    LibraryItemCard(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        onPlayClick = { onPlayClick(item.id) },
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isLocallyRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun LibraryFeedStateCard(
    state: LibraryFeedState,
    onRetry: () -> Unit,
) {
    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                libraryFeedStateTitle(state),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                libraryFeedStateMessage(state),
                style = MaterialTheme.typography.bodyMedium,
            )
            when (state) {
                LibraryFeedState.Loading -> {
                    Text(
                        "Die Bibliothek wird beim Start vorbereitet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LibraryFeedState.Empty -> {
                    Text(
                        "Ziehe nach unten, um die Bibliothek neu zu laden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is LibraryFeedState.Error -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onRetry) {
                            Text("Erneut versuchen")
                        }
                        TextButton(onClick = onRetry) {
                            Text("Aktualisieren")
                        }
                    }
                }
                is LibraryFeedState.Loaded, is LibraryFeedState.Refreshing -> {
                    Text(
                        "Ziehe nach unten, um die Liste zu aktualisieren.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    item: LibraryItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.elevatedCardColors(),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryItemThumbnail(item = item)
                Column(modifier = Modifier.fillMaxWidth(0.72f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.author, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatLibraryItemMetadata(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPlayClick) {
                    Text(libraryItemPrimaryActionLabel(item))
                }
                TextButton(onClick = onClick) {
                    Text(libraryItemSecondaryActionLabel())
                }
            }
        }
    }
}

@Composable
private fun LibraryItemThumbnail(item: LibraryItem) {
    if (!item.coverUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = item.coverUrl,
            contentDescription = "${item.title} Cover",
            contentScale = ContentScale.Crop,
            loading = { LibraryItemThumbnailFallback(item) },
            error = { LibraryItemThumbnailFallback(item) },
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        return
    }

    LibraryItemThumbnailFallback(item)
}

@Composable
private fun LibraryItemThumbnailFallback(item: LibraryItem) {
    val initials = item.title
        .trim()
        .take(2)
        .uppercase()
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
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
