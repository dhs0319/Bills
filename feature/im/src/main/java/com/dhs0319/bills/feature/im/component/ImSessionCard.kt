package com.dhs0319.bills.feature.im.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.ImUnreadBadgeIndicator
import com.dhs0319.bills.core.model.ImSessionItem
import com.dhs0319.bills.core.model.ImUnreadBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImSessionCard(
    item: ImSessionItem,
    nowMillis: State<Long>,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        val summaryText = remember(
            item.summary,
            item.isPinned,
            item.isMuted,
            item.sessionTypeLabel
        ) {
            buildSummary(item)
        }
        val unread = when (val text = item.unreadText) {
            null -> ImUnreadBadge.None
            "•" -> ImUnreadBadge.Dot
            else -> ImUnreadBadge.Count(item.unreadCount, text)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(
                url = item.avatar,
                contentDescription = item.name,
                modifier = Modifier.size(48.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ImSessionTimeLabel(timeMicros = item.timeMicros, nowMillis = nowMillis)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summaryText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    ImUnreadBadgeIndicator(
                        unread = unread,
                        modifier = if (unread is ImUnreadBadge.Count) {
                            Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                        } else Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun ImSessionTimeLabel(timeMicros: Long, nowMillis: State<Long>) {
    if (timeMicros <= 0L) return
    val timeText by remember(timeMicros, nowMillis) {
        derivedStateOf(structuralEqualityPolicy()) {
            formatImSessionTime(timeMicros, nowMillis.value)
        }
    }
    Text(
        text = timeText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

private fun buildSummary(item: ImSessionItem): String {
    val prefix = buildList {
        if (item.isPinned) add("置顶")
        if (item.isMuted) add("免打扰")
        item.sessionTypeLabel?.let(::add)
    }.joinToString(" · ")
    return if (prefix.isBlank()) item.summary else "$prefix · ${item.summary}"
}
