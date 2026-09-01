@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.compose.AsyncImage
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.ThreadRepository
import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.model.MediaRef
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.media.InternalMediaViewer

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
    val followed = remember(refreshKey) { repository.followed() }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        runCatching { repository.mergedFeed() }
            .onSuccess { feed = it }
            .onFailure { error = it.message ?: "Unable to load feed" }
        loading = false
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
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error.orEmpty(), modifier = Modifier.padding(16.dp))
                followed.isEmpty() -> Text("Follow at least one board to build your feed.", modifier = Modifier.padding(16.dp))
                feed.isEmpty() -> Text("No threads returned from followed boards.", modifier = Modifier.padding(16.dp))
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(feed, key = { "${it.provider}:${it.board}:${it.threadId}" }) { item ->
                        ListItem(
                            headlineContent = { Text(item.title.ifBlank { "Thread ${item.threadId}" }) },
                            supportingContent = { Text("${item.provider} /${item.board}/\n${item.excerpt.take(140)}") },
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
                }
            }
        }
    }
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
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var thread by remember { mutableStateOf<ThreadDetails?>(null) }
    var selectedMedia by remember { mutableStateOf<MediaRef?>(null) }

    LaunchedEffect(provider, board, threadId) {
        runCatching { repository.load(provider, board, threadId) }
            .onSuccess { thread = it }
            .onFailure { error = it.message ?: "Unable to load thread" }
        loading = false
    }

    selectedMedia?.let { media ->
        InternalMediaViewer(
            media = media,
            onClose = { selectedMedia = null },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("/$board/") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(onClick = onBack, modifier = Modifier.padding(16.dp)) { Text("Back") }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error.orEmpty(), modifier = Modifier.padding(16.dp))
                thread == null -> Text("Thread unavailable", modifier = Modifier.padding(16.dp))
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item { Text(thread!!.title, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    items(thread!!.posts, key = { it.id }) { post ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text("${post.author ?: "Anonymous"} · #${post.id}")
                            if (post.body.isNotBlank()) Text(post.body, modifier = Modifier.padding(top = 4.dp))
                            post.media.forEach { media ->
                                AsyncImage(
                                    model = media.thumbnailUrl ?: media.url,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 360.dp)
                                        .padding(top = 8.dp)
                                        .clickable { selectedMedia = media },
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
