package com.dhs0319.bills.feature.history.component

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCard
import com.dhs0319.bills.core.designsystem.component.UploaderBadge
import com.dhs0319.bills.core.designsystem.component.rememberHighlightedTitle
import com.dhs0319.bills.core.model.HistoryItem

@Composable
fun HistoryItemCard(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val titleText = rememberHighlightedTitle(
        text = item.title,
        highlightColor = MaterialTheme.colorScheme.primary
    )
    val ownerName = item.ownerName?.takeIf { it.isNotBlank() }
    val infoLine = remember(item, ownerName) { buildInfoLine(item, ownerName != null) }
    val metaLine = remember(item) { buildMetaLine(item) }
    val progress = remember(item) { progressText(item) }
    val progressFraction = remember(item) {
        item.durationSec?.takeIf { it > 0L }?.let { duration ->
            val watched = item.progressSec ?: 0L
            if (watched < 0L) 1f else (watched.toFloat() / duration).coerceIn(0f, 1f)
        }
    }
    val coverStatus = remember(item, progress) {
        if (item.type == "live") {
            item.badge?.takeIf { it.isNotBlank() }
        } else {
            progress
        }
    }

    HorizontalVideoCard(
        title = titleText,
        coverUrl = item.cover,
        durationText = null,
        onClick = onClick.takeIf { item.isOpenable },
        modifier = modifier,
        isTablet = isTablet,
        coverBottomContent = progressFraction?.let { fraction ->
            {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            }
        },
        coverOverlay = { coverWidth ->
            Text(
                text = historyCategory(item),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1
            )
            coverStatus?.let { text ->
                Text(
                    text = text,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Color.Black.copy(alpha = 0.68f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (coverWidth < 100.dp) 8.sp else 11.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (ownerName != null || infoLine.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ownerName?.let { name ->
                        Row(
                            modifier = if (infoLine.isNotBlank()) {
                                Modifier.weight(1f, fill = false)
                            } else {
                                Modifier
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            UploaderBadge()
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (ownerName != null && infoLine.isNotBlank()) {
                        Text(
                            text = " · $infoLine",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (ownerName == null && infoLine.isNotBlank()) {
                        Text(
                            text = infoLine,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metaLine,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                onDelete?.let { HistoryMoreMenu(onDelete = it) }
            }
        }
    }
}

@Composable
private fun HistoryMoreMenu(onDelete: () -> Unit) {
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
                text = { Text("删除记录") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

private fun historyCategory(item: HistoryItem): String {
    return when (item.type) {
        "live" -> "直播"
        "article" -> "专栏"
        else -> "视频"
    }
}

private fun progressText(item: HistoryItem): String? {
    val progress = item.progressSec ?: return null
    if (progress < 0L) return "已看完"
    val duration = item.durationSec?.takeIf { it > 0L }
        ?: return null
    if (progress >= duration) return "已看完"
    return "${formatVideoDuration(progress)}/${formatVideoDuration(duration)}"
}

private fun buildInfoLine(item: HistoryItem, hasOwner: Boolean): String {
    return listOfNotNull(
        item.badge?.takeIf {
            it.isNotBlank() && item.type != "live" && !isViewCountText(it)
        },
        item.subtitle?.takeIf { it.isNotBlank() && !isViewCountText(it) }
    ).joinToString(" · ").ifBlank { item.typeLabel.takeIf { !hasOwner }.orEmpty() }
}

private fun isViewCountText(value: String): Boolean {
    return value.contains("播放") || value.contains("观看")
}

private fun buildMetaLine(item: HistoryItem): String {
    return DateFormat.format("MM-dd HH:mm", item.viewedAtSec * 1000).toString()
}
