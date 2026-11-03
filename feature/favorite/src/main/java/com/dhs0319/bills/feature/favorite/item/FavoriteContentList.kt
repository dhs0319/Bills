package com.dhs0319.bills.feature.favorite.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.common.compactCount
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCard
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCardSkeleton
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.UploaderBadge
import com.dhs0319.bills.core.designsystem.component.rememberHorizontalVideoCardColumnCount
import com.dhs0319.bills.core.model.FavoriteContentItem
import com.dhs0319.bills.core.model.FavoriteContentTarget
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun FavoriteContentList(
    items: List<FavoriteContentItem>,
    isLoadingMore: Boolean,
    errorMessage: String?,
    errorOnLoadMore: Boolean,
    hasMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onRemoveContent: ((FavoriteContentItem) -> Unit)? = null,
    gridState: LazyStaggeredGridState,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val columns = rememberHorizontalVideoCardColumnCount()
    val headerItemCount = if (header != null) 1 else 0
    val loadMoreEnabled by rememberUpdatedState(canLoadMore && !errorOnLoadMore)
    val contentItemCount by rememberUpdatedState(items.size)
    val expectedItemCount by rememberUpdatedState(
        headerItemCount + items.size +
            (if (isLoadingMore) LOAD_MORE_SKELETON_COUNT * columns else 0) +
            (if (errorMessage != null && errorOnLoadMore) 1 else 0)
    )
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(gridState, columns, headerItemCount) {
        snapshotFlow {
            if (!loadMoreEnabled || contentItemCount == 0) {
                null
            } else {
                val layout = gridState.layoutInfo
                val last = layout.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
                // Ignore stale measurements and emit once per eligible content count.
                contentItemCount.takeIf {
                    layout.totalItemsCount == expectedItemCount &&
                        last - headerItemCount >= it - LOAD_MORE_TRIGGER_OFFSET * columns
                }
            }
        }
            .filterNotNull()
            .collect {
                currentOnLoadMore()
            }
    }

    FavoriteContentGrid(
        columns = columns,
        gridState = gridState,
        modifier = modifier
    ) {
        header?.let { content ->
            item(
                key = "favorite_content_header",
                contentType = "header",
                span = StaggeredGridItemSpan.FullLine
            ) {
                content()
            }
        }

        items(
            items = items,
            key = { it.key },
            contentType = { it.otype }
        ) { item ->
            FavoriteContentCard(
                item = item,
                onOpenContent = onOpenContent,
                onRemoveContent = onRemoveContent
                    ?.takeIf { item.oid > 0L }
                    ?.let { remove -> { remove(item) } },
                isTablet = isTablet
            )
        }

        if (isLoadingMore) {
            items(
                count = LOAD_MORE_SKELETON_COUNT * columns,
                key = { index -> "favorite_content_loading_$index" },
                contentType = { "loading" }
            ) {
                HorizontalVideoCardSkeleton(isTablet = isTablet)
            }
        }

        if (errorMessage != null && errorOnLoadMore) {
            item(
                key = "favorite_content_error",
                contentType = "error",
                span = StaggeredGridItemSpan.FullLine
            ) {
                StateMessageCard(
                    text = errorMessage.ifBlank { "加载收藏失败" },
                    isError = true,
                    actionText = "重试",
                    onAction = onRetry
                )
            }
        }

        if (!hasMore && !isLoadingMore && errorMessage == null) {
            item(
                key = "favorite_content_end",
                contentType = "end",
                span = StaggeredGridItemSpan.FullLine
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有更多啦~",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteContentCard(
    item: FavoriteContentItem,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onRemoveContent: (() -> Unit)?,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val target = item.target
    val ownerName = item.ownerName?.takeIf(String::isNotBlank)
    val viewText = remember(item.viewText) {
        item.viewText?.takeIf(String::isNotBlank)?.let(::compactCount)
    }
    val danmakuText = remember(item.danmakuText) {
        item.danmakuText?.takeIf(String::isNotBlank)?.let(::compactCount)
    }
    val coverLabel = remember(item.typeDesc, item.isInvalid) {
        val typeDesc = item.typeDesc?.takeIf(String::isNotBlank)
        when {
            typeDesc == null -> if (item.isInvalid) "已失效" else ""
            item.isInvalid -> "$typeDesc · 已失效"
            else -> typeDesc
        }
    }

    HorizontalVideoCard(
        title = item.title,
        coverUrl = item.cover,
        durationText = item.playbackDesc,
        onClick = target?.let { { onOpenContent(it) } },
        modifier = modifier,
        isTablet = isTablet,
        coverOverlay = { _ ->
            if (coverLabel.isNotEmpty()) {
                Text(
                    text = coverLabel,
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
            if (viewText != null || danmakuText != null || onRemoveContent != null) {
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
                    onRemoveContent?.let { remove ->
                        FavoriteContentMoreMenu(onRemoveContent = remove)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteContentMoreMenu(onRemoveContent: () -> Unit) {
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
                text = { Text("取消收藏") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRemoveContent()
                }
            )
        }
    }
}

private const val LOAD_MORE_SKELETON_COUNT = 2
private const val LOAD_MORE_TRIGGER_OFFSET = 3
