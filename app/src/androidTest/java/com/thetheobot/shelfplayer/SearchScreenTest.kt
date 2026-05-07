package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `clearing search cancels stale in-flight results`() {
        val repository = BlockingSearchRepository()

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
            repository.searchStarted.isCompleted
        }

        composeRule.onNodeWithText("Suche löschen").assertExists()
        composeRule.onNodeWithText("Suche löschen").performClick()
        composeRule.onNodeWithText("Suche löschen").assertDoesNotExist()
        composeRule.onNodeWithText("Suche in deiner Bibliothek").assertExists()

        repository.searchRelease.complete(
            listOf(
                LibraryItem(
                    id = "project-hail-mary",
                    title = "Project Hail Mary",
                    author = "Andy Weir",
                    progressPercent = 82,
                    itemType = LibraryItemType.Audiobook,
                ),
            ),
        )

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Project Hail Mary").assertDoesNotExist()
    }
}

private class BlockingSearchRepository : LibraryRepository {
    val searchStarted = CompletableDeferred<String>()
    val searchRelease = CompletableDeferred<List<LibraryItem>>()

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
        searchStarted.complete(query)
        return searchRelease.await()
    }

    override suspend fun getItemDetail(itemId: String): LibraryItemDetail? = null
}
