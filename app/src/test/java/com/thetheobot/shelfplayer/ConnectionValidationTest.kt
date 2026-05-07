package com.thetheobot.shelfplayer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection

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
    fun `validateServerUrl accepts uppercase remote http url`() {
        assertNull(validateServerUrl("HTTP://books.example.com"))
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
            validateServerUrl("https://user:***@books.example.com"),
        )
    }

    @Test
    fun `validateAccessToken rejects blanks`() {
        assertEquals("Access Token fehlt", validateAccessToken("   "))
    }

    @Test
    fun `verifyConnection reports failure on non success http status`() = runBlocking {
        val result = verifyConnection(
            ConnectionCredentials(
                serverUrl = "https://books.example.com",
                accessToken = "token-123",
            ),
        ) { url ->
            object : HttpURLConnection(url) {
                override fun connect() = Unit
                override fun disconnect() = Unit
                override fun usingProxy(): Boolean = false
                override fun getResponseCode(): Int = 404
            }
        }

        assertTrue(result is ConnectionVerificationResult.Failure)
        assertEquals(
            "Verbindungstest fehlgeschlagen: HTTP 404",
            (result as ConnectionVerificationResult.Failure).message,
        )
    }

    @Test
    fun `verifyConnection reports success for a reachable server`() = runBlocking {
        val result = verifyConnection(
            ConnectionCredentials(
                serverUrl = "https://books.example.com",
                accessToken = " token-123 ",
            ),
        ) { url ->
            object : HttpURLConnection(url) {
                override fun connect() = Unit
                override fun disconnect() = Unit
                override fun usingProxy(): Boolean = false
                override fun getResponseCode(): Int = 204
            }
        }

        assertEquals(ConnectionVerificationResult.Success, result)
    }

    @Test
    fun `verifyConnection reports failure when the server cannot be reached`() = runBlocking {
        val result = verifyConnection(
            ConnectionCredentials(
                serverUrl = "https://books.example.com",
                accessToken = "token-123",
            ),
        ) { url ->
            object : HttpURLConnection(url) {
                override fun connect() = Unit
                override fun disconnect() = Unit
                override fun usingProxy(): Boolean = false
                override fun getResponseCode(): Int {
                    throw IOException("boom")
                }
            }
        }

        assertTrue(result is ConnectionVerificationResult.Failure)
        assertEquals(
            "Verbindungstest fehlgeschlagen: boom",
            (result as ConnectionVerificationResult.Failure).message,
        )
    }

    @Test
    fun `encryptedConnectionCredentialsStore saves and loads credentials`() {
        val store = EncryptedConnectionCredentialsStore(FakeSharedPreferences())

        assertTrue(
            store.save(
                ConnectionCredentials(
                    serverUrl = "https://books.example.com",
                    accessToken = " token-123 ",
                ),
            ),
        )
        assertEquals(
            ConnectionCredentials("https://books.example.com", "token-123"),
            store.load(),
        )
    }

    @Test
    fun `validateConnectionForm reports both fields as valid`() {
        val validation = validateConnectionForm(
            serverUrl = "https://books.example.com",
            accessToken = "token-123",
        )

        assertNull(validation.serverUrlError)
        assertNull(validation.accessTokenError)
        assertTrue(validation.isValid)
    }

    @Test
    fun `validateConnectionForm reports both field errors`() {
        val validation = validateConnectionForm(
            serverUrl = "books.example.com",
            accessToken = "   ",
        )

        assertEquals("Server-URL muss mit http:// oder https:// beginnen", validation.serverUrlError)
        assertEquals("Access Token fehlt", validation.accessTokenError)
        assertTrue(!validation.isValid)
    }

    @Test
    fun `hasConnectionInputs trims whitespace before enabling actions`() {
        assertTrue(!hasConnectionInputs("   ", "token-123"))
        assertTrue(hasConnectionInputs("https://books.example.com", " token-123 "))
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
            connectionSessionStatusText(ConnectionSession(ConnectionCredentials("https://books.example.com", "token-123"))),
        )
    }
}
