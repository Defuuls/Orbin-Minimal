package com.orbin.minimal.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

    val player = remember(media.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(media.url))
            volume = 0f
            playWhenReady = false
            prepare()
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                player.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Media") },
        text = {
            Column {
                AndroidView(
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 520.dp),
                )
                Text(
                    text = if (muted) "Muted" else "Sound on",
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    muted = !muted
                    player.volume = if (muted) 0f else 1f
                },
            ) {
                Text(if (muted) "Unmute" else "Mute")
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        },
    )
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
