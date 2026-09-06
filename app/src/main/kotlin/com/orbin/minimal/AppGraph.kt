package com.orbin.minimal

import android.content.Context
import coil3.ImageLoader
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.FollowedBoardStore
import com.orbin.minimal.core.data.ThreadRepository
import com.orbin.minimal.core.network.HttpJsonClient
import com.orbin.minimal.core.network.NetworkFactory
import com.orbin.minimal.core.provider.ProviderRegistry
import com.orbin.minimal.media.ImageLoading
import com.orbin.minimal.media.ImagePreloader
import com.orbin.minimal.provider.LynxChanProvider
import com.orbin.minimal.provider.VichanProvider
import okhttp3.OkHttpClient

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    val okHttpClient: OkHttpClient = NetworkFactory.sharedOkHttp(appContext)
    private val http = HttpJsonClient(okHttpClient)
    val imageLoader: ImageLoader = ImageLoading.createImageLoader(appContext, okHttpClient)
    val imagePreloader: ImagePreloader = ImagePreloader(appContext, imageLoader)

    private val providers = ProviderRegistry(
        listOf(
            VichanProvider(http),
            LynxChanProvider(http),
        ),
    )
    private val followedBoards = FollowedBoardStore(appContext)

    val feedRepository = FeedRepository(providers, followedBoards)
    val threadRepository = ThreadRepository(providers)
}
