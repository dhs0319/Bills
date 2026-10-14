package com.dhs0319.bills.feature.dynamic.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.UpListRow
import com.dhs0319.bills.core.model.DynamicBody
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.DynamicImage
import com.dhs0319.bills.core.model.DynamicItem
import com.dhs0319.bills.core.model.DynamicUpList
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.comment.component.CommentRichText
import com.dhs0319.bills.feature.dynamic.component.DynamicStatsRow

@Composable
fun DynamicFeed(
    currentMid: Long,
    upList: DynamicUpList?,
    items: List<DynamicItem>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    errorMessage: String?,
    loadMoreError: String?,
    onRetryRefresh: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenLive: (LiveRoute) -> Unit,
    onOpenDynamic: (String) -> Unit,
    onAddToWatchLater: (Long) -> Unit,
    onUnfollow: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        upList?.let { data ->
            item(
                key = "dynamic_up_list",
                contentType = "dynamic_up_list"
            ) {
                UpListRow(
                    title = data.title,
                    items = data.items,
                    key = { it.uid },
                    name = { it.name },
                    face = { it.face },
                    onClick = { item ->
                        onOpenSpace(SpaceRoute(mid = item.uid, name = item.name))
                    }
                )
            }
        }

        items(
            items = items,
            key = { it.id },
            contentType = { it.type }
        ) { item ->
            DynamicCard(
                item = item,
                currentMid = currentMid,
                onOpenVideo = onOpenVideo,
                onOpenSpace = onOpenSpace,
                onOpenLive = onOpenLive,
                onOpenDynamic = onOpenDynamic,
                onAddToWatchLater = onAddToWatchLater,
                onUnfollow = onUnfollow
            )
        }

        if (isLoadingMore) {
            item(
                key = "dynamic_loading_more",
                contentType = "loading"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (errorMessage != null && items.isNotEmpty()) {
            item(
                key = "dynamic_error",
                contentType = "error"
            ) {
                StateMessageCard(
                    text = errorMessage.ifBlank { "加载动态失败" },
                    isError = true,
                    actionText = "点击重试",
                    onAction = onRetryRefresh
                )
            }
        }

        if (!isLoadingMore && loadMoreError != null) {
            item(
                key = "dynamic_load_more_error",
                contentType = "error"
            ) {
                StateMessageCard(
                    text = loadMoreError.ifBlank { "加载动态失败" },
                    isError = true,
                    actionText = "点击重试加载更多",
                    onAction = onRetryLoadMore
                )
            }
        }
    }
}

@Composable
private fun DynamicCard(
    item: DynamicItem,
    currentMid: Long,
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenLive: (LiveRoute) -> Unit,
    onOpenDynamic: (String) -> Unit,
    onAddToWatchLater: (Long) -> Unit,
    onUnfollow: (Long) -> Unit
) {
    val liveRoute = item.liveRoute
    val videoTarget = item.videoTarget
    val onClick = {
        when {
            liveRoute != null -> onOpenLive(liveRoute)
            videoTarget != null -> onOpenVideo(videoTarget)
            else -> onOpenDynamic(item.id)
        }
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        DynamicCardContent(
            item = item,
            currentMid = currentMid,
            onOpenSpace = onOpenSpace,
            onOpenContent = onClick,
            onAddToWatchLater = onAddToWatchLater,
            onUnfollow = onUnfollow
        )
    }
}

@Composable
private fun DynamicCardContent(
    item: DynamicItem,
    currentMid: Long,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenContent: () -> Unit,
    onAddToWatchLater: (Long) -> Unit,
    onUnfollow: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DynamicHeader(
            item = item,
            currentMid = currentMid,
            onOpenSpace = onOpenSpace,
            onAddToWatchLater = onAddToWatchLater,
            onUnfollow = onUnfollow
        )
        DynamicBodyContent(item = item, onOpenContent = onOpenContent)
        item.stats?.let { stats ->
            DynamicStatsRow(stats = stats, showZeroCounts = true, distributeEvenly = true)
        }
    }
}

