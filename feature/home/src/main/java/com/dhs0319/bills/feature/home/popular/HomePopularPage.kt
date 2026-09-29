package com.dhs0319.bills.feature.home.popular

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.designsystem.component.VideoListCardSkeleton
import com.dhs0319.bills.core.model.PopularVideoItem
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.home.component.UploaderBadge
import com.dhs0319.bills.feature.home.paging.HomeMediaGrid

private fun popularCoverWidth(cardWidth: Dp, isTablet: Boolean): Dp {
    return (cardWidth - 16.dp) * if (isTablet) 0.49f else 0.43f
}

private fun popularCoverAspectRatio(isTablet: Boolean): Float {
    return if (isTablet) 16f / 10f else 16f / 9f
}

@Composable
fun HomePopularPage(
    isActive: Boolean,
    refreshRequest: Int,
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    viewModel: HomePopularViewModel = hiltViewModel()
) {
    val paging = viewModel.uiState.collectAsStateWithLifecycle().value
    val actionMessage = viewModel.actionMessage.collectAsStateWithLifecycle().value
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val context = LocalContext.current

    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeActionMessage()
        }
    }

    LaunchedEffect(isActive, refreshRequest) {
        if (isActive && viewModel.activate(refreshRequest)) {
            gridState.scrollToItem(0)
        }
    }

    HomeMediaGrid(
        paging = paging,
        isActive = isActive,
        gridState = gridState,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetryLoadMore = viewModel::retryLoadMore,
        key = { _, item -> item.identityKey },
        contentType = { _, _ -> "popular_video" },
        columns = if (isTablet) 2 else 1,
        estimatedRowHeight = if (isTablet) 136.dp else 128.dp,
        loadingContent = {
            BoxWithConstraints {
                VideoListCardSkeleton(
                    coverWidth = popularCoverWidth(maxWidth, isTablet),
                    coverAspectRatio = popularCoverAspectRatio(isTablet),
                    contentPadding = 8.dp
                )
            }
        }
    ) { item ->
        PopularVideoCard(
            item = item,
            isTablet = isTablet,
            onOpenVideo = { onOpenVideo(item.target) },
            onOpenSpace = onOpenSpace,
            onAddToWatchLater = { viewModel.addToWatchLater(item.aid) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularVideoCard(
    item: PopularVideoItem,
    isTablet: Boolean,
    onOpenVideo: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onAddToWatchLater: () -> Unit
) {
    val publishTime = item.publishTimeText
    val titleStyle = if (isTablet) MaterialTheme.typography.titleSmall
        else MaterialTheme.typography.titleMedium
    val metadataStyle = MaterialTheme.typography.bodySmall
    val minimumContentHeight = with(LocalDensity.current) {
        (titleStyle.lineHeight * 2).toDp() + (metadataStyle.lineHeight * 2).toDp() +
            if (isTablet) 12.dp else 24.dp
    }
    Card(
        onClick = onOpenVideo,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val coverWidth = popularCoverWidth(maxWidth, isTablet)
            val coverHeight = coverWidth / popularCoverAspectRatio(isTablet)
            val contentHeight = maxOf(coverHeight, minimumContentHeight)
            val showExtendedMetadata = isTablet && maxWidth >= 420.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(
                    url = item.cover,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(coverWidth)
                        .height(coverHeight),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = item.durationText,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(Color.Black.copy(alpha = 0.68f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(contentHeight),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        style = titleStyle,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (showExtendedMetadata && publishTime.isNotEmpty()) {
                                Text(
                                    text = publishTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (item.ownerName.isNotBlank()) {
                                val ownerModifier = item.spaceRoute?.let { route ->
                                    Modifier.clickable { onOpenSpace(route) }
                                } ?: Modifier
                                Row(
                                    modifier = ownerModifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    UploaderBadge()
                                    Text(
                                        text = item.ownerName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircleOutline,
                                contentDescription = "播放量",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = item.viewCountText,
                                modifier = Modifier.padding(start = 3.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            if (!showExtendedMetadata && publishTime.isNotEmpty()) {
                                Text(
                                    text = " · $publishTime",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (showExtendedMetadata || publishTime.isEmpty()) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            PopularMoreMenu(onAddToWatchLater)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularMoreMenu(onAddToWatchLater: () -> Unit) {
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
                text = { Text("稍后再看") },
                leadingIcon = { Icon(Icons.Outlined.WatchLater, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToWatchLater()
                }
            )
        }
    }
}
