package com.dhs0319.bills.feature.favorite.folderdetail

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Cloud_off
import com.composables.icons.materialsymbols.outlined.Folder_open
import com.dhs0319.bills.core.designsystem.icon.MirroredSymbols
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.model.FavoriteContentTarget
import com.dhs0319.bills.feature.favorite.FavoriteLoading
import com.dhs0319.bills.feature.favorite.item.FavoriteContentList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteFolderDetailScreen(
    onBack: () -> Unit,
    fid: Long,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    viewModel: FavoriteFolderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeActionMessage()
        }
    }
    val onRemoveContent = remember(viewModel) { viewModel::removeItem }
    val gridState = rememberLazyStaggeredGridState()

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = { Text("收藏夹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = MirroredSymbols.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        BiliPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.items.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FavoriteFolderHeader(
                        title = state.title,
                        totalCount = state.totalCount,
                        createdAtSec = state.createdAtSec,
                        modifier = Modifier.padding(
                            start = 6.dp,
                            end = 6.dp,
                            top = 12.dp,
                            bottom = 8.dp
                        )
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            state.isLoading -> {
                                FavoriteLoading(modifier = Modifier.fillMaxSize())
                            }

                            state.errorMessage != null -> {
                                FavoriteFolderStatus(
                                    icon = MaterialSymbols.Outlined.Cloud_off,
                                    title = "暂时无法加载",
                                    description = state.errorMessage.orEmpty().ifBlank { "请稍后重试" },
                                    onRetry = viewModel::refresh
                                )
                            }

                            else -> {
                                FavoriteFolderStatus(
                                    icon = MaterialSymbols.Outlined.Folder_open,
                                    title = if (state.hasInvalid) "暂无可用内容" else "还没有收藏内容",
                                    description = if (state.hasInvalid) {
                                        "收藏夹中的内容暂时无法查看"
                                    } else {
                                        "收藏喜欢的内容后，会在这里展示"
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                FavoriteContentList(
                    items = state.items,
                    isLoadingMore = state.isLoadingMore,
                    errorMessage = state.errorMessage,
                    errorOnLoadMore = state.errorOnLoadMore,
                    hasMore = state.hasMore,
                    canLoadMore = state.canLoadMore,
                    onLoadMore = viewModel::loadMore,
                    onRetry = if (state.errorOnLoadMore) viewModel::loadMore else viewModel::refresh,
                    onOpenContent = onOpenContent,
                    onRemoveContent = onRemoveContent,
                    gridState = gridState,
                    header = {
                        FavoriteFolderHeader(
                            title = state.title,
                            totalCount = state.totalCount,
                            createdAtSec = state.createdAtSec
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteFolderStatus(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteFolderHeader(
    title: String,
    totalCount: Int,
    createdAtSec: Long,
    modifier: Modifier = Modifier
) {
    val createdAtText = remember(createdAtSec) {
        createdAtSec.takeIf { it > 0L }?.let {
            DateFormat.format("yyyy-MM-dd HH:mm", it * 1000L).toString()
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${totalCount}个内容",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        createdAtText?.let { time ->
            Text(
                text = "创建时间：$time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
