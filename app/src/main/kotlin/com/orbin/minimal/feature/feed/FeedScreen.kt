@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.orbin.minimal.feature.feed

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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.orbin.minimal.OrbinMinimalApplication
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.FeedSort
import com.orbin.minimal.core.data.sortedFor
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.media.ImageLoading
import com.orbin.minimal.media.rememberThumbnailRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private enum class FeedSite(
    val providerId: String,
    val label: String,
) {
    FOURCHAN("fourchan", "4chan"),
    BBWCHAN("bbwchan", "BBW Chan"),
}

private val CompactBreakpoint = 600.dp

/**
 * Soft cap on rendered feed rows (headers + threads). Catalog merges can be huge; beyond this
 * count items are omitted from composition. Compose Foundation 1.12 (BOM 2026.08) has no public
 * LazyColumn/LazyVerticalGrid `beyondBoundsItemCount`; windowing is this soft cap + contentType
 * + Coil prefetch. See docs/ARCHITECTURE.md § Feed windowing. Raise only with care for
 * scroll/memory cost.
 */
internal const val MAX_RENDERED_FEED_ENTRIES = 400

internal sealed interface FeedListEntry {
    data class Header(val label: String, val site: String) : FeedListEntry
    data class Row(val thread: FeedThread, val showBoard: Boolean) : FeedListEntry
}

internal fun buildFeedEntries(
    sort: FeedSort,
    ordered: List<FeedThread>,
    groups: List<Pair<String, List<FeedThread>>>,
    selectedSiteId: String,
    maxItems: Int = MAX_RENDERED_FEED_ENTRIES,
): List<FeedListEntry> {
    val raw: List<FeedListEntry> =
        if (sort == FeedSort.BOARD) {
            buildList {
                for ((label, threads) in groups) {
                    add(FeedListEntry.Header(label, selectedSiteId))
                    threads.forEach { add(FeedListEntry.Row(it, showBoard = false)) }
                }
            }
        } else {
            ordered.map { FeedListEntry.Row(it, showBoard = true) }
        }
    return if (raw.size <= maxItems) raw else raw.take(maxItems)
}

private fun FeedListEntry.key(): String =
    when (this) {
        is FeedListEntry.Header -> "header:$site:$label"
        is FeedListEntry.Row -> "${thread.provider}:${thread.board}:${thread.threadId}"
    }

private fun FeedListEntry.contentType(): String =
    when (this) {
        is FeedListEntry.Header -> "board-header"
        is FeedListEntry.Row -> if (thread.media?.thumbnailUrl != null) "feed-row-thumb" else "feed-row"
    }

@Composable
fun FeedScreen(
    repository: FeedRepository,
    onBoards: () -> Unit,
    onThread: (FeedThread) -> Unit,
) {
    val context = LocalContext.current
    val preloader = remember(context.applicationContext) {
        (context.applicationContext as? OrbinMinimalApplication)?.graph?.imagePreloader
    }
    val viewModel: FeedViewModel =
        viewModel(factory = remember(repository, preloader) { FeedViewModel.factory(repository, preloader) })
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
    val entries = remember(sort, ordered, groups, selectedSite) {
        buildFeedEntries(sort, ordered, groups, selectedSite.providerId)
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onBoards) { Text("Boards") }
                TextButton(onClick = { viewModel.refresh() }) { Text("Refresh") }
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
                            entries = entries,
                            onThread = onThread,
                            onPrefetch = viewModel::prefetchFeedThumbs,
                        )
                    } else {
                        NarrowFeedBody(
                            entries = entries,
                            onThread = onThread,
                            onPrefetch = viewModel::prefetchFeedThumbs,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedNearViewportPrefetch(
    entries: List<FeedListEntry>,
    visibleRange: () -> IntRange?,
    onPrefetch: (List<String>) -> Unit,
) {
    LaunchedEffect(entries, onPrefetch) {
        snapshotFlow { visibleRange() }
            .map { range ->
                if (range == null || entries.isEmpty()) emptyList()
                else {
                    val from = (range.first - 2).coerceAtLeast(0)
                    val to = (range.last + 5).coerceAtMost(entries.lastIndex)
                    if (from > to) emptyList()
                    else {
                        entries.subList(from, to + 1).mapNotNull { entry ->
                            (entry as? FeedListEntry.Row)?.thread?.media?.thumbnailUrl
                        }
                    }
                }
            }
            .distinctUntilChanged()
            .collect { urls -> if (urls.isNotEmpty()) onPrefetch(urls) }
    }
}

@Composable
private fun NarrowFeedBody(
    entries: List<FeedListEntry>,
    onThread: (FeedThread) -> Unit,
    onPrefetch: (List<String>) -> Unit,
) {
    val listState = rememberLazyListState()
    FeedNearViewportPrefetch(
        entries = entries,
        visibleRange = {
            val info = listState.layoutInfo.visibleItemsInfo
            if (info.isEmpty()) null else info.first().index..info.last().index
        },
        onPrefetch = onPrefetch,
    )
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp),
        // Modest windowing; default is higher and keeps more off-screen rows hot.
    ) {
        items(
            items = entries,
            key = { it.key() },
            contentType = { it.contentType() },
        ) { entry ->
            when (entry) {
                is FeedListEntry.Header -> BoardHeader(entry.label)
                is FeedListEntry.Row -> FeedRow(entry.thread, entry.showBoard, onThread)
            }
        }
    }
}

@Composable
private fun WideFeedBody(
    entries: List<FeedListEntry>,
    onThread: (FeedThread) -> Unit,
    onPrefetch: (List<String>) -> Unit,
) {
    val gridState = rememberLazyGridState()
    FeedNearViewportPrefetch(
        entries = entries,
        visibleRange = {
            val info = gridState.layoutInfo.visibleItemsInfo
            if (info.isEmpty()) null else info.first().index..info.last().index
        },
        onPrefetch = onPrefetch,
    )
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = entries,
                    key = { it.key() },
                    contentType = { it.contentType() },
                    span = { entry ->
                        if (entry is FeedListEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                    },
                ) { entry ->
                    when (entry) {
                        is FeedListEntry.Header -> BoardHeader(entry.label)
                        is FeedListEntry.Row -> FeedGridCard(entry.thread, entry.showBoard, onThread)
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
    // Single row of chips — no extra filled Surface/background layer (reduces chrome overdraw).
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Site",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 4.dp),
        )
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
    val thumb = rememberThumbnailRequest(item.media?.thumbnailUrl, ImageLoading.ListThumbDp)
    ListItem(
        headlineContent = { Text(item.title.ifBlank { "Thread ${item.threadId}" }) },
        supportingContent = {
            val excerpt = item.excerpt.take(140)
            Text(if (showBoard) "/${item.board}/\n$excerpt" else excerpt)
        },
        leadingContent = {
            if (thumb != null) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    modifier = Modifier.size(ImageLoading.ListThumbDp),
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
    val thumb = rememberThumbnailRequest(item.media?.thumbnailUrl, cellDp = 140.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onThread(item) }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (thumb != null) {
            AsyncImage(
                model = thumb,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(6.dp)),
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
