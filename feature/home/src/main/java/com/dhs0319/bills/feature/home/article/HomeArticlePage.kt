package com.dhs0319.bills.feature.home.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.feature.home.paging.HomeMediaGrid
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.article.ArticleRecommendItem

@Composable
fun HomeArticlePage(
    isActive: Boolean,
    refreshRequest: Int,
    onOpenArticle: (String, Int) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    viewModel: HomeArticleViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

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
        key = { _, item -> item.id }
    ) { item ->
        ArticleRecommendCard(
            item = item,
            onClick = { onOpenArticle(item.id.toString(), 1) },
            onOpenSpace = onOpenSpace
        )
    }
}

@Composable
private fun ArticleRecommendCard(
    item: ArticleRecommendItem,
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
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statLine = item.statLine
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                item.summary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ArticleAuthorRow(item = item, onOpenSpace = onOpenSpace)
                if (statLine != null || item.categoryName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (statLine != null) {
                            Text(
                                text = statLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        item.categoryName?.let { category ->
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleAuthorRow(
    item: ArticleRecommendItem,
    onOpenSpace: (SpaceRoute) -> Unit
) {
    val route = item.spaceRoute
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            url = item.authorFace,
            contentDescription = item.authorName ?: "作者",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = item.authorName ?: "专栏作者",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (route == null) {
                    Modifier
                } else {
                    Modifier.clickable { onOpenSpace(route) }
                }
            )
        }
        item.publishTimeText?.let { time ->
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
