package com.dhs0319.bills.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AdaptivePagerSlidingTabRow
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.VideoListCardSkeleton
import com.dhs0319.bills.core.model.HistoryTab
import com.dhs0319.bills.core.model.HistoryTarget
import com.dhs0319.bills.feature.history.component.HistoryItemCard
import com.dhs0319.bills.feature.history.component.HistoryListLoading
import com.dhs0319.bills.feature.history.component.LOAD_MORE_SKELETON_COUNT
import com.dhs0319.bills.feature.history.component.LoadMoreTrigger

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onSearch: (HistoryTab) -> Unit,
    onOpenHistoryTarget: (HistoryTarget?) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(
        initialPage = state.tab.ordinal,
        pageCount = { HistoryTab.entries.size }
    )
    val listStates = remember { HistoryTab.entries.map { LazyListState() } }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.selectTab(HistoryTab.entries[page])
        }
    }
    LoadMoreTrigger(
        listState = listStates[state.tab.ordinal],
        canLoadMore = {
            state.canLoadMore && !pagerState.isScrollInProgress &&
                pagerState.settledPage == state.tab.ordinal
        },
        isLoadingMore = { state.isLoadingMore },
        hasError = { state.errorMessage != null },
        onLoadMore = viewModel::loadMore
    )

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onSearch(state.tab) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索历史记录"
                        )
                    }
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
                tabs = HistoryTab.entries.map { it.title },
                pagerState = pagerState
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageState = viewModel.previewState(HistoryTab.entries[page])
                BiliPullToRefreshBox(
                    isRefreshing = pageState.isRefreshing,
                    onRefresh = {
                        if (!pagerState.isScrollInProgress && pageState.tab == state.tab) {
                            viewModel.refresh()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                when {
                    pageState.isLoading && pageState.items.isEmpty() -> {
                        HistoryListLoading(
                            skeletonPrefix = "history_skeleton",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    pageState.errorMessage != null && pageState.items.isEmpty() -> {
                        StateMessageCard(
                            text = pageState.errorMessage.orEmpty().ifBlank { "加载历史记录失败" },
                            modifier = Modifier.fillMaxSize(),
                            isError = true,
                            actionText = "重试",
                            onAction = { if (pageState.tab == state.tab) viewModel.refresh() }
                        )
                    }

                    pageState.items.isEmpty() -> {
                        StateMessageCard(
                            text = "暂无${pageState.tab.title}历史",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        LazyColumn(
                            state = listStates[page],
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = pageState.items,
                                key = { it.key },
                                contentType = { it.type }
                            ) { item ->
                                HistoryItemCard(
                                    item = item,
                                    onClick = { onOpenHistoryTarget(item.target) }
                                )
                            }

                            if (pageState.isLoadingMore) {
                                items(
                                    count = LOAD_MORE_SKELETON_COUNT,
                                    key = { index -> "history_loading_$index" },
                                    contentType = { "loading" }
                                ) {
                                    VideoListCardSkeleton()
                                }
                            }

                            if (pageState.errorMessage != null && pageState.items.isNotEmpty()) {
                                item(
                                    key = "history_error",
                                    contentType = "error"
                                ) {
                                    StateMessageCard(
                                        text = pageState.errorMessage.orEmpty().ifBlank { "加载历史记录失败" },
                                        isError = true,
                                        actionText = "重试",
                                        onAction = if (pageState.errorOnLoadMore) {
                                            viewModel::loadMore
                                        } else {
                                            viewModel::refresh
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
