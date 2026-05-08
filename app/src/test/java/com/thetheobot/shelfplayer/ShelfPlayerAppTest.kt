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
}
