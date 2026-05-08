package com.thetheobot.shelfplayer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {
    @Test
    fun `in memory repository exposes loading before the first refresh and then typed sample library feed state`() = runBlocking {
        val repository = InMemoryLibraryRepository()

        assertEquals(LibraryFeedState.Loading, repository.libraryFeedState.value)

        repository.refresh()

        val state = repository.libraryFeedState.value
        assertTrue(state is LibraryFeedState.Loaded)
        val items = (state as LibraryFeedState.Loaded).items
        assertTrue(items.isNotEmpty())
        assertEquals("Der Name des Windes", items.first().title)
        assertEquals(LibraryItemType.Audiobook, items.first().itemType)
    }

    @Test
    fun `in memory repository exposes chapter detail lookup for seeded items`() = runBlocking {
        val repository = InMemoryLibraryRepository()

        repository.refresh()

        val detail = repository.getItemDetail("project-hail-mary")

        assertEquals("Project Hail Mary", detail?.item?.title)
        assertTrue(detail?.description?.isNotBlank() == true)
        assertTrue(detail?.chapters?.isNotEmpty() == true)
        assertEquals("chapter-1", detail?.chapters?.first()?.id)
    }

    @Test
    fun `in memory repository copies the initial item list defensively`() = runBlocking {
        val mutableItems = mutableListOf(
            LibraryItem(
                id = "example-id",
                title = "Example Title",
                author = "Example Author",
                progressPercent = 12,
                itemType = LibraryItemType.Book,
            ),
        )

        val repository = InMemoryLibraryRepository(initialItems = mutableItems)
        mutableItems += LibraryItem(
            id = "mutated-id",
            title = "Mutated Title",
            author = "Mutated Author",
            progressPercent = 88,
            itemType = LibraryItemType.Podcast,
        )

        assertEquals(LibraryFeedState.Loading, repository.libraryFeedState.value)

        repository.refresh()

        val state = repository.libraryFeedState.value
        assertTrue(state is LibraryFeedState.Loaded)
        assertEquals(1, (state as LibraryFeedState.Loaded).items.size)
        assertEquals("example-id", state.items.single().id)
    }

    @Test
    fun `in memory repository supports empty custom content`() = runBlocking {
        val repository = InMemoryLibraryRepository(initialItems = emptyList())

        assertEquals(LibraryFeedState.Loading, repository.libraryFeedState.value)

        repository.refresh()

        assertEquals(LibraryFeedState.Empty, repository.libraryFeedState.value)
    }

    @Test
    fun `library feed helper maps empty and non empty lists to the expected states`() {
        val sampleItem = LibraryItem(
            id = "example-id",
            title = "Example Title",
            author = "Example Author",
            progressPercent = 12,
            itemType = LibraryItemType.Book,
        )

        assertEquals(LibraryFeedState.Empty, libraryFeedStateOf(emptyList()))
        assertEquals(LibraryFeedState.Loaded(listOf(sampleItem)), libraryFeedStateOf(listOf(sampleItem)))
        assertEquals(
            "1 Eintrag verfügbar",
            libraryFeedStateTitle(LibraryFeedState.Loaded(listOf(sampleItem))),
        )
        assertEquals(
            "Titel, Autor und Fortschritt werden direkt vom Audiobookshelf-Server geladen.",
            libraryFeedStateMessage(LibraryFeedState.Loaded(listOf(sampleItem))),
        )
    }

    @Test
    fun `library feed helper keeps refreshing items visible and exposes copy`() {
        val refreshingState = LibraryFeedState.Refreshing(
            items = listOf(
                LibraryItem(
                    id = "refresh-id",
                    title = "Refreshing Title",
                    author = "Refreshing Author",
                    progressPercent = 66,
                    itemType = LibraryItemType.Podcast,
                ),
            ),
        )

        assertEquals(listOf("refresh-id"), refreshingState.visibleItems().map { it.id })
        assertEquals("Bibliothek wird aktualisiert · 1 Eintrag sichtbar", libraryFeedStateTitle(refreshingState))
        assertEquals(
            "Vorhandene Einträge bleiben sichtbar, während neue Serverdaten nachgeladen werden.",
            libraryFeedStateMessage(refreshingState),
        )
    }

    @Test
    fun `library feed helper exposes stable copy for loading copy`() {
        assertEquals("Bibliothek wird geladen…", libraryFeedStateTitle(LibraryFeedState.Loading))
        assertEquals(
            "Audiobookshelf-Daten werden geladen.",
            libraryFeedStateMessage(LibraryFeedState.Loading),
        )
        assertEquals(
            "Bitte später erneut versuchen.",
            libraryFeedStateMessage(LibraryFeedState.Error(message = "   ")),
        )
    }

    @Test
    fun `library item model keeps the expected fields`() {
        val item = LibraryItem(
            id = "example-id",
            title = "Example Title",
            author = "Example Author",
            progressPercent = 12,
            itemType = LibraryItemType.Book,
            coverUrl = "https://example.com/cover.jpg",
        )

        assertEquals("example-id", item.id)
        assertEquals("Example Title", item.title)
        assertEquals("Example Author", item.author)
        assertEquals(12, item.progressPercent)
        assertEquals(LibraryItemType.Book, item.itemType)
        assertEquals("https://example.com/cover.jpg", item.coverUrl)
    }

    @Test
    fun `search helper matches title author and id tokens case insensitively`() {
        val items = listOf(
            LibraryItem(
                id = "project-hail-mary",
                title = "Project Hail Mary",
                author = "Andy Weir",
                progressPercent = 82,
                itemType = LibraryItemType.Audiobook,
            ),
            LibraryItem(
                id = "clean-architecture",
                title = "Clean Architecture",
                author = "Robert C. Martin",
                progressPercent = 23,
                itemType = LibraryItemType.Book,
            ),
        )

        assertEquals(listOf("project-hail-mary"), searchLibraryItems(items, "hail").map { it.id })
        assertEquals(listOf("clean-architecture"), searchLibraryItems(items, "robert").map { it.id })
        assertEquals(listOf("project-hail-mary"), searchLibraryItems(items, "PROJECT mary").map { it.id })
        assertTrue(searchLibraryItems(items, "   ").isEmpty())
    }

    @Test
    fun `search state helper formats idle searching results empty and error states`() {
        val resultsState = SearchState.Results(
            query = "project",
            items = listOf(
                LibraryItem(
                    id = "project-hail-mary",
                    title = "Project Hail Mary",
                    author = "Andy Weir",
                    progressPercent = 82,
                    itemType = LibraryItemType.Audiobook,
                ),
            ),
        )

        assertEquals("Suche in deiner Bibliothek", searchStateTitle(SearchState.Idle))
        assertEquals("Bestätige mit Suchen oder der Enter-Taste.", searchStateMessage(SearchState.Typing("project")))
        assertEquals("Suche läuft…", searchStateTitle(SearchState.Searching("project")))
        assertEquals("1 Treffer für \"project\"", searchStateTitle(resultsState))
        assertEquals(
            "Titel, Autor und ID werden lokal in der aktuellen Bibliothek gefiltert.",
            searchStateMessage(resultsState),
        )
        assertEquals("Keine Treffer für \"project\"", searchStateTitle(SearchState.NoResults("project")))
        assertEquals("Bitte später erneut versuchen.", searchStateMessage(SearchState.Error(query = "project", message = "   ")))
        assertEquals(listOf("project-hail-mary"), resultsState.resultsOrEmpty().map { it.id })
        assertEquals("project hail mary", normalizedSearchQuery("  project hail mary  "))
        assertEquals("Suche löschen", resultsState.clearSearchActionLabel())
        assertTrue(resultsState.canClearSearch())
        assertTrue(SearchState.NoResults("project").canClearSearch())
        assertEquals("Suche löschen", SearchState.Error(query = "project", message = "boom").clearSearchActionLabel())
        assertEquals("Suche löschen", SearchState.Error(query = "", message = "boom").clearSearchActionLabel())
        assertTrue(SearchState.Typing("project").canClearSearch())
        assertTrue(SearchState.Searching("project").canClearSearch())
        assertTrue(SearchState.Error(query = "", message = "boom").isRefreshErrorState())
        assertTrue(!SearchState.Error(query = "project", message = "boom").isRefreshErrorState())
        assertEquals(null, SearchState.Idle.clearSearchActionLabel())

        val tracker = SearchSubmissionTracker()
        val firstToken = tracker.nextToken()
        assertTrue(tracker.accepts(firstToken))
        tracker.invalidate()
        assertTrue(!tracker.accepts(firstToken))
        val secondToken = tracker.nextToken()
        assertTrue(tracker.accepts(secondToken))
    }

    @Test
    fun `in memory repository search filters the loaded items`() = runBlocking {
        val repository = InMemoryLibraryRepository()

        repository.refresh()

        assertEquals(listOf("project-hail-mary"), repository.search("hail").map { it.id })
        assertEquals(listOf("clean-architecture"), repository.search("robert").map { it.id })
    }

    @Test
    fun `in memory repository refresh exposes a refreshing state before restoring the sample feed`() = runBlocking {
        val refreshGate = CompletableDeferred<Unit>()
        val repository = InMemoryLibraryRepository(refreshPause = { refreshGate.await() })
        val refreshJob = launch { repository.refresh() }

        withTimeout(1_000) {
            while (repository.libraryFeedState.value !is LibraryFeedState.Refreshing) {
                yield()
            }
        }

        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Refreshing)
        refreshGate.complete(Unit)

        refreshJob.join()
        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Loaded)
    }

    @Test
    fun `in memory repository refresh recovers from cancellation without getting stuck refreshing`() = runBlocking {
        val refreshGate = CompletableDeferred<Unit>()
        val repository = InMemoryLibraryRepository(refreshPause = { refreshGate.await() })
        val refreshJob = launch { repository.refresh() }

        withTimeout(1_000) {
            while (repository.libraryFeedState.value !is LibraryFeedState.Refreshing) {
                yield()
            }
        }

        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Refreshing)

        refreshJob.cancel()
        refreshJob.join()
        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Loaded)
    }

    @Test
    fun `library screen helpers clamp progress translate item types and format metadata`() {
        val item = LibraryItem(
            id = "example-id",
            title = "Example Title",
            author = "Example Author",
            progressPercent = 44,
            itemType = LibraryItemType.Audiobook,
        )

        assertEquals("0%", formatProgress(-4))
        assertEquals("100%", formatProgress(103))
        assertEquals("Podcast", formatItemType(LibraryItemType.Podcast))
        assertEquals("Serie", formatItemType(LibraryItemType.Series))
        assertEquals("Hörbuch · 44%", formatLibraryItemMetadata(item))
    }

    @Test
    fun `item detail helpers summarize loading loaded and error states`() {
        val detail = LibraryItemDetail(
            item = LibraryItem(
                id = "example-id",
                title = "Example Title",
                author = "Example Author",
                progressPercent = 44,
                itemType = LibraryItemType.Audiobook,
            ),
            progressPercent = 44,
            description = "A long-form description for the item detail screen.",
            chapters = listOf(
                LibraryChapter(id = "chapter-1", title = "Intro", startSeconds = 0, endSeconds = 120),
            ),
        )

        assertEquals("Details werden geladen…", itemDetailStateTitle(ItemDetailState.Loading))
        assertEquals(
            "Die Detailansicht wird vorbereitet.",
            itemDetailStateMessage(ItemDetailState.Loading),
        )
        assertEquals("Example Title", itemDetailStateTitle(ItemDetailState.Loaded(detail)))
        assertEquals(
            "1 Kapitel · 44% · A long-form description for the item detail screen.",
            itemDetailStateMessage(ItemDetailState.Loaded(detail)),
        )
        assertEquals("Bitte später erneut versuchen.", itemDetailStateMessage(ItemDetailState.Error(message = "   ")))
        assertEquals("03:20 – 05:00", formatChapterRange(200, 300))
        assertEquals("03:20", formatChapterRange(200, null))
        assertEquals(
            "Intro · 00:00 – 02:00",
            selectedChapterDisplayLabel(
                chapters = listOf(
                    LibraryChapter(id = "chapter-1", title = "Intro", startSeconds = 0, endSeconds = 120),
                    LibraryChapter(id = "chapter-2", title = "Middle", startSeconds = 120, endSeconds = 240),
                ),
                selectedChapterId = "chapter-1",
            ),
        )
        assertEquals(
            "Ausgewähltes Kapitel",
            selectedChapterDisplayLabel(
                chapters = emptyList(),
                selectedChapterId = "chapter-unknown",
            ),
        )
        assertEquals(
            "Intro · 00:00 – 02:00",
            activeChapterDisplayLabel(
                LibraryChapter(id = "chapter-1", title = "Intro", startSeconds = 0, endSeconds = 120),
            ),
        )
        assertEquals("Kein Kapitel an dieser Position", activeChapterDisplayLabel(null))
    }

    @Test
    fun `chapter helper resolves the active chapter for playback position`() {
        val chapters = listOf(
            LibraryChapter(id = "chapter-1", title = "Intro", startSeconds = 0, endSeconds = 120),
            LibraryChapter(id = "chapter-2", title = "Middle", startSeconds = 120, endSeconds = 240),
            LibraryChapter(id = "chapter-3", title = "Outro", startSeconds = 240, endSeconds = null),
        )

        assertEquals("chapter-1", resolveActiveChapterForPlaybackPosition(chapters, 42_000)?.id)
        assertEquals("chapter-2", resolveActiveChapterForPlaybackPosition(chapters, 120_000)?.id)
        assertEquals(null, resolveActiveChapterForPlaybackPosition(chapters, -1))
        assertEquals(
            "chapter-later",
            resolveActiveChapterForPlaybackPosition(
                chapters = listOf(
                    LibraryChapter(id = "chapter-missing", title = "Missing", startSeconds = null, endSeconds = 60),
                    LibraryChapter(id = "chapter-later", title = "Later", startSeconds = 60, endSeconds = 120),
                ),
                playbackPositionMs = 75_000,
            )?.id,
        )
        assertEquals(
            null,
            resolveActiveChapterForPlaybackPosition(
                chapters = listOf(
                    LibraryChapter(id = "chapter-missing", title = "Missing", startSeconds = null, endSeconds = 60),
                    LibraryChapter(id = "chapter-later", title = "Later", startSeconds = 60, endSeconds = 120),
                ),
                playbackPositionMs = 30_000,
            ),
        )
    }

    @Test
    fun `playback action helpers reflect loading playing paused and idle states`() {
        assertEquals("Abspielen", playbackActionLabel(null, "item-1", isPreparingPlayback = false, isPlayingPlayback = false))
        assertEquals("Lädt…", playbackActionLabel("item-1", "item-1", isPreparingPlayback = true, isPlayingPlayback = false))
        assertEquals("Pause", playbackActionLabel("item-1", "item-1", isPreparingPlayback = false, isPlayingPlayback = true))
        assertEquals("Resume", playbackActionLabel("item-1", "item-1", isPreparingPlayback = false, isPlayingPlayback = false))
        assertTrue(playbackActionEnabled(null, "item-1", isPreparingPlayback = false))
        assertTrue(!playbackActionEnabled("item-1", "item-1", isPreparingPlayback = true))
    }

    @Test
    fun `player helpers clamp seek positions and format playback rates`() {
        assertEquals(0f, playbackProgressFraction(0, 0), 0f)
        assertEquals(0.5f, playbackProgressFraction(50, 100), 0f)
        assertEquals(1f, playbackProgressFraction(200, 100), 0f)
        assertEquals(10_000, seekPlaybackPosition(positionMs = 5_000, deltaMs = 5_000, durationMs = 20_000))
        assertEquals(0, seekPlaybackPosition(positionMs = 5_000, deltaMs = -9_000, durationMs = 20_000))
        assertEquals(0, seekPlaybackPosition(positionMs = 0, deltaMs = -1_000, durationMs = 0))
        assertEquals("1.0x", formatPlaybackRate(1f))
        assertEquals("1.25x", formatPlaybackRate(1.25f))
    }

    @Test
    fun `playback helpers report whether periodic progress sync should continue`() {
        assertTrue(shouldSyncPlaybackProgress("item-1", "item-1", isPlayingPlayback = true, isPreparingPlayback = false))
        assertTrue(!shouldSyncPlaybackProgress("item-1", "item-1", isPlayingPlayback = false, isPreparingPlayback = false))
        assertTrue(!shouldSyncPlaybackProgress("item-1", "item-1", isPlayingPlayback = true, isPreparingPlayback = true))
        assertTrue(!shouldSyncPlaybackProgress("item-1", "item-2", isPlayingPlayback = true, isPreparingPlayback = false))
    }

    @Test
    fun `direct playback stream urls omit the access token query parameter`() {
        assertEquals(
            "https://books.example.com/api/items/item%2F42/play",
            buildDirectPlaybackStreamUrl("https://books.example.com/", "item/42"),
        )
    }

    @Test
    fun `playback flush position prefers the media player position when available`() {
        assertEquals(42, resolvePlaybackFlushPositionMs(100, 42))
        assertEquals(100, resolvePlaybackFlushPositionMs(100, null))
        assertEquals(0, resolvePlaybackFlushPositionMs(-5, null))
    }

    @Test
    fun `playback progress helper reuses requested chapter or saved progress for the same item`() {
        val savedProgress = PlaybackProgressSnapshot(
            itemId = "item-1",
            positionMs = 123_000,
            durationMs = 500_000,
            recordedAtEpochMs = 1L,
        )

        assertEquals(
            42,
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = 42,
                latestProgress = savedProgress,
            ),
        )
        assertEquals(
            0,
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = -9,
                latestProgress = savedProgress,
            ),
        )
        assertEquals(
            123,
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = null,
                latestProgress = savedProgress,
            ),
        )
        assertEquals(
            null,
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = null,
                latestProgress = PlaybackProgressSnapshot(
                    itemId = "item-1",
                    positionMs = 500_000,
                    durationMs = 500_000,
                    recordedAtEpochMs = 2L,
                ),
            ),
        )
        assertEquals(
            null,
            resolvePlaybackStartPositionSeconds(
                itemId = "item-2",
                requestedStartPositionSeconds = null,
                latestProgress = savedProgress,
            ),
        )
        assertEquals(
            null,
            resolvePlaybackStartPositionSeconds(
                itemId = "item-1",
                requestedStartPositionSeconds = null,
                latestProgress = null,
            ),
        )
    }

    @Test
    fun `shared preferences playback progress repository stores the latest progress snapshot`() = runBlocking {
        val repository = SharedPreferencesPlaybackProgressRepository(FakeSharedPreferences())

        repository.syncProgress(itemId = "item-1", positionMs = 12_345, durationMs = 54_321)

        val snapshot = repository.latestProgress.value
        assertEquals("item-1", snapshot?.itemId)
        assertEquals(12_345, snapshot?.positionMs)
        assertEquals(54_321, snapshot?.durationMs)
        assertTrue((snapshot?.recordedAtEpochMs ?: 0L) > 0L)
    }

}
