package com.orbin.minimal.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun InternalMediaViewer(
    media: MediaRef,
    onClose: () -> Unit,
) {
    if (media.isVideo()) {
        VideoViewer(media = media, onClose = onClose)
    } else {
        ImageViewer(media = media, onClose = onClose)
    }
}

@Composable
private fun VideoViewer(
    media: MediaRef,
    onClose: () -> Unit,
) {
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

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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

                    TextButton(onClick = { fullscreen = !fullscreen }) {
                        Text(if (fullscreen) "Exit fullscreen" else "Fullscreen")
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
private fun ImageViewer(
    media: MediaRef,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        },
        text = {
            AsyncImage(
                model = media.url,
                contentDescription = "Thread media",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        },
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
