package com.orbin.minimal.core.data

import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.core.provider.ProviderRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class ProviderFailure(
    val provider: String,
    val operation: String,
    val message: String,
)

data class LoadResult<T>(
    val value: T,
    val failures: List<ProviderFailure> = emptyList(),
) {
    val isPartial: Boolean get() = failures.isNotEmpty() && when (value) {
        is Collection<*> -> value.isNotEmpty()
        else -> true
    }

    val isTotalFailure: Boolean get() = failures.isNotEmpty() && when (value) {
        is Collection<*> -> value.isEmpty()
        else -> false
    }
}

private data class ProviderResult<T>(
    val value: T,
    val failure: ProviderFailure? = null,
)

class FeedRepository(
    private val providers: ProviderRegistry,
    private val followedBoards: FollowedBoardStore,
    maxConcurrentRequests: Int = DEFAULT_MAX_CONCURRENT_REQUESTS,
) {
    private val requestGate = Semaphore(maxConcurrentRequests.coerceAtLeast(1))

    fun followed(): List<BoardRef> = followedBoards.all()

    fun toggle(board: BoardRef): Boolean = followedBoards.toggle(board)

    /** Compatibility surface for simple callers. Prefer [availableBoardsDetailed] when failures matter. */
    suspend fun availableBoards(): List<BoardRef> = availableBoardsDetailed().value

    suspend fun availableBoardsDetailed(): LoadResult<List<BoardRef>> =
        coroutineScope {
            val results = providers.all()
                .map { provider ->
                    async {
                        requestGate.withPermit {
                            providerCall(
                                provider = provider.id,
                                operation = "load boards",
                                fallback = emptyList(),
                            ) { provider.boards() }
                        }
                    }
                }
                .awaitAll()

            LoadResult(
                value = results.flatMap(ProviderResult<List<BoardRef>>::value)
                    .sortedWith(compareBy(BoardRef::provider, BoardRef::board)),
                failures = results.mapNotNull(ProviderResult<List<BoardRef>>::failure),
            )
        }

    /** Compatibility surface for simple callers. Prefer [mergedFeedDetailed] when failures matter. */
    suspend fun mergedFeed(sort: FeedSort = FeedSort.DEFAULT): List<FeedThread> =
        mergedFeedDetailed(sort).value

    suspend fun mergedFeedDetailed(sort: FeedSort = FeedSort.DEFAULT): LoadResult<List<FeedThread>> =
        coroutineScope {
            val results = followedBoards.all()
                .map { board ->
                    async {
                        requestGate.withPermit {
                            providerCall(
                                provider = board.provider,
                                operation = "load /${board.board}/ catalog",
                                fallback = emptyList(),
                            ) {
                                providers.require(board.provider).catalog(board.board)
                            }
                        }
                    }
                }
                .awaitAll()

            LoadResult(
                value = results.flatMap(ProviderResult<List<FeedThread>>::value).sortedFor(sort),
                failures = results.mapNotNull(ProviderResult<List<FeedThread>>::failure),
            )
        }

    private suspend fun <T> providerCall(
        provider: String,
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): ProviderResult<T> =
        try {
            ProviderResult(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ProviderResult(
                value = fallback,
                failure = ProviderFailure(
                    provider = provider,
                    operation = operation,
                    message = error.message ?: error::class.java.simpleName,
                ),
            )
        }

    private companion object {
        const val DEFAULT_MAX_CONCURRENT_REQUESTS = 6
    }
}

class ThreadRepository(
    private val providers: ProviderRegistry,
) {
    suspend fun load(provider: String, board: String, threadId: Long): ThreadDetails =
        providers.require(provider).thread(board, threadId)
}
