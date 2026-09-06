package com.orbin.minimal.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalLinksTest {
    @Test
    fun `extracts https links only`() {
        assertEquals(
            listOf("https://example.com/a"),
            extractExternalLinks("see https://example.com/a and http://example.org/b"),
        )
    }

    @Test
    fun `decodes amp entity and trims sentence punctuation`() {
        assertEquals(
            listOf("https://example.com/watch?a=1&b=2"),
            extractExternalLinks("https://example.com/watch?a=1&amp;b=2)."),
        )
    }

    @Test
    fun `deduplicates links while keeping first seen order`() {
        assertEquals(
            listOf("https://example.com/a", "https://example.com/b"),
            extractExternalLinks("https://example.com/a https://example.com/a https://example.com/b"),
        )
    }

    @Test
    fun `ignores non web schemes and cleartext http`() {
        assertEquals(
            emptyList<String>(),
            extractExternalLinks("ftp://example.com file://local/path http://insecure.example/x"),
        )
    }

    @Test
    fun `isSafeExternalLink requires https`() {
        assertTrue(isSafeExternalLink("https://example.com"))
        assertFalse(isSafeExternalLink("http://example.com"))
        assertFalse(isSafeExternalLink("javascript:alert(1)"))
    }
}
