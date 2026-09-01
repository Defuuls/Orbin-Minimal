@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private data class FeedItem(
    val provider: String,
    val board: String,
    val threadId: Long,
    val title: String,
)

private val previewFeed = listOf(
    FeedItem("vichan", "g", 1, "Minimal feed scaffold"),
    FeedItem("lynxchan", "tech", 2, "Provider layer comes next"),
)

@Composable
fun OrbinMinimalApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") {
            FeedScreen(
                onBoards = { navController.navigate("boards") },
                onThread = { item ->
                    navController.navigate("thread/${item.provider}/${item.board}/${item.threadId}")
                },
            )
        }
        composable("boards") {
            BoardsScreen(onBack = { navController.popBackStack() })
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
    onBoards: () -> Unit,
    onThread: (FeedItem) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Orbin Minimal") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onBoards,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text("Boards")
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(previewFeed) { item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text("${item.provider} /${item.board}/") },
                        modifier = Modifier.fillMaxWidth().clickable { onThread(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardsScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Boards") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Followed-board persistence will live in Minimal's own data layer.")
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun ThreadScreen(
    provider: String,
    board: String,
    threadId: Long,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("/$board/") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Thread $threadId via $provider")
            Text(
                "This screen is intentionally Minimal-owned; provider parsing and repository integration are the next porting boundary.",
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text("Back")
            }
        }
    }
}