@Composable
private fun DynamicHeader(
    item: DynamicItem,
    currentMid: Long,
    onOpenSpace: (SpaceRoute) -> Unit,
    onAddToWatchLater: (Long) -> Unit,
    onUnfollow: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            url = item.author?.avatar,
            contentDescription = item.author?.name ?: "用户",
            modifier = Modifier.size(42.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            val route = item.spaceRoute
            Text(
                text = item.author?.name ?: "动态",
                style = MaterialTheme.typography.titleSmall,
                modifier = if (route == null) {
                    Modifier
                } else {
                    Modifier.clickable { onOpenSpace(route) }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOfNotNull(item.publishedText, item.author?.pubLocation)
                .joinToString(" · ")
                .ifBlank { item.type }
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val watchLaterAid = item.videoTarget?.watchLaterAid()
        val unfollowMid = item.author?.mid?.takeIf { it > 0L && it != currentMid }
        if (watchLaterAid != null || unfollowMid != null) {
            DynamicMoreMenu(
                watchLaterAid = watchLaterAid,
                unfollowMid = unfollowMid,
                authorName = item.author?.name,
                onAddToWatchLater = onAddToWatchLater,
                onUnfollow = onUnfollow
            )
        }
    }
}

private fun VideoTarget.watchLaterAid(): Long? = when (this) {
    is VideoTarget.Ugc -> aid
    is VideoTarget.Pgc -> aid
    is VideoTarget.Pugv -> aid
}.takeIf { it > 0L }

@Composable
private fun DynamicMoreMenu(
    watchLaterAid: Long?,
    unfollowMid: Long?,
    authorName: String?,
    onAddToWatchLater: (Long) -> Unit,
    onUnfollow: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmUnfollow by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多操作",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            watchLaterAid?.let { aid ->
                DropdownMenuItem(
                    text = { Text("稍后再看") },
                    leadingIcon = { Icon(Icons.Outlined.WatchLater, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onAddToWatchLater(aid)
                    }
                )
            }
            if (unfollowMid != null) {
                DropdownMenuItem(
                    text = { Text("取消关注") },
                    leadingIcon = { Icon(Icons.Outlined.PersonRemove, contentDescription = null) },
                    onClick = {
                        expanded = false
                        confirmUnfollow = true
                    }
                )
            }
        }
    }

    if (confirmUnfollow && unfollowMid != null) {
        AlertDialog(
            onDismissRequest = { confirmUnfollow = false },
            title = { Text("取消关注") },
            text = { Text("确定要取消关注${authorName?.takeIf(String::isNotBlank) ?: "该用户"}吗？") },
            dismissButton = {
                TextButton(onClick = { confirmUnfollow = false }) {
                    Text("取消")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnfollow = false
                    onUnfollow(unfollowMid)
                }) {
                    Text("取消关注")
                }
            }
        )
    }
}

@Composable
private fun DynamicBodyContent(item: DynamicItem, onOpenContent: () -> Unit) {
    when (val body = item.body) {
        is DynamicBody.Text -> {
            DynamicText(body.text, item.emotes, onOpenContent)
        }

        is DynamicBody.Draw -> {
            body.text?.let { text ->
                DynamicText(text, item.emotes, onOpenContent)
            }
            if (body.images.isNotEmpty()) {
                DynamicImageRow(body.images)
            }
        }

        is DynamicBody.Archive -> {
            body.text?.let { text ->
                DynamicText(text, item.emotes, onOpenContent)
            }
            DynamicMediaCard(
                title = body.title,
                subTitle = body.subTitle,
                cover = body.cover,
                badge = body.badge
            )
        }

        is DynamicBody.Article -> {
            body.text?.let { text ->
                DynamicText(text, item.emotes, onOpenContent)
            }
            DynamicMediaCard(
                title = body.title,
                subTitle = body.subTitle,
                cover = body.cover,
                badge = null
            )
        }

        is DynamicBody.Live -> {
            body.text?.let { text ->
                DynamicText(text, item.emotes, onOpenContent)
            }
            DynamicMediaCard(
                title = body.title,
                subTitle = body.subTitle,
                cover = body.cover,
                badge = body.badge
            )
        }

        is DynamicBody.Forward -> {
            body.text?.let { text ->
                DynamicText(text, item.emotes, onOpenContent)
            }
            body.origin?.let { origin ->
                val forwardBgColor = MaterialTheme.colorScheme.surface
                val forwardBgShape = MaterialTheme.shapes.medium
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = forwardBgColor, shape = forwardBgShape)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    origin.authorName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    origin.bodyText?.let { text ->
                        DynamicText(text, origin.emotes, onOpenContent)
                    }
                    if (!origin.title.isNullOrBlank() || !origin.cover.isNullOrBlank()) {
                        DynamicMediaCard(
                            title = origin.title.orEmpty(),
                            subTitle = null,
                            cover = origin.cover,
                            badge = origin.badge
                        )
                    }
                }
            }
        }

        is DynamicBody.Unknown -> {
            body.text?.let { text ->
                DynamicText(text, item.emotes, onOpenContent)
            }
        }
    }
}

@Composable
private fun DynamicMediaCard(
    title: String,
    subTitle: String?,
    cover: String?,
    badge: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cover?.let {
            CoverImage(
                url = it,
                modifier = Modifier
                    .width(132.dp)
                    .aspectRatio(16f / 10f),
                shape = MaterialTheme.shapes.small
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subTitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            badge?.let {
                val badgeBgColor = MaterialTheme.colorScheme.secondaryContainer
                val badgeBgShape = MaterialTheme.shapes.extraSmall
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(badgeBgColor, badgeBgShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DynamicImageRow(images: List<DynamicImage>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(
            items = images,
            key = { it.url }
        ) { image ->
            CoverImage(
                url = image.url,
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(image.displayAspectRatio()),
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@Composable
private fun DynamicText(text: String, emotes: List<CommentEmote>, onTap: () -> Unit) {
    if (text.isBlank()) return
    CommentRichText(
        text = text,
        emotes = emotes,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier,
        maxLines = 8,
        overflow = TextOverflow.Ellipsis,
        onTap = onTap
    )
}

private fun DynamicImage.displayAspectRatio(): Float {
    return if (width > 0 && height > 0) {
        (width.toFloat() / height.toFloat()).coerceIn(0.72f, 1.5f)
    } else {
        1f
    }
}

