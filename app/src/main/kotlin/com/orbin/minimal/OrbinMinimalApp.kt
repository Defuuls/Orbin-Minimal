@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.compose.AsyncImage
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.FeedSort
import com.orbin.minimal.core.data.sortedFor
import com.orbin.minimal.core.data.ThreadRepository
import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.media.InternalMediaViewer
import com.orbin.minimal.media.ThreadMediaSync

@Composable
fun OrbinMinimalApp() {
    val context = LocalContext.current
    val graph = remember(context.applicationContext) { AppGraph(context.applicationContext) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") {
            FeedScreen(
                repository = graph.feedRepository,
                onBoards = { navController.navigate("boards") },
                onThread = { item ->
                    navController.navigate("thread/${item.provider}/${item.board}/${item.threadId}")
                },
            )
        }
        composable("boards") {
            BoardsScreen(
                repository = graph.feedRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "thread/{provider}/{board}/{threadId}",
            arguments = listOf(
                navArgument("provider") { type = NavType.StringType },
                navArgument("board") { type = NavType.StringType },
                navArgument("threadId") { type = NavType.LongType },
            ),
        ) { entry ->
            ThreadScreen(
                repository = graph.threadRepository,
                provider = entry.arguments?.getString("provider").orEmpty(),
                board = entry.arguments?.getString("board").orEmpty(),
                threadId = entry.arguments?.getLong("threadId") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun FeedScreen(
    repository: FeedRepository,
    onBoards: () -> Unit,
    onThread: (FeedThread) -> Unit,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var feed by remember { mutableStateOf(emptyList<FeedThread>()) }
    var sort by remember { mutableStateOf(FeedSort.DEFAULT) }
    val followed = remember(refreshKey) { repository.followed() }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        runCatching { repository.mergedFeed() }
            .onSuccess { feed = it }
            .onFailure { error = it.message ?: "Unable to load feed" }
        loading = false
    }

    // The repository applies the default order; changing mode is a local
    // reorder of what is already loaded, so it must not trigger a refetch.
    val ordered = remember(feed, sort) { feed.sortedFor(sort) }
    // Board sort renders as labelled groups; the grouping is derived once per
    // reorder rather than tracked while items compose.
    val groups = remember(ordered, sort) {
        if (sort == FeedSort.BOARD) {
            ordered.groupBy { "${it.provider} /${it.board}/" }.toList()
        } else {
            emptyList()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Orbin Minimal") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Button(onClick = onBoards) { Text("Boards") }
                TextButton(
                    onClick = { refreshKey++ },
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) { Text("Refresh") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Sort", modifier = Modifier.align(Alignment.CenterVertically))
                FeedSort.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(option.label) },
                    )
                }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error.orEmpty(), modifier = Modifier.padding(16.dp))
                followed.isEmpty() -> Text("Follow at least one board to build your feed.", modifier = Modifier.padding(16.dp))
                feed.isEmpty() -> Text("No threads returned from followed boards.", modifier = Modifier.padding(16.dp))
                sort == FeedSort.BOARD -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    groups.forEach { (label, threads) ->
                        item(key = "header:$label") { BoardHeader(label) }
                        items(threads, key = { "${it.provider}:${it.board}:${it.threadId}" }) { item ->
                            FeedRow(item, showBoard = false, onThread = onThread)
                        }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(ordered, key = { "${it.provider}:${it.board}:${it.threadId}" }) { item ->
                        FeedRow(item, showBoard = true, onThread = onThread)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardHeader(label: String) {
    Column {
        HorizontalDivider()
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun FeedRow(
    item: FeedThread,
    showBoard: Boolean,
    onThread: (FeedThread) -> Unit,
) {
    ListItem(
        headlineContent = { Text(item.title.ifBlank { "Thread ${item.threadId}" }) },
        supportingContent = {
            // Under a board header the board line would just repeat the header.
            val excerpt = item.excerpt.take(140)
            Text(if (showBoard) "${item.provider} /${item.board}/\n$excerpt" else excerpt)
        },
        leadingContent = item.media?.thumbnailUrl?.let { thumbnail ->
            {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(112.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        },
        modifier = Modifier.fillMaxWidth().clickable { onThread(item) },
    )
}

@Composable
private fun BoardsScreen(
    repository: FeedRepository,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var boards by remember { mutableStateOf(emptyList<BoardRef>()) }
    var followed by remember { mutableStateOf(repository.followed().map { "${it.provider}:${it.board}" }.toSet()) }

    LaunchedEffect(Unit) {
        runCatching { repository.availableBoards() }
            .onSuccess { boards = it }
            .onFailure { error = it.message ?: "Unable to load boards" }
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Boards") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(onClick = onBack, modifier = Modifier.padding(16.dp)) { Text("Back") }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error.orEmpty(), modifier = Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(boards, key = { "${it.provider}:${it.board}" }) { board ->
                        val key = "${board.provider}:${board.board}"
                        ListItem(
                            headlineContent = { Text("/${board.board}/ — ${board.title}") },
                            supportingContent = { Text(board.provider) },
                            trailingContent = {
                                Checkbox(
                                    checked = key in followed,
                                    onCheckedChange = {
                                        repository.toggle(board)
                                        followed = repository.followed().map { "${it.provider}:${it.board}" }.toSet()
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadScreen(
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
                error != null -> Text(error.orEmpty(), modifier = Modifier.padding(16.dp))
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
