@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal.feature.thread

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.orbin.minimal.core.data.ThreadRepository
import com.orbin.minimal.core.model.MediaRef
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.core.model.ThreadPost
import com.orbin.minimal.media.InternalMediaViewer
import com.orbin.minimal.media.ThreadMediaSync
import com.orbin.minimal.media.isSafeExternalLink
import com.orbin.minimal.media.rememberThumbnailRequest

private data class PostWithMediaOffset(
    val post: ThreadPost,
    val mediaOffset: Int,
)

private fun flattenThreadMedia(thread: ThreadDetails): Pair<List<MediaRef>, List<PostWithMediaOffset>> {
    val media = ArrayList<MediaRef>()
    val posts = ArrayList<PostWithMediaOffset>(thread.posts.size)
    for (post in thread.posts) {
        posts.add(PostWithMediaOffset(post, media.size))
        media.addAll(post.media)
    }
    return media to posts
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
fun ThreadScreen(
    repository: ThreadRepository,
    provider: String,
    board: String,
    threadId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val mediaSync = remember(context.applicationContext) { ThreadMediaSync(context.applicationContext) }
    val viewModel: ThreadViewModel =
        viewModel(
            key = "$provider/$board/$threadId",
            factory = remember(repository, provider, board, threadId) {
                ThreadViewModel.factory(repository, provider, board, threadId)
            },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedMediaIndex by remember { mutableStateOf<Int?>(null) }
    var pendingExternalUrl by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    val thread = state.thread
    val (threadMedia, postsWithOffset) = remember(thread) {
        if (thread == null) emptyList<MediaRef>() to emptyList()
        else flattenThreadMedia(thread)
    }

    pendingExternalUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingExternalUrl = null },
            title = { Text("Open external link?") },
            text = {
                Text("Only HTTPS links can be opened.\n\n$url")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExternalUrl = null
                        if (isSafeExternalLink(url)) {
                            runCatching { uriHandler.openUri(url) }
                        }
                    },
                ) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { pendingExternalUrl = null }) { Text("Cancel") }
            },
        )
    }

    selectedMediaIndex?.let { index ->
        if (threadMedia.isNotEmpty()) {
            InternalMediaViewer(
                media = threadMedia,
                initialIndex = index,
                onClose = { selectedMediaIndex = null },
            )
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("/$board/") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(onClick = onBack, modifier = Modifier.padding(16.dp)) { Text("Back") }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Text(
                    state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                thread == null -> Text("Thread unavailable", modifier = Modifier.padding(16.dp))
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        item(key = "thread-header", contentType = "thread-header") {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(thread.title)
                                if (threadMedia.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            val count = mediaSync.sync(thread)
                                            Toast.makeText(
                                                context,
                                                "$count media file${if (count == 1) "" else "s"} queued to Downloads/Orbin Minimal/$board/",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        },
                                    ) {
                                        Text("Sync media (${threadMedia.size})")
                                    }
                                }
                            }
                        }
                        items(
                            items = postsWithOffset,
                            key = { it.post.id },
                            contentType = { if (it.post.media.isNotEmpty()) "post-media" else "post" },
                        ) { entry ->
                            val post = entry.post
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text("${post.author ?: "Anonymous"} · #${post.id}")
                                if (post.body.isNotBlank()) Text(post.body, modifier = Modifier.padding(top = 4.dp))
                                post.links.forEach { url ->
                                    TextButton(
                                        onClick = {
                                            if (isSafeExternalLink(url)) {
                                                pendingExternalUrl = url
                                            }
                                        },
                                        modifier = Modifier.padding(top = 4.dp),
                                    ) {
                                        Text("Open external video link")
                                    }
                                    Text(url.take(100))
                                }
                                post.media.forEachIndexed { localIndex, media ->
                                    val thumbRequest = rememberThumbnailRequest(
                                        media.thumbnailUrl ?: media.url,
                                        cellDp = 360.dp,
                                    )
                                    if (thumbRequest != null) {
                                        AsyncImage(
                                            model = thumbRequest,
                                            contentDescription = "Attachment",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 360.dp)
                                                .padding(top = 8.dp)
                                                .clickable {
                                                    selectedMediaIndex = entry.mediaOffset + localIndex
                                                },
                                            contentScale = ContentScale.Fit,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
