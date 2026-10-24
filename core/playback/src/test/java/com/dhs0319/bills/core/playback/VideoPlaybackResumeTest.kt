package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.model.PlayBiz
import com.dhs0319.bills.core.model.PlaybackHistory
import com.dhs0319.bills.core.model.PlaybackRequest
import com.dhs0319.bills.core.model.PlaybackSource
import com.dhs0319.bills.core.model.ResolvedVideoIds
import com.dhs0319.bills.core.model.VideoRequestIds
import com.dhs0319.bills.core.model.VideoResumePosition
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.isSameEntry
import com.dhs0319.bills.core.model.toPlayableParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackResumeTest {
    private val target = VideoTarget.Ugc(aid = 1L, cid = 10L)
    private val source = PlaybackSource(
        biz = PlayBiz.UGC,
        durationMs = 100_000L,
        streams = emptyList(),
        audios = emptyList(),
        qualityOptions = emptyList(),
        resumePositionMs = 20_000L,
        isPreview = false,
        supportProject = false,
        supplementType = null
    )

    @Test
    fun historyProgressReachesPlaybackForEveryVideoBusiness() {
        val resume = VideoResumePosition(positionMs = 45_000L, updatedAtSec = 200L)
        val targets = listOf(
            target.copy(resumePosition = resume),
            VideoTarget.Pgc(epId = 2L, resumePosition = resume),
            VideoTarget.Pugv(epId = 3L, resumePosition = resume)
        )
        for (entry in targets) {
            val request = entry.toPlayableParams().getResolveParams()
            assertEquals(45_000L, resolveVideoStartPosition(request, source, null))
        }
    }

    @Test
    fun newerHistoryProgressOverridesOlderLocalProgress() {
        assertEquals(
            45_000L,
            resolveVideoStartPosition(request(45_000L), source, local(60_000L))
        )
    }

    @Test
    fun newerLocalProgressOverridesAStaleHistoryPage() {
        assertEquals(
            60_000L,
            resolveVideoStartPosition(request(45_000L), source, local(60_000L, updatedAt = 201_000L))
        )
    }

    @Test
    fun localProgressWinsWhenBothRecordsAreFromTheSameSecond() {
        assertEquals(
            60_000L,
            resolveVideoStartPosition(request(45_000L), source, local(60_000L, updatedAt = 200_999L))
        )
    }

    @Test
    fun completedHistoryDoesNotRestoreAnOlderUnfinishedPosition() {
        assertEquals(0L, resolveVideoStartPosition(request(0L), source, local(60_000L)))
    }

    @Test
    fun completedLocalHistoryDoesNotRestoreOlderCloudProgress() {
        assertEquals(
            0L,
            resolveVideoStartPosition(
                request(45_000L),
                source,
                local(99_000L, updatedAt = 201_000L, finished = true)
            )
        )
    }

    @Test
    fun explicitSeekIncludingZeroOverridesAllResumePositions() {
        for (seekToMs in listOf(0L, 15_000L, 99_000L)) {
            assertEquals(
                seekToMs,
                resolveVideoStartPosition(request(45_000L, seekToMs = seekToMs), source, local(60_000L))
            )
        }
    }

    @Test
    fun localAndSourceResumeStillWorkWithoutAHistoryPagePosition() {
        assertEquals(60_000L, resolveVideoStartPosition(request(), source, local(60_000L)))
        assertEquals(20_000L, resolveVideoStartPosition(request(), source, null))
        assertEquals(0L, resolveVideoStartPosition(request(), source.copy(resumePositionMs = null), null))
    }

    @Test
    fun progressInTheLastThreeSecondsRestartsFromTheBeginning() {
        for (positionMs in listOf(97_000L, 100_000L, 105_000L)) {
            assertEquals(0L, resolveVideoStartPosition(request(positionMs), source, null))
            assertEquals(0L, resolveVideoStartPosition(request(), source, local(positionMs)))
            assertEquals(
                0L,
                resolveVideoStartPosition(request(), source.copy(resumePositionMs = positionMs), null)
            )
        }
        assertEquals(96_999L, resolveVideoStartPosition(request(96_999L), source, null))
    }

    @Test
    fun unknownDurationDoesNotDiscardAValidResumePosition() {
        assertEquals(45_000L, resolveVideoStartPosition(request(45_000L), source.copy(durationMs = 0L), null))
    }

    @Test
    fun resolvingMissingIdsPreservesTheHistoryPosition() {
        val resume = VideoResumePosition(positionMs = 45_000L, updatedAtSec = 200L)
        val request = VideoTarget.Ugc(aid = 0L, cid = 0L, bvid = "BV-history", resumePosition = resume)
            .toPlayableParams().getResolveParams()
        val resolved = request.withResolvedVideoIds(ResolvedVideoIds(aid = 1L, cid = 10L, bvid = "BV-history"))

        assertEquals(VideoRequestIds(aid = 1L, cid = 10L, bvid = "BV-history"), resolved.ids)
        assertEquals(45_000L, resolveVideoStartPosition(resolved, source, null))
    }

    @Test
    fun resolvingEpisodeIdsPreservesTheRequestedEpisodeAndPart() {
        val request = VideoTarget.Pgc(aid = 1L, cid = 20L, epId = 2L)
            .toPlayableParams().getResolveParams()
        val resolved = request.withResolvedVideoIds(
            ResolvedVideoIds(aid = 1L, cid = 10L, epId = 3L, seasonId = 4L, bvid = "BV-resolved")
        )

        assertEquals(
            VideoRequestIds(aid = 1L, cid = 20L, epId = 2L, seasonId = 4L, bvid = "BV-resolved"),
            resolved.ids
        )
    }

    @Test
    fun differentBvidOnlyHistoryEntriesAreNotTheSameVideo() {
        val first = VideoTarget.Ugc(aid = 0L, cid = 0L, bvid = "BV-first")
        val second = VideoTarget.Ugc(aid = 0L, cid = 0L, bvid = "BV-second")
        assertFalse(first.isSameEntry(second))
        assertTrue(first.isSameEntry(first.copy(aid = 1L, cid = 10L)))
        assertFalse(first.copy(bvid = null).isSameEntry(second.copy(bvid = null)))
    }

    private fun request(positionMs: Long? = null, seekToMs: Long? = null): PlaybackRequest {
        return target.copy(
            resumePosition = positionMs?.let { VideoResumePosition(positionMs = it, updatedAtSec = 200L) }
        ).toPlayableParams().getResolveParams(seekToMs = seekToMs)
    }

    private fun local(progressMs: Long, updatedAt: Long = 100_000L, finished: Boolean = false): PlaybackHistory {
        return PlaybackHistory(
            uid = 1L,
            key = "ugc:1:10",
            biz = PlayBiz.UGC.name,
            aid = 1L,
            cid = 10L,
            durationMs = source.durationMs,
            progressMs = progressMs,
            updatedAt = updatedAt,
            finished = finished
        )
    }
}
