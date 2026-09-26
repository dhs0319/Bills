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
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.AdaptiveMediaGrid
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.VideoGridCardSkeleton
import com.dhs0319.bills.core.designsystem.component.rememberAdaptiveGridColumnCount
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
    headerContent: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    separatorIndex: Int? = null,
    separatorContent: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    itemContent: @Composable LazyStaggeredGridItemScope.(T) -> Unit
) {
    val scope = rememberCoroutineScope()
    val refresh = {
        onRefresh()
        scope.launch { gridState.scrollToItem(0) }
        Unit
    }
    val columns = rememberAdaptiveGridColumnCount()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Match VideoGridCardSkeleton: 16:10 cover, 72 dp text area and 6 dp gap.
        // An extra row covers partial rows and differences in real card heights.
        val cardWidth = ((maxWidth.value - 12f - (columns - 1) * 6f) / columns).coerceAtLeast(1f)
        val rowHeight = cardWidth * 10f / 16f + 72f + 6f
        val placeholderCount = (ceil(maxHeight.value / rowHeight).toInt() + 1) * columns
        if (paging.items.isEmpty() && (paging.isInitialLoading || (!paging.hasLoaded && paging.errorMessage == null))) {
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
                items(placeholderCount) { VideoGridCardSkeleton() }
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
                errorMessage = paging.errorMessage,
                loadMoreEnabled = isActive && paging.hasLoaded && paging.hasMore &&
                    paging.errorMessage == null && paging.loadMoreError == null,
                prefetchScreens = 1,
                loadMoreError = paging.loadMoreError,
                onRetryLoadMore = onRetryLoadMore,
                endReached = paging.hasLoaded && !paging.hasMore,
                key = key,
                contentType = contentType,
                loadingContent = { VideoGridCardSkeleton() },
                headerContent = headerContent,
                separatorIndex = separatorIndex,
                separatorContent = separatorContent,
                emptyContent = {
                    StateMessageCard(
                        text = paging.errorMessage ?: paging.loadMoreError ?: "暂无内容，下拉试试重新获取",
                        isError = paging.errorMessage != null || paging.loadMoreError != null,
                        actionText = "重试",
                        onAction = if (paging.loadMoreError != null) onRetryLoadMore else refresh,
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
