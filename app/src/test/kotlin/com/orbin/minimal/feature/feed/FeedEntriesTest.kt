package com.orbin.minimal.feature.feed

import com.orbin.minimal.core.model.FeedThread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedEntriesTest {
    private fun thread(board: String, id: Long) =
        FeedThread(
            provider = "fourchan",
            board = board,
            threadId = id,
            title = "t$id",
            lastActivityEpochMillis = id,
            createdAtEpochMillis = id,
        )

    @Test
    fun `every board group starts with a header`() {
        val groups =
            listOf(
                "/a/" to listOf(thread("a", 2), thread("a", 1)),
                "/b/" to listOf(thread("b", 3)),
            )

        val entries = buildFeedEntries(groups, "fourchan")

        assertEquals(5, entries.size)
        assertEquals(
            listOf("header:/a/", "row:2", "row:1", "header:/b/", "row:3"),
            entries.map { entry ->
                when (entry) {
                    is FeedListEntry.Header -> "header:${entry.label}"
                    is FeedListEntry.Row -> "row:${entry.thread.threadId}"
                }
            },
        )
        assertTrue(entries.filterIsInstance<FeedListEntry.Header>().all { it.site == "fourchan" })
        assertFalse(entries.filterIsInstance<FeedListEntry.Row>().any(FeedListEntry.Row::showBoard))
    }

    @Test
    fun `board groups and headers count toward the soft cap`() {
        val groups =
            listOf(
                "/a/" to List(10) { thread("a", it.toLong()) },
                "/b/" to List(10) { thread("b", 100L + it) },
            )

        val capped =
            buildFeedEntries(
                groups = groups,
                selectedSiteId = "fourchan",
                maxItems = 5,
            )

        assertEquals(5, capped.size)
        assertTrue(capped.first() is FeedListEntry.Header)
    }
}
