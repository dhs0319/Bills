package com.dhs0319.bills.core.designsystem.component

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.dhs0319.bills.core.model.ImUnreadBadge

@Composable
fun ImUnreadBadgedBox(
    unread: ImUnreadBadge,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    if (unread == ImUnreadBadge.None) {
        content(modifier)
        return
    }

    BadgedBox(
        modifier = modifier,
        badge = { ImUnreadBadgeIndicator(unread) }
    ) {
        content(Modifier)
    }
}

@Composable
fun ImUnreadBadgeIndicator(unread: ImUnreadBadge, modifier: Modifier = Modifier) {
    when (unread) {
        ImUnreadBadge.None -> Unit
        ImUnreadBadge.Dot -> Badge(
            modifier = modifier.clearAndSetSemantics {
                contentDescription = "有未读消息"
            }
        )
        is ImUnreadBadge.Count -> Badge(
            modifier = modifier.clearAndSetSemantics {
                contentDescription = "${unread.count} 条未读消息"
            }
        ) {
            Text(text = unread.text, maxLines = 1)
        }
    }
}
