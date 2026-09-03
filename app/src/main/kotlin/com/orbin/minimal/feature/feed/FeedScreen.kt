@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.FeedSort
import com.orbin.minimal.core.data.ProviderFailure
import com.orbin.minimal.core.data.sortedFor
import com.orbin.minimal.core.model.FeedThread

private enum class FeedSite(
    val providerId: String,
    val label: String,
) {
    FOURCHAN("fourchan", "4chan"),
    BBWCHAN("bbwchan", "BBW Chan"),
}

@Composable
fun FeedScreen(
    repository: FeedRepository,
    onBoards: () -> Unit,
    onThread: (FeedThread) -> Unit,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var warnings by remember { mutableStateOf(emptyList<ProviderFailure>()) }
    var feed by remember { mutableStateOf(emptyList<FeedThread>()) }
    var sort by remember { mutableStateOf(FeedSort.DEFAULT) }
    var selectedSite by remember { mutableStateOf(FeedSite.FOURCHAN) }
    val followed = remember(refreshKey) { repository.followed() }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        warnings = emptyList()
        runCatching { repository.mergedFeedDetailed() }
            .onSuccess { result ->
                feed = result.value
                warnings = result.failures
                if (result.isTotalFailure) error = result.failureSummary()
            }
            .onFailure { error = it.message ?: "Unable to load feed" }
        loading = false
    }

    val selectedFeed = remember(feed, selectedSite) {
        feed.filter { it.provider == selectedSite.providerId }
    }
    val selectedFollowed = remember(followed, selectedSite) {
        followed.filter { it.provider == selectedSite.providerId }
    }
    val selectedWarnings = remember(warnings, selectedSite) {
        warnings.filter { it.provider == selectedSite.providerId }
    }
    val ordered = remember(selectedFeed, sort) { selectedFeed.sortedFor(sort) }
    val groups = remember(ordered, sort) {
        if (sort == FeedSort.BOARD) {
            ordered.groupBy { "/${it.board}/" }.toList()
        } else {
            emptyList()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Orbin Minimal") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SiteSelector(
                selectedSite = selectedSite,
                onSiteSelected = { selectedSite = it },
            )
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
            if (selectedWarnings.isNotEmpty() && error == null) {
                Text(
                    text = "${selectedSite.label} could not fully refresh.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                selectedFollowed.isEmpty() -> Text(
                    "Follow at least one ${selectedSite.label} board to build this feed.",
                    modifier = Modifier.padding(16.dp),
                )
                selectedFeed.isEmpty() -> Text(
                    "No threads returned from followed ${selectedSite.label} boards.",
                    modifier = Modifier.padding(16.dp),
                )
                sort == FeedSort.BOARD -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    groups.forEach { (label, threads) ->
                        item(key = "header:${selectedSite.providerId}:$label") { BoardHeader(label) }
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
private fun SiteSelector(
    selectedSite: FeedSite,
    onSiteSelected: (FeedSite) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Select a site", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                    .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FeedSite.entries.forEach { site ->
                FilterChip(
                    selected = selectedSite == site,
                    onClick = { onSiteSelected(site) },
                    label = { Text(site.label, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
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
            val excerpt = item.excerpt.take(140)
            Text(if (showBoard) "/${item.board}/\n$excerpt" else excerpt)
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
