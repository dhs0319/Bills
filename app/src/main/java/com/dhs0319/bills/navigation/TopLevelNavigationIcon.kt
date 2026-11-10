package com.dhs0319.bills.navigation

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.model.ImUnreadBadge

@Composable
internal fun TopLevelNavigationIcon(
    tab: TopLevelRoute,
    selected: Boolean,
    messageUnreadBadge: ImUnreadBadge,
    modifier: Modifier = Modifier,
    contentDescription: String? = tab.label,
    tint: Color? = null,
    onClick: (() -> Unit)? = null
) {
    // Keep the badge outside button clipping, anchored to the centered 24 dp icon
    // inside the button's 48 dp touch target.
    val badgeModifier = if (onClick != null) {
        Modifier.offset(x = (-12).dp, y = 12.dp)
    } else {
        Modifier
    }
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = contentDescription,
            tint = tint ?: LocalContentColor.current
        )
    }
    BadgedBox(
        modifier = modifier,
        badge = {
            if (tab == TopLevelRoute.MESSAGE) {
                when (val unread = messageUnreadBadge) {
                    ImUnreadBadge.None -> Unit
                    ImUnreadBadge.Dot -> Badge(
                        modifier = badgeModifier.clearAndSetSemantics {
                            this.contentDescription = "有未读消息"
                        }
                    )
                    is ImUnreadBadge.Count -> Badge(
                        modifier = badgeModifier.clearAndSetSemantics {
                            this.contentDescription = "${unread.count} 条未读消息"
                        }
                    ) {
                        Text(text = unread.text, maxLines = 1)
                    }
                }
            }
        }
    ) {
        when {
            onClick == null -> icon()
            selected -> FilledIconButton(onClick = onClick, content = icon)
            else -> IconButton(onClick = onClick, content = icon)
        }
    }
}
