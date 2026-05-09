package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Story14PolishScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `library screen loaded state renders primary and secondary actions and forwards detail click`() {
        val item = LibraryItem(
            id = "item-1",
            title = "Example Title",
            author = "Example Author",
            progressPercent = 42,
            itemType = LibraryItemType.Audiobook,
        )
        val repository = FakeLibraryRepository(LibraryFeedState.Loaded(listOf(item)))
        var clickedItemId: String? = null

        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    padding = PaddingValues(),
                    repository = repository,
                    onItemClick = { clickedItemId = it },
                )
            }
        }

        composeRule.onNodeWithText("Fortsetzen").assertExists()
        composeRule.onNodeWithText("Details ansehen").assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals("item-1", clickedItemId)
        }
    }

    @Test
    fun `item detail screen loaded state renders the hero summary rows`() {
        val detail = LibraryItemDetail(
            item = LibraryItem(
                id = "item-1",
                title = "Example Title",
                author = "Example Author",
                progressPercent = 10,
                itemType = LibraryItemType.Audiobook,
            ),
            progressPercent = 10,
            description = "A concise description for the detail screen.",
            chapters = listOf(
                LibraryChapter(id = "chapter-1", title = "Intro", startSeconds = 0, endSeconds = 60),
                LibraryChapter(id = "chapter-2", title = "Main", startSeconds = 60, endSeconds = 120),
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                ItemDetailScreen(
                    padding = PaddingValues(),
                    state = ItemDetailState.Loaded(detail),
                    selectedChapterId = null,
                    playbackActionLabel = "Pausieren",
                    playbackActionEnabled = true,
                    onBackClick = {},
                    onPlaybackAction = {},
                    onChapterSelected = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Typ").assertExists()
        composeRule.onNodeWithText("Hörbuch").assertExists()
        composeRule.onNodeWithText("Fortschritt").assertExists()
        composeRule.onNodeWithText("10%").assertExists()
        composeRule.onNodeWithText("Kapitel").assertExists()
        composeRule.onNodeWithText("2 Kapitel").assertExists()
    }

    @Test
    fun `item detail screen keeps the chapter start action disabled while playback is preparing`() {
        val detail = LibraryItemDetail(
            item = LibraryItem(
                id = "item-1",
                title = "Example Title",
                author = "Example Author",
                progressPercent = 10,
                itemType = LibraryItemType.Audiobook,
            ),
            progressPercent = 10,
            description = "A concise description for the detail screen.",
            chapters = listOf(
                LibraryChapter(id = "chapter-1", title = "Intro", startSeconds = 0, endSeconds = 60),
            ),
        )

        composeRule.setContent {
            MaterialTheme {
                ItemDetailScreen(
                    padding = PaddingValues(),
                    state = ItemDetailState.Loaded(detail),
                    selectedChapterId = null,
                    playbackActionLabel = "Lädt…",
                    playbackActionEnabled = false,
                    onBackClick = {},
                    onPlaybackAction = {},
                    onChapterSelected = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Ab Kapitel starten").assertExists().assertIsNotEnabled()
        composeRule.onNodeWithText("Intro").assertExists().assertIsNotEnabled()
    }

    @Test
    fun `item detail screen chapter card forwards selection when playback is enabled`() {
        val detail = LibraryItemDetail(
            item = LibraryItem(
                id = "item-1",
                title = "Example Title",
                author = "Example Author",
                progressPercent = 10,
                itemType = LibraryItemType.Audiobook,
            ),
            progressPercent = 10,
            description = "A concise description for the detail screen.",
            chapters = listOf(
                LibraryChapter(id = "chapter-1", title = "Intro", startSeconds = 0, endSeconds = 60),
            ),
        )
        var selectedChapterId: String? = null

        composeRule.setContent {
            MaterialTheme {
                ItemDetailScreen(
                    padding = PaddingValues(),
                    state = ItemDetailState.Loaded(detail),
                    selectedChapterId = null,
                    playbackActionLabel = "Pausieren",
                    playbackActionEnabled = true,
                    onBackClick = {},
                    onPlaybackAction = {},
                    onChapterSelected = { selectedChapterId = it },
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithText("Intro").assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals("chapter-1", selectedChapterId)
        }
    }
}

private class FakeLibraryRepository(
    initialState: LibraryFeedState,
) : LibraryRepository {
    override val libraryFeedState = MutableStateFlow(initialState)

    override suspend fun refresh() = Unit

    override suspend fun search(query: String): List<LibraryItem> = emptyList()

    override suspend fun getItemDetail(itemId: String): LibraryItemDetail? = null
}
