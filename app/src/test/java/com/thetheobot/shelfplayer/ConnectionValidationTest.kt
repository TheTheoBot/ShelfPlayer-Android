package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionValidationTest {
    @Test
    fun `normalizeServerUrl trims whitespace and trailing slash`() {
        assertEquals("https://books.example.com", normalizeServerUrl("  https://books.example.com/  "))
    }

    @Test
    fun `validateServerUrl accepts secure absolute url`() {
        assertNull(validateServerUrl("https://books.example.com"))
    }

    @Test
    fun `validateServerUrl rejects blanks`() {
        assertEquals("Server-URL fehlt", validateServerUrl("   "))
    }

    @Test
    fun `validateServerUrl rejects missing host`() {
        assertEquals(
            "Server-URL braucht einen Hostnamen",
            validateServerUrl("https://"),
        )
    }

    @Test
    fun `validateServerUrl rejects unsupported scheme`() {
        assertEquals(
            "Server-URL muss mit http:// oder https:// beginnen",
            validateServerUrl("ftp://books.example.com"),
        )
    }

    @Test
    fun `validateServerUrl rejects malformed url`() {
        assertEquals(
            "Server-URL ist ungültig",
            validateServerUrl("https://books.example.com/invalid path"),
        )
    }

    @Test
    fun `validateServerUrl rejects missing scheme`() {
        assertEquals(
            "Server-URL muss mit http:// oder https:// beginnen",
            validateServerUrl("books.example.com"),
        )
    }

    @Test
    fun `validateServerUrl accepts uppercase https scheme`() {
        assertNull(validateServerUrl("HTTPS://books.example.com"))
    }

    @Test
    fun `validateServerUrl rejects uppercase remote http url`() {
        assertEquals(
            "Server-URL muss mit https:// beginnen",
            validateServerUrl("HTTP://books.example.com"),
        )
    }

    @Test
    fun `validateServerUrl accepts local http url for localhost development`() {
        assertNull(validateServerUrl("HTTP://localhost:1337"))
    }

    @Test
    fun `validateServerUrl accepts local http url for emulator development`() {
        assertNull(validateServerUrl("http://10.0.2.2:1337"))
    }

    @Test
    fun `validateServerUrl rejects embedded credentials`() {
        assertEquals(
            "Server-URL darf keine Zugangsdaten enthalten",
            validateServerUrl("https://user:pass@books.example.com"),
        )
    }

    @Test
    fun `validateAccessToken rejects blanks`() {
        assertEquals("Access Token fehlt", validateAccessToken("   "))
    }

    @Test
    fun `connectionSessionStatusText shows empty state when no server is remembered`() {
        assertEquals(
            "Noch kein Server gespeichert",
            connectionSessionStatusText(ConnectionSession()),
        )
    }

    @Test
    fun `connectionSessionStatusText shows remembered server`() {
        assertEquals(
            "Gespeicherter Server: https://books.example.com",
            connectionSessionStatusText(ConnectionSession("https://books.example.com")),
        )
    }
}
