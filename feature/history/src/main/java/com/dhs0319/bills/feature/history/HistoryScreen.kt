package com.dhs0319.bills.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AdaptivePagerSlidingTabRow
import com.dhs0319.bills.core.designsystem.component.AdaptiveMediaGrid
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCardSkeleton
import com.dhs0319.bills.core.model.HistoryTab
import com.dhs0319.bills.core.model.HistoryTarget
import com.dhs0319.bills.feature.history.component.HistoryItemCard

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onSearch: (HistoryTab) -> Unit,
    onOpenHistoryTarget: (HistoryTarget?) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(
        initialPage = HistoryTab.ALL.ordinal,
        pageCount = { HistoryTab.entries.size }
    )
    val tabTitles = remember { HistoryTab.entries.map { it.title } }
    val gridStates = HistoryTab.entries.map {
        rememberLazyStaggeredGridState()
    }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val showTwoColumns = when (configuration.orientation) {
        android.content.res.Configuration.ORIENTATION_PORTRAIT -> isTablet
        android.content.res.Configuration.ORIENTATION_LANDSCAPE -> true
        else -> false
    }
    val columns = if (showTwoColumns) 2 else 1
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.selectTab(HistoryTab.entries[page])
        }
    }
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
                    IconButton(onClick = { onSearch(HistoryTab.entries[pagerState.settledPage]) }) {
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
                tabs = tabTitles,
                pagerState = pagerState
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                HistoryPage(
                    tab = HistoryTab.entries[page],
                    pagerState = pagerState,
                    gridState = gridStates[page],
                    columns = columns,
                    isTablet = isTablet,
                    viewModel = viewModel,
                    onOpenHistoryTarget = onOpenHistoryTarget
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryPage(
    tab: HistoryTab,
    pagerState: PagerState,
    gridState: LazyStaggeredGridState,
    columns: Int,
    isTablet: Boolean,
    viewModel: HistoryViewModel,
    onOpenHistoryTarget: (HistoryTarget?) -> Unit
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

    AdaptiveMediaGrid(
        items = pageState.items,
        isRefreshing = pageState.isRefreshing || pageState.isLoading,
        isLoadingMore = pageState.isLoadingMore,
        onRefresh = {
            if (isActivePage) viewModel.refresh()
        },
        onLoadMore = viewModel::loadMore,
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        columns = columns,
        loadMoreEnabled = pageState.canLoadMore &&
            pageState.errorMessage == null && isActivePage,
        prefetchScreens = 1,
        loadMoreError = pageState.errorMessage?.takeIf { pageState.errorOnLoadMore },
        onRetryLoadMore = viewModel::loadMore,
        errorMessage = pageState.errorMessage?.takeIf { !pageState.errorOnLoadMore },
        errorContent = { message ->
            StateMessageCard(
                text = message,
                isError = true,
                actionText = "重试",
                onAction = viewModel::refresh,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        },
        endReached = pageState.items.isNotEmpty() && !pageState.hasMore,
        key = { _, item -> item.key },
        contentType = { _, item -> item.type },
        loadingPlaceholderCount = columns * 8,
        loadingContent = {
            HorizontalVideoCardSkeleton(isTablet = isTablet)
        },
        emptyContent = {
            StateMessageCard(
                text = pageState.errorMessage ?: "暂无${pageState.tab.title}历史",
                isError = pageState.errorMessage != null,
                actionText = pageState.errorMessage?.let { "重试" },
                onAction = pageState.errorMessage?.let { { viewModel.refresh() } },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp)
            )
        }
    ) { item ->
        HistoryItemCard(
            item = item,
            onClick = { onOpenHistoryTarget(item.target) }
        )
    }
}
