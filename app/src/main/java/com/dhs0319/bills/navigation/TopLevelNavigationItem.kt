package com.dhs0319.bills.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.model.ImUnreadBadge

@Composable
internal fun TopLevelNavigationItem(
    tab: TopLevelRoute,
    selected: Boolean,
    messageUnreadBadge: ImUnreadBadge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indicatorAlpha = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "top level menu selection"
    )
    val indicatorColor = MaterialTheme.colorScheme.secondaryContainer
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .drawBehind {
                    // Read animation state during drawing, without a separate alpha layer.
                    val alpha = indicatorAlpha.value
                    if (alpha > 0f) {
                        val radius = size.minDimension / 2f
                        drawRoundRect(
                            color = indicatorColor.copy(alpha = indicatorColor.alpha * alpha),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            TopLevelNavigationIcon(
                tab = tab,
                selected = selected,
                messageUnreadBadge = messageUnreadBadge,
                modifier = Modifier.size(24.dp),
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Text(
            text = tab.label,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
