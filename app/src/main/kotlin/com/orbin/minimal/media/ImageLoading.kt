package com.orbin.minimal.media

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    /** Soft decode cap for fullscreen viewer pages (px). */
    const val VIEWER_SIZE_PX = 2048

    private const val IMAGE_CACHE_DIR = "image_cache"
    private const val IMAGE_CACHE_BYTES = 64L * 1024L * 1024L

    /** Default list thumb cell on phone — slightly under the old 112.dp. */
    val ListThumbDp: Dp = 96.dp

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
            // Grids/lists must not animate crossfade on fling.
            .crossfade(false)
            .build()

    /** Decode size from density for a [cellDp] thumb (not a fixed 224px). */
    fun thumbSizePx(density: Float, cellDp: Float = ListThumbDp.value): Int =
        (cellDp * density).toInt().coerceIn(96, 320)

    fun thumbnailRequest(context: Context, url: String?, sizePx: Int): ImageRequest? {
        val safe = MediaHosts.filterUrl(url) ?: return null
        return ImageRequest.Builder(context)
            .data(safe)
            .size(sizePx)
            .crossfade(false)
            .build()
    }

    fun viewerRequest(context: Context, url: String?, sizePx: Int = VIEWER_SIZE_PX): ImageRequest? {
        val safe = MediaHosts.filterUrl(url) ?: return null
        val capped = sizePx.coerceIn(1, VIEWER_SIZE_PX)
        return ImageRequest.Builder(context)
            .data(safe)
            .size(capped)
            .crossfade(false)
            .build()
    }
}

@Composable
fun rememberThumbnailRequest(url: String?, cellDp: Dp = ImageLoading.ListThumbDp): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = remember(density.density, cellDp) {
        ImageLoading.thumbSizePx(density.density, cellDp.value)
    }
    return remember(url, sizePx) { ImageLoading.thumbnailRequest(context, url, sizePx) }
}
