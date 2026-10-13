package com.dhs0319.bills.feature.im.likes

import com.dhs0319.bills.core.model.im.MsgFeedCursor
import com.dhs0319.bills.core.model.im.ReceivedLikeItem
import com.dhs0319.bills.core.model.im.ReceivedLikeType
import com.dhs0319.bills.core.model.im.ReceivedLikesPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceivedLikesUiStateTest {
    private val firstCursor = MsgFeedCursor(1, 100, 0, false)
    private val nextCursor = MsgFeedCursor(2, 90, 0, false)

    @Test
    fun overlappingPagesKeepUniqueKeysAndDifferentTypes() {
        val state = ReceivedLikesUiState(items = listOf(item(1)), cursor = firstCursor)
            .withPage(
                ReceivedLikesPage(listOf(item(1), item(1, ReceivedLikeType.COIN), item(2)), nextCursor),
                reset = false
            )
        assertEquals(listOf("LIKE:1", "COIN:1", "LIKE:2"), state.items.map { it.key })
        assertTrue(state.canLoadMore)
    }

    @Test
    fun repeatedCursorStopsPaginationEvenForDuplicateOnlyPage() {
        val state = ReceivedLikesUiState(items = listOf(item(1)), cursor = firstCursor)
            .withPage(ReceivedLikesPage(listOf(item(1)), firstCursor), reset = false)
        assertFalse(state.canLoadMore)
        assertEquals(1, state.items.size)
    }

    @Test
    fun refreshReplacesItemsAndClearsPreviousErrors() {
        val state = ReceivedLikesUiState(
            items = listOf(item(1)), cursor = firstCursor,
            isRefreshing = true, errorMessage = "error", loadMoreError = "error"
        ).withPage(ReceivedLikesPage(listOf(item(2)), firstCursor), reset = true)
        assertEquals(listOf(2L), state.items.map { it.msgId })
        assertTrue(state.canLoadMore)
        assertFalse(state.isBusy)
        assertNull(state.errorMessage)
        assertNull(state.loadMoreError)
    }

    @Test
    fun emptyMappedPageCanContinueWhenCursorAdvances() {
        val state = ReceivedLikesUiState(cursor = firstCursor)
            .withPage(ReceivedLikesPage(emptyList(), nextCursor), reset = false)
        assertTrue(state.canLoadMore)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun missingOrEndedCursorStopsPagination() {
        for (cursor in listOf(null, nextCursor.copy(isEnd = true))) {
            val state = ReceivedLikesUiState(cursor = firstCursor)
                .withPage(ReceivedLikesPage(emptyList(), cursor), reset = false)
            assertFalse(state.canLoadMore)
        }
    }

    private fun item(id: Long, type: ReceivedLikeType = ReceivedLikeType.LIKE) = ReceivedLikeItem(
        msgId = id, msgTime = 100, type = type, users = emptyList(), count = 1,
        description = "", coverText = "", coverImage = null
    )
}
