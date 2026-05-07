package com.thetheobot.shelfplayer

enum class AppRootState {
    Loading,
    NoConnection,
    LoadError,
    FatalError,
    Ready,
}

fun resolveAppRootState(
    connectionStoreReady: Boolean,
    connectionInitFailed: Boolean,
    connectionLoadFailed: Boolean,
    connectionSession: ConnectionSession,
): AppRootState {
    return when {
        !connectionStoreReady -> AppRootState.Loading
        connectionInitFailed -> AppRootState.FatalError
        connectionLoadFailed -> AppRootState.LoadError
        connectionSession.shouldShowOnboarding() -> AppRootState.NoConnection
        else -> AppRootState.Ready
    }
}
