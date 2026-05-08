package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntil
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `typing a query auto submits search after debounce`() {
        val repository = RecordingSearchRepository()
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    padding = PaddingValues(),
                    repository = repository,
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("project")
        composeRule.mainClock.advanceTimeBy(SEARCH_AUTOSUBMIT_DEBOUNCE_MS + 1)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.startedQueries.isNotEmpty()
        }

        assertEquals(listOf("project"), repository.startedQueries.toList())

        repository.searchRelease.complete(Unit)

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Project Hail Mary").assertExists()
    }

    @Test
    fun `changing the query before debounce elapses only searches the latest query`() {
        val repository = RecordingSearchRepository()
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    padding = PaddingValues(),
                    repository = repository,
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("project")
        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("clean")
        composeRule.mainClock.advanceTimeBy(SEARCH_AUTOSUBMIT_DEBOUNCE_MS + 1)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.startedQueries.isNotEmpty()
        }

        assertEquals(listOf("clean"), repository.startedQueries.toList())

        repository.searchRelease.complete(Unit)

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Project Hail Mary").assertExists()
    }

    @Test
    fun `manual submit during debounce window does not start the same search twice`() {
        val repository = RecordingSearchRepository()
        composeRule.mainClock.autoAdvance = false

        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    padding = PaddingValues(),
                    repository = repository,
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("project")
        composeRule.onNodeWithText("Suchen").performClick()
        composeRule.mainClock.advanceTimeBy(SEARCH_AUTOSUBMIT_DEBOUNCE_MS + 1)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.startedQueries.isNotEmpty()
        }

        assertEquals(listOf("project"), repository.startedQueries.toList())

        repository.searchRelease.complete(Unit)

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Project Hail Mary").assertExists()
    }

    @Test
    fun `repeating submit on the same active query keeps the in-flight result intact`() {
        val repository = RecordingSearchRepository()

        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    padding = PaddingValues(),
                    repository = repository,
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("project")
        composeRule.onNodeWithText("Suchen").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.startedQueries.isNotEmpty()
        }

        composeRule.onNodeWithText("Suchen").performClick()
        assertEquals(listOf("project"), repository.startedQueries.toList())

        repository.searchRelease.complete(Unit)

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Project Hail Mary").assertExists()
    }

    @Test
    fun `clearing search cancels stale in-flight results`() {
        val repository = RecordingSearchRepository()

        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    padding = PaddingValues(),
                    repository = repository,
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("project")
        composeRule.onNodeWithText("Suchen").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.startedQueries.isNotEmpty()
        }

        composeRule.onNodeWithText("Suche löschen").assertExists()
        composeRule.onNodeWithText("Suche löschen").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.cancelledQueries.contains("project")
        }

        repository.searchRelease.complete(Unit)

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Suche löschen").assertDoesNotExist()
        composeRule.onNodeWithText("Suche in deiner Bibliothek").assertExists()
        composeRule.onNodeWithText("Project Hail Mary").assertDoesNotExist()
        assertEquals(listOf("project"), repository.startedQueries.toList())
        assertEquals(listOf("project"), repository.cancelledQueries.toList())
    }

    @Test
    fun `replacement search waits for cancelled search to finish before starting`() {
        val repository = CancellationBlockingSearchRepository()

        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    padding = PaddingValues(),
                    repository = repository,
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("project")
        composeRule.onNodeWithText("Suchen").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.startedQueries.contains("project")
        }

        composeRule.onNode(hasSetTextAction()).performTextClearance()
        composeRule.onNode(hasSetTextAction()).performTextInput("clean")
        composeRule.onNodeWithText("Suchen").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.cancelledQueries.contains("project")
        }

        assertEquals(listOf("project"), repository.startedQueries.toList())

        repository.allowCancelledSearchToFinish.complete(Unit)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.startedQueries.contains("clean")
        }

        assertEquals(listOf("project", "clean"), repository.startedQueries.toList())
    }

    @Test
    fun `search result details button invokes navigation callback with item id`() {
        val repository = ImmediateSearchRepository()
        val selectedItemId = CompletableDeferred<String>()

        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    padding = PaddingValues(),
                    repository = repository,
                    onResultClick = { selectedItemId.complete(it) },
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("project")
        composeRule.onNodeWithText("Suchen").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithText("Project Hail Mary").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Details").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            selectedItemId.isCompleted
        }

        assertEquals("project-hail-mary", selectedItemId.getCompleted())
    }
}

