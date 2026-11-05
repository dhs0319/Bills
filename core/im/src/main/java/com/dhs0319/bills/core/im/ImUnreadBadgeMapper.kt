package com.dhs0319.bills.core.im

import com.bapis.bilibili.app.im.v1.Unread
import com.bapis.bilibili.app.im.v1.UnreadStyle
import com.dhs0319.bills.core.model.ImUnreadBadge

internal fun Unread.toImUnreadBadge(): ImUnreadBadge = when (style) {
    UnreadStyle.UNREAD_STYLE_DOT -> ImUnreadBadge.Dot
    UnreadStyle.UNREAD_STYLE_NUMBER -> if (number > 0L) {
        ImUnreadBadge.Count(
            count = number,
            text = numberShow.ifBlank { if (number > 99L) "99+" else number.toString() }
        )
    } else {
        ImUnreadBadge.None
    }
    else -> ImUnreadBadge.None
}
