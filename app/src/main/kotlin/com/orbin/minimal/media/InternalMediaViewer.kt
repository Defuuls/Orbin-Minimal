package com.orbin.minimal.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.orbin.minimal.core.model.MediaRef
import com.orbin.minimal.core.security.MediaHosts
import kotlinx.coroutines.launch

private val ViewerContentColor = Color.White
private val ViewerMutedColor = Color.LightGray

@UnstableApi
@Composable
fun InternalMediaViewer(
    media: List<MediaRef>,
    initialIndex: Int,
    onClose: () -> Unit,
) {
    if (media.isEmpty()) return

    val startIndex = initialIndex.coerceIn(media.indices)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { media.size })
    val scope = rememberCoroutineScope()
    val videoPositions = remember { mutableStateMapOf<String, Long>() }
    var currentImageZoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !currentImageZoomed,
                    ) { page ->
                        val item = media[page]
                        if (item.isVideo()) {
                            val safeUrl = MediaHosts.filterUrl(item.url)
                            if (safeUrl == null) {
                                Text("Blocked media host", color = ViewerMutedColor)
                            } else {
                                VideoPage(
                                    media = item.copy(url = safeUrl),
                                    initialPositionMs = videoPositions[safeUrl] ?: 0L,
                                    onPositionChanged = { videoPositions[safeUrl] = it },
                                )
                            }
                        } else {
                            ImagePage(
                                media = item,
                                onZoomChanged = { zoomed ->
                                    if (page == pagerState.currentPage) {
                                        currentImageZoomed = zoomed
                                    }
                                },
                            )
                        }
                    }
                }

                LaunchedEffect(pagerState.currentPage) {
                    currentImageZoomed = false
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                    ) {
                        Text(
                            "Previous",
                            color = if (pagerState.currentPage > 0) ViewerContentColor else ViewerMutedColor,
                        )
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${media.size}",
                        color = ViewerContentColor,
                    )

                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage + 1).coerceAtMost(media.lastIndex),
                                )
                            }
                        },
                        enabled = pagerState.currentPage < media.lastIndex,
                    ) {
                        Text(
                            "Next",
                            color = if (pagerState.currentPage < media.lastIndex) {
                                ViewerContentColor
                            } else {
                                ViewerMutedColor
                            },
                        )
                    }

                    TextButton(onClick = onClose) {
                        Text("Close", color = ViewerContentColor)
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun VideoPage(
    media: MediaRef,
    initialPositionMs: Long,
    onPositionChanged: (Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var muted by remember(media.url) { mutableStateOf(true) }
    var expanded by remember(media.url) { mutableStateOf(false) }
    var buffering by remember(media.url) { mutableStateOf(true) }
    var playbackError by remember(media.url) { mutableStateOf<PlaybackException?>(null) }

    val player = remember(media.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(media.url))
            if (initialPositionMs > 0L) seekTo(initialPositionMs)
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) playbackError = null
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error
                buffering = false
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> if (playbackError == null) player.play()
                else -> Unit
            }
        }

        player.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            onPositionChanged(player.currentPosition.coerceAtLeast(0L))
            player.removeListener(playerListener)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            player.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        controllerAutoShow = true
                        controllerShowTimeoutMs = 2500
                        keepScreenOn = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    }
                },
                update = { it.player = player },
                modifier = if (expanded) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 720.dp)
                },
            )

            if (buffering && playbackError == null) {
                CircularProgressIndicator()
            }

            playbackError?.let {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(20.dp),
                ) {
                    Text("Video could not be played.", color = ViewerContentColor)
                    Button(
                        onClick = {
                            playbackError = null
                            buffering = true
                            player.prepare()
                            player.play()
                        },
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    muted = !muted
                    player.volume = if (muted) 0f else 1f
                },
            ) {
                Text(if (muted) "Unmute" else "Mute", color = ViewerContentColor)
            }

            Text(
                text = if (playbackError != null) {
                    "Playback error"
                } else if (muted) {
                    "Playing muted"
                } else {
                    "Sound on"
                },
                color = if (playbackError != null) ViewerMutedColor else ViewerContentColor,
            )

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Fit" else "Expand", color = ViewerContentColor)
            }
        }
    }
}

@Composable
private fun ImagePage(
    media: MediaRef,
    onZoomChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val request = remember(media.url) { ImageLoading.viewerRequest(context, media.url) }
    var scale by remember(media.url) { mutableFloatStateOf(1f) }
    var offset by remember(media.url) { mutableStateOf(Offset.Zero) }
    var loading by remember(media.url) { mutableStateOf(true) }
    var loadFailed by remember(media.url) { mutableStateOf(false) }

    fun updateScale(newScale: Float) {
        // Lighter zoom: cap at 3x instead of 5x, snap pan when near 1x.
        scale = newScale.coerceIn(1f, 3f)
        if (scale <= 1.05f) {
            scale = 1f
            offset = Offset.Zero
        }
        onZoomChanged(scale > 1.01f)
    }

    if (request == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Blocked media host", color = ViewerMutedColor)
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = "Thread media",
            onLoading = {
                loading = true
                loadFailed = false
            },
            onSuccess = {
                loading = false
                loadFailed = false
            },
            onError = {
                loading = false
                loadFailed = true
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(media.url) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.01f) {
                                offset = Offset.Zero
                                updateScale(1f)
                            } else {
                                updateScale(2f)
                            }
                        },
                    )
                }
                .pointerInput(media.url, scale) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 3f)
                        offset = if (nextScale <= 1.05f) Offset.Zero else offset + pan
                        updateScale(nextScale)
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit,
        )

        if (loading) {
            CircularProgressIndicator()
        }

        if (loadFailed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(20.dp),
            ) {
                Text("Image could not be loaded.", color = ViewerContentColor)
                Text("Swipe to another item or close the viewer.", color = ViewerMutedColor)
            }
        }
    }
}

private fun MediaRef.isVideo(): Boolean {
    if (mimeType?.startsWith("video/", ignoreCase = true) == true) return true

    val normalizedUrl = url.substringBefore('?').substringBefore('#').lowercase()
    return normalizedUrl.endsWith(".mp4") ||
        normalizedUrl.endsWith(".webm") ||
        normalizedUrl.endsWith(".m4v") ||
        normalizedUrl.endsWith(".mov") ||
        normalizedUrl.endsWith(".3gp")
}
