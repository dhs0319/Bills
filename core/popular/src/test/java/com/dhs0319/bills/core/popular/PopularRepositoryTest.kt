package com.dhs0319.bills.core.popular

import com.bapis.bilibili.app.card.v1.AdInfo
import com.bapis.bilibili.app.card.v1.Base
import com.bapis.bilibili.app.card.v1.Card
import com.bapis.bilibili.app.card.v1.RcmdOneItem
import com.bapis.bilibili.app.card.v1.SharePlane
import com.bapis.bilibili.app.card.v1.SmallCoverV5
import com.bapis.bilibili.app.card.v1.ThreePointV4
import com.bapis.bilibili.app.show.popular.v1.PopularReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PopularRepositoryTest {
    @Test
    fun shareMetadataProvidesPlaybackAndUploaderIdsWithoutPlayerArgs() {
        val page = PopularReply.newBuilder()
            .addItems(videoCard(123L, 1L))
            .setVer("version-1")
            .build()
            .toPopularPage(PopularCursor())

        val video = page.items.single()
        assertEquals("BV1test", video.bvid)
        assertEquals(456L, video.target.cid)
        assertEquals(789L, video.spaceRoute?.mid)
        assertEquals("上传者", video.ownerName)
        assertEquals("https://example.com/cover.jpg", video.cover)
        assertEquals("2:18", video.durationText)
        assertEquals("21.9万", video.viewCountText)
        assertEquals("4小时前", video.publishTimeText)
    }

    @Test
    fun filteredLastCardOwnsCursorAndVideoOrderIsPreserved() {
        val lastCard = Card.newBuilder().setRcmdOneItem(
            RcmdOneItem.newBuilder().setBase(
                Base.newBuilder().setGoto("mid").setIdx(3L).setParam("999")
            )
        ).build()
        val page = PopularReply.newBuilder()
            .addItems(videoCard(200L, 1L))
            .addItems(videoCard(100L, 2L))
            .addItems(lastCard)
            .setVer("version-1")
            .build()
            .toPopularPage(PopularCursor())

        assertEquals(listOf(200L, 100L), page.items.map { it.aid })
        assertEquals(PopularCursor(3L, "999", "version-1"), page.nextCursor)
    }

    @Test
    fun filteredPageKeepsPagingAndUriProvidesMissingCid() {
        val adBase = Base.newBuilder()
            .setGoto("av").setParam("123").setIdx(1L).setTitle("广告")
            .setAdInfo(AdInfo.newBuilder().setIsAd(true))
        val adPage = PopularReply.newBuilder().addItems(
            Card.newBuilder().setSmallCoverV5(SmallCoverV5.newBuilder().setBase(adBase))
        ).build().toPopularPage(PopularCursor(ver = "version-1"))
        assertTrue(adPage.items.isEmpty())
        assertEquals(PopularCursor(1L, "123", "version-1"), adPage.nextCursor)

        val card = videoCard(123L, 2L)
        val base = card.smallCoverV5.base.toBuilder()
            .clearThreePointV4()
            .setUri("bilibili://video/123?cid=456")
        val page = PopularReply.newBuilder().addItems(
            card.toBuilder().setSmallCoverV5(card.smallCoverV5.toBuilder().setBase(base))
        ).build().toPopularPage(PopularCursor(1L, "123", "version-1"))
        assertEquals(456L, page.items.single().target.cid)
        assertEquals("21.9万", page.items.single().viewCountText)
        assertFalse(page.items.single().ownerName.isBlank())
    }

    @Test
    fun emptyOrUnchangedServerCursorStopsPagination() {
        val cursor = PopularCursor(1L, "123", "version-1")
        assertNull(PopularReply.getDefaultInstance().toPopularPage(cursor).nextCursor)
        assertNull(
            PopularReply.newBuilder().addItems(videoCard(123L, 1L))
                .build().toPopularPage(cursor).nextCursor
        )
    }

    private fun videoCard(aid: Long, idx: Long): Card {
        val share = SharePlane.newBuilder()
            .setAid(aid).setBvid("BV1test").setFirstCid(456L)
            .setAuthor("上传者").setAuthorId(789L).setPlayNumber("21.9万")
        val base = Base.newBuilder()
            .setGoto("av").setParam(aid.toString()).setIdx(idx)
            .setTitle("视频 $aid").setCover("http://example.com/cover.jpg")
            .setThreePointV4(ThreePointV4.newBuilder().setSharePlane(share))
        return Card.newBuilder().setSmallCoverV5(
            SmallCoverV5.newBuilder().setBase(base)
                .setCoverRightText1("2:18")
                .setRightDesc1("上传者").setRightDesc2("21.9万播放 · 4小时前")
        ).build()
    }
}
