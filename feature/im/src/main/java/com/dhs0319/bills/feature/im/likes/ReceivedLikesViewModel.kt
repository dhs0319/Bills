package com.dhs0319.bills.feature.im.likes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.im.ImRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReceivedLikesViewModel @Inject constructor(
    private val imRepo: ImRepository,
    authRepo: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ReceivedLikesUiState(
            isLoggedIn = authRepo.currentMidFlow.value > 0L,
            isLoading = authRepo.currentMidFlow.value > 0L
        )
    )
    val uiState = _uiState.asStateFlow()
    private var requestId = 0L
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            authRepo.currentMidFlow.collect { mid ->
                requestId++
                loadJob?.cancel()
                _uiState.value = ReceivedLikesUiState(isLoggedIn = mid > 0L)
                if (mid > 0L) refresh()
            }
        }
    }

    fun refresh() = load(reset = true)

    fun loadMore() = load(reset = false)

    private fun load(reset: Boolean) {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isBusy || (!reset && !state.canLoadMore)) return
        val callId = ++requestId
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
                val page = imRepo.fetchReceivedLikes(cursor = if (reset) null else state.cursor)
                if (callId != requestId) return@launch
                _uiState.update { it.withPage(page, reset) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (callId != requestId) return@launch
                val message = e.message?.takeIf { it.isNotBlank() }
                    ?: if (reset) "加载收到的喜欢失败" else "加载更多失败"
                Logger.e("ReceivedLikesViewModel", e) { message }
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
}
