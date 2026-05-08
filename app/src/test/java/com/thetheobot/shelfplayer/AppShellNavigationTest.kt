package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppShellNavigationTest {
    @Test
    fun `bottom navigation exposes the four MVP tabs and hides connection management`() {
        assertEquals(
            listOf(
                AppTab.Library,
                AppTab.Search,
                AppTab.Player,
                AppTab.Settings,
            ),
            bottomNavigationTabs(),
        )
        assertFalse(bottomNavigationTabs().contains(AppTab.Connect))
    }
}
