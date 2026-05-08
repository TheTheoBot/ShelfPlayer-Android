package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRootStateTest {
    @Test
    fun `resolveAppRootState returns loading while credentials store initializes`() {
        assertEquals(
            AppRootState.Loading,
            resolveAppRootState(
                connectionStoreReady = false,
                connectionInitFailed = false,
                connectionLoadFailed = false,
                connectionSession = ConnectionSession(),
            ),
        )
    }

    @Test
    fun `resolveAppRootState keeps loading precedence over downstream failure flags`() {
        assertEquals(
            AppRootState.Loading,
            resolveAppRootState(
                connectionStoreReady = false,
                connectionInitFailed = true,
                connectionLoadFailed = true,
                connectionSession = ConnectionSession(
                    ConnectionCredentials(
                        serverUrl = "https://books.example.com",
                        accessToken = "token-123",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `resolveAppRootState returns no connection when store is ready without saved server`() {
        assertEquals(
            AppRootState.NoConnection,
            resolveAppRootState(
                connectionStoreReady = true,
                connectionInitFailed = false,
                connectionLoadFailed = false,
                connectionSession = ConnectionSession(
                    ConnectionCredentials(
                        serverUrl = "   ",
                        accessToken = "token-123",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `resolveAppRootState returns ready when a server is saved`() {
        assertEquals(
            AppRootState.Ready,
            resolveAppRootState(
                connectionStoreReady = true,
                connectionInitFailed = false,
                connectionLoadFailed = false,
                connectionSession = ConnectionSession(
                    ConnectionCredentials(
                        serverUrl = "https://books.example.com",
                        accessToken = "token-123",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `resolveAppRootState returns load error when saved connection cannot be loaded`() {
        assertEquals(
            AppRootState.LoadError,
            resolveAppRootState(
                connectionStoreReady = true,
                connectionInitFailed = false,
                connectionLoadFailed = true,
                connectionSession = ConnectionSession(),
            ),
        )
    }

    @Test
    fun `resolveAppRootState returns fatal error when store initialization failed`() {
        assertEquals(
            AppRootState.FatalError,
            resolveAppRootState(
                connectionStoreReady = true,
                connectionInitFailed = true,
                connectionLoadFailed = false,
                connectionSession = ConnectionSession(),
            ),
        )
    }

    @Test
    fun `resolveAppRootState returns fatal error before load error when both failure flags are set`() {
        assertEquals(
            AppRootState.FatalError,
            resolveAppRootState(
                connectionStoreReady = true,
                connectionInitFailed = true,
                connectionLoadFailed = true,
                connectionSession = ConnectionSession(
                    ConnectionCredentials(
                        serverUrl = "https://books.example.com",
                        accessToken = "token-123",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `connection session shows empty status when no server is saved`() {
        assertEquals(
            "Noch kein Server gespeichert",
            connectionSessionStatusText(ConnectionSession()),
        )
    }

    @Test
    fun `connection session treats whitespace server as missing`() {
        val session = ConnectionSession(
            ConnectionCredentials(
                serverUrl = "   ",
                accessToken = "token-123",
            ),
        )

        assertEquals("Noch kein Server gespeichert", connectionSessionStatusText(session))
        assertTrue(session.shouldShowOnboarding())
    }

    @Test
    fun `connection session trims trailing slash from remembered server status`() {
        val session = ConnectionSession(
            ConnectionCredentials(
                serverUrl = " https://books.example.com/ ",
                accessToken = "token-123",
            ),
        )

        assertEquals(
            "Gespeicherter Server: https://books.example.com",
            connectionSessionStatusText(session),
        )
        assertFalse(session.shouldShowOnboarding())
    }

    @Test
    fun `connection session starts onboarding without a saved server`() {
        assertTrue(ConnectionSession().shouldShowOnboarding())
    }

    @Test
    fun `connection session skips onboarding when a server is remembered`() {
        assertFalse(
            ConnectionSession(
                ConnectionCredentials(
                    serverUrl = "https://books.example.com",
                    accessToken = "token-123",
                ),
            ).shouldShowOnboarding(),
        )
    }
}
