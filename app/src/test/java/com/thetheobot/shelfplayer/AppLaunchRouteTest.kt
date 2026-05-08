package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLaunchRouteTest {
    @Test
    fun `item detail route opens library tab with the item selected`() {
        assertEquals(
            AppLaunchSelection(
                tab = AppTab.Library,
                itemId = "abc123",
            ),
            appLaunchSelectionForRoute(AppRoute.ItemDetail(itemId = "abc123")),
        )
    }

    @Test
    fun `player route opens the player tab`() {
        assertEquals(
            AppLaunchSelection(tab = AppTab.Player),
            appLaunchSelectionForRoute(AppRoute.Player),
        )
    }

    @Test
    fun `initial launch state starts from event id one for deep links and zero for launcher starts`() {
        assertEquals(
            AppLaunchState(route = AppRoute.Player, eventId = 1, isDeepLink = true),
            appLaunchStateForInitialIntent(
                route = AppRoute.Player,
                isDeepLink = true,
            ),
        )
        assertEquals(
            AppLaunchState(route = null, eventId = 0, isDeepLink = false),
            appLaunchStateForInitialIntent(
                route = null,
                isDeepLink = false,
            ),
        )
    }

    @Test
    fun `next launch state increments the event id and keeps the latest route metadata`() {
        val initialState = AppLaunchState(route = AppRoute.Player, eventId = 1, isDeepLink = true)
        assertEquals(
            AppLaunchState(route = AppRoute.ItemDetail(itemId = "abc123"), eventId = 2, isDeepLink = false),
            appLaunchStateForNextIntent(
                previousState = initialState,
                route = AppRoute.ItemDetail(itemId = "abc123"),
                isDeepLink = false,
            ),
        )
    }

    @Test
    fun `runtime route selection applies only when the visible route changes`() {
        assertTrue(
            shouldApplyAppLaunchSelection(
                selectedTab = AppTab.Library,
                selectedLibraryItemId = null,
                launchSelection = AppLaunchSelection(tab = AppTab.Player),
            ),
        )
        assertFalse(
            shouldApplyAppLaunchSelection(
                selectedTab = AppTab.Library,
                selectedLibraryItemId = "abc123",
                launchSelection = AppLaunchSelection(tab = AppTab.Library, itemId = "abc123"),
            ),
        )
    }

    @Test
    fun `default library reset only runs when the visible route changed away from the root`() {
        assertTrue(shouldResetToDefaultLibraryState(AppTab.Player, null))
        assertTrue(shouldResetToDefaultLibraryState(AppTab.Library, "abc123"))
        assertFalse(shouldResetToDefaultLibraryState(AppTab.Library, null))
    }

    @Test
    fun `invalid deep-link routes are ignored while launcher resets are allowed`() {
        assertTrue(shouldIgnoreDeepLinkLaunch(null, true))
        assertFalse(shouldIgnoreDeepLinkLaunch(AppRoute.Player, true))
        assertFalse(shouldIgnoreDeepLinkLaunch(null, false))
    }

    @Test
    fun `back navigation closes an open item detail before changing tabs`() {
        assertEquals(
            AppBackNavigation.CloseItemDetail,
            resolveAppBackNavigation(
                selectedTab = AppTab.Player,
                selectedLibraryItemId = "abc123",
            ),
        )
    }

    @Test
    fun `back navigation switches to library when another tab is active`() {
        assertEquals(
            AppBackNavigation.SwitchToLibrary,
            resolveAppBackNavigation(
                selectedTab = AppTab.Settings,
                selectedLibraryItemId = null,
            ),
        )
    }

    @Test
    fun `back navigation is unhandled at the library root`() {
        assertEquals(
            AppBackNavigation.Unhandled,
            resolveAppBackNavigation(
                selectedTab = AppTab.Library,
                selectedLibraryItemId = null,
            ),
        )
    }

    @Test
    fun `default item detail reset returns to the library root with loading detail state`() {
        assertEquals(
            ItemDetailResetState(),
            defaultItemDetailResetState(),
        )
    }

    @Test
    fun `null route does not request a special launch destination`() {
        assertNull(appLaunchSelectionForRoute(null))
    }
}
