package com.orbin.minimal.media

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.orbin.minimal.core.security.MediaHosts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Prefetches images into Coil caches.
 *
 * Callers must pass a lifecycle-scoped [CoroutineScope] (typically `viewModelScope`). Work is
 * cancelled when that scope is cancelled (screen leave / ViewModel clear).
 */
class ImagePreloader(
    private val context: Context,
    private val imageLoader: ImageLoader,
) {
    fun prefetch(
        url: String,
        scope: CoroutineScope,
        maxDimensionPx: Int = DEFAULT_PREFETCH_MAX_PX,
    ) {
        val safe = MediaHosts.filterUrl(url) ?: return
        scope.launch {
            imageLoader.execute(buildRequest(safe, maxDimensionPx))
        }
    }

    fun prefetchBatch(
        urls: List<String>,
        scope: CoroutineScope,
        maxDimensionPx: Int = DEFAULT_PREFETCH_MAX_PX,
    ) {
        val distinct = urls.mapNotNull(MediaHosts::filterUrl).distinct()
        if (distinct.isEmpty()) return
        scope.launch {
            distinct.forEach { url ->
                imageLoader.execute(buildRequest(url, maxDimensionPx))
            }
        }
    }

    private fun buildRequest(url: String, maxDimensionPx: Int): ImageRequest {
        val builder =
            ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
        if (maxDimensionPx > 0) {
            builder.size(Size(maxDimensionPx, maxDimensionPx))
        }
        return builder.build()
    }

    private companion object {
        const val DEFAULT_PREFETCH_MAX_PX = 720
    }
}
