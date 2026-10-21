package com.dhs0319.bills.feature.home.popular

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCard
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCardSkeleton
import com.dhs0319.bills.core.designsystem.component.UploaderBadge
import com.dhs0319.bills.core.designsystem.component.WatchLaterMoreMenu
import com.dhs0319.bills.core.model.PopularVideoItem
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.home.paging.HomeMediaGrid

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
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val showTwoColumns = when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> isTablet
        Configuration.ORIENTATION_LANDSCAPE -> true
        else -> false
    }
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
        columns = if (showTwoColumns) 2 else 1,
        estimatedRowHeight = if (showTwoColumns) 136.dp else 128.dp,
        loadingContent = {
            HorizontalVideoCardSkeleton(isTablet = isTablet)
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

@Composable
private fun PopularVideoCard(
    item: PopularVideoItem,
    isTablet: Boolean,
    onOpenVideo: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onAddToWatchLater: () -> Unit
) {
    val publishTime = item.publishTimeText
    HorizontalVideoCard(
        title = item.title,
        coverUrl = item.cover,
        durationText = item.durationText,
        onClick = onOpenVideo,
        isTablet = isTablet
    ) { cardWidth ->
        val showExtendedMetadata = isTablet && cardWidth >= 420.dp
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
                WatchLaterMoreMenu(onAddToWatchLater)
            }
        }
    }
}
