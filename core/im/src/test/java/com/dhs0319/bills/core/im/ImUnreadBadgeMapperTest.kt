package com.dhs0319.bills.core.im

import com.bapis.bilibili.app.im.v1.GetQuickLinkUnreadReply
import com.bapis.bilibili.app.im.v1.QuickLinkItemType
import com.bapis.bilibili.app.im.v1.QuickLinkItemType.QUICK_LINK_ITEM_TYPE_AT
import com.bapis.bilibili.app.im.v1.QuickLinkItemType.QUICK_LINK_ITEM_TYPE_FOLLOW
import com.bapis.bilibili.app.im.v1.QuickLinkItemType.QUICK_LINK_ITEM_TYPE_LIKE
import com.bapis.bilibili.app.im.v1.QuickLinkItemType.QUICK_LINK_ITEM_TYPE_OLD_LIKE
import com.bapis.bilibili.app.im.v1.QuickLinkItemType.QUICK_LINK_ITEM_TYPE_OLD_REPLY
import com.bapis.bilibili.app.im.v1.QuickLinkItemType.QUICK_LINK_ITEM_TYPE_REPLY
import com.bapis.bilibili.app.im.v1.QuickLinkItemType.QUICK_LINK_ITEM_TYPE_SYSTEM
import com.bapis.bilibili.app.im.v1.QuickLinkUnreadItem
import com.bapis.bilibili.app.im.v1.Unread
import com.bapis.bilibili.app.im.v1.UnreadStyle
import com.dhs0319.bills.core.model.ImNotificationBadges
import com.dhs0319.bills.core.model.ImUnreadBadge
import org.junit.Assert.assertEquals
import org.junit.Test

class ImUnreadBadgeMapperTest {
    @Test
    fun mapsCurrentEntriesWithoutDoubleCountingLegacyItems() {
        val result = GetQuickLinkUnreadReply.newBuilder()
            .addItems(count(QUICK_LINK_ITEM_TYPE_REPLY, 103, "99+"))
            .addItems(count(QUICK_LINK_ITEM_TYPE_OLD_REPLY, 71))
            .addItems(count(QUICK_LINK_ITEM_TYPE_AT, 32))
            .addItems(item(QUICK_LINK_ITEM_TYPE_LIKE, Unread.getDefaultInstance()))
            .addItems(count(QUICK_LINK_ITEM_TYPE_OLD_LIKE, 8))
            .addItems(
                item(
                    QUICK_LINK_ITEM_TYPE_FOLLOW,
                    Unread.newBuilder().setStyle(UnreadStyle.UNREAD_STYLE_DOT).build()
                )
            )
            .addItems(count(QUICK_LINK_ITEM_TYPE_SYSTEM, 1))
            .build()
            .toImNotificationBadges()

        assertEquals(
            ImNotificationBadges(
                repliesAndMentions = ImUnreadBadge.Count(103, "99+"),
                followers = ImUnreadBadge.Dot
            ),
            result
        )
    }

    @Test
    fun combinesLegacyRepliesAndMentionsWhenTheCurrentEntryIsMissing() {
        val result = GetQuickLinkUnreadReply.newBuilder()
            .addItems(count(QUICK_LINK_ITEM_TYPE_OLD_REPLY, 75))
            .addItems(count(QUICK_LINK_ITEM_TYPE_AT, 33))
            .addItems(count(QUICK_LINK_ITEM_TYPE_OLD_LIKE, 4))
            .build()
            .toImNotificationBadges()

        assertEquals(
            ImNotificationBadges(
                repliesAndMentions = ImUnreadBadge.Count(108, "99+"),
                likes = ImUnreadBadge.Count(4, "4")
            ),
            result
        )
    }

    private fun count(type: QuickLinkItemType, count: Long, text: String = "") = item(
        type,
        Unread.newBuilder()
            .setStyle(UnreadStyle.UNREAD_STYLE_NUMBER)
            .setNumber(count)
            .setNumberShow(text)
            .build()
    )

    private fun item(type: QuickLinkItemType, unread: Unread): QuickLinkUnreadItem =
        QuickLinkUnreadItem.newBuilder().setItemType(type).setUnread(unread).build()
}
