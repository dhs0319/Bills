package com.dhs0319.bills.feature.im.followers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.im.ImRepository
import com.dhs0319.bills.core.space.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NewFollowersViewModel @Inject constructor(
    private val imRepository: ImRepository,
    private val spaceRepository: SpaceRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        NewFollowersUiState(
            isLoggedIn = authRepository.currentMidFlow.value > 0L,
            isLoading = authRepository.currentMidFlow.value > 0L
        )
    )
    val uiState = _uiState.asStateFlow()
    private var accountGeneration = 0L
    private var currentMid = 0L
    private var loadJob: Job? = null
    private val followJobs = mutableMapOf<Long, Job>()

    init {
        viewModelScope.launch {
            authRepository.currentMidFlow.collect { mid ->
                accountGeneration++
                currentMid = mid
                loadJob?.cancel()
                followJobs.values.forEach { it.cancel() }
                followJobs.clear()
                _uiState.value = NewFollowersUiState(isLoggedIn = mid > 0L)
                if (mid > 0L) refresh()
            }
        }
    }

    fun refresh() = load(reset = true)

    fun loadMore() = load(reset = false)

    private fun isCurrentAccount(generation: Long): Boolean =
        generation == accountGeneration && currentMid == authRepository.currentMidFlow.value

    private fun load(reset: Boolean) {
        val state = _uiState.value
        // Serialize list refreshes with relation changes so a stale page cannot undo a successful follow.
        if (!state.isLoggedIn || state.isBusy || state.followingMids.isNotEmpty() ||
            state.pendingUnfollowMid != null ||
            !isCurrentAccount(accountGeneration) || (!reset && !state.hasMore)
        ) return
        val generation = accountGeneration
        val page = if (reset) 1 else state.page + 1
        _uiState.update {
            it.copy(
                isLoading = reset && it.items.isEmpty(),
                isRefreshing = reset && it.items.isNotEmpty(),
                isLoadingMore = !reset,
                errorMessage = null,
                loadMoreError = null
            )
        }
        loadJob = viewModelScope.launch {
            try {
                val result = imRepository.fetchNewFollowers(page = page)
                if (!isCurrentAccount(generation)) return@launch
                _uiState.update { it.withPage(result, page) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isCurrentAccount(generation)) return@launch
                val message = if (reset) "加载新增粉丝失败，请重试" else "加载更多粉丝失败，请重试"
                Logger.e("NewFollowersViewModel", e) { message }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = if (reset) message else null,
                        loadMoreError = if (reset) null else message
                    )
                }
            }
        }
    }

    fun onFollowClick(mid: Long) {
        val state = _uiState.value
        val item = state.items.firstOrNull { it.mid == mid } ?: return
        if (!state.isLoggedIn || state.isBusy || mid <= 0L || mid == currentMid ||
            state.pendingUnfollowMid != null || mid in state.followingMids ||
            !isCurrentAccount(accountGeneration)
        ) return
        if (item.isFollowing) {
            _uiState.update { it.copy(pendingUnfollowMid = mid) }
        } else {
            setFollowing(mid, isFollow = true)
        }
    }

    fun confirmUnfollow() {
        val state = _uiState.value
        val mid = state.pendingUnfollowMid ?: return
        if (state.items.none { it.mid == mid && it.isFollowing }) {
            dismissUnfollow()
            return
        }
        setFollowing(mid, isFollow = false)
    }

    fun dismissUnfollow() {
        _uiState.update { it.copy(pendingUnfollowMid = null) }
    }

    private fun setFollowing(mid: Long, isFollow: Boolean) {
        val state = _uiState.value
        val item = state.items.firstOrNull { it.mid == mid } ?: return
        if (!state.isLoggedIn || state.isBusy || mid <= 0L || mid == currentMid ||
            item.isFollowing == isFollow || mid in state.followingMids ||
            !isCurrentAccount(accountGeneration) ||
            (isFollow && state.pendingUnfollowMid != null) ||
            (!isFollow && state.pendingUnfollowMid != mid)
        ) return
        val generation = accountGeneration
        _uiState.update {
            it.copy(
                followingMids = it.followingMids + mid,
                pendingUnfollowMid = null,
                followError = null
            )
        }
        followJobs[mid] = viewModelScope.launch {
            try {
                spaceRepository.modifyRelation(fid = mid, isFollow = isFollow)
                if (!isCurrentAccount(generation)) return@launch
                _uiState.update { state ->
                    state.copy(items = state.items.map { if (it.mid == mid) it.copy(isFollowing = isFollow) else it })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isCurrentAccount(generation)) return@launch
                val action = if (isFollow) "回关" else "取消关注"
                Logger.e("NewFollowersViewModel", e) { "${action}失败" }
                _uiState.update { it.copy(followError = "${action}失败，请稍后重试") }
            } finally {
                if (isCurrentAccount(generation)) {
                    followJobs.remove(mid)
                    _uiState.update { it.copy(followingMids = it.followingMids - mid) }
                }
            }
        }
    }

    fun dismissFollowError() {
        _uiState.update { it.copy(followError = null) }
    }
}
