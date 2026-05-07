package com.thetheobot.shelfplayer

data class ConnectionSession(
    val serverUrl: String = "",
) {
    val hasSavedServer: Boolean
        get() = serverUrl.isNotBlank()
}

fun connectionSessionStatusText(session: ConnectionSession): String {
    return if (session.hasSavedServer) {
        "Gespeicherter Server: ${session.serverUrl}"
    } else {
        "Noch kein Server gespeichert"
    }
}
