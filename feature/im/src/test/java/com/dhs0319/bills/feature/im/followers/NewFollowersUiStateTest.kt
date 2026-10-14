package com.dhs0319.bills.feature.im.followers

import com.dhs0319.bills.core.model.im.NewFollowerItem
import com.dhs0319.bills.core.model.im.NewFollowersPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFollowersUiStateTest {
    @Test
    fun overlappingPagesKeepUniqueFollowersAndSuccessfulFollowState() {
        val state = NewFollowersUiState(items = listOf(follower(1, isFollowing = true)), page = 1)
            .withPage(NewFollowersPage(listOf(follower(1), follower(2)), hasMore = true), requestedPage = 2)

        assertEquals(listOf(1L, 2L), state.items.map { it.mid })
        assertTrue(state.items.first().isFollowing)
        assertTrue(state.hasMore)
        assertEquals(2, state.page)
    }

    @Test
    fun overlappingPageDoesNotUndoSuccessfulUnfollow() {
        val state = NewFollowersUiState(items = listOf(follower(1, isFollowing = false)), page = 1)
            .withPage(
                NewFollowersPage(listOf(follower(1, isFollowing = true), follower(2)), hasMore = true),
                requestedPage = 2
            )

        assertEquals(listOf(1L, 2L), state.items.map { it.mid })
        assertFalse(state.items.first().isFollowing)
        assertTrue(state.hasMore)
    }

    @Test
    fun repeatedPageStopsAutomaticPagination() {
        val state = NewFollowersUiState(items = listOf(follower(1)), page = 1)
            .withPage(NewFollowersPage(listOf(follower(1)), hasMore = true), requestedPage = 2)

        assertFalse(state.hasMore)
        assertEquals(1, state.items.size)
    }

    @Test
    fun emptyPageStopsEvenWhenServerClaimsMore() {
        val state = NewFollowersUiState(items = listOf(follower(1)), page = 1)
            .withPage(NewFollowersPage(emptyList(), hasMore = true), requestedPage = 2)

        assertFalse(state.hasMore)
        assertEquals(listOf(1L), state.items.map { it.mid })
    }

    @Test
    fun refreshReplacesOldFollowersAndUpdatesRelationships() {
        val state = NewFollowersUiState(
            items = listOf(follower(1, isFollowing = true), follower(2)),
            page = 4,
            isRefreshing = true,
            errorMessage = "error",
            loadMoreError = "error"
        ).withPage(NewFollowersPage(listOf(follower(1)), hasMore = false), requestedPage = 1)

        assertEquals(listOf(1L), state.items.map { it.mid })
        assertFalse(state.items.first().isFollowing)
        assertFalse(state.isBusy)
        assertFalse(state.hasMore)
        assertEquals(1, state.page)
        assertNull(state.errorMessage)
        assertNull(state.loadMoreError)
    }

    private fun follower(mid: Long, isFollowing: Boolean = false) =
        NewFollowerItem(mid, "用户$mid", null, 100, isFollowing)
}
