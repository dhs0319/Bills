package com.dhs0319.bills.feature.favorite

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.dhs0319.bills.core.model.FavoriteContentTarget
import com.dhs0319.bills.feature.favorite.folder.FavoriteFolderList
import com.dhs0319.bills.feature.favorite.item.FavoriteContentList

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    onBack: () -> Unit,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onOpenFolder: (Long) -> Unit,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contentListState = rememberLazyListState()
    val folderListState = rememberLazyListState()
    val pagerState = rememberPagerState(
        initialPage = state.tab.ordinal,
        pageCount = { FavoriteTab.entries.size }
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.selectTab(FavoriteTab.entries[page])
        }
    }

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = { Text("收藏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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
                tabs = FavoriteTab.entries.map { it.title },
                pagerState = pagerState
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val tab = FavoriteTab.entries[page]
                BiliPullToRefreshBox(
                    isRefreshing = when (tab) {
                        FavoriteTab.CONTENT -> state.content.isRefreshing
                        FavoriteTab.FOLDER -> state.folder.isRefreshing
                    },
                    onRefresh = {
                        if (!pagerState.isScrollInProgress && state.tab == tab) {
                            viewModel.refresh()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    FavoriteBody(
                        tab = tab,
                        state = state,
                        isActive = !pagerState.isScrollInProgress &&
                            pagerState.settledPage == page && state.tab == tab,
                        contentListState = contentListState,
                        folderListState = folderListState,
                        onOpenContent = onOpenContent,
                        onOpenFolder = onOpenFolder,
                        onRetry = { if (state.tab == tab) viewModel.refresh() },
                        onLoadMore = viewModel::loadMore
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteBody(
    tab: FavoriteTab,
    state: FavoriteUiState,
    isActive: Boolean,
    contentListState: LazyListState,
    folderListState: LazyListState,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onOpenFolder: (Long) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    val content = state.content
    val folder = state.folder
    val hasCurrentItems = when (tab) {
        FavoriteTab.CONTENT -> content.items.isNotEmpty()
        FavoriteTab.FOLDER -> folder.folders.isNotEmpty()
    }
    when {
        ((tab == FavoriteTab.CONTENT && content.isLoading) ||
            (tab == FavoriteTab.FOLDER && folder.isLoading) || state.tab != tab) && !hasCurrentItems -> {
            FavoriteLoading(modifier = Modifier.fillMaxSize())
        }

        ((tab == FavoriteTab.CONTENT && content.errorMessage != null) ||
            (tab == FavoriteTab.FOLDER && folder.errorMessage != null)) && !hasCurrentItems -> {
            StateMessageCard(
                text = when (tab) {
                    FavoriteTab.CONTENT -> content.errorMessage.orEmpty()
                    FavoriteTab.FOLDER -> folder.errorMessage.orEmpty()
                }.ifBlank { "加载收藏失败" },
                modifier = Modifier.fillMaxSize(),
                isError = true,
                actionText = "重试",
                onAction = onRetry
            )
        }

        !hasCurrentItems -> {
            StateMessageCard(
                text = when (tab) {
                    FavoriteTab.CONTENT -> "暂无收藏内容"
                    FavoriteTab.FOLDER -> "暂无收藏夹"
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        tab == FavoriteTab.CONTENT -> {
            FavoriteContentList(
                items = content.items,
                isLoadingMore = content.isLoadingMore,
                errorMessage = content.errorMessage,
                errorOnLoadMore = content.errorOnLoadMore,
                canLoadMore = content.canLoadMore && isActive,
                onLoadMore = onLoadMore,
                onRetry = if (content.errorOnLoadMore) onLoadMore else onRetry,
                onOpenContent = onOpenContent,
                listState = contentListState
            )
        }

        tab == FavoriteTab.FOLDER -> {
            FavoriteFolderList(
                folders = folder.folders,
                listState = folderListState,
                onOpenFolder = onOpenFolder
            )
        }
    }
}

@Composable
fun FavoriteLoading(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            count = LOADING_COUNT,
            key = { index -> "favorite_loading_$index" },
            contentType = { "loading" }
        ) {
            VideoListCardSkeleton()
        }
    }
}

private const val LOADING_COUNT = 8
