package com.dhs0319.bills.feature.history.search

import androidx.lifecycle.SavedStateHandle
import com.dhs0319.bills.core.model.HistoryItem
import com.dhs0319.bills.core.model.HistorySearchPage
import com.dhs0319.bills.core.model.HistoryTab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistorySearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun clearingKeywordImmediatelyRemovesResultsAndDisablesPagination() = runTest {
        for (blank in listOf("", "   ")) {
            var requests = 0
            val viewModel = HistorySearchViewModel(
                SavedStateHandle(mapOf("tab" to HistoryTab.ARCHIVE.business))
            ) { _, _, _ ->
                requests++
                HistorySearchPage(listOf(item("old")), hasMore = true)
            }
            viewModel.updateKeyword("old")
            viewModel.submitSearch()
            runCurrent()
            assertTrue(viewModel.uiState.value.items.isNotEmpty())
            assertTrue(viewModel.uiState.value.canLoadMore)

            viewModel.updateKeyword(blank)

            assertEquals(
                HistorySearchUiState(tab = HistoryTab.ARCHIVE, input = blank),
                viewModel.uiState.value
            )
            assertFalse(viewModel.uiState.value.canLoadMore)
            viewModel.loadMore()
            runCurrent()
            assertEquals(1, requests)
        }
    }

    @Test
    fun clearingKeywordCancelsInitialSearch() = runTest {
        val pending = CompletableDeferred<HistorySearchPage>()
        var cancelled = false
        val viewModel = HistorySearchViewModel(SavedStateHandle()) { _, _, _ ->
            try {
                pending.await()
            } finally {
                cancelled = true
            }
        }
        viewModel.updateKeyword("old")
        viewModel.submitSearch()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.updateKeyword("")

        assertEquals(HistorySearchUiState(), viewModel.uiState.value)
        runCurrent()
        assertTrue(cancelled)
        pending.complete(HistorySearchPage(listOf(item("old")), hasMore = true))
        runCurrent()
        assertEquals(HistorySearchUiState(), viewModel.uiState.value)
    }

    @Test
    fun clearingDuringPaginationDiscardsLateResultsAndRestartsAtPageOne() = runTest {
        val oldPage = CompletableDeferred<HistorySearchPage>()
        val requests = mutableListOf<Pair<String, Int>>()
        val viewModel = HistorySearchViewModel(SavedStateHandle()) { keyword, _, page ->
            requests += keyword to page
            if (keyword == "old" && page == 2) {
                withContext(NonCancellable) { oldPage.await() }
            } else {
                HistorySearchPage(listOf(item("$keyword:$page")), hasMore = true)
            }
        }
        viewModel.updateKeyword("old")
        viewModel.submitSearch()
        runCurrent()
        viewModel.loadMore()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoadingMore)

        viewModel.updateKeyword("")
        assertEquals(HistorySearchUiState(), viewModel.uiState.value)
        viewModel.updateKeyword("new")
        viewModel.submitSearch()
        runCurrent()
        val newState = viewModel.uiState.value
        assertEquals(listOf("new:1"), newState.items.map { it.key })

        oldPage.complete(HistorySearchPage(listOf(item("old:2")), hasMore = false))
        runCurrent()
        assertEquals(newState, viewModel.uiState.value)
        viewModel.loadMore()
        runCurrent()
        assertEquals(listOf("new:1", "new:2"), viewModel.uiState.value.items.map { it.key })
        assertEquals(
            listOf("old" to 1, "old" to 2, "new" to 1, "new" to 2),
            requests
        )
    }

    @Test
    fun clearingKeywordRemovesPreviousError() = runTest {
        val viewModel = HistorySearchViewModel(SavedStateHandle()) { _, _, _ ->
            error("Search failed")
        }
        viewModel.updateKeyword("old")
        viewModel.submitSearch()
        runCurrent()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.updateKeyword("")

        assertEquals(HistorySearchUiState(), viewModel.uiState.value)
    }

    @Test
    fun failedCancelledSearchCannotRestoreErrorAfterClearing() = runTest {
        val pending = CompletableDeferred<Unit>()
        val viewModel = HistorySearchViewModel(SavedStateHandle()) { _, _, _ ->
            withContext(NonCancellable) { pending.await() }
            error("Late failure")
        }
        viewModel.updateKeyword("old")
        viewModel.submitSearch()
        runCurrent()
        viewModel.updateKeyword("")

        pending.complete(Unit)
        runCurrent()

        assertEquals(HistorySearchUiState(), viewModel.uiState.value)
    }

    private fun item(key: String) = HistoryItem(
        key = key,
        type = "archive",
        typeLabel = "视频",
        title = key,
        cover = null,
        ownerName = null,
        badge = null,
        subtitle = null,
        deviceLabel = null,
        viewedAtSec = 0,
        progressSec = null,
        durationSec = null,
        target = null
    )
}
