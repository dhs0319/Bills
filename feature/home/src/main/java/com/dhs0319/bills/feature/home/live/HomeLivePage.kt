package com.dhs0319.bills.feature.home.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.feature.home.paging.HomeMediaGrid
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.designsystem.component.UpListRow
import com.dhs0319.bills.core.model.LiveRecommendItem
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.feature.home.component.UploaderBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLivePage(
    isActive: Boolean,
    refreshRequest: Int,
    onOpenLive: (LiveRoute) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    viewModel: HomeLiveViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(isActive, refreshRequest) {
        if (isActive && viewModel.activate(refreshRequest)) {
            gridState.scrollToItem(0)
        }
    }
    HomeMediaGrid(
        paging = state.paging,
        isActive = isActive,
        gridState = gridState,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetryLoadMore = viewModel::retryLoadMore,
        key = { _, item -> item.actionKey() },
        headerContent = state.upList?.let { upList ->
            {
                UpListRow(
                    title = upList.title,
                    items = upList.items,
                    key = { it.uid },
                    name = { it.name },
                    face = { it.face },
                    onClick = { item ->
                        onOpenLive(item.route)
                    }
                )
            }
        }
    ) { item ->
        LiveRecommendCard(
            item = item,
            onClick = { onOpenLive(item.route) },
            onOpenSpace = onOpenSpace
        )
    }
}

private fun LiveRecommendItem.actionKey(): String {
    return "${roomId}_${sessionId.orEmpty()}"
}

@Composable
private fun LiveRecommendCard(
    item: LiveRecommendItem,
    onClick: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            CoverImage(
                url = item.cover,
                contentDescription = item.title,
                shape = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                LiveCoverMetadata(item, Modifier.align(Alignment.BottomCenter))
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                val spaceRoute = remember(item.ownerMid, item.ownerName) {
                    item.ownerMid?.let { mid ->
                        SpaceRoute(
                            mid = mid,
                            name = item.ownerName
                        )
                    }
                }
                Text(
                    text = item.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )

                item.ownerName?.takeIf(String::isNotBlank)?.let { ownerName ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (spaceRoute == null) Modifier
                                else Modifier.clickable { onOpenSpace(spaceRoute) }
                            ),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UploaderBadge()
                        Text(
                            text = ownerName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCoverMetadata(item: LiveRecommendItem, modifier: Modifier = Modifier) {
    val viewers = item.onlineText?.takeIf(String::isNotBlank)
    val area = item.areaName?.takeIf(String::isNotBlank)
    if (viewers == null && area == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.72f)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                viewers?.let { text ->
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "观看",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            area?.let { text ->
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
