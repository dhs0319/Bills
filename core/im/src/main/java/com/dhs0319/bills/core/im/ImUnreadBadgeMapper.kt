package com.dhs0319.bills.core.im

import com.bapis.bilibili.app.im.v1.GetQuickLinkUnreadReply
import com.bapis.bilibili.app.im.v1.QuickLinkItemType
import com.bapis.bilibili.app.im.v1.Unread
import com.bapis.bilibili.app.im.v1.UnreadStyle
import com.dhs0319.bills.core.model.ImNotificationBadges
import com.dhs0319.bills.core.model.ImUnreadBadge

internal fun Unread.toImUnreadBadge(): ImUnreadBadge = when (style) {
    UnreadStyle.UNREAD_STYLE_DOT -> ImUnreadBadge.Dot
    UnreadStyle.UNREAD_STYLE_NUMBER -> if (number > 0L) {
        ImUnreadBadge.Count(
            count = number,
            text = numberShow.ifBlank { number.toBadgeText() }
        )
    } else {
        ImUnreadBadge.None
    }
    else -> ImUnreadBadge.None
}

internal fun GetQuickLinkUnreadReply.toImNotificationBadges(): ImNotificationBadges {
    val unreadByType = itemsList.associate { it.itemType to it.unread.toImUnreadBadge() }
    return ImNotificationBadges(
        repliesAndMentions = unreadByType[QuickLinkItemType.QUICK_LINK_ITEM_TYPE_REPLY]
            ?: mergeUnreadBadges(
                unreadByType[QuickLinkItemType.QUICK_LINK_ITEM_TYPE_OLD_REPLY],
                unreadByType[QuickLinkItemType.QUICK_LINK_ITEM_TYPE_AT]
            ),
        likes = unreadByType[QuickLinkItemType.QUICK_LINK_ITEM_TYPE_LIKE]
            ?: unreadByType[QuickLinkItemType.QUICK_LINK_ITEM_TYPE_OLD_LIKE]
            ?: ImUnreadBadge.None,
        followers = unreadByType[QuickLinkItemType.QUICK_LINK_ITEM_TYPE_FOLLOW] ?: ImUnreadBadge.None
    )
}

private fun mergeUnreadBadges(reply: ImUnreadBadge?, mention: ImUnreadBadge?): ImUnreadBadge = when {
    reply == ImUnreadBadge.Dot || mention == ImUnreadBadge.Dot -> ImUnreadBadge.Dot
    reply is ImUnreadBadge.Count && mention is ImUnreadBadge.Count -> {
        val count = reply.count + mention.count.coerceAtMost(Long.MAX_VALUE - reply.count)
        ImUnreadBadge.Count(count, count.toBadgeText())
    }
    reply is ImUnreadBadge.Count -> reply
    mention is ImUnreadBadge.Count -> mention
    else -> ImUnreadBadge.None
}

private fun Long.toBadgeText(): String = if (this > 99L) "99+" else toString()
