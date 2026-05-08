package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemDetailModelsTest {
    @Test
    fun `resolve active chapter returns matching chapter for in range playback positions`() {
        val chapters = listOf(
            LibraryChapter(id = "chapter-b", title = "Chapter B", startSeconds = 60, endSeconds = 120),
            LibraryChapter(id = "chapter-a", title = "Chapter A", startSeconds = 0, endSeconds = 70),
            LibraryChapter(id = "chapter-c", title = "Chapter C", startSeconds = 120, endSeconds = 180),
        )

        assertEquals("chapter-a", resolveActiveChapterForPlaybackPosition(chapters, 30_000)?.id)
        assertEquals("chapter-b", resolveActiveChapterForPlaybackPosition(chapters, 75_000)?.id)
        assertEquals("chapter-c", resolveActiveChapterForPlaybackPosition(chapters, 150_000)?.id)
    }

    @Test
    fun `resolve active chapter respects chapter end and next chapter start boundaries`() {
        val chapters = listOf(
            LibraryChapter(id = "chapter-b", title = "Chapter B", startSeconds = 60, endSeconds = 120),
            LibraryChapter(id = "chapter-a", title = "Chapter A", startSeconds = 0, endSeconds = 70),
            LibraryChapter(id = "chapter-c", title = "Chapter C", startSeconds = 120, endSeconds = 180),
        )

        assertEquals("chapter-a", resolveActiveChapterForPlaybackPosition(chapters, 59_999)?.id)
        assertEquals("chapter-b", resolveActiveChapterForPlaybackPosition(chapters, 60_000)?.id)
        assertEquals("chapter-b", resolveActiveChapterForPlaybackPosition(chapters, 119_999)?.id)
        assertEquals("chapter-c", resolveActiveChapterForPlaybackPosition(chapters, 120_000)?.id)
        assertNull(resolveActiveChapterForPlaybackPosition(chapters, -1))
    }

    @Test
    fun `active chapter display label falls back for null blank and trimmed titles`() {
        assertEquals("Kein Kapitel an dieser Position", activeChapterDisplayLabel(null))
        assertEquals(
            "Unbenanntes Kapitel · 00:15 – 01:00",
            activeChapterDisplayLabel(
                LibraryChapter(
                    id = "chapter-1",
                    title = "   ",
                    startSeconds = 15,
                    endSeconds = 60,
                ),
            ),
        )
        assertEquals(
            "Kapitel 12 · 00:45",
            activeChapterDisplayLabel(
                LibraryChapter(
                    id = "chapter-2",
                    title = "  Kapitel 12  ",
                    startSeconds = 45,
                ),
            ),
        )
    }

    @Test
    fun `active chapter context display text wraps the active chapter label`() {
        assertEquals(
            "Aktuelles Kapitel: Kein Kapitel an dieser Position",
            activeChapterContextDisplayText(null),
        )
        assertEquals(
            "Aktuelles Kapitel: Kapitel 12 · 00:45",
            activeChapterContextDisplayText(
                LibraryChapter(
                    id = "chapter-2",
                    title = "Kapitel 12",
                    startSeconds = 45,
                ),
            ),
        )
    }

    @Test
    fun `player chapter context prefers the active chapter over the selected chapter label fallback`() {
        assertEquals(
            "Aktuelles Kapitel: Kapitel 12 · 00:45",
            playerChapterContextDisplayText(
                activeChapter = LibraryChapter(
                    id = "chapter-2",
                    title = "Kapitel 12",
                    startSeconds = 45,
                ),
                selectedChapterLabel = "Kapitel 01 · 00:05 – 00:15",
            ),
        )
    }

    @Test
    fun `player chapter context uses the selected chapter label when no active chapter is resolved`() {
        assertEquals(
            "Kapitel 01 · 00:05 – 00:15",
            playerChapterContextDisplayText(
                activeChapter = null,
                selectedChapterLabel = "  Kapitel 01 · 00:05 – 00:15  ",
            ),
        )
    }

    @Test
    fun `player chapter context falls back safely when the selected chapter label is blank or null`() {
        assertEquals(
            "Aktuelles Kapitel: Kein Kapitel an dieser Position",
            playerChapterContextDisplayText(
                activeChapter = null,
                selectedChapterLabel = null,
            ),
        )
        assertEquals(
            "Aktuelles Kapitel: Kein Kapitel an dieser Position",
            playerChapterContextDisplayText(
                activeChapter = null,
                selectedChapterLabel = "   ",
            ),
        )
    }

    @Test
    fun `selected chapter context resolves only for the active playback item`() {
        val detail = LibraryItemDetail(
            item = LibraryItem(
                id = "abc123",
                title = "Test Item",
                author = "Author",
                progressPercent = 10,
                itemType = LibraryItemType.Podcast,
            ),
            progressPercent = 10,
            chapters = listOf(
                LibraryChapter(id = "chapter-1", title = "Chapter 1", startSeconds = 5, endSeconds = 15),
            ),
        )

        val matchingContext = resolvePlayerSelectedChapterContext(
            playbackActiveItemId = " abc123 ",
            playbackLibraryItemDetail = detail,
            selectedChapterId = "chapter-1",
            selectedChapterStartSeconds = 5,
        )
        val mismatchingContext = resolvePlayerSelectedChapterContext(
            playbackActiveItemId = "different-id",
            playbackLibraryItemDetail = detail,
            selectedChapterId = "chapter-1",
            selectedChapterStartSeconds = 5,
        )

        assertEquals("Chapter 1 · 00:05 – 00:15", matchingContext.label)
        assertEquals(Integer.valueOf(5), matchingContext.startSeconds)
        assertNull(mismatchingContext.label)
        assertNull(mismatchingContext.startSeconds)
    }

    @Test
    fun `selected chapter context keeps the selected start time when the active item matches`() {
        val detail = LibraryItemDetail(
            item = LibraryItem(
                id = "abc123",
                title = "Test Item",
                author = "Author",
                progressPercent = 10,
                itemType = LibraryItemType.Podcast,
            ),
            progressPercent = 10,
            chapters = emptyList(),
        )

        val context = resolvePlayerSelectedChapterContext(
            playbackActiveItemId = "abc123",
            playbackLibraryItemDetail = detail,
            selectedChapterId = null,
            selectedChapterStartSeconds = 42,
        )

        assertNull(context.label)
        assertEquals(Integer.valueOf(42), context.startSeconds)
    }

    @Test
    fun `selected chapter context matches the active playback item only when ids align`() {
        assertEquals(false, isSelectedChapterContextActivePlaybackItem(null, "abc123"))
        assertEquals(true, isSelectedChapterContextActivePlaybackItem(" abc123 ", "abc123"))
        assertEquals(false, isSelectedChapterContextActivePlaybackItem("abc123", "different-id"))
        assertEquals(false, isSelectedChapterContextActivePlaybackItem("   ", "abc123"))
    }

    @Test
    fun `selected chapter context resets when the active playback item changes`() {
        assertEquals(false, shouldResetSelectedChapterContext(null, "abc123"))
        assertEquals(false, shouldResetSelectedChapterContext("abc123", "abc123"))
        assertEquals(true, shouldResetSelectedChapterContext("abc123", "different-id"))
        assertEquals(true, shouldResetSelectedChapterContext("abc123", null))
        assertEquals(false, shouldResetSelectedChapterContext("   ", "different-id"))
    }

    @Test
    fun `selected chapter display label falls back safely for missing and blank ids`() {
        val chapters = listOf(
            LibraryChapter(id = "chapter-1", title = "Chapter 1", startSeconds = 5, endSeconds = 15),
        )

        assertNull(selectedChapterDisplayLabel(chapters, null))
        assertNull(selectedChapterDisplayLabel(chapters, "   "))
        assertEquals("Ausgewähltes Kapitel", selectedChapterDisplayLabel(chapters, "missing-id"))
    }

    @Test
    fun `item detail action copy keeps playback label primary and chapter start secondary only when chapters exist`() {
        val withChapters = itemDetailActionCopy(
            playbackActionLabel = " Abspielen ",
            hasChapters = true,
        )
        val withoutChapters = itemDetailActionCopy(
            playbackActionLabel = "Lädt…",
            hasChapters = false,
        )

        assertEquals("Abspielen", withChapters.primaryActionLabel)
        assertEquals(true, withChapters.showChapterStartAction)
        assertEquals(true, withChapters.chapterStartActionEnabled)

        assertEquals("Lädt…", withoutChapters.primaryActionLabel)
        assertEquals(false, withoutChapters.showChapterStartAction)
        assertEquals(false, withoutChapters.chapterStartActionEnabled)
    }

    @Test
    fun `item detail action copy falls back to a safe primary label when playback copy is blank`() {
        val copy = itemDetailActionCopy(
            playbackActionLabel = "   ",
            hasChapters = true,
        )

        assertEquals("Abspielen", copy.primaryActionLabel)
        assertEquals(true, copy.showChapterStartAction)
        assertEquals(true, copy.chapterStartActionEnabled)
    }
}
