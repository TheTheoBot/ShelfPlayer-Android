package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfPlayerAppTest {
    @Test
    fun `player state helper reports loading while playback is preparing`() {
        assertEquals(
            "Lädt…",
            playerStateStatusText(
                isPreparingPlayback = true,
                isPlayingPlayback = false,
                hasActivePlaybackItem = true,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state helper reports active playback while playing`() {
        assertEquals(
            "Wiedergabe läuft",
            playerStateStatusText(
                isPreparingPlayback = false,
                isPlayingPlayback = true,
                hasActivePlaybackItem = true,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state helper reports paused when an active item is selected but playback is idle`() {
        assertEquals(
            "Pausiert",
            playerStateStatusText(
                isPreparingPlayback = false,
                isPlayingPlayback = false,
                hasActivePlaybackItem = true,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state helper reports ready when nothing is active`() {
        assertEquals(
            "Bereit",
            playerStateStatusText(
                isPreparingPlayback = false,
                isPlayingPlayback = false,
                hasActivePlaybackItem = false,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state helper reports error before any other state`() {
        assertEquals(
            "Fehler",
            playerStateStatusText(
                isPreparingPlayback = true,
                isPlayingPlayback = true,
                hasActivePlaybackItem = true,
                playbackError = "Network failed",
            ),
        )
    }

    @Test
    fun `player state helper ignores blank playback errors`() {
        assertEquals(
            "Lädt…",
            playerStateStatusText(
                isPreparingPlayback = true,
                isPlayingPlayback = false,
                hasActivePlaybackItem = true,
                playbackError = "   ",
            ),
        )
        assertEquals(
            "Lädt",
            playerStateAccessibilityDescription(
                isPreparingPlayback = true,
                isPlayingPlayback = false,
                hasActivePlaybackItem = true,
                playbackError = "   ",
            ),
        )
    }

    @Test
    fun `player state accessibility helper reports loading when playback is preparing`() {
        assertEquals(
            "Lädt",
            playerStateAccessibilityDescription(
                isPreparingPlayback = true,
                isPlayingPlayback = false,
                hasActivePlaybackItem = true,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state accessibility helper reports playing when playback is active`() {
        assertEquals(
            "Wiedergabe läuft",
            playerStateAccessibilityDescription(
                isPreparingPlayback = false,
                isPlayingPlayback = true,
                hasActivePlaybackItem = true,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state accessibility helper reports paused when an active item is selected but playback is idle`() {
        assertEquals(
            "Pausiert",
            playerStateAccessibilityDescription(
                isPreparingPlayback = false,
                isPlayingPlayback = false,
                hasActivePlaybackItem = true,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state accessibility helper reports ready when nothing is active`() {
        assertEquals(
            "Bereit",
            playerStateAccessibilityDescription(
                isPreparingPlayback = false,
                isPlayingPlayback = false,
                hasActivePlaybackItem = false,
                playbackError = null,
            ),
        )
    }

    @Test
    fun `player state accessibility helper reports error before any other state`() {
        assertEquals(
            "Fehler",
            playerStateAccessibilityDescription(
                isPreparingPlayback = true,
                isPlayingPlayback = true,
                hasActivePlaybackItem = true,
                playbackError = "Network failed",
            ),
        )
    }

    @Test
    fun `chapter quick access helper reports the active chapter state for accessibility`() {
        assertEquals(
            "Aktuelles Kapitel",
            chapterQuickAccessStateDescription(isActiveChapter = true),
        )
    }

    @Test
    fun `chapter quick access helper reports inactive chapters as available`() {
        assertEquals(
            "Kapitel verfügbar",
            chapterQuickAccessStateDescription(isActiveChapter = false),
        )
    }
}
