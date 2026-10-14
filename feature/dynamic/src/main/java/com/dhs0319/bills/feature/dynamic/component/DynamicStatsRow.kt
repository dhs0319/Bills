package com.dhs0319.bills.feature.dynamic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.model.DynamicStats

@Composable
internal fun DynamicStatsRow(
    stats: DynamicStats,
    modifier: Modifier = Modifier,
    showZeroCounts: Boolean = false,
    distributeEvenly: Boolean = false
) {
    if (!showZeroCounts && stats.repost <= 0 && stats.reply <= 0 && stats.like <= 0) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (distributeEvenly) 8.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val itemModifier = if (distributeEvenly) Modifier.weight(1f) else Modifier
        if (showZeroCounts || stats.repost > 0) {
            DynamicStat(Icons.AutoMirrored.Outlined.OpenInNew, "转发", stats.repost, itemModifier, distributeEvenly)
        }
        if (showZeroCounts || stats.reply > 0) {
            DynamicStat(Icons.Outlined.ChatBubbleOutline, "评论", stats.reply, itemModifier, distributeEvenly)
        }
        if (showZeroCounts || stats.like > 0) {
            DynamicStat(Icons.Outlined.ThumbUpOffAlt, "点赞", stats.like, itemModifier, distributeEvenly)
        }
    }
}

@Composable
private fun DynamicStat(
    icon: ImageVector,
    label: String,
    count: Long,
    modifier: Modifier,
    centerContent: Boolean
) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label $count" },
        horizontalArrangement = if (centerContent) Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
