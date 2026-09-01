package com.orbin.minimal.core.data

import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.core.provider.ProviderRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class FeedRepository(
    private val providers: ProviderRegistry,
    private val followedBoards: FollowedBoardStore,
) {
    fun followed(): List<BoardRef> = followedBoards.all()

    fun toggle(board: BoardRef): Boolean = followedBoards.toggle(board)

    suspend fun availableBoards(): List<BoardRef> =
        coroutineScope {
            providers.all()
                .map { provider -> async { runCatching { provider.boards() }.getOrDefault(emptyList()) } }
                .awaitAll()
                .flatten()
                .sortedWith(compareBy(BoardRef::provider, BoardRef::board))
        }

    suspend fun mergedFeed(): List<FeedThread> =
        coroutineScope {
            followedBoards.all()
                .map { board ->
                    async {
                        runCatching { providers.require(board.provider).catalog(board.board) }
                            .getOrDefault(emptyList())
                    }
                }
                .awaitAll()
                .flatten()
                .sortedByDescending(FeedThread::lastActivityEpochMillis)
        }
}

class ThreadRepository(
    private val providers: ProviderRegistry,
) {
    suspend fun load(provider: String, board: String, threadId: Long): ThreadDetails =
        providers.require(provider).thread(board, threadId)
}
