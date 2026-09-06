package com.orbin.minimal.media

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.orbin.minimal.core.security.MediaHosts
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

object ImageLoading {
    private const val THUMB_SIZE_PX = 224
    private const val VIEWER_SIZE_PX = 2048
    private const val IMAGE_CACHE_DIR = "image_cache"
    private const val IMAGE_CACHE_BYTES = 64L * 1024L * 1024L

    fun createImageLoader(context: Context, okHttpClient: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context.applicationContext)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.2)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(IMAGE_CACHE_DIR).toOkioPath())
                    .maxSizeBytes(IMAGE_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    fun thumbnailRequest(context: Context, url: String?): ImageRequest? {
        val safe = MediaHosts.filterUrl(url) ?: return null
        return ImageRequest.Builder(context)
            .data(safe)
            .size(THUMB_SIZE_PX)
            .build()
    }

    fun viewerRequest(context: Context, url: String?): ImageRequest? {
        val safe = MediaHosts.filterUrl(url) ?: return null
        return ImageRequest.Builder(context)
            .data(safe)
            .size(VIEWER_SIZE_PX)
            .build()
    }
}
