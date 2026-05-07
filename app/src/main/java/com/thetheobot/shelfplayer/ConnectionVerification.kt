package com.thetheobot.shelfplayer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface ConnectionVerificationResult {
    data object Success : ConnectionVerificationResult

    data class Failure(val message: String) : ConnectionVerificationResult
}

suspend fun verifyConnection(
    credentials: ConnectionCredentials,
    connectionFactory: (URL) -> HttpURLConnection = ::openHttpConnection,
): ConnectionVerificationResult {
    return withContext(Dispatchers.IO) {
        runSuspendCatchingPreservingCancellation {
            val url = URL(normalizeServerUrl(credentials.serverUrl))
            val connection = connectionFactory(url).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")

                val token = credentials.accessToken.trim()
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..399) {
                    throw IOException("HTTP $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }.fold(
            onSuccess = { ConnectionVerificationResult.Success },
            onFailure = { throwable ->
                ConnectionVerificationResult.Failure(
                    "Verbindungstest fehlgeschlagen: ${throwable.message ?: "unbekannter Fehler"}",
                )
            },
        )
    }
}

private fun openHttpConnection(url: URL): HttpURLConnection {
    return (url.openConnection() as HttpURLConnection)
}
