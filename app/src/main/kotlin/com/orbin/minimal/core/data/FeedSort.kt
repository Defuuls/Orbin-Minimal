package com.orbin.minimal.core.data

import com.orbin.minimal.core.model.FeedThread

/** How the merged feed is ordered. */
enum class FeedSort(val label: String) {
    /**
     * Group threads by board, newest thread first within each board. The
     * default: it keeps a board's threads together and surfaces fresh ones.
     */
    BOARD("Board"),

    /** Group by board, most recently active thread first within each board. */
    ACTIVITY("Latest activity"),
    ;

    companion object {
        val DEFAULT = BOARD
    }
}

private val byBoardThenNewest =
    compareBy<FeedThread>({ it.provider }, { it.board })
        .thenByDescending(FeedThread::createdAtEpochMillis)
        // Stable tiebreak: two threads can share a creation second, and a
        // feed that reshuffles on refresh is worse than an arbitrary order.
        .thenByDescending(FeedThread::threadId)

private val byBoardThenActivity =
    compareBy<FeedThread>({ it.provider }, { it.board })
        .thenByDescending(FeedThread::lastActivityEpochMillis)
        .thenByDescending(FeedThread::threadId)

fun List<FeedThread>.sortedFor(sort: FeedSort): List<FeedThread> =
    when (sort) {
        FeedSort.BOARD -> sortedWith(byBoardThenNewest)
        FeedSort.ACTIVITY -> sortedWith(byBoardThenActivity)
    }
