package com.orbin.minimal.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowlistsTest {
    @Test
    fun `board slugs accept alnum underscore`() {
        assertTrue(BoardSlugs.isValid("g"))
        assertTrue(BoardSlugs.isValid("bbw"))
        assertTrue(BoardSlugs.isValid("Board_01"))
        assertFalse(BoardSlugs.isValid(""))
        assertFalse(BoardSlugs.isValid("../etc"))
        assertFalse(BoardSlugs.isValid("a/b"))
        assertFalse(BoardSlugs.isValid("has space"))
        assertNull(BoardSlugs.sanitizeOrNull(".."))
    }

    @Test
    fun `media hosts allow provider cdns only over https`() {
        assertTrue(MediaHosts.isAllowedUrl("https://i.4cdn.org/g/123.jpg"))
        assertTrue(MediaHosts.isAllowedUrl("https://bbw-chan.link/.media/file.jpg"))
        assertFalse(MediaHosts.isAllowedUrl("http://i.4cdn.org/g/123.jpg"))
        assertFalse(MediaHosts.isAllowedUrl("https://evil.example/x.jpg"))
        assertNull(MediaHosts.filterUrl("https://evil.example/x.jpg"))
    }

    @Test
    fun `media paths reject open redirects and traversal`() {
        assertEquals("/.media/a.jpg", MediaPaths.toSafeMediaLocation("/.media/a.jpg"))
        assertNull(MediaPaths.toSafeMediaLocation("//evil.example/a.jpg"))
        assertNull(MediaPaths.toSafeMediaLocation("/../../etc/passwd"))
        assertNull(MediaPaths.toSafeMediaLocation("https://evil.example/a.jpg"))
        assertEquals(
            "https://bbw-chan.link/.media/a.jpg",
            MediaPaths.toSafeMediaLocation("https://bbw-chan.link/.media/a.jpg"),
        )
        assertEquals(
            "https://bbw-chan.link/.media/a.jpg",
            MediaPaths.resolveAbsolute("https://bbw-chan.link", "/.media/a.jpg"),
        )
    }
}
