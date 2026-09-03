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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.orbin.minimal.core.data.ThreadRepository
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.media.InternalMediaViewer
import com.orbin.minimal.media.ThreadMediaSync

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
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var thread by remember { mutableStateOf<ThreadDetails?>(null) }
    var selectedMediaIndex by remember { mutableStateOf<Int?>(null) }
    val uriHandler = LocalUriHandler.current
    val threadMedia = thread?.posts?.flatMap { it.media }.orEmpty()

    LaunchedEffect(provider, board, threadId) {
        loading = true
        error = null
        runCatching { repository.load(provider, board, threadId) }
            .onSuccess { thread = it }
            .onFailure { error = it.message ?: "Unable to load thread" }
        loading = false
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
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                thread == null -> Text("Thread unavailable", modifier = Modifier.padding(16.dp))
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(thread!!.title)
                            if (threadMedia.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        val count = mediaSync.sync(thread!!)
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
                    items(thread!!.posts, key = { it.id }) { post ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text("${post.author ?: "Anonymous"} · #${post.id}")
                            if (post.body.isNotBlank()) Text(post.body, modifier = Modifier.padding(top = 4.dp))
                            post.links.forEach { url ->
                                TextButton(
                                    onClick = { uriHandler.openUri(url) },
                                    modifier = Modifier.padding(top = 4.dp),
                                ) {
                                    Text("Open external video link")
                                }
                                Text(url.take(100))
                            }
                            post.media.forEach { media ->
                                AsyncImage(
                                    model = media.thumbnailUrl ?: media.url,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 360.dp)
                                        .padding(top = 8.dp)
                                        .clickable {
                                            selectedMediaIndex = threadMedia.indexOf(media).takeIf { it >= 0 }
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
