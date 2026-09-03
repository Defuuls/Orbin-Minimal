@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.orbin.minimal.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.orbin.minimal.core.model.MediaRef
import kotlin.math.abs

private val ViewerContentColor = Color.White
private val ViewerMutedColor = Color.LightGray

@Composable
fun InternalMediaViewer(
    media: List<MediaRef>,
    initialIndex: Int,
    onClose: () -> Unit,
) {
    if (media.isEmpty()) return

    var index by remember(media, initialIndex) {
        mutableStateOf(initialIndex.coerceIn(media.indices))
    }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var currentImageZoomed by remember(index) { mutableStateOf(false) }
    val videoPositions = remember { mutableStateMapOf<String, Long>() }
    val current = media[index]

    fun previous() {
        if (index > 0) index--
    }

    fun next() {
        if (index < media.lastIndex) index++
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(index, media.size, currentImageZoomed) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!currentImageZoomed) {
                                change.consume()
                                dragDistance += dragAmount
                            }
                        },
                        onDragEnd = {
                            if (!currentImageZoomed && abs(dragDistance) >= 80f) {
                                if (dragDistance < 0f) next() else previous()
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                },
            color = Color.Black,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (current.isVideo()) {
                        VideoPage(
                            media = current,
                            initialPositionMs = videoPositions[current.url] ?: 0L,
                            onPositionChanged = { videoPositions[current.url] = it },
                        )
                    } else {
                        ImagePage(
                            media = current,
                            onZoomChanged = { currentImageZoomed = it },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = ::previous, enabled = index > 0) {
                        Text("Previous", color = if (index > 0) ViewerContentColor else ViewerMutedColor)
                    }

                    Text(
                        text = "${index + 1} / ${media.size}",
                        color = ViewerContentColor,
                    )

                    TextButton(onClick = ::next, enabled = index < media.lastIndex) {
                        Text("Next", color = if (index < media.lastIndex) ViewerContentColor else ViewerMutedColor)
                    }

                    TextButton(onClick = onClose) {
                        Text("Close", color = ViewerContentColor)
                    }
                }
            }
        }
    }
}

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
                text = if (playbackError != null) "Playback error" else if (muted) "Playing muted" else "Sound on",
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
    var scale by remember(media.url) { mutableFloatStateOf(1f) }
    var offset by remember(media.url) { mutableStateOf(Offset.Zero) }
    var loading by remember(media.url) { mutableStateOf(true) }
    var loadFailed by remember(media.url) { mutableStateOf(false) }

    fun updateScale(newScale: Float) {
        scale = newScale.coerceIn(1f, 5f)
        if (scale <= 1f) offset = Offset.Zero
        onZoomChanged(scale > 1.01f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = media.url,
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
                                updateScale(2.5f)
                            }
                        },
                    )
                }
                .pointerInput(media.url, scale) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (nextScale <= 1f) Offset.Zero else offset + pan
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
