package com.dhs0319.bills.core.playback.subtitle

import com.dhs0319.bills.core.model.ResolvedVideoIds
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoSubtitleSessionTest {
    private val ids = ResolvedVideoIds(aid = 1, cid = 2)
    private val chinese = track("zh", "zh-Hans")
    private val chineseAi = track("ai-zh", "ai-zh", true)
    private val englishAi = track("ai-en", "ai-en", true)
    private val english = track("en", "en-US")
    private val timeline = SubtitleTimeline.create(listOf(SubtitleCue(100, 300, "test")))

    @Test
    fun disabledSubtitlesDoNoRequestsAndRepeatedMenuOpeningSharesMetadata() = runTest {
        var trackCalls = 0
        var contentCalls = 0
        val metadata = CompletableDeferred<List<VideoSubtitleTrack>>()
        val session = VideoSubtitleSession(backgroundScope,
            { trackCalls++; metadata.await() }, { contentCalls++; timeline })
        session.setSource(ids)
        session.setLargeText(true)
        runCurrent()
        assertEquals(0, trackCalls)
        repeat(10) { session.loadTracks() }
        runCurrent()
        assertEquals(1, trackCalls)
        metadata.complete(listOf(chinese))
        runCurrent()
        session.loadTracks()
        runCurrent()
        assertEquals(1, trackCalls)
        assertEquals(0, contentCalls)
    }

    @Test
    fun reEnablingCurrentTrackUsesParsedCacheAndDisablingClearsDisplayedContent() = runTest {
        var calls = 0
        val session = VideoSubtitleSession(backgroundScope, { listOf(chinese) }, { calls++; timeline })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        runCurrent()
        assertSame(timeline, session.state.value.primary)
        session.selectTrack(null)
        assertNull(session.state.value.primary)
        assertNull(session.state.value.selectedTrackId)
        session.selectTrack(chinese.id)
        runCurrent()
        assertEquals(1, calls)
        assertSame(timeline, session.state.value.primary)
    }

    @Test
    fun closingSubtitlesDuringDownloadCannotPublishStaleContent() = runTest {
        lateinit var pending: Continuation<SubtitleTimeline>
        val session = VideoSubtitleSession(backgroundScope, { listOf(chinese) }, {
            suspendCoroutine { pending = it }
        })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        runCurrent()
        session.selectTrack(null)
        pending.resume(timeline)
        runCurrent()
        assertNull(session.state.value.primary)
        assertFalse(session.state.value.loadingContent)
    }

    @Test
    fun rapidTrackSwitchDoesNotDisplayPreviousTracksLateResponse() = runTest {
        lateinit var pending: Continuation<SubtitleTimeline>
        val session = VideoSubtitleSession(backgroundScope, { listOf(chinese, english) }, {
            if (it == chinese) suspendCoroutine { continuation -> pending = continuation } else timeline
        })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        runCurrent()
        session.selectTrack(english.id)
        runCurrent()
        val obsolete = SubtitleTimeline.create(listOf(SubtitleCue(0, 100, "obsolete")))
        pending.resume(obsolete)
        runCurrent()
        assertEquals(english.id, session.state.value.selectedTrackId)
        assertSame(timeline, session.state.value.primary)
    }

    @Test
    fun allSubtitlesRequireManualEnablingOnEveryVideoAndPart() = runTest {
        listOf(chinese, chineseAi).forEach { track ->
            var contentCalls = 0
            val requestedParts = mutableListOf<Long>()
            val session = VideoSubtitleSession(backgroundScope,
                { requestedParts += it.cid; listOf(track) }, { contentCalls++; timeline })
            session.setSource(ids)
            session.loadTracks()
            runCurrent()
            session.selectTrack(track.id)
            runCurrent()
            listOf(ids.copy(cid = 3), ids.copy(aid = 4, cid = 5), ids).forEachIndexed { index, next ->
                session.setSource(next)
                runCurrent()
                assertNull(session.state.value.selectedTrackId)
                assertNull(session.state.value.primary)
                assertNull(session.state.value.secondary)
                assertFalse(session.state.value.loadingTracks)
                assertFalse(session.state.value.loadingContent)
                assertEquals(index + 1, requestedParts.size)
                assertEquals(index + 1, contentCalls)

                session.loadTracks()
                runCurrent()
                assertNull(session.state.value.selectedTrackId)
                assertNull(session.state.value.primary)
                assertEquals(index + 1, contentCalls)

                session.selectTrack(track.id)
                runCurrent()
                assertSame(timeline, session.state.value.primary)
                assertEquals(index + 2, contentCalls)
            }
            assertEquals(listOf(2L, 3L, 5L, 2L), requestedParts)
        }
    }

    @Test
    fun reopeningSameVideoClosesAllSubtitleTypesAndDiscardsPreviousDownload() = runTest {
        listOf(chinese, chineseAi).forEach { track ->
            lateinit var pending: Continuation<SubtitleTimeline>
            var contentCalls = 0
            val session = VideoSubtitleSession(backgroundScope, { listOf(track) }, {
                contentCalls++
                suspendCoroutine { pending = it }
            })
            session.setSource(ids)
            session.loadTracks()
            runCurrent()
            session.selectTrack(track.id)
            runCurrent()

            session.onVideoOpened()
            pending.resume(timeline)
            runCurrent()
            assertNull(session.state.value.selectedTrackId)
            assertNull(session.state.value.primary)
            assertFalse(session.state.value.loadingContent)
            assertFalse(session.state.value.loadingTracks)

            session.loadTracks()
            runCurrent()
            assertNull(session.state.value.selectedTrackId)
            assertNull(session.state.value.primary)
            assertEquals(1, contentCalls)
        }
    }

    @Test
    fun aiOnlyAndMissingSubtitlesStayOffAfterLeavingOrdinarySubtitles() = runTest {
        val downloads = mutableListOf<String>()
        val session = VideoSubtitleSession(backgroundScope, {
            when (it.cid) {
                2L -> listOf(chinese)
                3L -> listOf(chineseAi, englishAi)
                else -> emptyList()
            }
        }, { downloads += it.id; timeline })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        runCurrent()
        assertEquals(chinese.id, session.state.value.selectedTrackId)

        listOf(3L, 4L).forEach { cid ->
            session.setSource(ids.copy(cid = cid))
            session.loadTracks()
            runCurrent()
            assertTrue(session.state.value.tracksLoaded)
            assertNull(session.state.value.selectedTrackId)
            assertNull(session.state.value.primary)
            assertFalse(session.state.value.loadingContent)
        }
        assertEquals(listOf(chinese.id), downloads)

        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        assertNull(session.state.value.selectedTrackId)
        assertNull(session.state.value.primary)
        assertEquals(listOf(chinese.id), downloads)
    }

    @Test
    fun emptySubtitleContentLeavesCurrentVideoOff() = runTest {
        val session = VideoSubtitleSession(backgroundScope, { listOf(chineseAi, chinese) }, {
            if (it == chinese) SubtitleTimeline.Empty else timeline
        })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        runCurrent()
        assertNull(session.state.value.selectedTrackId)
        assertNull(session.state.value.primary)
        assertFalse(session.state.value.loadingContent)
        assertEquals("这条字幕暂无内容", session.state.value.error)
    }

    @Test
    fun reopeningVideoAndChangingPartCloseBothSubtitleTracks() = runTest {
        val session = VideoSubtitleSession(backgroundScope, { listOf(chinese, englishAi) }, { timeline })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        runCurrent()
        session.setBilingual(true)
        runCurrent()
        assertEquals(englishAi.id, session.state.value.secondaryTrackId)
        session.onVideoOpened()
        runCurrent()
        assertNull(session.state.value.selectedTrackId)
        assertNull(session.state.value.primary)
        assertNull(session.state.value.secondaryTrackId)
        assertNull(session.state.value.secondary)
        assertFalse(session.state.value.bilingual)

        session.selectTrack(chinese.id)
        session.setBilingual(true)
        runCurrent()
        session.setSource(ids.copy(cid = 3))
        runCurrent()
        assertNull(session.state.value.selectedTrackId)
        assertNull(session.state.value.primary)
        assertNull(session.state.value.secondaryTrackId)
        assertNull(session.state.value.secondary)
        assertFalse(session.state.value.bilingual)
    }

    @Test
    fun obsoleteMetadataCannotReplaceCurrentPartEvenWhenReturningToSameIds() = runTest {
        val pending = mutableListOf<Continuation<List<VideoSubtitleTrack>>>()
        val session = VideoSubtitleSession(backgroundScope,
            { suspendCoroutine { pending += it } }, { timeline })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.setSource(ids.copy(cid = 3))
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        pending[0].resume(listOf(english))
        runCurrent()
        assertTrue(session.state.value.tracks.isEmpty())
        pending[1].resume(listOf(chinese))
        runCurrent()
        assertEquals(listOf(chinese), session.state.value.tracks)
    }

    @Test
    fun bilingualChoosesDifferentLanguageAndPrefersHumanTrack() = runTest {
        val requested = mutableListOf<String>()
        val session = VideoSubtitleSession(backgroundScope,
            { listOf(chinese, chineseAi, englishAi, english) }, { requested += it.id; timeline })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        runCurrent()
        session.setBilingual(true)
        runCurrent()
        assertEquals(english.id, session.state.value.secondaryTrackId)
        assertEquals(setOf(chinese.id, english.id), requested.toSet())
        assertEquals(2, requested.size)
        session.setBilingual(false)
        runCurrent()
        assertNull(session.state.value.secondary)
        assertNull(session.state.value.secondaryTrackId)
        assertEquals(2, requested.size)
    }

    @Test
    fun slowOrFailedSecondaryDoesNotPreventPrimaryCaptions() = runTest {
        val secondary = CompletableDeferred<SubtitleTimeline>()
        val session = VideoSubtitleSession(backgroundScope, { listOf(chinese, english) }, {
            if (it == english) secondary.await() else timeline
        })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        session.selectTrack(chinese.id)
        session.setBilingual(true)
        runCurrent()
        assertSame(timeline, session.state.value.primary)
        assertTrue(session.state.value.loadingContent)
        secondary.completeExceptionally(IllegalStateException("offline"))
        runCurrent()
        assertSame(timeline, session.state.value.primary)
        assertFalse(session.state.value.loadingContent)
        assertEquals("副字幕加载失败，请重试", session.state.value.error)
    }

    @Test
    fun failedMetadataCanBeRetriedAndUnknownTrackIsIgnored() = runTest {
        var calls = 0
        val session = VideoSubtitleSession(backgroundScope, {
            if (++calls == 1) error("offline") else listOf(chinese)
        }, { timeline })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        assertFalse(session.state.value.loadingTracks)
        assertFalse(session.state.value.tracksLoaded)
        session.retry()
        runCurrent()
        assertTrue(session.state.value.tracksLoaded)
        assertNull(session.state.value.error)
        session.selectTrack("unknown")
        assertNull(session.state.value.selectedTrackId)
    }

    @Test
    fun parsedCacheHasABoundAndLargeTextDoesNotReloadSubtitles() = runTest {
        val tracks = List(5) { track("$it", "lang-$it") }
        val calls = mutableListOf<String>()
        val session = VideoSubtitleSession(backgroundScope, { tracks }, { calls += it.id; timeline })
        session.setSource(ids)
        session.loadTracks()
        runCurrent()
        tracks.forEach {
            session.selectTrack(it.id)
            runCurrent()
        }
        session.setLargeText(true)
        runCurrent()
        assertEquals(5, calls.size)
        session.selectTrack(tracks.first().id)
        runCurrent()
        assertEquals(6, calls.size)
    }

    private fun track(id: String, language: String, ai: Boolean = false) =
        VideoSubtitleTrack(id, language, language, "https://example.com/$id.json", ai)
}
