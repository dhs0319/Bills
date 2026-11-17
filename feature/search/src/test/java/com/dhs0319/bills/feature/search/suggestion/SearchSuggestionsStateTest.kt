package com.dhs0319.bills.feature.search.suggestion

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchSuggestionsStateTest {
    @Test
    fun rapidInputOnlyRequestsTheLatestTrimmedKeyword() = runTest {
        val requests = mutableListOf<String>()
        val state = SearchSuggestionsState(backgroundScope) {
            requests += it
            listOf("${it}联想")
        }

        state.update("野")
        advanceTimeBy(200)
        state.update(" 野火 ")
        advanceTimeBy(249)
        runCurrent()
        assertTrue(state.isVisible)
        assertTrue(requests.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("野火"), requests)
        assertEquals(listOf("野火联想"), state.items)

        state.update("野火")
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("野火"), requests)
    }

    @Test
    fun changingInputImmediatelyRemovesOldSuggestionsAndIgnoresLateResponses() = runTest {
        val oldResponse = CompletableDeferred<List<String>>()
        val state = SearchSuggestionsState(backgroundScope) {
            if (it == "old") {
                withContext(NonCancellable) { oldResponse.await() }
            } else {
                listOf("$it result")
            }
        }

        state.update("first")
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("first result"), state.items)

        state.update("old")
        assertTrue(state.items.isEmpty())
        advanceTimeBy(250)
        runCurrent()
        state.update("new")
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("new result"), state.items)

        oldResponse.complete(listOf("old result"))
        runCurrent()
        assertEquals("new", state.query)
        assertEquals(listOf("new result"), state.items)
    }

    @Test
    fun blankInputAndDismissalCancelRequestsAndHideSuggestions() = runTest {
        for (blank in listOf("", "   ", null)) {
            val pending = CompletableDeferred<List<String>>()
            val state = SearchSuggestionsState(backgroundScope) {
                withContext(NonCancellable) { pending.await() }
            }
            state.update("野")
            advanceTimeBy(250)
            runCurrent()

            if (blank == null) state.clear() else state.update(blank)
            assertFalse(state.isVisible)
            assertTrue(state.items.isEmpty())

            pending.complete(listOf("野火"))
            runCurrent()
            assertFalse(state.isVisible)
            assertTrue(state.items.isEmpty())
        }
    }

    @Test
    fun requestFailureAllowsTheNextKeywordToLoad() = runTest {
        val state = SearchSuggestionsState(backgroundScope) {
            if (it == "old") throw IOException("offline")
            listOf("$it result")
        }
        state.update("old")
        advanceTimeBy(250)
        runCurrent()
        assertTrue(state.items.isEmpty())

        state.update("new")
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("new result"), state.items)
    }
}
