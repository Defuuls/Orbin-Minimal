@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.security.BoardSlugs
import com.orbin.minimal.feature.boards.BoardsScreen
import com.orbin.minimal.feature.feed.FeedScreen
import com.orbin.minimal.feature.thread.ThreadScreen

@Composable
fun OrbinMinimalApp() {
    val context = LocalContext.current
    val application = context.applicationContext as? OrbinMinimalApplication
    val graph = remember(context.applicationContext) {
        application?.graph ?: AppGraph(context.applicationContext)
    }
    val navController = rememberNavController()

    val onBoards: () -> Unit = remember(navController) {
        {
            navController.navigate("boards")
            Unit
        }
    }
    val onThread: (FeedThread) -> Unit = remember(navController) {
        { item ->
            val board = BoardSlugs.sanitizeOrNull(item.board)
            if (board != null) {
                navController.navigate("thread/${item.provider}/$board/${item.threadId}")
            }
        }
    }
    val onBack: () -> Unit = remember(navController) {
        {
            navController.popBackStack()
            Unit
        }
    }

    NavHost(navController = navController, startDestination = "feed") {
        composable("feed") {
            FeedScreen(
                repository = graph.feedRepository,
                onBoards = onBoards,
                onThread = onThread,
            )
        }
        composable("boards") {
            BoardsScreen(
                repository = graph.feedRepository,
                onBack = onBack,
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
            val boardArg = entry.arguments?.getString("board").orEmpty()
            val safeBoard = BoardSlugs.sanitizeOrNull(boardArg)
            if (safeBoard == null) {
                Text("Invalid board")
                return@composable
            }
            ThreadScreen(
                repository = graph.threadRepository,
                provider = entry.arguments?.getString("provider").orEmpty(),
                board = safeBoard,
                threadId = entry.arguments?.getLong("threadId") ?: 0L,
                onBack = onBack,
            )
        }
    }
}
