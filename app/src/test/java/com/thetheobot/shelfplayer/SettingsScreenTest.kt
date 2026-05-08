package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsScreenTest {
    @Test
    fun `format skip interval summary renders singular and plural german labels`() {
        assertEquals("1 Sekunde", formatSkipIntervalSummary(1))
        assertEquals("15 Sekunden", formatSkipIntervalSummary(15))
    }

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
            "Verbindung verwalten",
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
    fun `settings playback summary rows expose the normalized values in a compact labeled form`() {
        assertEquals(
            listOf(
                SettingsPlaybackSummaryRow("Sprungintervall", "15 Sekunden"),
                SettingsPlaybackSummaryRow("Standardrate", "1.0x"),
                SettingsPlaybackSummaryRow("Darstellung", "System"),
            ),
            settingsPlaybackSummaryRows(AppSettings()),
        )

        assertEquals(
            listOf(
                SettingsPlaybackSummaryRow("Sprungintervall", "1 Sekunde"),
                SettingsPlaybackSummaryRow("Standardrate", "1.25x"),
                SettingsPlaybackSummaryRow("Darstellung", "Dunkel"),
            ),
            settingsPlaybackSummaryRows(
                AppSettings(
                    playbackSkipIntervalSeconds = 1,
                    defaultPlaybackRate = 1.25f,
                    themeMode = ThemeMode.DARK,
                ),
            ),
        )
    }
}
