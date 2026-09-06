@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.FeedSort
import com.orbin.minimal.core.data.sortedFor
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.media.ImageLoading

private enum class FeedSite(
    val providerId: String,
    val label: String,
) {
    FOURCHAN("fourchan", "4chan"),
    BBWCHAN("bbwchan", "BBW Chan"),
}

private val CompactBreakpoint = 600.dp

@Composable
fun FeedScreen(
    repository: FeedRepository,
    onBoards: () -> Unit,
    onThread: (FeedThread) -> Unit,
) {
    val viewModel: FeedViewModel =
        viewModel(factory = remember(repository) { FeedViewModel.factory(repository) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedSite by rememberSaveable { mutableStateOf(FeedSite.FOURCHAN) }
    var sortName by rememberSaveable { mutableStateOf(FeedSort.DEFAULT.name) }
    val sort = remember(sortName) {
        runCatching { FeedSort.valueOf(sortName) }.getOrDefault(FeedSort.DEFAULT)
    }

    LaunchedEffect(sort) {
        if (state.sort != sort) viewModel.setSort(sort)
    }

    val selectedFeed = remember(state.feed, selectedSite) {
        state.feed.filter { it.provider == selectedSite.providerId }
    }
    val selectedFollowed = remember(state.followed, selectedSite) {
        state.followed.filter { it.provider == selectedSite.providerId }
    }
    val selectedWarnings = remember(state.warnings, selectedSite) {
        state.warnings.filter { it.provider == selectedSite.providerId }
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
                    onClick = { viewModel.refresh() },
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
                        onClick = {
                            sortName = option.name
                            viewModel.setSort(option)
                        },
                        label = { Text(option.label) },
                    )
                }
            }
            if (selectedWarnings.isNotEmpty() && state.error == null) {
                Text(
                    text = "${selectedSite.label} could not fully refresh.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Text(
                    state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                selectedFollowed.isEmpty() -> Text(
                    "Follow at least one ${selectedSite.label} board to build this feed.",
                    modifier = Modifier.padding(16.dp),
                )
                selectedFeed.isEmpty() -> Text(
                    "No threads returned from followed ${selectedSite.label} boards.",
                    modifier = Modifier.padding(16.dp),
                )
                else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= CompactBreakpoint
                    if (wide) {
                        WideFeedBody(
                            sort = sort,
                            ordered = ordered,
                            groups = groups,
                            selectedSite = selectedSite,
                            onThread = onThread,
                        )
                    } else {
                        NarrowFeedBody(
                            sort = sort,
                            ordered = ordered,
                            groups = groups,
                            selectedSite = selectedSite,
                            onThread = onThread,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NarrowFeedBody(
    sort: FeedSort,
    ordered: List<FeedThread>,
    groups: List<Pair<String, List<FeedThread>>>,
    selectedSite: FeedSite,
    onThread: (FeedThread) -> Unit,
) {
    if (sort == FeedSort.BOARD) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            groups.forEach { (label, threads) ->
                item(key = "header:${selectedSite.providerId}:$label") { BoardHeader(label) }
                items(threads, key = { "${it.provider}:${it.board}:${it.threadId}" }) { item ->
                    FeedRow(item, showBoard = false, onThread = onThread)
                }
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(ordered, key = { "${it.provider}:${it.board}:${it.threadId}" }) { item ->
                FeedRow(item, showBoard = true, onThread = onThread)
            }
        }
    }
}

@Composable
private fun WideFeedBody(
    sort: FeedSort,
    ordered: List<FeedThread>,
    groups: List<Pair<String, List<FeedThread>>>,
    selectedSite: FeedSite,
    onThread: (FeedThread) -> Unit,
) {
    // Two-pane: board groups (or flat list) denser grid on the left; detail hint on the right.
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (sort == FeedSort.BOARD) {
                    groups.forEach { (label, threads) ->
                        item(key = "header:${selectedSite.providerId}:$label", span = {
                            GridItemSpan(maxLineSpan)
                        }) { BoardHeader(label) }
                        items(threads, key = { "${it.provider}:${it.board}:${it.threadId}" }) { item ->
                            FeedGridCard(item, showBoard = false, onThread = onThread)
                        }
                    }
                } else {
                    items(ordered, key = { "${it.provider}:${it.board}:${it.threadId}" }) { item ->
                        FeedGridCard(item, showBoard = true, onThread = onThread)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Select a thread to open it.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    val context = LocalContext.current
    val thumb = remember(item.media?.thumbnailUrl) {
        ImageLoading.thumbnailRequest(context, item.media?.thumbnailUrl)
    }
    ListItem(
        headlineContent = { Text(item.title.ifBlank { "Thread ${item.threadId}" }) },
        supportingContent = {
            val excerpt = item.excerpt.take(140)
            Text(if (showBoard) "/${item.board}/\n$excerpt" else excerpt)
        },
        leadingContent = thumb?.let { request ->
            {
                AsyncImage(
                    model = request,
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
private fun FeedGridCard(
    item: FeedThread,
    showBoard: Boolean,
    onThread: (FeedThread) -> Unit,
) {
    val context = LocalContext.current
    val thumb = remember(item.media?.thumbnailUrl) {
        ImageLoading.thumbnailRequest(context, item.media?.thumbnailUrl)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
            .clickable { onThread(item) }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentScale = ContentScale.Crop,
            )
        }
        Text(item.title.ifBlank { "Thread ${item.threadId}" }, maxLines = 2)
        Text(
            if (showBoard) "/${item.board}/ · ${item.excerpt.take(80)}" else item.excerpt.take(80),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
        )
    }
}
