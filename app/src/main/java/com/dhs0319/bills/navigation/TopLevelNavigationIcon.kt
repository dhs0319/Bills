package com.dhs0319.bills.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.dhs0319.bills.core.model.ImUnreadBadge

@Composable
internal fun TopLevelNavigationIcon(
    tab: TopLevelRoute,
    selected: Boolean,
    messageUnreadBadge: ImUnreadBadge,
    modifier: Modifier = Modifier,
    contentDescription: String? = tab.label,
    tint: Color? = null
) {
    val icon: @Composable (Modifier) -> Unit = { iconModifier ->
        Icon(
            modifier = iconModifier,
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = contentDescription,
            tint = tint ?: LocalContentColor.current
        )
    }
    if (tab != TopLevelRoute.MESSAGE || messageUnreadBadge == ImUnreadBadge.None) {
        icon(modifier)
        return
    }

    BadgedBox(
        modifier = modifier,
        badge = {
            when (val unread = messageUnreadBadge) {
                ImUnreadBadge.None -> Unit
                ImUnreadBadge.Dot -> Badge(
                    modifier = Modifier.clearAndSetSemantics {
                        this.contentDescription = "有未读消息"
                    }
                )
                is ImUnreadBadge.Count -> Badge(
                    modifier = Modifier.clearAndSetSemantics {
                        this.contentDescription = "${unread.count} 条未读消息"
                    }
                ) {
                    Text(text = unread.text, maxLines = 1)
                }
            }
        }
    ) {
        icon(Modifier)
    }
}
