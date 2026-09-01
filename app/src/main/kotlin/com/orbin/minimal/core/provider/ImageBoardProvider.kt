package com.orbin.minimal.core.provider

import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.model.ThreadDetails

interface ImageBoardProvider {
    val id: String
    val displayName: String

    suspend fun boards(): List<BoardRef>
    suspend fun catalog(board: String): List<FeedThread>
    suspend fun thread(board: String, threadId: Long): ThreadDetails
}

class ProviderRegistry(
    providers: List<ImageBoardProvider>,
) {
    private val providersById = providers.associateBy(ImageBoardProvider::id)

    fun all(): List<ImageBoardProvider> = providersById.values.toList()

    fun require(id: String): ImageBoardProvider =
        providersById[id] ?: error("Unknown provider: $id")
}
