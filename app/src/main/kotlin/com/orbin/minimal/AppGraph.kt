package com.orbin.minimal

import android.content.Context
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.FollowedBoardStore
import com.orbin.minimal.core.data.ThreadRepository
import com.orbin.minimal.core.network.HttpJsonClient
import com.orbin.minimal.core.provider.ProviderRegistry
import com.orbin.minimal.provider.LynxChanProvider
import com.orbin.minimal.provider.VichanProvider

class AppGraph(context: Context) {
    private val http = HttpJsonClient()
    private val providers = ProviderRegistry(
        listOf(
            VichanProvider(http),
            LynxChanProvider(http),
        ),
    )
    private val followedBoards = FollowedBoardStore(context.applicationContext)

    val feedRepository = FeedRepository(providers, followedBoards)
    val threadRepository = ThreadRepository(providers)
}
