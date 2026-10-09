package com.dhs0319.bills.feature.download.player

import com.dhs0319.bills.core.model.DanmakuItem
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.DownloadDanmakuCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDanmakuSessionTest {
    @Test
    fun loadPublishesLatestPositionAndSortsOnlyItsWindow() = runTest {
        val cache = CompletableDeferred<DownloadDanmakuCache?>()
        val session = OfflineDanmakuSession(backgroundScope, { null }, { cache.await() })
        session.bind(1L)
        runCurrent()
        session.onTick(400_000L)
        cache.complete(DownloadDanmakuCache(10L, 20L, listOf(
            item(1L, 370_000), item(2L, 365_000), item(3L, 0)
        )))
        session.state.awaitLoaded()
        assertEquals("download:1:10:20", session.state.value.sourceKey)
        assertEquals(2L, session.state.value.window?.id)
        assertEquals(listOf(2L, 1L), session.state.value.window?.items?.map { it.id })
        session.onTick(0L)
        assertEquals(listOf(3L), session.state.value.window?.items?.map { it.id })
    }

    @Test
    fun gapsAndMissingCachePublishReadyEmptyWindows() = runTest {
        val session = OfflineDanmakuSession(backgroundScope, { null }, {
            DownloadDanmakuCache(10L, 20L, listOf(item(1L, 0)))
        })
        session.bind(1L)
        session.state.awaitLoaded()
        session.onTick(800_000L)
        assertEquals(3L, session.state.value.window?.id)
        assertTrue(session.state.value.window!!.items.isEmpty())
        val empty = OfflineDanmakuSession(backgroundScope, { null }, { null })
        empty.bind(2L)
        empty.state.awaitLoaded()
        assertEquals("download:2:0:0", empty.state.value.sourceKey)
        assertEquals(1L, empty.state.value.window?.id)
        assertTrue(empty.state.value.window!!.items.isEmpty())
    }

    @Test
    fun duplicateBindUsesCacheAndForcedReloadPreservesPosition() = runTest {
        var loads = 0
        val session = OfflineDanmakuSession(backgroundScope, { null }, {
            loads++
            DownloadDanmakuCache(10L, 20L, emptyList())
        })
        session.bind(1L)
        session.state.awaitLoaded()
        session.onTick(400_000L)
        session.bind(1L)
        runCurrent()
        assertEquals(1, loads)
        session.bind(1L, forceReload = true)
        session.state.awaitLoaded()
        assertEquals(2, loads)
        assertEquals(2L, session.state.value.window?.id)
    }

    @Test
    fun switchingTaskCancelsOldLoadAndCancellationDoesNotBecomeAnError() = runTest {
        val first = CompletableDeferred<DownloadDanmakuCache?>()
        val second = CompletableDeferred<DownloadDanmakuCache?>()
        val cancelled = mutableListOf<Long>()
        val session = OfflineDanmakuSession(backgroundScope, { null }, { taskId ->
            try {
                (if (taskId == 1L) first else second).await()
            } finally {
                cancelled += taskId
            }
        })
        session.bind(1L)
        runCurrent()
        session.bind(2L)
        runCurrent()
        assertEquals(listOf(1L), cancelled)
        second.complete(DownloadDanmakuCache(30L, 40L, emptyList()))
        session.state.awaitLoaded()
        first.complete(DownloadDanmakuCache(10L, 20L, emptyList()))
        runCurrent()
        assertEquals("download:2:30:40", session.state.value.sourceKey)
        assertNull(session.state.value.lastError)
    }

    @Test
    fun taskLookupFailureIsReportedAndBindCanRetry() = runTest {
        var attempts = 0
        val session = OfflineDanmakuSession(backgroundScope, {
            attempts++
            if (attempts == 1) error("task unavailable")
            null
        }, { null })
        session.bind(1L)
        runCurrent()
        assertEquals("task unavailable", session.state.value.lastError)
        session.bind(1L)
        session.state.awaitLoaded()
        assertEquals(2, attempts)
        assertNull(session.state.value.lastError)
    }

    @Test
    fun clearCancelsLoadingAndCannotPublishFailureLater() = runTest {
        val pending = CompletableDeferred<DownloadDanmakuCache?>()
        val session = OfflineDanmakuSession(backgroundScope, { null }, { pending.await() })
        session.bind(1L)
        runCurrent()
        session.clear()
        runCurrent()
        pending.complete(DownloadDanmakuCache(1L, 2L, emptyList()))
        runCurrent()
        assertEquals(DanmakuSessionState(), session.state.value)
        session.bind(0L)
        assertEquals(DanmakuSessionState(), session.state.value)
    }

    private suspend fun StateFlow<DanmakuSessionState>.awaitLoaded() {
        first { it.window != null }
    }

    private fun item(id: Long, progressMs: Int) = DanmakuItem(
        id = id, idStr = id.toString(), progressMs = progressMs, mode = 1, fontSize = 25,
        color = 0xffffff, midHash = "", content = "弹幕$id", createdAtEpochSecond = 0L,
        weight = 2, action = "", pool = 0, attr = 0, likeCount = 0L, animation = "",
        extra = "", colorfulType = 0, type = 0, oid = 0L, dmFromType = 0
    )
}
