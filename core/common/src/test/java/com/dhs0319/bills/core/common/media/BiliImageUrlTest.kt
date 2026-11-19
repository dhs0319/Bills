package com.dhs0319.bills.core.common.media

import org.junit.Assert.assertEquals
import org.junit.Test

class BiliImageUrlTest {
    @Test
    fun playerCoverUrlRemovesCardCropAndPreservesQuery() {
        val url = "http://i0.hdslb.com/bfs/archive/video@575w_360h_1e_1c_85q.webp?token=abc"

        assertEquals(
            "https://i0.hdslb.com/bfs/archive/video@1280w_85q.webp?token=abc",
            playerCoverUrl(url)
        )
    }

    @Test
    fun playerCoverUrlRoundsWidthUpToTheNextSupportedSize() {
        val url = "https://i0.hdslb.com/bfs/archive/video.jpg"

        assertEquals("$url@640w_85q.webp", playerCoverUrl(url, displayWidthPx = 640))
        assertEquals("$url@960w_85q.webp", playerCoverUrl(url, displayWidthPx = 641))
        assertEquals("$url@1080w_85q.webp", playerCoverUrl(url, displayWidthPx = 1080))
        assertEquals("$url@1280w_85q.webp", playerCoverUrl(url, displayWidthPx = 1081))
        assertEquals("$url@1280w_85q.webp", playerCoverUrl(url, displayWidthPx = 1920))
    }
}
