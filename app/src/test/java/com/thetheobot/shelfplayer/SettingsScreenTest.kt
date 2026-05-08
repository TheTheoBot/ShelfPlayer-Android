package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsScreenTest {
    @Test
    fun `format skip interval renders seconds with suffix`() {
        assertEquals("15s", formatSkipInterval(15))
        assertEquals("60s", formatSkipInterval(60))
    }

    @Test
    fun `connect shortcut button label reflects normalized saved connection state`() {
        assertEquals(
            "Verbindung einrichten",
            settingsConnectShortcutButtonLabel(ConnectionSession()),
        )
        assertEquals(
            "Verbindung einrichten",
            settingsConnectShortcutButtonLabel(
                ConnectionSession(
                    ConnectionCredentials(
                        serverUrl = "   ",
                        accessToken = "token-123",
                    ),
                ),
            ),
        )
        assertEquals(
            "Connect öffnen",
            settingsConnectShortcutButtonLabel(
                ConnectionSession(
                    ConnectionCredentials(
                        serverUrl = " https://books.example.com/ ",
                        accessToken = "token-123",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `settings playback summary text reflects the current defaults and theme`() {
        assertEquals(
            "Sprung 15s · Rate 1.0x · Darstellung System",
            settingsPlaybackSummaryText(AppSettings()),
        )
        assertEquals(
            "Sprung 30s · Rate 1.25x · Darstellung Dunkel",
            settingsPlaybackSummaryText(
                AppSettings(
                    playbackSkipIntervalSeconds = 30,
                    defaultPlaybackRate = 1.25f,
                    themeMode = ThemeMode.DARK,
                ),
            ),
        )
    }
}
