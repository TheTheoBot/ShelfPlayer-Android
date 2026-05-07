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
    fun `validateAccessToken rejects blanks`() {
        assertEquals("Access Token fehlt", validateAccessToken("   "))
    }
}
