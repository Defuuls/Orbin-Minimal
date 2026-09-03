@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orbin.minimal.feature.boards.BoardsScreen
import com.orbin.minimal.feature.feed.FeedScreen
import com.orbin.minimal.feature.thread.ThreadScreen

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
