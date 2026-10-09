package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.ResolvedVideoIds
import com.dhs0319.bills.core.model.VodDanmakuRequest
import com.dhs0319.bills.core.model.VodDanmakuSegment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class VodDanmakuSessionTest {
    private val ids = ResolvedVideoIds(aid = 1L, cid = 2L)

    @Test
    fun immediatelyCompletedFailureDoesNotLeaveARequestMarker() = runTest {
        var attempts = 0
        val immediateScope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val session = VodDanmakuSession(immediateScope, { request ->
            attempts++
            if (attempts == 1) error("failed immediately")
            segment(request)
        })
        session.setSource(ids, 360_000L)
        session.onProgress(0L)
        assertEquals("failed immediately", session.state.value.lastError)
        session.seekTo(0L)
        assertEquals(2, attempts)
        assertEquals(1L, session.state.value.window?.id)
        assertNull(session.state.value.lastError)
    }

    @Test
    fun repeatedProgressSharesCurrentAndPrefetchRequests() = runTest {
        val requests = mutableListOf<VodDanmakuRequest>()
        val result = CompletableDeferred<VodDanmakuSegment>()
        val session = VodDanmakuSession(backgroundScope, { request ->
            requests += request
            result.await()
        })
        session.setSource(ids, 1_000_000L)
        repeat(20) { session.onProgress(it * 1_000L) }
        runCurrent()
        assertEquals(listOf(1L, 2L), requests.map { it.segmentIndex })
        session.seekTo(30_000L)
        runCurrent()
        assertEquals(2, requests.size)
    }

    @Test
    fun inFlightPrefetchBecomesCurrentWithoutAnotherFetch() = runTest {
        val requests = mutableListOf<VodDanmakuRequest>()
        val pending = mutableMapOf<Long, CompletableDeferred<VodDanmakuSegment>>()
        val session = VodDanmakuSession(backgroundScope, { request ->
            requests += request
            pending.getOrPut(request.segmentIndex) { CompletableDeferred() }.await()
        })
        session.setSource(ids, 1_000_000L)
        session.onProgress(0L)
        runCurrent()
        pending.getValue(1L).complete(segment(requests.first()))
        runCurrent()
        session.onProgress(360_000L)
        runCurrent()
        assertEquals(listOf(1L, 2L, 3L), requests.map { it.segmentIndex })
        assertEquals(1L, session.state.value.window?.id)
        pending.getValue(2L).complete(segment(requests[1]))
        runCurrent()
        assertEquals(2L, session.state.value.window?.id)
        assertNull(session.state.value.prefetchWindow)
    }

    @Test
    fun readyPrefetchSurvivesSeekInSameWindowAndIsPromotedAtBoundary() = runTest {
        val requests = mutableListOf<VodDanmakuRequest>()
        val session = VodDanmakuSession(backgroundScope, { request ->
            requests += request
            segment(request)
        })
        session.setSource(ids, 1_000_000L)
        session.onProgress(0L)
        runCurrent()
        val prefetched = session.state.value.prefetchWindow
        session.seekTo(50_000L)
        runCurrent()
        assertTrue(prefetched === session.state.value.prefetchWindow)
        assertEquals(2, requests.size)
        session.onProgress(360_000L)
        runCurrent()
        assertTrue(prefetched === session.state.value.window)
        assertEquals(listOf(1L, 2L, 3L), requests.map { it.segmentIndex })
    }

    @Test
    fun lateCancelledRequestCannotPublishOrRemoveItsReplacement() = runTest {
        val requests = mutableListOf<Pair<VodDanmakuRequest, Continuation<VodDanmakuSegment>>>()
        val session = VodDanmakuSession(backgroundScope, { request ->
            suspendCoroutine { continuation -> requests += request to continuation }
        })
        session.setSource(ids, 360_000L)
        session.onProgress(0L)
        runCurrent()
        session.setSource(ids.copy(cid = 3L), 360_000L)
        session.setSource(ids, 360_000L)
        session.onProgress(0L)
        runCurrent()
        assertEquals(2, requests.size)
        requests[0].let { (request, continuation) -> continuation.resume(segment(request)) }
        runCurrent()
        assertNull(session.state.value.window)
        session.seekTo(0L)
        runCurrent()
        assertEquals(2, requests.size)
        requests[1].let { (request, continuation) -> continuation.resume(segment(request)) }
        runCurrent()
        assertEquals(1L, session.state.value.window?.id)
        assertNull(session.state.value.lastError)
    }

    @Test
    fun failedCurrentWindowRetriesWithCooldownAndExplicitSeekRetriesImmediately() = runTest {
        var nowNanos = 0L
        var attempts = 0
        val session = VodDanmakuSession(backgroundScope, { request ->
            attempts++
            if (attempts < 3) error("offline")
            segment(request)
        }, nanoTime = { nowNanos })
        session.setSource(ids, 360_000L)
        session.onProgress(0L)
        runCurrent()
        assertEquals("offline", session.state.value.lastError)
        nowNanos = 4_999_999_999L
        session.onProgress(1_000L)
        runCurrent()
        assertEquals(1, attempts)
        nowNanos = 5_000_000_000L
        session.onProgress(2_000L)
        runCurrent()
        assertEquals(2, attempts)
        session.seekTo(3_000L)
        runCurrent()
        assertEquals(3, attempts)
        assertNotNull(session.state.value.window)
        assertNull(session.state.value.lastError)
    }

    @Test
    fun failedPrefetchIsRetriedWhenItBecomesCurrent() = runTest {
        val requests = mutableListOf<Long>()
        val session = VodDanmakuSession(backgroundScope, { request ->
            requests += request.segmentIndex
            if (request.segmentIndex == 2L && requests.count { it == 2L } == 1) error("prefetch failed")
            segment(request)
        })
        session.setSource(ids, 720_000L)
        session.onProgress(0L)
        runCurrent()
        assertNull(session.state.value.lastError)
        session.onProgress(360_000L)
        runCurrent()
        assertEquals(listOf(1L, 2L, 2L), requests)
        assertEquals(2L, session.state.value.window?.id)
    }

    @Test
    fun durationBoundaryNeverRequestsPhantomWindow() = runTest {
        val requests = mutableListOf<Long>()
        val session = VodDanmakuSession(backgroundScope, { request ->
            requests += request.segmentIndex
            segment(request)
        })
        session.setSource(ids, 720_000L)
        session.onProgress(720_000L)
        runCurrent()
        assertEquals(listOf(2L), requests)
        assertEquals(2L, session.state.value.window?.id)
        session.onProgress(Long.MAX_VALUE)
        runCurrent()
        assertEquals(1, requests.size)
    }

    @Test
    fun obsoleteSeekRequestsAreCancelledAndClearCannotPublishAnError() = runTest {
        val requested = mutableListOf<Long>()
        val cancelled = mutableListOf<Long>()
        val session = VodDanmakuSession(backgroundScope, { request ->
            requested += request.segmentIndex
            try {
                CompletableDeferred<VodDanmakuSegment>().await()
            } finally {
                cancelled += request.segmentIndex
            }
        })
        session.setSource(ids, 0L)
        session.onProgress(-1L)
        runCurrent()
        session.seekTo(1_080_000L)
        runCurrent()
        assertEquals(listOf(1L, 2L, 4L, 5L), requested)
        assertEquals(listOf(1L, 2L), cancelled)
        session.clear()
        runCurrent()
        assertEquals(requested.toSet(), cancelled.toSet())
        assertEquals(DanmakuSessionState(), session.state.value)
        session.setSource(ids.copy(aid = 0L), 0L)
        session.onProgress(0L)
        runCurrent()
        assertEquals(4, requested.size)
    }

    private fun segment(request: VodDanmakuRequest) = VodDanmakuSegment(
        request, emptyList(), 0, emptyList(), emptyMap(), ""
    )
}
