package com.thetheobot.shelfplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    padding: PaddingValues,
    repository: LibraryRepository,
    onResultClick: (String) -> Unit = {},
) {
    val libraryFeedState by repository.libraryFeedState.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var searchState by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val searchSubmissionTracker = remember { SearchSubmissionTracker() }

    fun refreshLibrary() {
        scope.launch {
            val refreshResult = runSuspendCatchingPreservingCancellation {
                repository.refresh()
            }
            val refreshErrorMessage = refreshResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
            loadErrorMessage = refreshErrorMessage ?: if (refreshResult.isFailure) {
                "Bibliothek konnte nicht geladen werden"
            } else {
                null
            }
            if (refreshResult.isSuccess && searchState.isRefreshErrorState()) {
                searchState = SearchState.Idle
            }
            if (loadErrorMessage != null && searchState is SearchState.Idle && normalizedSearchQuery(query).isBlank()) {
                searchState = SearchState.Error(
                    query = "",
                    message = loadErrorMessage ?: "Bibliothek konnte nicht geladen werden",
                )
            }
        }
    }

    fun clearSearch() {
        query = ""
        searchState = SearchState.Idle
        loadErrorMessage = null
        searchSubmissionTracker.invalidate()
    }

    fun submitSearch() {
        val normalizedQuery = normalizedSearchQuery(query)
        if (normalizedQuery.isBlank()) {
            searchState = SearchState.Idle
            return
        }

        val requestToken = searchSubmissionTracker.nextToken()
        scope.launch {
            if (!searchSubmissionTracker.accepts(requestToken)) {
                return@launch
            }
            searchState = SearchState.Searching(normalizedQuery)
            loadErrorMessage = null
            val result = runSuspendCatchingPreservingCancellation {
                repository.search(normalizedQuery)
            }
            if (!searchSubmissionTracker.accepts(requestToken)) {
                return@launch
            }
            result.fold(
                onSuccess = { items ->
                    searchState = if (items.isEmpty()) {
                        SearchState.NoResults(normalizedQuery)
                    } else {
                        SearchState.Results(normalizedQuery, items)
                    }
                },
                onFailure = { throwable ->
                    searchState = SearchState.Error(
                        query = normalizedQuery,
                        message = throwable.message ?: "Suche konnte nicht ausgeführt werden",
                    )
                },
            )
            keyboardController?.hide()
        }
    }

    LaunchedEffect(Unit) {
        refreshLibrary()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Suche",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                searchSubmissionTracker.invalidate()
                searchState = if (normalizedSearchQuery(it).isBlank()) {
                    SearchState.Idle
                } else {
                    SearchState.Typing(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Titel, Autor oder ID") },
            placeholder = { Text("Z. B. Project Hail Mary") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            trailingIcon = {
                Button(onClick = { submitSearch() }, enabled = query.trim().isNotBlank()) {
                    Text("Suchen")
                }
            },
        )

        if (loadErrorMessage != null && searchState !is SearchState.Results && searchState !is SearchState.NoResults) {
            val errorState = SearchState.Error(query = query.trim(), message = loadErrorMessage.orEmpty())
            val clearActionLabel = errorState.clearSearchActionLabel()
            SearchStatusCard(
                title = searchStateTitle(errorState),
                message = searchStateMessage(errorState),
                showSpinner = false,
                onRetry = { refreshLibrary() },
                onClear = clearActionLabel?.let { { clearSearch() } },
                clearActionLabel = clearActionLabel,
            )
        } else {
            val clearActionLabel = searchState.clearSearchActionLabel()
            SearchStatusCard(
                title = searchStateTitle(searchState),
                message = searchStateMessage(searchState),
                showSpinner = searchState is SearchState.Searching,
                onRetry = if (searchState is SearchState.Error) ({ submitSearch() }) else null,
                onClear = clearActionLabel?.let { { clearSearch() } },
                clearActionLabel = clearActionLabel,
            )
        }

        when (val state = searchState) {
            is SearchState.Results -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        SearchResultCard(
                            item = item,
                            onClick = { onResultClick(item.id) },
                        )
                    }
                }
            }

            is SearchState.NoResults -> {
                Text(
                    "Tipp: Versuche auch den Autor oder die ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is SearchState.Error -> {
                Text(
                    "Du kannst die Suche erneut auslösen oder die Verbindung prüfen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                val visibleItems = libraryFeedState.visibleItems()
                if (visibleItems.isNotEmpty() && query.trim().isBlank()) {
                    Text(
                        "Die Bibliothek wurde geladen. Suche mit einem Begriff, um Ergebnisse zu filtern.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchStatusCard(
    title: String,
    message: String,
    showSpinner: Boolean,
    onRetry: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    clearActionLabel: String? = null,
) {
    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showSpinner) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(message, style = MaterialTheme.typography.bodyMedium)
            val actions = listOfNotNull(
                onRetry?.let { "Erneut versuchen" to it },
                onClear?.let { (clearActionLabel ?: "Suche löschen") to it },
            )
            if (actions.isNotEmpty()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 360.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            actions.forEach { (label, action) ->
                                OutlinedButton(
                                    onClick = action,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            actions.forEach { (label, action) ->
                                OutlinedButton(onClick = action) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: LibraryItem,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.elevatedCardColors(),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchResultThumbnail(item)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(item.author, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatLibraryItemMetadata(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text("Details")
            }
        }
    }
}

@Composable
private fun SearchResultThumbnail(item: LibraryItem) {
    if (!item.coverUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = item.coverUrl,
            contentDescription = "${item.title} Cover",
            contentScale = ContentScale.Crop,
            loading = { SearchResultThumbnailFallback(item) },
            error = { SearchResultThumbnailFallback(item) },
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        return
    }

    SearchResultThumbnailFallback(item)
}

@Composable
private fun SearchResultThumbnailFallback(item: LibraryItem) {
    val initials = item.title.trim().take(2).uppercase().ifBlank { "?" }

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
