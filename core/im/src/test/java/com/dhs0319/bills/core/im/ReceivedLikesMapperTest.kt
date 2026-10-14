package com.dhs0319.bills.core.im

import com.bapis.bilibili.app.dynamic.v2.LinkNode
import com.bapis.bilibili.app.dynamic.v2.Paragraph
import com.bapis.bilibili.app.dynamic.v2.TextNode
import com.bapis.bilibili.app.dynamic.v2.TextParagraph
import com.bapis.bilibili.app.dynamic.v2.WordNode
import com.bapis.bilibili.im.gateway.interfaces.v1.CoinCard
import com.bapis.bilibili.im.gateway.interfaces.v1.CommonMsgCard
import com.bapis.bilibili.im.gateway.interfaces.v1.Cursor
import com.bapis.bilibili.im.gateway.interfaces.v1.FavoriteCard
import com.bapis.bilibili.im.gateway.interfaces.v1.LikeCard
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgFeedFilterType
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgFeedMsgCard
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgItem
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgListLimit
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgListRsp
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgTabType
import com.bapis.bilibili.im.gateway.interfaces.v1.RichTextContent
import com.bapis.bilibili.im.gateway.interfaces.v1.User
import com.dhs0319.bills.core.model.im.MsgFeedCursor
import com.dhs0319.bills.core.model.im.ReceivedLikeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceivedLikesMapperTest {
    @Test
    fun requestsReceivedLikesAndRoundTripsAllCursorFields() {
        val first = receivedLikesRequest(null, 20)
        assertEquals(MsgTabType.ReceiveLike, first.tabType)
        assertEquals(MsgFeedFilterType.MsgFeed_All, first.filterType)
        assertEquals(20L, first.pagesize)
        assertFalse(first.hasCursor())

        val cursor = MsgFeedCursor(id = 21, time = 123456, replymsgid = 42, isEnd = false)
        val page = MsgListRsp.newBuilder()
            .setCursor(receivedLikesRequest(cursor, 20).cursor)
            .build().toReceivedLikesPage()
        assertEquals(cursor, page.cursor)
    }

    @Test
    fun mapsUsersRichTextContentAndTimestampFallback() {
        val message = CommonMsgCard.newBuilder()
            .setMsgTime(123456)
            .setCounts(3)
            .setNativeUri("bilibili://video/123?comment_root_id=456&comment_secondary_id=789")
            .setItemId(789)
            .setOriginId(123)
            .setBizType("reply")
            .setCoverText("我的评论")
            .setCoverImage("http://i0.hdslb.com/cover.jpg")
            .addUsers(User.newBuilder().setMid(7).setNickname("小明").setAvatar("//i0.hdslb.com/avatar.jpg"))
            .setRichTextContent(
                RichTextContent.newBuilder().addParagraphs(
                    Paragraph.newBuilder().setText(
                        TextParagraph.newBuilder()
                            .addNodes(TextNode.newBuilder().setLink(LinkNode.newBuilder().setShowText(WordNode.newBuilder().setWords("小明"))))
                            .addNodes(TextNode.newBuilder().setWord(WordNode.newBuilder().setWords("等3人")))
                            .addNodes(TextNode.newBuilder().setRawText("赞了你的评论"))
                    )
                )
            )
            .build()
        val item = MsgListRsp.newBuilder().addMsgCards(card(1, ReceivedLikeType.LIKE, message))
            .build().toReceivedLikesPage().items.single()
        assertEquals("小明等3人赞了你的评论", item.description)
        assertEquals("我的评论", item.coverText)
        assertEquals("https://i0.hdslb.com/cover.jpg", item.coverImage)
        assertEquals("https://i0.hdslb.com/avatar.jpg", item.users.single().avatar)
        assertEquals(7L, item.users.single().mid)
        assertEquals(3L, item.count)
        assertEquals(123456L, item.msgTime)
        assertEquals("bilibili://video/123?comment_root_id=456&comment_secondary_id=789", item.nativeUri)
        assertEquals(789L, item.itemId)
        assertEquals(123L, item.originId)
        assertEquals("reply", item.bizType)
    }

    @Test
    fun deduplicatesPinnedCardsButKeepsDifferentInteractionTypesWithSameId() {
        val like = card(1, ReceivedLikeType.LIKE)
        val page = MsgListRsp.newBuilder()
            .addTopCards(like)
            .addMsgCards(like)
            .addMsgCards(card(1, ReceivedLikeType.COIN))
            .addMsgCards(card(1, ReceivedLikeType.FAVORITE))
            .addMsgCards(MsgFeedMsgCard.getDefaultInstance())
            .build().toReceivedLikesPage()
        assertEquals(ReceivedLikeType.entries, page.items.map { it.type })
        assertEquals(3, page.items.map { it.key }.distinct().size)
    }

    @Test
    fun toleratesMissingUserImageAndRichText() {
        val item = MsgListRsp.newBuilder().addMsgCards(card(1, ReceivedLikeType.LIKE))
            .build().toReceivedLikesPage().items.single()
        assertTrue(item.users.isEmpty())
        assertEquals("", item.description)
        assertNull(item.coverImage)
    }

    @Test
    fun stopsPaginationWhenLimitedAndPreservesExplanation() {
        val page = MsgListRsp.newBuilder()
            .setCursor(Cursor.newBuilder().setId(10).setIsEnd(false))
            .setLimit(MsgListLimit.newBuilder().setLimit(true).setDesc("仅展示近期通知"))
            .build().toReceivedLikesPage()
        assertNull(page.cursor)
        assertEquals("仅展示近期通知", page.notice)
    }

    private fun card(
        id: Long,
        type: ReceivedLikeType,
        message: CommonMsgCard = CommonMsgCard.getDefaultInstance()
    ): MsgFeedMsgCard {
        val item = MsgItem.newBuilder().apply {
            when (type) {
                ReceivedLikeType.LIKE -> setLikeCard(LikeCard.newBuilder().setLikeMsg(message))
                ReceivedLikeType.COIN -> setCoinCard(CoinCard.newBuilder().setCoinMsg(message))
                ReceivedLikeType.FAVORITE -> setFavoriteCard(FavoriteCard.newBuilder().setFavMsg(message))
            }
        }
        return MsgFeedMsgCard.newBuilder().setMsgId(id).setMsgItem(item).build()
    }
}
