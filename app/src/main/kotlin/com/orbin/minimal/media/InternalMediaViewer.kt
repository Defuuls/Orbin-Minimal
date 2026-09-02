package com.orbin.minimal.media

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.orbin.minimal.core.model.MediaRef
import kotlin.math.abs

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
                .pointerInput(index, media.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragDistance += dragAmount
                        },
                        onDragEnd = {
                            if (abs(dragDistance) >= 80f) {
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
                        VideoPage(media = current)
                    } else {
                        ImagePage(media = current)
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
                        Text("Previous")
                    }

                    Text(
                        text = "${index + 1} / ${media.size}",
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    TextButton(onClick = ::next, enabled = index < media.lastIndex) {
                        Text("Next")
                    }

                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPage(media: MediaRef) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var muted by remember(media.url) { mutableStateOf(true) }
    var fullscreen by remember(media.url) { mutableStateOf(false) }

    val player = remember(media.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(media.url))
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                modifier = if (fullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 720.dp)
                },
            )
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
                Text(if (muted) "Unmute" else "Mute")
            }

            Text(
                text = if (muted) "Playing muted" else "Sound on",
                color = MaterialTheme.colorScheme.onSurface,
            )

            TextButton(onClick = { fullscreen = !fullscreen }) {
                Text(if (fullscreen) "Exit fullscreen" else "Fullscreen")
            }
        }
    }
}

@Composable
private fun ImagePage(media: MediaRef) {
    AsyncImage(
        model = media.url,
        contentDescription = "Thread media",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
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
