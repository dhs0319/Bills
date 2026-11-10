package com.dhs0319.bills.feature.favorite.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.icon.MirroredSymbols
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.SkeletonBlock
import com.dhs0319.bills.core.designsystem.component.rememberHorizontalVideoCardColumnCount
import com.dhs0319.bills.core.model.FavoriteContentItem
import com.dhs0319.bills.core.model.FavoriteFolder
import com.dhs0319.bills.feature.favorite.FAVORITE_FOLDER_PREVIEW_COUNT

@Composable
fun FavoriteFolderList(
    folders: List<FavoriteFolder>,
    previews: Map<Long, List<FavoriteContentItem>>,
    previewGeneration: Int,
    isActive: Boolean,
    gridState: LazyStaggeredGridState,
    onOpenFolder: (FavoriteFolder) -> Unit,
    onLoadPreview: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = rememberHorizontalVideoCardColumnCount()
    LaunchedEffect(gridState, previewGeneration, isActive) {
        if (!isActive) return@LaunchedEffect
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Long }
        }.collect { folderIds ->
            folderIds.forEach(onLoadPreview)
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = FolderListPadding,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(
            items = folders,
            key = { it.fid },
            contentType = { "folder" }
        ) { folder ->
            FavoriteFolderCard(
                folder = folder,
                previewItems = previews[folder.fid],
                onClick = { onOpenFolder(folder) }
            )
        }
    }
}

@Composable
private fun FavoriteFolderCard(
    folder: FavoriteFolder,
    previewItems: List<FavoriteContentItem>?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FolderCardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = folder.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    Text(
                        text = "${folder.mediaCount}个内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = hintColor,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = MirroredSymbols.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = hintColor
                    )
                }
            }

            FavoriteFolderPreviews(
                items = if (folder.mediaCount > 0) previewItems else emptyList(),
                fallbackCover = folder.cover.takeIf { folder.mediaCount > 0 },
                showTitles = folder.mediaCount > 0
            )
        }
    }
}

@Composable
private fun FavoriteFolderPreviews(
    items: List<FavoriteContentItem>?,
    fallbackCover: String? = null,
    showTitles: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(FAVORITE_FOLDER_PREVIEW_COUNT) { index ->
            val item = items?.getOrNull(index)
            val cover = item?.cover ?: fallbackCover.takeIf { index == 0 }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!cover.isNullOrBlank()) {
                        BiliAsyncImage(
                            url = cover,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (showTitles) {
                    if (items == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(12.dp),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                            SkeletonBlock(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(12.dp),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                        }
                    } else {
                        Text(
                            text = item?.title.orEmpty(),
                            modifier = Modifier.padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteFolderListSkeleton(modifier: Modifier = Modifier) {
    val columns = rememberHorizontalVideoCardColumnCount()
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = FolderListPadding,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalItemSpacing = 8.dp,
        userScrollEnabled = false
    ) {
        items(count = 4 * columns, contentType = { "folder_loading" }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FolderCardPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(24.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .width(56.dp)
                                .height(14.dp),
                            shape = MaterialTheme.shapes.extraSmall
                        )
                    }
                    FavoriteFolderPreviews(items = null)
                }
            }
        }
    }
}

private val FolderListPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
private val FolderCardPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
