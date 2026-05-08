package com.thetheobot.shelfplayer

data class ConnectionSession(
    val savedConnection: ConnectionCredentials? = null,
) {
    val serverUrl: String
        get() = normalizeServerUrl(savedConnection?.serverUrl.orEmpty())

    val hasSavedServer: Boolean
        get() = serverUrl.isNotBlank()
}

fun ConnectionSession.shouldShowOnboarding(): Boolean {
    return !hasSavedServer
}

fun connectionSessionStatusText(session: ConnectionSession): String {
    return if (session.hasSavedServer) {
        "Gespeicherter Server: ${session.serverUrl}"
    } else {
        "Noch kein Server gespeichert"
    }
}
