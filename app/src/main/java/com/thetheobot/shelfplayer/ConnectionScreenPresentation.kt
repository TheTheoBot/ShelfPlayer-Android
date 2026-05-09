package com.thetheobot.shelfplayer

data class ConnectionScreenSummary(
    val title: String,
    val message: String,
    val statusLabel: String? = null,
)

private enum class ConnectionServerClassification {
    HTTPS,
    LOCAL_HTTP,
    REMOTE_HTTP,
    UNKNOWN,
}

private fun classifyConnectionServerUrl(rawServerUrl: String): ConnectionServerClassification? {
    val trimmed = rawServerUrl.trim()
    if (trimmed.isBlank()) return null

    val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return ConnectionServerClassification.UNKNOWN
    val isHttps = uri.scheme?.equals("https", ignoreCase = true) == true
    val isHttp = uri.scheme?.equals("http", ignoreCase = true) == true

    if (isHttps) {
        return ConnectionServerClassification.HTTPS
    }

    if (!isHttp) {
        return ConnectionServerClassification.UNKNOWN
    }

    val host = uri.host ?: extractHostFromAuthority(trimmed)
    return if (isLikelyLocalHttpHost(host)) {
        ConnectionServerClassification.LOCAL_HTTP
    } else {
        ConnectionServerClassification.REMOTE_HTTP
    }
}

fun connectionScreenSavedConnectionSummary(session: ConnectionSession): ConnectionScreenSummary {
    return if (session.hasSavedServer) {
        ConnectionScreenSummary(
            title = "Gespeicherte Verbindung",
            message = "Server: ${session.serverUrl}\nToken bleibt verschlüsselt auf dem Gerät gespeichert.",
            statusLabel = connectionScreenSavedConnectionStatusLabel(session),
        )
    } else {
        ConnectionScreenSummary(
            title = "Noch keine Verbindung gespeichert",
            message = "Trage Server-URL und Access Token ein, um mit dem Einrichten zu beginnen.",
        )
    }
}

fun connectionScreenSavedConnectionStatusLabel(session: ConnectionSession): String? {
    if (!session.hasSavedServer) return null

    return when (classifyConnectionServerUrl(session.serverUrl)) {
        ConnectionServerClassification.HTTPS -> "HTTPS"
        ConnectionServerClassification.LOCAL_HTTP -> "lokales HTTP"
        else -> "unbekannt"
    }
}

fun connectionScreenServerUrlGuidance(rawServerUrl: String): String {
    return when (classifyConnectionServerUrl(rawServerUrl)) {
        null -> "Noch keine Verbindung gespeichert. Trage Server-URL und Access Token ein."
        ConnectionServerClassification.HTTPS -> "HTTPS ist für öffentliche Server empfohlen."
        ConnectionServerClassification.LOCAL_HTTP -> "HTTP ist für lokale oder selbst gehostete Setups okay; für öffentliche Server bitte HTTPS verwenden."
        ConnectionServerClassification.REMOTE_HTTP -> "HTTP ist unverschlüsselt; für öffentliche Server bitte HTTPS verwenden."
        ConnectionServerClassification.UNKNOWN -> "Server-URL prüfen und für öffentliche Server HTTPS verwenden."
    }
}

private fun isLikelyLocalHttpHost(host: String?): Boolean {
    val normalizedHost = host?.trim()?.lowercase().orEmpty()
    if (normalizedHost.isBlank()) return false

    if (
        normalizedHost == "localhost" ||
        normalizedHost == "127.0.0.1" ||
        normalizedHost == "::1" ||
        normalizedHost == "10.0.2.2"
    ) {
        return true
    }

    if (normalizedHost.endsWith(".local")) return true

    val ipv4Octets = normalizedHost.split('.')
    if (ipv4Octets.size == 4 && ipv4Octets.all { octet -> octet.all(Char::isDigit) }) {
        val parsedOctets = ipv4Octets.mapNotNull { it.toIntOrNull() }
        if (parsedOctets.size == 4 && parsedOctets.all { it in 0..255 }) {
            val firstOctet = parsedOctets[0]
            val secondOctet = parsedOctets[1]
            return when (firstOctet) {
                10 -> true
                127 -> true
                172 -> secondOctet in 16..31
                192 -> secondOctet == 168
                else -> false
            }
        }
    }

    return false
}