private class CancellationBlockingSearchRepository : LibraryRepository {
    val startedQueries = CopyOnWriteArrayList<String>()
    val cancelledQueries = CopyOnWriteArrayList<String>()
    val allowCancelledSearchToFinish = CompletableDeferred<Unit>()

    override val libraryFeedState = MutableStateFlow(
        LibraryFeedState.Loaded(
            listOf(
                LibraryItem(
                    id = "sample-item",
                    title = "Sample Item",
                    author = "Sample Author",
                    progressPercent = 25,
                    itemType = LibraryItemType.Book,
                ),
            ),
        ),
    )

    override suspend fun refresh() = Unit

    override suspend fun search(query: String): List<LibraryItem> {
        startedQueries += query
        try {
            if (query == "project") {
                CompletableDeferred<Unit>().await()
            }
        } catch (cancellation: CancellationException) {
            cancelledQueries += query
            throw cancellation
        } finally {
            if (query == "project") {
                withContext(NonCancellable) {
                    allowCancelledSearchToFinish.await()
                }
            }
        }

        return listOf(
            LibraryItem(
                id = "project-hail-mary",
                title = "Project Hail Mary",
                author = "Andy Weir",
                progressPercent = 82,
                itemType = LibraryItemType.Audiobook,
            ),
        )
    }

    override suspend fun getItemDetail(itemId: String): LibraryItemDetail? = null
}

private class RecordingSearchRepository : LibraryRepository {
    val startedQueries = CopyOnWriteArrayList<String>()
    val cancelledQueries = CopyOnWriteArrayList<String>()
    val searchRelease = CompletableDeferred<Unit>()

    override val libraryFeedState = MutableStateFlow(
        LibraryFeedState.Loaded(
            listOf(
                LibraryItem(
                    id = "sample-item",
                    title = "Sample Item",
                    author = "Sample Author",
                    progressPercent = 25,
                    itemType = LibraryItemType.Book,
                ),
            ),
        ),
    )

    override suspend fun refresh() = Unit

    override suspend fun search(query: String): List<LibraryItem> {
        startedQueries += query
        try {
            searchRelease.await()
        } catch (cancellation: CancellationException) {
            cancelledQueries += query
            throw cancellation
        }

        return listOf(
            LibraryItem(
                id = "project-hail-mary",
                title = "Project Hail Mary",
                author = "Andy Weir",
                progressPercent = 82,
                itemType = LibraryItemType.Audiobook,
            ),
        )
    }

    override suspend fun getItemDetail(itemId: String): LibraryItemDetail? = null
}

private class ImmediateSearchRepository : LibraryRepository {
    override val libraryFeedState = MutableStateFlow(
        LibraryFeedState.Loaded(
            listOf(
                LibraryItem(
                    id = "sample-item",
                    title = "Sample Item",
                    author = "Sample Author",
                    progressPercent = 25,
                    itemType = LibraryItemType.Book,
                ),
            ),
        ),
    )

    override suspend fun refresh() = Unit

    override suspend fun search(query: String): List<LibraryItem> = listOf(
        LibraryItem(
            id = "project-hail-mary",
            title = "Project Hail Mary",
            author = "Andy Weir",
            progressPercent = 82,
            itemType = LibraryItemType.Audiobook,
        ),
    )

    override suspend fun getItemDetail(itemId: String): LibraryItemDetail? = null
}
