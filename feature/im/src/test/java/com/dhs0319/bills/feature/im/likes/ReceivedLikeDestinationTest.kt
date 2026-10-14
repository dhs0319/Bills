package com.dhs0319.bills.feature.im.likes

import com.dhs0319.bills.core.model.User
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.WebLinkTarget
import com.dhs0319.bills.core.model.im.ReceivedLikeItem
import com.dhs0319.bills.core.model.im.ReceivedLikeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceivedLikeDestinationTest {
    @Test
    fun childCommentOpensItsThreadAndHighlightsTheLikedReply() {
        val item = notification("bilibili://video/123?comment_root_id=456&comment_secondary_id=789")
            .copy(itemId = 789, originId = 123, bizType = "reply")
        val record = (item.destination() as ReceivedLikeDestination.Comment).record
        assertEquals(123L, record.targetId)
        assertEquals(1L, record.targetType)
        assertEquals(456L, record.rootId)
        assertEquals(789L, record.itemId)
        assertEquals("被点赞的评论", record.content)
        // Liking a comment does not make the liker its author.
        assertEquals(0L, record.senderMid)
        assertEquals("", record.senderName)
    }

    @Test
    fun rootCommentUsesTheRootWhenSecondaryIdIsZero() {
        val record = (notification("bilibili://video/123?comment_root_id=456&comment_secondary_id=0")
            .destination() as ReceivedLikeDestination.Comment).record
        assertEquals(456L, record.rootId)
        assertEquals(456L, record.itemId)
    }

    @Test
    fun replyItemIdIsUsedWhenSecondaryIdIsMissing() {
        val record = (notification("bilibili://video/123?comment_root_id=456")
            .copy(itemId = 789, bizType = "reply")
            .destination() as ReceivedLikeDestination.Comment).record
        assertEquals(456L, record.rootId)
        assertEquals(789L, record.itemId)
    }

    @Test
    fun videoLikeOpensVideoWithoutTreatingItemIdAsAComment() {
        val destination = notification("bilibili://video/123")
            .copy(itemId = 999, bizType = "archive", type = ReceivedLikeType.COIN).destination()
            as ReceivedLikeDestination.Content
        val video = (destination.target as WebLinkTarget.ToVideo).target as VideoTarget.Ugc
        assertEquals(123L, video.aid)
    }

    @Test
    fun dynamicCommentPreservesItsSubjectAndReply() {
        val record = (notification("bilibili://following/detail/123?comment_root_id=456&comment_secondary_id=789")
            .destination() as ReceivedLikeDestination.Comment).record
        assertEquals(123L, record.targetId)
        assertEquals(17L, record.targetType)
        assertEquals(456L, record.rootId)
        assertEquals(789L, record.itemId)
    }

    @Test
    fun dynamicLikeOpensTheOriginalPost() {
        for (url in listOf("bilibili://opus/detail/123", "https://t.bilibili.com/123", "https://www.bilibili.com/opus/123")) {
            assertEquals(ReceivedLikeDestination.Dynamic("123"), notification(url).destination())
        }
    }

    @Test
    fun malformedOrUnavailableTargetsAreRejected() {
        for (url in listOf("", "not a uri", "bilibili://unknown/123", "javascript:alert(1)", "bilibili://video/123?comment_root_id=%GG")) {
            assertNull(notification(url).destination())
        }
        assertNull(notification("bilibili://video/123").copy(bizType = "reply").destination())
    }

    private fun notification(url: String) = ReceivedLikeItem(
        msgId = 1, msgTime = 100, type = ReceivedLikeType.LIKE,
        users = listOf(User(10, "点赞者", "")), count = 1,
        description = "点赞者赞了我的评论", coverText = "被点赞的评论", coverImage = null,
        nativeUri = url
    )
}
