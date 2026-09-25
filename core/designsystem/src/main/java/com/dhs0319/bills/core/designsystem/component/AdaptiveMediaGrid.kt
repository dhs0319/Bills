package com.dhs0319.bills.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberAdaptiveGridColumnCount(
    compact: Int = 2,
    medium: Int = 3,
    expanded: Int = 4
): Int {
    val configuration = LocalConfiguration.current
    val isPhoneLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            configuration.smallestScreenWidthDp < 600
    return when {
        isPhoneLandscape -> medium
        configuration.screenWidthDp >= 840 -> expanded
        configuration.screenWidthDp >= 600 -> medium
        else -> compact
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AdaptiveMediaGrid(
    items: List<T>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyStaggeredGridState? = null,
    errorMessage: String? = null,
    loadMoreEnabled: Boolean = true,
    prefetchScreens: Int = 0,
    loadMoreError: String? = null,
    onRetryLoadMore: () -> Unit = onLoadMore,
    endReached: Boolean = false,
    columns: Int = rememberAdaptiveGridColumnCount(),
    loadingPlaceholderCount: Int = 10,
    horizontalSpacing: Dp = 6.dp,
    verticalSpacing: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(
        start = 6.dp,
        top = 0.dp,
        end = 6.dp,
        bottom = 4.dp
    ),
    key: (index: Int, item: T) -> Any = { index, _ -> index },
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    loadingContent: @Composable LazyStaggeredGridItemScope.() -> Unit,
    headerContent: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    emptyContent: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    errorContent: @Composable LazyStaggeredGridItemScope.(String) -> Unit = { msg ->
        DefaultGridError(msg)
    },
    loadMoreContent: @Composable LazyStaggeredGridItemScope.() -> Unit = {
        DefaultGridLoadMore()
    },
    itemContent: @Composable LazyStaggeredGridItemScope.(item: T) -> Unit
) {
    val gridState = state ?: rememberLazyStaggeredGridState()
    val currentItems by rememberUpdatedState(items)
    val expectedItemCount by rememberUpdatedState(
        items.size + (if (headerContent != null) 1 else 0) +
            (if (!errorMessage.isNullOrBlank()) 1 else 0) +
            (if (isLoadingMore || loadMoreError != null || endReached) 1 else 0)
    )
    val shouldLoadMore by remember(gridState, loadMoreEnabled, prefetchScreens, columns) {
        derivedStateOf {
            val layout = gridState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.maxOfOrNull { it.index }
            loadMoreEnabled && currentItems.isNotEmpty() &&
                if (prefetchScreens == 0) {
                    !gridState.canScrollForward
                } else {
                    // Wait for the current data to be measured; stale layout can request twice.
                    lastVisible != null && layout.totalItemsCount == expectedItemCount &&
                        layout.totalItemsCount - lastVisible - 1 <=
                        maxOf(layout.visibleItemsInfo.size * prefetchScreens, columns * 2)
                }
        }
    }

    LaunchedEffect(shouldLoadMore, loadMoreEnabled, isRefreshing, isLoadingMore, loadMoreError) {
        if (loadMoreEnabled && shouldLoadMore && !isRefreshing && !isLoadingMore && loadMoreError == null) {
            onLoadMore()
        }
    }

    BiliPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalItemSpacing = verticalSpacing,
            contentPadding = contentPadding
        ) {
            when {
                items.isEmpty() && isRefreshing -> {
                    items(
                        count = loadingPlaceholderCount,
                        key = { index -> "loading_$index" },
                        contentType = { "loading" }
                    ) {
                        loadingContent()
                    }
                }

                items.isEmpty() -> {
                    when {
                        emptyContent != null -> {
                            item(
                                key = "empty",
                                span = StaggeredGridItemSpan.FullLine,
                                contentType = "empty"
                            ) {
                                emptyContent()
                            }
                        }

                        !errorMessage.isNullOrBlank() -> {
                            val err = errorMessage.orEmpty()
                            item(
                                key = "error",
                                span = StaggeredGridItemSpan.FullLine,
                                contentType = "error"
                            ) {
                                errorContent(err)
                            }
                        }
                    }
                }

                else -> {
                    if (!errorMessage.isNullOrBlank()) {
                        val err = errorMessage.orEmpty()
                        item(
                            key = "error",
                            span = StaggeredGridItemSpan.FullLine,
                            contentType = "error"
                        ) {
                            errorContent(err)
                        }
                    }

                    if (headerContent != null) {
                        item(
                            key = "header",
                            contentType = "header",
                            span = StaggeredGridItemSpan.FullLine
                        ) {
                            headerContent()
                        }
                    }

                    items(
                        count = items.size,
                        key = { index -> key(index, items[index]) },
                        contentType = { index -> contentType(index, items[index]) }
                    ) { index ->
                        itemContent(items[index])
                    }

                    if (isLoadingMore) {
                        item(
                            key = "loading_more",
                            span = StaggeredGridItemSpan.FullLine,
                            contentType = "loading_more"
                        ) {
                            loadMoreContent()
                        }
                    } else if (loadMoreError != null) {
                        item(key = "load_more_error", span = StaggeredGridItemSpan.FullLine) {
                            StateMessageCard(
                                text = loadMoreError,
                                isError = true,
                                actionText = "重试",
                                onAction = onRetryLoadMore
                            )
                        }
                    } else if (endReached) {
                        item(key = "end_reached", span = StaggeredGridItemSpan.FullLine) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("没有更多内容了", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultGridError(message: String) {
    Text(
        text = "加载失败: $message",
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun DefaultGridLoadMore() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}
