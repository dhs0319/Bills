package com.dhs0319.bills.feature.home.paging

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomePagerTest {
    @Test
    fun smallResponsesFillOneBatchAndPublishTheFirstPageImmediately() = runTest {
        val secondPage = CompletableDeferred<Unit>()
        val calls = mutableListOf<Int>()
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            calls += key
            if (key == 1) secondPage.await()
            HomePage((key * 10 until key * 10 + 10).toList(), key + 1)
        })
        pager.activate(0)
        runCurrent()
        assertEquals(10, pager.state.value.items.size)
        assertTrue(pager.state.value.isRefreshing)
        secondPage.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(0, 1), calls)
        assertEquals(20, pager.state.value.items.size)
        assertFalse(pager.state.value.isRefreshing)
    }

    @Test
    fun fullPageDoesNotCauseAnUnnecessarySecondRequest() = runTest {
        var calls = 0
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            calls++
            HomePage((0 until 20).toList(), key + 1)
        })
        pager.activate(0)
        advanceUntilIdle()
        pager.activate(0)
        advanceUntilIdle()
        assertEquals(1, calls)
    }

    @Test
    fun requestsAreGuardedBeforeCoroutineStarts() = runTest {
        val calls = mutableListOf<Int>()
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            calls += key
            HomePage(listOf(key), key + 1)
        }, batchSize = 1)
        pager.activate(0)
        pager.activate(0)
        pager.refresh()
        pager.loadMore()
        advanceUntilIdle()
        pager.loadMore()
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(0, 1), calls)
    }

    @Test
    fun reenteringTabDoesNotReplayConsumedRefresh() = runTest {
        var calls = 0
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, {
            calls++
            HomePage(listOf(calls), null)
        })
        pager.activate(0)
        advanceUntilIdle()
        assertTrue(pager.activate(1))
        advanceUntilIdle()
        assertFalse(pager.activate(1))
        advanceUntilIdle()
        assertEquals(2, calls)
        assertEquals(listOf(2), pager.state.value.items)
    }

    @Test
    fun failedAppendWaitsForExplicitRetryAndReusesCursor() = runTest {
        var fail = true
        val calls = mutableListOf<Int>()
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            calls += key
            if (key == 1 && fail) error("offline")
            HomePage(listOf(key), key + 1)
        }, batchSize = 1)
        pager.refresh()
        advanceUntilIdle()
        pager.loadMore()
        advanceUntilIdle()
        repeat(3) { pager.loadMore() }
        advanceUntilIdle()
        assertEquals(listOf(0, 1), calls)
        assertEquals("offline", pager.state.value.loadMoreError)
        fail = false
        pager.retryLoadMore()
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 1), calls)
        assertEquals(listOf(0, 1), pager.state.value.items)
        assertNull(pager.state.value.loadMoreError)
    }

    @Test
    fun partialBatchFailureKeepsDataAndRetriesOnlyFailedPage() = runTest {
        var fail = true
        val calls = mutableListOf<Int>()
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            calls += key
            if (key == 1 && fail) error("offline")
            HomePage(listOf(key), if (key == 1) null else 1)
        })
        pager.refresh()
        advanceUntilIdle()
        assertEquals(listOf(0), pager.state.value.items)
        assertTrue(pager.state.value.hasLoaded)
        assertNotNull(pager.state.value.loadMoreError)
        fail = false
        pager.retryLoadMore()
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 1), calls)
        assertEquals(listOf(0, 1), pager.state.value.items)
        assertFalse(pager.state.value.hasMore)
    }

    @Test
    fun refreshDiscardsLateAppendEvenIfLoaderIgnoresCancellation() = runTest {
        var initial = 10
        var obsoleteAccepted = false
        val lateResponse = CompletableDeferred<Unit>()
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            if (key == 0) HomePage(listOf(initial), 1)
            else {
                withContext(NonCancellable) { lateResponse.await() }
                HomePage(listOf(99), 2) { obsoleteAccepted = true }
            }
        }, batchSize = 1)
        pager.refresh()
        advanceUntilIdle()
        pager.loadMore()
        runCurrent()
        initial = 20
        pager.refresh()
        runCurrent()
        lateResponse.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(20), pager.state.value.items)
        assertFalse(obsoleteAccepted)
        assertFalse(pager.state.value.isLoadingMore)
        assertNull(pager.state.value.loadMoreError)
    }

    @Test
    fun failedRefreshPreservesDataAndPausesAppendUntilRefreshRetry() = runTest {
        var fail = false
        var calls = 0
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, {
            calls++
            if (fail) error("offline")
            HomePage(listOf(calls), 1)
        }, batchSize = 1)
        pager.refresh()
        advanceUntilIdle()
        fail = true
        pager.refresh()
        advanceUntilIdle()
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(2, calls)
        assertEquals(listOf(1), pager.state.value.items)
        assertNotNull(pager.state.value.errorMessage)
        fail = false
        pager.refresh()
        advanceUntilIdle()
        assertEquals(listOf(3), pager.state.value.items)
        assertNull(pager.state.value.errorMessage)
    }

    @Test
    fun failedInitialLoadCanBeRetriedWithoutAnActivationLoop() = runTest {
        var fail = true
        var calls = 0
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, {
            calls++
            if (fail) error("offline")
            HomePage(listOf(1), null)
        })
        pager.activate(0)
        advanceUntilIdle()
        pager.activate(0)
        advanceUntilIdle()
        assertEquals(1, calls)
        assertFalse(pager.state.value.hasLoaded)
        fail = false
        pager.refresh()
        advanceUntilIdle()
        assertEquals(listOf(1), pager.state.value.items)
    }

    @Test
    fun emptyAndDuplicatePagesHaveABoundedRequestBudget() = runTest {
        val calls = mutableListOf<Int>()
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            calls += key
            HomePage(emptyList(), key + 1)
        })
        pager.refresh()
        advanceUntilIdle()
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(0, 1, 2), calls)
        assertNotNull(pager.state.value.loadMoreError)
        assertTrue(pager.state.value.hasMore)
    }

    @Test
    fun repeatedCursorStopsAndDuplicatesDoNotCreateDuplicateKeys() = runTest {
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            if (key == 0) HomePage(listOf(1, 1, 2), 1)
            else HomePage(listOf(2, 3, 3), 1)
        })
        pager.refresh()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), pager.state.value.items)
        assertFalse(pager.state.value.hasMore)
        pager.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1, 2, 3), pager.state.value.items)
    }

    @Test
    fun refreshCanRestartInitialBufferingForAnExplicitPreferenceChange() = runTest {
        var initial = 1
        val obsolete = CompletableDeferred<Unit>()
        val pager = HomePager<Int, Int>(this, { 0 }, { it }, { key ->
            if (key == 0) HomePage(listOf(initial), 1)
            else {
                obsolete.await()
                HomePage(listOf(99), null)
            }
        })
        pager.refresh()
        runCurrent()
        initial = 2
        pager.refresh(restart = true)
        runCurrent()
        assertEquals(listOf(2), pager.state.value.items)
        obsolete.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(2, 99), pager.state.value.items)
    }
}
