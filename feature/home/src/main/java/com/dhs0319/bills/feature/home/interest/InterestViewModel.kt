package com.dhs0319.bills.feature.home.interest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.feed.InterestRepository
import com.dhs0319.bills.core.model.UinterestResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterestUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val response: UinterestResponse? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class InterestViewModel @Inject constructor(
    private val repo: InterestRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InterestUiState(isLoggedIn = authRepo.currentMidFlow.value > 0L)
    )
    val uiState: StateFlow<InterestUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            authRepo.currentMidFlow.collect { mid ->
                loadJob?.cancel()
                _uiState.value = InterestUiState(isLoggedIn = mid > 0L)
                if (mid > 0L) refresh()
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isLoading || state.isRefreshing) return
        _uiState.update {
            it.copy(
                isLoading = it.response == null,
                isRefreshing = it.response != null,
                errorMessage = null
            )
        }
        loadJob = viewModelScope.launch { load() }
    }

    private suspend fun load() {
        try {
            val response = repo.fetchUinterest()
            currentCoroutineContext().ensureActive()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                    response = response
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Logger.e(TAG, e) { "加载兴趣标签失败" }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = e.message ?: "加载兴趣标签失败"
                )
            }
        }
    }

    private companion object {
        const val TAG = "InterestViewModel"
    }
}
