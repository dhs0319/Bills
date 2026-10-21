package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.model.PlayBiz
import com.dhs0319.bills.core.model.PlaybackError
import com.dhs0319.bills.core.model.PlaybackSource
import com.dhs0319.bills.core.model.PlaybackState
import com.dhs0319.bills.core.model.ResolvedVideoIds
import com.dhs0319.bills.core.model.VideoDetail
import com.dhs0319.bills.core.model.VideoPagePart
import com.dhs0319.bills.core.model.VideoPlaybackState
import com.dhs0319.bills.core.model.VideoSeason
import com.dhs0319.bills.core.model.VideoSeasonEpisode
import com.dhs0319.bills.core.model.VideoSeasonSection
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.VideoTargetTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoAutoAdvanceTest {
    private val target = VideoTarget.Ugc(
        aid = 1L,
        cid = 30L,
        bvid = "BV-current",
        src = VideoTargetTool.history()
    )
    private val pages = listOf(30L, 10L, 20L).mapIndexed { index, cid ->
        VideoPagePart(cid = cid, part = "P${index + 1}", durationSec = 60L)
    }
    private val detail = VideoDetail(title = "分 P 视频", pages = pages)
    private val ended = VideoPlaybackState(
        biz = PlayBiz.UGC,
        ids = ResolvedVideoIds(aid = target.aid, cid = target.cid, bvid = target.bvid),
        detail = detail,
        playbackSource = PlaybackSource(
            biz = PlayBiz.UGC,
            durationMs = 60_000L,
            streams = emptyList(),
            audios = emptyList(),
            qualityOptions = emptyList(),
            resumePositionMs = 45_000L,
            isPreview = false,
            supportProject = false,
            supplementType = null
        ),
        playWhenReady = true,
        playbackState = PlaybackState.Ended
    )

    @Test
    fun partsFollowListOrderAndStopAfterTheLastPart() {
        var current = target
        for (nextCid in listOf(10L, 20L)) {
            val state = ended.copy(ids = ended.ids.copy(cid = current.cid))
            val next = nextVideoTarget(current, state)
            assertEquals(current.copy(cid = nextCid), next)
            current = requireNotNull(next)
        }
        assertNull(nextVideoTarget(current, ended.copy(ids = ended.ids.copy(cid = current.cid))))
    }

    @Test
    fun resolvedIdsAllowAnEntryWithoutAnExplicitCidOrAidToAdvance() {
        val unresolved = target.copy(aid = 0L, cid = 0L)
        assertEquals(target.copy(cid = 10L), nextVideoTarget(unresolved, ended))
    }

    @Test
    fun anEntryStartingAtAMiddlePartContinuesFromThatPart() {
        val middle = target.copy(cid = 10L)
        assertEquals(
            target.copy(cid = 20L),
            nextVideoTarget(middle, ended.copy(ids = ended.ids.copy(cid = middle.cid)))
        )
    }

    @Test
    fun aSingleVideoDoesNotAdvanceToRelatedRecommendations() {
        assertNull(nextVideoTarget(target, ended.copy(detail = detail.copy(pages = pages.take(1)))))
    }

    @Test
    fun pausedOrUnfinishedPlaybackDoesNotAdvance() {
        assertNull(nextVideoTarget(target, ended.copy(playWhenReady = false)))
        for (state in PlaybackState.entries.filter { it != PlaybackState.Ended }) {
            assertNull(nextVideoTarget(target, ended.copy(playbackState = state)))
        }
    }

    @Test
    fun loadingOrMissingPlaybackDataDoesNotAdvance() {
        val states = listOf(
            ended.copy(isPreparing = true),
            ended.copy(waitingForPlay = true),
            ended.copy(detailLoading = true),
            ended.copy(detail = null),
            ended.copy(playbackSource = null)
        )
        states.forEach { assertNull(nextVideoTarget(target, it)) }
    }

    @Test
    fun playbackErrorsDoNotAdvance() {
        assertNull(nextVideoTarget(target, ended.copy(error = PlaybackError.RequestFailed("offline"))))
        assertNull(nextVideoTarget(target, ended.copy(playerError = "decode failed")))
    }

    @Test
    fun lateStateFromAnotherVideoOrPartDoesNotAdvanceTheCurrentTarget() {
        assertNull(nextVideoTarget(target.copy(aid = 2L), ended))
        assertNull(nextVideoTarget(target.copy(cid = 10L), ended))
    }

    @Test
    fun unknownCurrentPartAndInvalidNextPartStopPlayback() {
        assertNull(nextVideoTarget(target.copy(cid = 99L), ended.copy(ids = ended.ids.copy(cid = 99L))))
        assertNull(nextVideoTarget(target, ended.copy(ids = ended.ids.copy(aid = 0L))))
        assertNull(nextVideoTarget(target.copy(cid = 0L), ended.copy(ids = ended.ids.copy(cid = 0L))))
        for (cid in listOf(0L, target.cid)) {
            val invalidDetail = detail.copy(pages = listOf(pages.first(), pages[1].copy(cid = cid)))
            assertNull(nextVideoTarget(target, ended.copy(detail = invalidDetail)))
        }
    }

    @Test
    fun collectionsDoNotAdvanceToAnotherVideo() {
        val next = VideoTarget.Ugc(aid = 2L, cid = 100L, src = VideoTargetTool.relate())
        val season = season(listOf(target), listOf(next))
        for (parts in listOf(emptyList(), pages.take(1))) {
            val state = ended.copy(detail = detail.copy(pages = parts, season = season))
            assertNull(nextVideoTarget(target, state))
        }
    }

    @Test
    fun partsStillAdvanceWhenTheVideoBelongsToACollection() {
        val next = VideoTarget.Ugc(aid = 2L, cid = 100L)
        val state = ended.copy(detail = detail.copy(season = season(listOf(target, next))))
        assertEquals(target.copy(cid = 10L), nextVideoTarget(target, state))
    }

    @Test
    fun lastPartStopsEvenWhenTheCollectionHasAnotherVideo() {
        val next = VideoTarget.Ugc(aid = 2L, cid = 100L)
        val lastPart = target.copy(cid = pages.last().cid)
        val state = ended.copy(
            ids = ended.ids.copy(cid = lastPart.cid),
            detail = detail.copy(season = season(listOf(target, next)))
        )
        assertNull(nextVideoTarget(lastPart, state))
    }

    @Test
    fun otherVideoBusinessesKeepTheirExistingBehavior() {
        assertNull(nextVideoTarget(VideoTarget.Pgc(aid = 1L, cid = 30L, epId = 3L), ended))
        assertNull(nextVideoTarget(target, ended.copy(biz = PlayBiz.PGC)))
        assertNull(nextVideoTarget(VideoTarget.Pugv(aid = 1L, epId = 3L), ended))
    }

    private fun season(vararg sections: List<VideoTarget.Ugc>): VideoSeason {
        return VideoSeason(
            title = "合集",
            subTitle = null,
            sections = sections.mapIndexed { index, targets ->
                VideoSeasonSection(
                    title = "分组 $index",
                    eps = targets.map {
                        VideoSeasonEpisode(
                            target = it,
                            cid = it.cid,
                            title = "视频 ${it.aid}",
                            subTitle = null,
                            cover = null
                        )
                    }
                )
            }
        )
    }
}
