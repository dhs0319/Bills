package com.dhs0319.bills.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dhs0319.bills.core.designsystem.component.ImUnreadBadgedBox
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
    ImUnreadBadgedBox(
        unread = if (tab == TopLevelRoute.MESSAGE) messageUnreadBadge else ImUnreadBadge.None,
        modifier = modifier
    ) { iconModifier ->
        Icon(
            modifier = iconModifier,
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = contentDescription,
            tint = tint ?: LocalContentColor.current
        )
    }
}
