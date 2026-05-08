package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppRouteTest {
    @Test
    fun `item route parses item detail destination`() {
        assertEquals(
            AppRoute.ItemDetail(itemId = "abc123"),
            parseInternalAppRoute("item/abc123"),
        )
    }

    @Test
    fun `player route parses player destination`() {
        assertEquals(
            AppRoute.Player,
            parseInternalAppRoute("player"),
        )
    }

    @Test
    fun `route parser trims whitespace and leading slashes`() {
        assertEquals(
            AppRoute.Player,
            parseInternalAppRoute("  ///player  "),
        )
        assertEquals(
            AppRoute.ItemDetail(itemId = "abc123"),
            parseInternalAppRoute("\n//item/abc123\t"),
        )
    }

    @Test
    fun `route parser decodes encoded item path segments`() {
        assertEquals(
            AppRoute.ItemDetail(itemId = "abc 123"),
            parseInternalAppRoute("/item/abc%20123"),
        )
    }

    @Test
    fun `unsupported or malformed routes return null`() {
        assertNull(parseInternalAppRoute(null))
        assertNull(parseInternalAppRoute(""))
        assertNull(parseInternalAppRoute("   "))
        assertNull(parseInternalAppRoute("item"))
        assertNull(parseInternalAppRoute("item/"))
        assertNull(parseInternalAppRoute("player/extra"))
        assertNull(parseInternalAppRoute("items/abc123"))
        assertNull(parseInternalAppRoute("Player"))
        assertNull(parseInternalAppRoute("/item/abc123/extra"))
        assertNull(parseInternalAppRoute("/item/abc%2F123"))
    }
}
