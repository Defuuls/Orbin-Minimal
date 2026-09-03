@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal.feature.boards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.ProviderFailure
import com.orbin.minimal.core.model.BoardRef

@Composable
fun BoardsScreen(
    repository: FeedRepository,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var warnings by remember { mutableStateOf(emptyList<ProviderFailure>()) }
    var boards by remember { mutableStateOf(emptyList<BoardRef>()) }
    var followed by remember { mutableStateOf(repository.followed().map { "${it.provider}:${it.board}" }.toSet()) }

    LaunchedEffect(Unit) {
        runCatching { repository.availableBoardsDetailed() }
            .onSuccess { result ->
                boards = result.value
                warnings = result.failures
                if (result.isTotalFailure) error = result.failureSummary()
            }
            .onFailure { error = it.message ?: "Unable to load boards" }
        loading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Boards") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(onClick = onBack, modifier = Modifier.padding(16.dp)) { Text("Back") }
            if (warnings.isNotEmpty() && error == null) {
                Text(
                    text = "Some providers are unavailable: " + warnings.joinToString { it.provider },
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
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
