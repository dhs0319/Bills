package com.dhs0319.bills.feature.search.landing

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Refresh
import com.dhs0319.bills.core.designsystem.component.AnimatedLiveBadge
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.model.SearchSuggestion
import com.dhs0319.bills.core.model.SearchSuggestionBadge
import com.dhs0319.bills.feature.search.history.SearchHistoryActions
import com.dhs0319.bills.feature.search.history.SearchHistoryPanel
import com.dhs0319.bills.feature.search.history.rememberSearchHistoryPanelState

@Composable
internal fun SearchLandingPanel(
    state: SearchLandingUiState,
    histories: List<String>,
    onSearch: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRetry: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val primaryListState = rememberLazyListState()
    val discoveryListState = rememberLazyListState()
    val historyState = rememberSearchHistoryPanelState()
    val trendingRows = remember(state.trending) { state.trending.chunked(2) }
    val discoveryRows = remember(state.discovery) { state.discovery.chunked(2) }
    val panelModifier = modifier.fillMaxSize().navigationBarsPadding().imePadding()
    val contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
    val primaryItems: LazyListScope.() -> Unit = {
        trendingItems(state, trendingRows, onSearch, onRetry)
        item(key = "before_history", contentType = "section_gap") { Spacer(Modifier.height(20.dp)) }
        item(key = "history", contentType = "history") {
            SearchHistoryPanel(
                histories = histories,
                state = historyState,
                onSearch = onSearch,
                onDelete = onDeleteHistory
            )
        }
    }

    SearchHistoryActions(state = historyState, onClear = onClearHistory)

    if (isLandscape) {
        Row(modifier = panelModifier) {
            LazyColumn(
                state = primaryListState,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = contentPadding,
                content = primaryItems
            )
            LazyColumn(
                state = discoveryListState,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = contentPadding
            ) {
                discoveryItems(state, discoveryRows, onSearch, onRefreshDiscovery)
            }
        }
    } else {
        LazyColumn(
            state = primaryListState,
            modifier = panelModifier,
            contentPadding = contentPadding
        ) {
            primaryItems()
            item(key = "before_discovery", contentType = "section_gap") { Spacer(Modifier.height(20.dp)) }
            discoveryItems(state, discoveryRows, onSearch, onRefreshDiscovery)
        }
    }
}

private fun LazyListScope.trendingItems(
    state: SearchLandingUiState,
    rows: List<List<SearchSuggestion>>,
    onSearch: (String) -> Unit,
    onRetry: () -> Unit
) {
    item(key = "trending_header", contentType = "header") {
        SearchSectionHeader(title = "热搜榜")
    }
    when {
        state.isLoading && rows.isEmpty() -> placeholderRows("trending", 5)
        rows.isNotEmpty() -> items(
            items = rows,
            key = { "trending_${it.first().keyword}" },
            contentType = { "trending_row" }
        ) { row ->
            SearchSuggestionRow(row) { SearchTrendingWord(it, onSearch) }
        }
        else -> item(key = "trending_state", contentType = "state") {
            StateMessageCard(
                text = state.trendingError ?: "暂无热搜",
                isError = state.trendingError != null,
                actionText = "重试",
                onAction = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun LazyListScope.discoveryItems(
    state: SearchLandingUiState,
    rows: List<List<SearchSuggestion>>,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val isLoading = state.isLoading || state.isRefreshingDiscovery
    item(key = "discovery_header", contentType = "discovery_header") {
        SearchSectionHeader(title = "搜索发现") {
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = MaterialSymbols.Outlined.Refresh,
                        contentDescription = "换一批搜索发现",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    when {
        isLoading && rows.isEmpty() -> placeholderRows("discovery", 6)
        rows.isNotEmpty() -> itemsIndexed(
            items = rows,
            key = { _, row -> "discovery_${row.first().keyword}" },
            contentType = { _, _ -> "discovery_row" }
        ) { index, row ->
            SearchSuggestionRow(
                items = row,
                modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp)
            ) { SearchDiscoveryChip(it, onSearch) }
        }
        else -> item(key = "discovery_state", contentType = "state") {
            StateMessageCard(
                text = state.discoveryError ?: "暂无搜索发现",
                isError = state.discoveryError != null,
                actionText = "换一批",
                onAction = onRefresh,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    if (rows.isNotEmpty() && state.discoveryError != null) {
        item(key = "discovery_error", contentType = "state") {
            StateMessageCard(
                text = state.discoveryError,
                isError = true,
                actionText = "重试",
                onAction = onRefresh,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SearchTrendingWord(suggestion: SearchSuggestion, onSearch: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.Button) { onSearch(suggestion.keyword) }
            .heightIn(min = 40.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = suggestion.title,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        suggestion.badge?.let { SearchHotBadge(it) }
    }
}

@Composable
private fun SearchDiscoveryChip(suggestion: SearchSuggestion, onSearch: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.Button) { onSearch(suggestion.keyword) },
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = suggestion.title,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            suggestion.subtitle?.let { subtitle ->
                Text(
                    text = " · $subtitle",
                    // Reserve title space during measurement, without subcomposing each chip.
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = 0,
                                maxWidth = (constraints.maxWidth - 40.dp.roundToPx()).coerceAtLeast(0)
                            )
                        )
                        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchHotBadge(badge: SearchSuggestionBadge) {
    if (badge == SearchSuggestionBadge.LIVE) {
        AnimatedLiveBadge()
    } else {
        val (label, color, contentColor) = when (badge) {
            SearchSuggestionBadge.NEW -> Triple(
                "新", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer
            )
            SearchSuggestionBadge.HOT -> {
                val colors = MaterialTheme.colorScheme
                val isDark = colors.surface.luminance() < colors.onSurface.luminance()
                Triple(
                    "热",
                    if (isDark) lerp(colors.surfaceContainer, colors.errorContainer, 0.25f) else colors.errorContainer,
                    if (isDark) lerp(colors.onSurfaceVariant, colors.onErrorContainer, 0.5f) else colors.onErrorContainer
                )
            }
            SearchSuggestionBadge.EXCLUSIVE -> Triple(
                "独家", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer
            )
            else -> Triple(
                "大促", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Surface(shape = MaterialTheme.shapes.extraSmall, color = color, contentColor = contentColor) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun <T> SearchSuggestionRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Box(modifier = Modifier.weight(1f)) { content(item) }
        }
        if (items.size == 1) Spacer(modifier = Modifier.weight(1f))
    }
}

private fun LazyListScope.placeholderRows(section: String, count: Int) {
    items(count = count, key = { "${section}_placeholder_$it" }, contentType = { "placeholder_row" }) { index ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = if (index == count - 1) 0.dp else 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier.weight(1f).height(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraSmall)
                )
            }
        }
    }
}
