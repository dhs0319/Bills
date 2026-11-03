package com.dhs0319.bills.feature.history.watchlater

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.common.compactCount
import com.dhs0319.bills.core.designsystem.component.AdaptiveMediaGrid
import com.dhs0319.bills.core.designsystem.component.AdaptivePagerSlidingTabRow
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCard
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCardSkeleton
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.UploaderBadge
import com.dhs0319.bills.core.designsystem.component.rememberHorizontalVideoCardColumnCount
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.WatchLaterItem
import com.dhs0319.bills.core.model.WatchLaterTab
import com.dhs0319.bills.feature.history.component.LOAD_MORE_SKELETON_COUNT
import com.dhs0319.bills.feature.history.component.formatVideoDuration

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WatchLaterScreen(
    onBack: () -> Unit,
    onOpenVideo: (VideoTarget) -> Unit,
    viewModel: WatchLaterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeActionMessage()
        }
    }
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val columns = rememberHorizontalVideoCardColumnCount()
    val tabs = remember { WatchLaterTab.entries.map { it.title } }
    val onSelectSort = remember(viewModel) { viewModel::selectSort }
    val pagerState = rememberPagerState(
        initialPage = state.tab.ordinal,
        pageCount = { WatchLaterTab.entries.size }
    )
    val gridStates = key(state.asc) {
        WatchLaterTab.entries.map { rememberLazyStaggeredGridState() }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.selectTab(WatchLaterTab.entries[page])
        }
    }

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = { Text("稍后再看") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    WatchLaterSortDropdown(
                        asc = state.asc,
                        onSelect = onSelectSort,
                        modifier = Modifier.padding(end = 4.dp),
                        enabled = !state.isLoading && !state.isRefreshing &&
                            !state.isLoadingMore && !pagerState.isScrollInProgress
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AdaptivePagerSlidingTabRow(
                tabs = tabs,
                pagerState = pagerState
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                WatchLaterPage(
                    tab = WatchLaterTab.entries[page],
                    pagerState = pagerState,
                    gridState = gridStates[page],
                    columns = columns,
                    isTablet = isTablet,
                    viewModel = viewModel,
                    onOpenVideo = onOpenVideo
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchLaterPage(
    tab: WatchLaterTab,
    pagerState: PagerState,
    gridState: LazyStaggeredGridState,
    columns: Int,
    isTablet: Boolean,
    viewModel: WatchLaterViewModel,
    onOpenVideo: (VideoTarget) -> Unit
) {
    val pageStates = remember(viewModel, tab) { viewModel.observeTab(tab) }
    val pageState by pageStates.collectAsStateWithLifecycle(
        initialValue = viewModel.previewState(tab)
    )
    val isActivePage by remember(pagerState, tab) {
        derivedStateOf {
            !pagerState.isScrollInProgress && tab.ordinal == pagerState.settledPage
        }
    }
    val onRefresh = remember(viewModel, pagerState, tab) {
        {
            if (!pagerState.isScrollInProgress && pagerState.settledPage == tab.ordinal &&
                viewModel.uiState.value.tab == tab
            ) {
                viewModel.refresh()
            }
        }
    }
    val onLoadMore = remember(viewModel) { viewModel::loadMore }
    val onRemoveItem = remember(viewModel) { viewModel::removeItem }

    AdaptiveMediaGrid(
        items = pageState.items,
        isRefreshing = pageState.isRefreshing || pageState.isLoading,
        isLoadingMore = pageState.isLoadingMore,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        columns = columns,
        loadMoreEnabled = pageState.canLoadMore &&
            pageState.errorMessage == null && isActivePage,
        prefetchScreens = 1,
        loadMoreError = pageState.errorMessage
            ?.takeIf { pageState.errorOnLoadMore }
            ?.ifBlank { "加载稍后再看失败" },
        onRetryLoadMore = onLoadMore,
        errorMessage = pageState.errorMessage
            ?.takeIf { !pageState.errorOnLoadMore }
            ?.ifBlank { "加载稍后再看失败" },
        errorContent = { message ->
            StateMessageCard(
                text = message.ifBlank { "加载稍后再看失败" },
                isError = true,
                actionText = "重试",
                onAction = onRefresh
            )
        },
        endReached = pageState.items.isNotEmpty() && !pageState.hasMore &&
            !pageState.isRefreshing && pageState.errorMessage == null,
        key = { _, item -> item.key },
        contentType = { _, item -> item.cardType },
        loadingPlaceholderCount = columns * 8,
        loadingContent = {
            HorizontalVideoCardSkeleton(isTablet = isTablet)
        },
        loadMoreContent = {
            WatchLaterLoadingRows(columns = columns, isTablet = isTablet)
        },
        emptyContent = {
            StateMessageCard(
                text = pageState.errorMessage?.ifBlank { "加载稍后再看失败" }
                    ?: "暂无${pageState.tab.title}内容",
                isError = pageState.errorMessage != null,
                actionText = pageState.errorMessage?.let { "重试" },
                onAction = pageState.errorMessage?.let { onRefresh },
                modifier = Modifier.padding(vertical = 48.dp)
            )
        }
    ) { item ->
        WatchLaterItemCard(
            item = item,
            isTablet = isTablet,
            onDelete = if (item.aid > 0L) {
                { onRemoveItem(item) }
            } else null,
            onClick = {
                item.target?.let(onOpenVideo)
            }
        )
    }
}

@Composable
private fun WatchLaterLoadingRows(columns: Int, isTablet: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(LOAD_MORE_SKELETON_COUNT) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(columns) {
                    HorizontalVideoCardSkeleton(
                        modifier = Modifier.weight(1f),
                        isTablet = isTablet
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchLaterSortDropdown(
    asc: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { listOf(false to "最新添加", true to "最早添加") }
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = !expanded },
            enabled = enabled,
            contentPadding = PaddingValues(start = 12.dp, end = 8.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (asc) "最早添加" else "最近添加",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 128.dp)
        ) {
            options.forEach { (option, label) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    trailingIcon = {
                        if (option == asc) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    modifier = Modifier.semantics { selected = option == asc }
                )
            }
        }
    }
}

@Composable
private fun WatchLaterItemCard(
    item: WatchLaterItem,
    isTablet: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!item.isOpenable) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                onDelete?.let { WatchLaterMoreMenu(onDelete = it) }
            }
        }
        return
    }

    val ownerName = item.ownerName?.takeIf(String::isNotBlank)
    val badge = item.badge?.takeIf(String::isNotBlank)
    val viewText = remember(item.viewText) {
        item.viewText?.takeIf(String::isNotBlank)?.let(::compactCount)
    }
    val danmakuText = remember(item.danmakuText) {
        item.danmakuText?.takeIf(String::isNotBlank)?.let(::compactCount)
    }
    val progress = remember(item.progressSec, item.durationSec) {
        watchLaterProgress(item.progressSec, item.durationSec)
    }
    val duration = remember(item.durationSec) { durationText(item.durationSec) }

    HorizontalVideoCard(
        title = item.title,
        coverUrl = item.cover,
        durationText = progress?.timeText ?: duration,
        onClick = onClick.takeIf { item.isOpenable },
        modifier = modifier,
        isTablet = isTablet,
        coverOverlay = { _ ->
            badge?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .background(Color.Black.copy(alpha = 0.68f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        coverBottomContent = progress?.fraction?.let { fraction ->
            {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ownerName?.let { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UploaderBadge()
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (viewText != null || danmakuText != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewText != null || danmakuText != null) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            viewText?.let { text ->
                                Icon(
                                    imageVector = Icons.Outlined.PlayCircleOutline,
                                    contentDescription = "播放量",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = text,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(start = 3.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            danmakuText?.let { text ->
                                Icon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = "弹幕数",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(start = if (viewText != null) 8.dp else 0.dp)
                                        .size(15.dp)
                                )
                                Text(
                                    text = text,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(start = 3.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    onDelete?.let { WatchLaterMoreMenu(onDelete = it) }
                }
            }
        }
    }
}

@Composable
private fun WatchLaterMoreMenu(onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多操作",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

private fun durationText(sec: Long?): String? {
    val value = sec ?: return null
    if (value <= 0L) return null
    return formatVideoDuration(value)
}
