package com.dhs0319.bills.feature.home.video

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.feature.home.paging.HomeMediaGrid
import com.dhs0319.bills.feature.home.paging.HomePagingState
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.model.FeedItem
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.ThreePointItem
import com.dhs0319.bills.core.model.ThreePointReason
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.home.component.UploaderBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeVideoPage(
    paging: HomePagingState<FeedItem>,
    isActive: Boolean,
    onActivate: (Int) -> Boolean,
    onRetryLoadMore: () -> Unit,
    toastMessage: String,
    dislikedReasons: Map<String, String>,
    refreshRequest: Int,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenLive: (LiveRoute) -> Unit,
    onOpenDynamic: (String) -> Unit,
    onDislike: (FeedItem, ThreePointReason) -> Unit,
    onCancelDislike: (FeedItem) -> Unit,
    onToastShown: () -> Unit,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(toastMessage, context) {
        if (toastMessage.isNotEmpty()) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            onToastShown()
        }
    }
    LaunchedEffect(isActive, refreshRequest) {
        if (isActive && onActivate(refreshRequest)) gridState.scrollToItem(0)
    }
    val firstItemKey = paging.items.firstOrNull()?.actionKey()
    var shownFirstItemKey by rememberSaveable { mutableStateOf(firstItemKey) }
    LaunchedEffect(isActive, firstItemKey) {
        if (isActive) {
            if (shownFirstItemKey != null && firstItemKey != shownFirstItemKey) {
                gridState.scrollToItem(0)
            }
            shownFirstItemKey = firstItemKey
        }
    }
    HomeMediaGrid(
        paging = paging,
        isActive = isActive,
        gridState = gridState,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        onRetryLoadMore = onRetryLoadMore,
        key = { _, item -> item.actionKey() },
        contentType = { _, item -> item.cardType },
        separatorIndex = paging.refreshBoundaryIndex,
        separatorContent = {
            LastSeenBoundaryCard(
                enabled = !paging.isRefreshing,
                onClick = {
                    onRefresh()
                    scope.launch { gridState.scrollToItem(0) }
                }
            )
        }
    ) { item ->
        FeedCard(
            item = item,
            onOpenSpace = onOpenSpace,
            dislikedReason = dislikedReasons[item.actionKey()],
            onDislike = onDislike,
            onCancelDislike = onCancelDislike,
            onClick = {
                if (item.cardGoto == "dynamic") {
                    onOpenDynamic(item.param)
                } else {
                    item.liveRoute?.let(onOpenLive)
                        ?: item.target?.let(onOpenVideo)
                }
            }
        )
    }
}

@Composable
private fun LastSeenBoundaryCard(enabled: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("上次看到这里", style = MaterialTheme.typography.bodyLarge)
            Text("点击刷新", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FeedCard(
    item: FeedItem,
    onOpenSpace: (SpaceRoute) -> Unit,
    dislikedReason: String?,
    onDislike: (FeedItem, ThreePointReason) -> Unit,
    onCancelDislike: (FeedItem) -> Unit,
    onClick: () -> Unit
) {
    val isDisliked = dislikedReason != null
    val isDynamic = item.cardGoto == "dynamic"
    val canOpen = !isDisliked && (item.target != null || item.liveRoute != null || isDynamic)
    Card(
        onClick = onClick,
        enabled = canOpen,
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
                if (dislikedReason != null) {
                    DislikedOverlay(
                        reason = dislikedReason,
                        onUndo = { onCancelDislike(item) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    FeedCoverMetadata(item, Modifier.align(Alignment.BottomCenter))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                val threePoint = item.threePointV2
                val hasMoreMenu = !isDisliked && !threePoint.isNullOrEmpty()
                val recommendationReason = item.rcmdReason?.takeIf { it.text.isNotEmpty() }
                val spaceRoute = remember(item.args, item.target) {
                    item.args?.let { args ->
                        if (args.upId <= 0L && args.upName.isNullOrBlank()) {
                            null
                        } else {
                            SpaceRoute(
                                mid = args.upId,
                                name = args.upName,
                                fromViewAid = args.aid.takeIf { it > 0L }
                                    ?: (item.target as? VideoTarget.Ugc)?.aid?.takeIf { it > 0L }
                            )
                        }
                    }
                }
                Text(
                    text = item.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val upName = item.descButton?.text ?: item.args?.upName ?: ""
                    if (upName.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (spaceRoute == null || isDisliked) Modifier
                                    else Modifier.clickable { onOpenSpace(spaceRoute) }
                                ),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UploaderBadge()
                            Text(
                                text = upName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (hasMoreMenu && recommendationReason == null) {
                        MoreMenu(
                            item = item,
                            items = threePoint.orEmpty(),
                            onDislike = onDislike
                        )
                    }
                }

                recommendationReason?.let { reason ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reason.text,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.shapes.extraSmall
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasMoreMenu) {
                            MoreMenu(
                                item = item,
                                items = threePoint.orEmpty(),
                                onDislike = onDislike
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedCoverMetadata(item: FeedItem, modifier: Modifier = Modifier) {
    val views = item.coverLeftText1?.takeIf(String::isNotBlank)
    val danmaku = item.coverLeftText2?.takeIf(String::isNotBlank)
        .takeIf { item.target != null }
    val duration = item.coverRightText?.takeIf(String::isNotBlank)
    if (views == null && danmaku == null && duration == null) return

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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                views?.let {
                    CoverMetric(
                        icon = if (item.target != null) Icons.Outlined.PlayCircleOutline else Icons.Outlined.Visibility,
                        label = if (item.target != null) "播放" else "观看",
                        value = it,
                        modifier = Modifier.weight(1.2f, fill = false)
                    )
                }
                danmaku?.let {
                    CoverMetric(
                        icon = Icons.Outlined.Subtitles,
                        label = "弹幕",
                        value = it,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            duration?.let {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = it,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CoverMetric(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoreMenu(
    item: FeedItem,
    items: List<ThreePointItem>,
    onDislike: (FeedItem, ThreePointReason) -> Unit
) {
    var show by remember { mutableStateOf(false) }
    IconButton(
        onClick = { show = true },
        modifier = Modifier.size(24.dp)
    ) {
        Icon(Icons.Default.MoreVert, contentDescription = null)
    }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = { show = false }) { Text("取消") }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    items.forEachIndexed { index, menuItem ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        TextButton(
                            onClick = { show = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(menuItem.title, style = MaterialTheme.typography.bodyLarge)
                        }
                        val options = menuItem.reasons.orEmpty() + menuItem.feedbacks.orEmpty()
                        if (options.isNotEmpty()) {
                            options.chunked(2).forEach { pair ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    pair.forEach { reason ->
                                        TextButton(
                                            onClick = {
                                                show = false
                                                onDislike(item, reason)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                reason.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun DislikedOverlay(
    reason: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onUndo) {
                Text("撤回")
            }
        }
    }
}

private fun FeedItem.actionKey(): String {
    return "$goto|$param|$idx"
}
