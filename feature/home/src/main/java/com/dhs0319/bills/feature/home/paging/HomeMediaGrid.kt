package com.dhs0319.bills.feature.home.paging

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.AdaptiveMediaGrid
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.LoadFailedState
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.VideoGridCardSkeleton
import com.dhs0319.bills.core.designsystem.component.VideoGridCardSkeletonStyle
import com.dhs0319.bills.core.designsystem.component.rememberAdaptiveGridColumnCount
import com.dhs0319.bills.core.designsystem.component.videoGridCardSkeletonHeight
import com.dhs0319.bills.core.designsystem.theme.LocalTopLevelNavSpace
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
internal fun <T> HomeMediaGrid(
    paging: HomePagingState<T>,
    isActive: Boolean,
    gridState: LazyStaggeredGridState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    key: (Int, T) -> Any,
    contentType: (Int, T) -> Any? = { _, _ -> null },
    columns: Int = rememberAdaptiveGridColumnCount(),
    estimatedRowHeight: Dp? = null,
    skeletonStyle: VideoGridCardSkeletonStyle = VideoGridCardSkeletonStyle.VIDEO,
    loadingContent: @Composable LazyStaggeredGridItemScope.() -> Unit = { VideoGridCardSkeleton(style = skeletonStyle) },
    headerContent: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    separatorIndex: Int? = null,
    separatorContent: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) {
    val scope = rememberCoroutineScope()
    val refresh = {
        onRefresh()
        if (paging.items.isNotEmpty()) {
            scope.launch { gridState.scrollToItem(0) }
        }
        Unit
    }
    val loadError = paging.errorMessage ?: paging.loadMoreError
    val awaitingContent = paging.isInitialLoading || paging.isLoadingMore ||
        (!paging.hasLoaded && loadError == null)
    if (paging.items.isEmpty() && !awaitingContent && loadError != null) {
        BiliPullToRefreshBox(
            isRefreshing = false,
            onRefresh = refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LoadFailedState(
                description = loadError,
                onRetry = if (paging.loadMoreError != null) onRetryLoadMore else refresh,
                modifier = Modifier.padding(bottom = LocalTopLevelNavSpace.current)
            )
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Use the skeleton's own typography and layout metrics, including font/UI scaling.
        // An extra row covers partial rows and differences in real card heights.
        val cardWidth = ((maxWidth.value - 12f - (columns - 1) * 6f) / columns).coerceAtLeast(1f)
        val rowHeight = estimatedRowHeight?.value ?: (videoGridCardSkeletonHeight(cardWidth.dp, skeletonStyle) + 6.dp).value
        val placeholderCount = (ceil(maxHeight.value / rowHeight).toInt() + 1) * columns
        if (paging.items.isEmpty() && awaitingContent) {
            // Skeletons must never participate in the real grid's lane assignment
            // or scroll anchor. Reveal real cards as soon as the first page arrives.
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalItemSpacing = 6.dp,
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 4.dp)
            ) {
                items(placeholderCount, contentType = { "loading" }) { loadingContent() }
            }
        } else {
            AdaptiveMediaGrid(
                items = paging.items,
                isRefreshing = paging.isRefreshing && !paging.isInitialLoading,
                isLoadingMore = paging.isLoadingMore || paging.isInitialLoading,
                onRefresh = refresh,
                onLoadMore = onLoadMore,
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = columns,
                loadingPlaceholderCount = placeholderCount,
                errorMessage = paging.errorMessage.takeUnless { paging.connectionFailed },
                loadMoreEnabled = isActive && paging.hasLoaded && paging.hasMore &&
                    paging.errorMessage == null && paging.loadMoreError == null,
                prefetchScreens = 1,
                loadMoreError = paging.loadMoreError,
                showLoadMoreErrorMessage = !paging.connectionFailed,
                onRetryLoadMore = onRetryLoadMore,
                endReached = paging.hasLoaded && !paging.hasMore,
                key = key,
                contentType = contentType,
                loadingContent = loadingContent,
                headerContent = headerContent,
                separatorIndex = separatorIndex,
                separatorContent = separatorContent,
                emptyContent = {
                    StateMessageCard(
                        text = "暂无内容，下拉试试重新获取",
                        actionText = "重试",
                        onAction = refresh,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp)
                    )
                },
                errorContent = { message ->
                    StateMessageCard(text = message, isError = true, actionText = "重试", onAction = refresh)
                },
                itemContent = itemContent
            )
        }
    }
}
