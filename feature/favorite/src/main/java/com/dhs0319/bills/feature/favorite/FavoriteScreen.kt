package com.dhs0319.bills.feature.favorite

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AdaptivePagerSlidingTabRow
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCardSkeleton
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.rememberHorizontalVideoCardColumnCount
import com.dhs0319.bills.core.model.FavoriteContentItem
import com.dhs0319.bills.core.model.FavoriteContentTarget
import com.dhs0319.bills.core.model.FavoriteFolder
import com.dhs0319.bills.feature.favorite.folder.FavoriteFolderList
import com.dhs0319.bills.feature.favorite.folder.FavoriteFolderListSkeleton
import com.dhs0319.bills.feature.favorite.item.FavoriteContentGrid
import com.dhs0319.bills.feature.favorite.item.FavoriteContentList

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    onBack: () -> Unit,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onOpenFolder: (FavoriteFolder) -> Unit,
    viewModel: FavoriteViewModel = hiltViewModel()
) {
    val selectedTabFlow = remember(viewModel) { viewModel.observeSelectedTab() }
    val selectedTab by selectedTabFlow.collectAsStateWithLifecycle(
        initialValue = viewModel.uiState.value.tab
    )
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeActionMessage()
        }
    }
    val tabs = remember { FavoriteTab.entries.map { it.title } }
    val onLoadMore = remember(viewModel) { viewModel::loadMore }
    val onRemoveContent = remember(viewModel) { viewModel::removeContent }
    val onLoadFolderPreview = remember(viewModel) { viewModel::loadFolderPreview }
    val contentGridState = rememberLazyStaggeredGridState()
    val folderGridState = rememberLazyStaggeredGridState()
    val pagerState = rememberPagerState(
        initialPage = selectedTab.ordinal,
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
                tabs = tabs,
                pagerState = pagerState
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (FavoriteTab.entries[page]) {
                    FavoriteTab.CONTENT -> FavoriteContentPage(
                        selectedTab = selectedTab,
                        pagerState = pagerState,
                        gridState = contentGridState,
                        onOpenContent = onOpenContent,
                        onRemoveContent = onRemoveContent,
                        onLoadMore = onLoadMore,
                        viewModel = viewModel
                    )

                    FavoriteTab.FOLDER -> FavoriteFolderPage(
                        selectedTab = selectedTab,
                        pagerState = pagerState,
                        gridState = folderGridState,
                        onOpenFolder = onOpenFolder,
                        onLoadPreview = onLoadFolderPreview,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteContentPage(
    selectedTab: FavoriteTab,
    pagerState: PagerState,
    gridState: LazyStaggeredGridState,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onRemoveContent: (FavoriteContentItem) -> Unit,
    onLoadMore: () -> Unit,
    viewModel: FavoriteViewModel
) {
    val pageStates = remember(viewModel) { viewModel.observeContentState() }
    val state by pageStates.collectAsStateWithLifecycle(
        initialValue = viewModel.uiState.value.content
    )
    val isActive by remember(pagerState, selectedTab) {
        derivedStateOf {
            !pagerState.isScrollInProgress &&
                pagerState.settledPage == FavoriteTab.CONTENT.ordinal &&
                selectedTab == FavoriteTab.CONTENT
        }
    }
    val onRefresh = remember(viewModel, pagerState) {
        {
            if (!pagerState.isScrollInProgress &&
                pagerState.settledPage == FavoriteTab.CONTENT.ordinal &&
                viewModel.uiState.value.tab == FavoriteTab.CONTENT
            ) {
                viewModel.refresh()
            }
        }
    }

    BiliPullToRefreshBox(
        isRefreshing = state.isRefreshing || state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        FavoriteContentBody(
            state = state,
            isActive = isActive,
            gridState = gridState,
            onOpenContent = onOpenContent,
            onRemoveContent = onRemoveContent,
            onRetry = onRefresh,
            onLoadMore = onLoadMore
        )
    }
}

@Composable
private fun FavoriteFolderPage(
    selectedTab: FavoriteTab,
    pagerState: PagerState,
    gridState: LazyStaggeredGridState,
    onOpenFolder: (FavoriteFolder) -> Unit,
    onLoadPreview: (Long) -> Unit,
    viewModel: FavoriteViewModel
) {
    val pageStates = remember(viewModel) { viewModel.observeFolderState() }
    val state by pageStates.collectAsStateWithLifecycle(
        initialValue = viewModel.uiState.value.folder
    )
    val isActive by remember(pagerState, selectedTab) {
        derivedStateOf {
            !pagerState.isScrollInProgress &&
                pagerState.settledPage == FavoriteTab.FOLDER.ordinal &&
                selectedTab == FavoriteTab.FOLDER
        }
    }
    val onRefresh = remember(viewModel, pagerState) {
        {
            if (!pagerState.isScrollInProgress &&
                pagerState.settledPage == FavoriteTab.FOLDER.ordinal &&
                viewModel.uiState.value.tab == FavoriteTab.FOLDER
            ) {
                viewModel.refresh()
            }
        }
    }

    BiliPullToRefreshBox(
        isRefreshing = state.isRefreshing || state.isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        FavoriteFolderBody(
            state = state,
            isActive = isActive,
            gridState = gridState,
            onOpenFolder = onOpenFolder,
            onLoadPreview = onLoadPreview,
            onRetry = onRefresh
        )
    }
}

@Composable
private fun FavoriteContentBody(
    state: FavoriteContentUiState,
    isActive: Boolean,
    gridState: LazyStaggeredGridState,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onRemoveContent: (FavoriteContentItem) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit
) {
    when {
        state.items.isEmpty() && (state.isLoading || (!state.hasLoaded && state.errorMessage == null)) -> {
            FavoriteLoading(modifier = Modifier.fillMaxSize())
        }

        state.items.isEmpty() && state.errorMessage != null -> {
            StateMessageCard(
                text = state.errorMessage.ifBlank { "加载收藏失败" },
                modifier = Modifier.fillMaxSize(),
                isError = true,
                actionText = "重试",
                onAction = onRetry
            )
        }

        state.items.isEmpty() -> {
            StateMessageCard(
                text = "暂无收藏内容",
                modifier = Modifier.fillMaxSize()
            )
        }

        else -> {
            FavoriteContentList(
                items = state.items,
                isLoadingMore = state.isLoadingMore,
                errorMessage = state.errorMessage,
                errorOnLoadMore = state.errorOnLoadMore,
                hasMore = state.hasMore,
                canLoadMore = state.canLoadMore && isActive,
                onLoadMore = onLoadMore,
                onRetry = if (state.errorOnLoadMore) onLoadMore else onRetry,
                onOpenContent = onOpenContent,
                onRemoveContent = onRemoveContent,
                gridState = gridState
            )
        }
    }
}

@Composable
private fun FavoriteFolderBody(
    state: FavoriteFolderUiState,
    isActive: Boolean,
    gridState: LazyStaggeredGridState,
    onOpenFolder: (FavoriteFolder) -> Unit,
    onLoadPreview: (Long) -> Unit,
    onRetry: () -> Unit
) {
    when {
        state.folders.isEmpty() && (state.isLoading || (!state.hasLoaded && state.errorMessage == null)) -> {
            FavoriteLoading(modifier = Modifier.fillMaxSize(), isContent = false)
        }

        state.folders.isEmpty() && state.errorMessage != null -> {
            StateMessageCard(
                text = state.errorMessage.ifBlank { "加载收藏失败" },
                modifier = Modifier.fillMaxSize(),
                isError = true,
                actionText = "重试",
                onAction = onRetry
            )
        }

        state.folders.isEmpty() -> {
            StateMessageCard(
                text = "暂无收藏夹",
                modifier = Modifier.fillMaxSize()
            )
        }

        else -> {
            FavoriteFolderList(
                folders = state.folders,
                previews = state.previews,
                previewGeneration = state.previewGeneration,
                isActive = isActive && !state.isRefreshing,
                gridState = gridState,
                onLoadPreview = onLoadPreview,
                onOpenFolder = onOpenFolder
            )
        }
    }
}

@Composable
fun FavoriteLoading(
    modifier: Modifier = Modifier,
    isContent: Boolean = true
) {
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val columns = rememberHorizontalVideoCardColumnCount()
    if (isContent) {
        FavoriteContentGrid(
            columns = columns,
            modifier = modifier,
            userScrollEnabled = false
        ) {
            items(
                count = LOADING_COUNT * columns,
                key = { index -> "favorite_loading_$index" },
                contentType = { "loading" }
            ) {
                HorizontalVideoCardSkeleton(isTablet = isTablet)
            }
        }
    } else {
        FavoriteFolderListSkeleton(modifier = modifier)
    }
}

private const val LOADING_COUNT = 8
