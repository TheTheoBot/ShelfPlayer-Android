package com.thetheobot.shelfplayer

data class ConnectionScreenSummary(
    val title: String,
    val message: String,
    val statusLabel: String? = null,
)

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

    val rawServerUrl = session.serverUrl.trim()
    val uri = runCatching { java.net.URI(rawServerUrl) }.getOrNull() ?: return "unbekannt"
    val isHttps = uri.scheme?.equals("https", ignoreCase = true) == true
    val isHttp = uri.scheme?.equals("http", ignoreCase = true) == true

    if (isHttps) {
        return "HTTPS"
    }

    if (!isHttp) {
        return "unbekannt"
    }

    val host = uri.host ?: extractHostFromAuthority(rawServerUrl)
    return if (isLikelyLocalHttpHost(host)) {
        "lokales HTTP"
    } else {
        "unbekannt"
    }
}

fun connectionScreenServerUrlGuidance(rawServerUrl: String): String {
    val trimmed = rawServerUrl.trim()
    if (trimmed.isBlank()) {
        return "Noch keine Verbindung gespeichert. Trage Server-URL und Access Token ein."
    }

    val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return "Server-URL prüfen und für öffentliche Server HTTPS verwenden."
    val isHttp = uri.scheme?.equals("http", ignoreCase = true) == true
    val isHttps = uri.scheme?.equals("https", ignoreCase = true) == true

    if (isHttps) {
        return "HTTPS ist für öffentliche Server empfohlen."
    }

    if (!isHttp) {
        return "Server-URL prüfen und für öffentliche Server HTTPS verwenden."
    }

    val host = uri.host ?: extractHostFromAuthority(trimmed)
    return if (isLikelyLocalHttpHost(host)) {
        "HTTP ist für lokale oder selbst gehostete Setups okay; für öffentliche Server bitte HTTPS verwenden."
    } else {
        "HTTP ist unverschlüsselt; für öffentliche Server bitte HTTPS verwenden."
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
    if (normalizedHost.startsWith("192.168.")) return true
    if (normalizedHost.startsWith("10.")) return true

    if (normalizedHost.startsWith("172.")) {
        val secondOctet = normalizedHost
            .split('.')
            .getOrNull(1)
            ?.toIntOrNull()
        if (secondOctet != null && secondOctet in 16..31) {
            return true
        }
    }

    return false
}
