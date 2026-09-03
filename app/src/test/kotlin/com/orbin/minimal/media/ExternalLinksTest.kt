package com.orbin.minimal.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalLinksTest {
    @Test
    fun `extracts http and https links`() {
        assertEquals(
            listOf("https://example.com/a", "http://example.org/b"),
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
    fun `ignores non web schemes`() {
        assertEquals(emptyList<String>(), extractExternalLinks("ftp://example.com file://local/path"))
    }
}
