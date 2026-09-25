package com.dhs0319.bills.feature.home.listen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.feature.home.paging.HomeMediaGrid
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.model.listen.ListenItem
import com.dhs0319.bills.feature.home.component.UploaderBadge

private val listenCoverMetadataBrush = Brush.verticalGradient(
    0f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.72f)
)

@Composable
fun ListenHomePage(
    isActive: Boolean,
    refreshRequest: Int,
    onItemClick: (ListenItem) -> Unit,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    viewModel: ListenHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isActive, refreshRequest) {
        if (isActive && viewModel.activate(refreshRequest)) {
            gridState.scrollToItem(0)
        }
    }
    HomeMediaGrid(
        paging = state,
        isActive = isActive,
        gridState = gridState,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onRetryLoadMore = viewModel::retryLoadMore,
        key = { _, item -> item.identityKey }
    ) { item ->
        ListenCard(
            item = item,
            onClick = { onItemClick(item) }
        )
    }
}
@Composable
private fun ListenCard(
    item: ListenItem,
    onClick: () -> Unit
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
                if (item.duration > 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(listenCoverMetadataBrush)
                    ) {
                        Text(
                            text = item.durationText,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    text = item.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (item.author.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UploaderBadge()
                        Text(
                            text = item.author,
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
