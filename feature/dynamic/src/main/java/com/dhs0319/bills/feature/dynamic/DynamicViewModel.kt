package com.dhs0319.bills.feature.dynamic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.dynamic.DynamicRepository
import com.dhs0319.bills.core.history.WatchLaterRepository
import com.dhs0319.bills.core.model.DynamicCursor
import com.dhs0319.bills.core.model.DynamicItem
import com.dhs0319.bills.core.model.DynamicRefresh
import com.dhs0319.bills.core.model.DynamicUpItem
import com.dhs0319.bills.core.model.DynamicUpList
import com.dhs0319.bills.core.space.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DynamicViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val repo: DynamicRepository,
    private val watchLaterRepo: WatchLaterRepository,
    private val spaceRepo: SpaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DynamicUiState(
            currentMid = authRepo.currentMidFlow.value,
            isLoggedIn = authRepo.currentMidFlow.value > 0
        )
    )
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()
    private val addingToWatchLater = mutableSetOf<Long>()
    private val unfollowingMids = mutableSetOf<Long>()
    private val visitedUpMids = mutableSetOf<Long>()
    private val pendingUpVisits = mutableMapOf<Long, PendingUpVisit>()
    private var upListRefreshJob: Job? = null
    private var upListRequestId = 0L
    private var authVersion = 0L

    init {
        viewModelScope.launch {
            authRepo.currentMidFlow.collect { mid ->
                authVersion += 1L
                _actionMessage.value = null
                upListRefreshJob?.cancel()
                pendingUpVisits.values.forEach { it.job?.cancel() }
                pendingUpVisits.clear()
                visitedUpMids.clear()
                _uiState.value = DynamicUiState(currentMid = mid, isLoggedIn = mid > 0L)
                if (mid > 0L) refresh()
            }
        }
    }

    fun visitUp(mid: Long) {
        val state = _uiState.value
        if (!state.isLoggedIn) return
        val upList = state.upList ?: return
        val item = upList.items.firstOrNull { it.uid == mid && it.hasUpdate } ?: return
        upListRequestId++
        visitedUpMids.add(mid)
        _uiState.update {
            it.copy(upList = it.upList?.copy(items = it.upList.items.map { up ->
                if (up.uid == mid) up.copy(hasUpdate = false) else up
            }))
        }
        val visit = PendingUpVisit(item, upList.footprint)
        pendingUpVisits[mid] = visit
        syncUpVisit(visit, authVersion)
    }

    fun refreshVisitedUps() {
        if (!_uiState.value.isLoggedIn ||
            (visitedUpMids.isEmpty() && pendingUpVisits.isEmpty()) ||
            upListRefreshJob?.isActive == true
        ) return
        val version = authVersion
        val visits = pendingUpVisits.values.toList()
        upListRefreshJob = viewModelScope.launch {
            try {
                visits.forEach { visit ->
                    visit.job?.join()
                    if (version != authVersion) return@launch
                    if (pendingUpVisits[visit.item.uid] === visit) {
                        syncUpVisit(visit, version).join()
                    }
                }
                val requestId = ++upListRequestId
                val upList = repo.fetchUpList() ?: return@launch
                if (version != authVersion || requestId != upListRequestId) return@launch
                val updated = upList.withVisitedBadges()
                _uiState.update { it.copy(upList = updated) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "刷新最近访问角标失败" }
            }
        }
    }

    private fun syncUpVisit(visit: PendingUpVisit, version: Long): Job {
        val job = viewModelScope.launch {
            try {
                repo.markUpVisited(visit.item, visit.footprint)
                if (version == authVersion && pendingUpVisits[visit.item.uid] === visit) {
                    pendingUpVisits.remove(visit.item.uid)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "同步最近访问已读状态失败" }
            }
        }
        visit.job = job
        return job
    }

    private fun DynamicUpList.withVisitedBadges(): DynamicUpList {
        // Keep the local dismissal until the server confirms the previous update is read.
        items.filter { !it.hasUpdate && it.uid !in pendingUpVisits }
            .forEach { visitedUpMids.remove(it.uid) }
        return copy(items = items.map { item ->
            if (item.uid in visitedUpMids) item.copy(hasUpdate = false) else item
        })
    }

    fun refresh() {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isLoading || state.isRefreshing || state.isLoadingMore) return
        val hasItems = state.items.isNotEmpty()
        _uiState.update {
            it.copy(
                isLoading = !hasItems,
                isRefreshing = hasItems,
                isLoadingMore = false,
                errorMessage = null,
                loadMoreError = null
            )
        }
        viewModelScope.launch {
            load(refresh = DynamicRefresh.NEW, reset = true)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.canLoadMore) return
        _uiState.update {
            it.copy(
                isLoadingMore = true,
                loadMoreError = null
            )
        }
        viewModelScope.launch {
            load(refresh = DynamicRefresh.HISTORY, reset = false)
        }
    }

    fun addToWatchLater(aid: Long) {
        if (aid <= 0L || !_uiState.value.isLoggedIn || !addingToWatchLater.add(aid)) return
        val version = authVersion
        viewModelScope.launch {
            try {
                watchLaterRepo.addVideo(aid)
                if (version == authVersion) _actionMessage.value = "已加入稍后再看"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "加入稍后再看失败" }
                if (version == authVersion) {
                    _actionMessage.value = e.message?.takeIf(String::isNotBlank) ?: "加入稍后再看失败"
                }
            } finally {
                addingToWatchLater.remove(aid)
            }
        }
    }

    fun unfollow(mid: Long) {
        val state = _uiState.value
        if (!state.isLoggedIn || mid <= 0L || mid == state.currentMid ||
            !unfollowingMids.add(mid)
        ) return
        val version = authVersion
        viewModelScope.launch {
            try {
                spaceRepo.modifyRelation(fid = mid, isFollow = false)
                if (version == authVersion) {
                    _actionMessage.value = "已取消关注"
                    refresh()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "取消关注失败" }
                if (version == authVersion) {
                    _actionMessage.value = e.message?.takeIf(String::isNotBlank) ?: "取消关注失败"
                }
            } finally {
                unfollowingMids.remove(mid)
            }
        }
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    private suspend fun load(
        refresh: DynamicRefresh,
        reset: Boolean
    ) {
        val state = _uiState.value
        val version = authVersion
        val requestId = ++upListRequestId
        try {
            val page = repo.fetchAll(
                cursor = if (reset) DEFAULT_CURSOR else state.cursor,
                refresh = refresh
            )
            if (version != authVersion) return
            val upList = if (requestId == upListRequestId) {
                page.upList?.withVisitedBadges()
            } else {
                null
            }
            _uiState.update {
                if (version != authVersion || !it.isLoggedIn) return@update it
                it.copy(
                    upList = upList ?: it.upList,
                    items = if (reset) page.items else it.items.mergePage(page.items),
                    cursor = page.cursor,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    hasMore = page.hasMore,
                    errorMessage = null,
                    loadMoreError = null
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (version != authVersion) return
            Logger.e(TAG, e) { "加载动态失败" }
            _uiState.update {
                if (!it.isLoggedIn) return@update it
                val msg = e.message ?: if (reset) "加载动态失败" else "加载更多动态失败"
                if (reset) {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        loadMoreError = msg
                    )
                }
            }
        }
    }

    private fun List<DynamicItem>.mergePage(
        incoming: List<DynamicItem>
    ): List<DynamicItem> {
        if (isEmpty()) return incoming
        if (incoming.isEmpty()) return this
        val seen = HashSet<String>(size + incoming.size)
        return buildList(size + incoming.size) {
            for (item in this@mergePage) {
                if (seen.add(item.id)) add(item)
            }
            for (item in incoming) {
                if (seen.add(item.id)) add(item)
            }
        }
    }

    private class PendingUpVisit(
        val item: DynamicUpItem,
        val footprint: String,
        var job: Job? = null
    )

    private companion object {
        const val TAG = "DynamicViewModel"
        val DEFAULT_CURSOR = DynamicCursor()
    }
}
