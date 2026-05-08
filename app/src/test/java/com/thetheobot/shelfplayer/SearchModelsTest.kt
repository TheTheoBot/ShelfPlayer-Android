package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchModelsTest {
    @Test
    fun `normalizedSearchQuery trims surrounding whitespace`() {
        assertEquals("project hail mary", normalizedSearchQuery("  project hail mary  "))
    }

    @Test
    fun `search state title uses singular and plural counts`() {
        val singleResultState = SearchState.Results(
            query = "project",
            items = listOf(sampleItem("project-hail-mary", "Project Hail Mary", "Andy Weir")),
        )
        val pluralResultState = SearchState.Results(
            query = "project",
            items = listOf(
                sampleItem("project-hail-mary", "Project Hail Mary", "Andy Weir"),
                sampleItem("clean-architecture", "Clean Architecture", "Robert C. Martin"),
            ),
        )

        assertEquals("1 Treffer für \"project\"", searchStateTitle(singleResultState))
        assertEquals("2 Treffer für \"project\"", searchStateTitle(pluralResultState))
    }

    @Test
    fun `search state message falls back for blank errors`() {
        assertEquals(
            "Bitte später erneut versuchen.",
            searchStateMessage(SearchState.Error(query = "project", message = "   ")),
        )
    }

    @Test
    fun `clear action state helpers match each search state`() {
        assertStateAction(SearchState.Idle, canClear = false, clearLabel = null)
        assertStateAction(SearchState.Typing(query = "   "), canClear = false, clearLabel = null)
        assertStateAction(SearchState.Typing(query = "project"), canClear = true, clearLabel = "Suche löschen")
        assertStateAction(SearchState.Searching(query = "project"), canClear = true, clearLabel = "Suche löschen")
        assertStateAction(
            SearchState.Results(
                query = "project",
                items = listOf(sampleItem("project-hail-mary", "Project Hail Mary", "Andy Weir")),
            ),
            canClear = true,
            clearLabel = "Suche löschen",
        )
        assertStateAction(SearchState.NoResults(query = "project"), canClear = true, clearLabel = "Suche löschen")
        assertStateAction(SearchState.Error(query = "project", message = "boom"), canClear = true, clearLabel = "Suche löschen")
        assertStateAction(SearchState.Error(query = "", message = "boom"), canClear = true, clearLabel = "Suche löschen")
    }

    @Test
    fun `refresh error state only applies to blank query errors`() {
        assertTrue(SearchState.Error(query = "", message = "boom").isRefreshErrorState())
        assertFalse(SearchState.Error(query = "project", message = "boom").isRefreshErrorState())
        assertFalse(SearchState.Results(query = "project", items = emptyList()).isRefreshErrorState())
    }

    @Test
    fun `search library items matches tokens against title author and id`() {
        val items = listOf(
            sampleItem("project-hail-mary", "Project Hail Mary", "Andy Weir"),
            sampleItem("clean-architecture", "Clean Architecture", "Robert C. Martin"),
        )

        assertEquals(listOf("project-hail-mary"), searchLibraryItems(items, "hail").map { it.id })
        assertEquals(listOf("clean-architecture"), searchLibraryItems(items, "robert").map { it.id })
        assertEquals(listOf("project-hail-mary"), searchLibraryItems(items, "project mary").map { it.id })
        assertEquals(listOf("clean-architecture"), searchLibraryItems(items, "clean-architecture").map { it.id })
        assertTrue(searchLibraryItems(items, "   ").isEmpty())
    }

    @Test
    fun `submission tracker invalidates stale tokens`() {
        val tracker = SearchSubmissionTracker()
        val firstToken = tracker.nextToken()

        assertTrue(tracker.accepts(firstToken))
        tracker.invalidate()
        assertFalse(tracker.accepts(firstToken))

        val secondToken = tracker.nextToken()
        assertTrue(tracker.accepts(secondToken))
    }

    private fun assertStateAction(
        state: SearchState,
        canClear: Boolean,
        clearLabel: String?,
    ) {
        assertEquals(canClear, state.canClearSearch())
        assertEquals(clearLabel, state.clearSearchActionLabel())
    }

    private fun sampleItem(
        id: String,
        title: String,
        author: String,
    ): LibraryItem {
        return LibraryItem(
            id = id,
            title = title,
            author = author,
            progressPercent = 0,
            itemType = LibraryItemType.Book,
        )
    }
}
