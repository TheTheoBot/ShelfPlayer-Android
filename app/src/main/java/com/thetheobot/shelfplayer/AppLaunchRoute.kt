package com.thetheobot.shelfplayer

internal data class AppLaunchSelection(
    val tab: AppTab,
    val itemId: String? = null,
)

internal data class AppLaunchState(
    val route: AppRoute?,
    val eventId: Int,
    val isDeepLink: Boolean,
)

internal enum class AppBackNavigation {
    CloseItemDetail,
    SwitchToLibrary,
    Unhandled,
}

internal fun resolveAppBackNavigation(
    selectedTab: AppTab,
    selectedLibraryItemId: String?,
): AppBackNavigation {
    return when {
        selectedLibraryItemId != null -> AppBackNavigation.CloseItemDetail
        selectedTab != AppTab.Library -> AppBackNavigation.SwitchToLibrary
        else -> AppBackNavigation.Unhandled
    }
}

internal fun appLaunchSelectionForRoute(route: AppRoute?): AppLaunchSelection? {
    return when (route) {
        is AppRoute.ItemDetail -> AppLaunchSelection(
            tab = AppTab.Library,
            itemId = route.itemId,
        )
        AppRoute.Player -> AppLaunchSelection(tab = AppTab.Player)
        null -> null
    }
}

internal fun appLaunchStateForInitialIntent(
    route: AppRoute?,
    isDeepLink: Boolean,
): AppLaunchState {
    val eventId = if (route == null) 0 else 1
    return AppLaunchState(
        route = route,
        eventId = eventId,
        isDeepLink = isDeepLink,
    )
}

internal fun appLaunchStateForNextIntent(
    previousState: AppLaunchState,
    route: AppRoute?,
    isDeepLink: Boolean,
): AppLaunchState {
    return AppLaunchState(
        route = route,
        eventId = previousState.eventId + 1,
        isDeepLink = isDeepLink,
    )
}

internal fun shouldApplyAppLaunchSelection(
    selectedTab: AppTab,
    selectedLibraryItemId: String?,
    launchSelection: AppLaunchSelection,
): Boolean {
    return selectedTab != launchSelection.tab || selectedLibraryItemId != launchSelection.itemId
}

internal fun shouldResetToDefaultLibraryState(
    selectedTab: AppTab,
    selectedLibraryItemId: String?,
): Boolean {
    return selectedTab != AppTab.Library || selectedLibraryItemId != null
}

internal fun shouldIgnoreDeepLinkLaunch(
    initialRoute: AppRoute?,
    launchIntentIsDeepLink: Boolean,
): Boolean {
    return launchIntentIsDeepLink && initialRoute == null
}

internal data class ItemDetailResetState(
    val selectedTab: AppTab = AppTab.Library,
    val selectedLibraryItemDetailState: ItemDetailState = ItemDetailState.Loading,
    val selectedLibraryItemId: String? = null,
    val selectedChapterId: String? = null,
    val selectedChapterStartSeconds: Int? = null,
)

internal fun defaultItemDetailResetState(): ItemDetailResetState {
    return ItemDetailResetState()
}
