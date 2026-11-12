package com.dhs0319.bills.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.model.SearchAuthor
import com.dhs0319.bills.core.model.SearchFilter
import com.dhs0319.bills.core.model.SearchOrder
import com.dhs0319.bills.core.model.SearchReq
import com.dhs0319.bills.core.model.SearchTime
import com.dhs0319.bills.core.model.SearchVideo
import com.dhs0319.bills.core.search.SearchLandingRepository
import com.dhs0319.bills.core.search.SearchRepository
import com.dhs0319.bills.feature.search.landing.SearchLandingUiState
import com.dhs0319.bills.feature.search.landing.selectSearchPlaceholder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: SearchRepository,
    private val landingRepo: SearchLandingRepository
) : ViewModel() {

    var input by mutableStateOf("")
        private set

    var keyword by mutableStateOf("")
        private set

    var order by mutableStateOf(SearchOrder.DEFAULT)
        private set

    var filters by mutableStateOf<List<SearchFilter>>(emptyList())
        private set

    var time by mutableStateOf(SearchTime())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    val canLoadMore: Boolean
        get() = next.isNotBlank()

    private val _authors = MutableStateFlow<List<SearchAuthor>>(emptyList())
    val authors = _authors.asStateFlow()

    private val _videos = MutableStateFlow<List<SearchVideo>>(emptyList())
    val videos = _videos.asStateFlow()

    private val authorIds = hashSetOf<Long>()
    private val videoIds = hashSetOf<Pair<Long, Long>>()

    var landingState by mutableStateOf(SearchLandingUiState())
        private set

    val histories = repo.observeHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private var next = ""
    private var sel by mutableStateOf<Map<String, Set<String>>>(emptyMap())
    private var searchGeneration = 0L
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        loadLanding()
    }

    fun loadLanding(forceRefresh: Boolean = false) {
        if (landingState.isLoading || landingState.isRefreshingDiscovery) return
        landingState = landingState.copy(
            isLoading = true,
            trendingError = null,
            discoveryError = null
        )
        viewModelScope.launch {
            try {
                val data = landingRepo.load(forceRefresh)
                landingState = landingState.copy(
                    trending = data.trending,
                    discovery = data.discovery,
                    discoveryError = null,
                    searchPlaceholder = landingState.searchPlaceholder ?: selectSearchPlaceholder(data.discovery)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "搜索首页加载失败" }
                landingState = landingState.copy(
                    trendingError = "热搜加载失败，请重试",
                    discoveryError = "搜索发现加载失败，请重试"
                )
            } finally {
                landingState = landingState.copy(isLoading = false)
            }
        }
    }

    fun refreshDiscovery() {
        if (landingState.isLoading || landingState.isRefreshingDiscovery) return
        landingState = landingState.copy(isRefreshingDiscovery = true, discoveryError = null)
        viewModelScope.launch {
            try {
                val discovery = landingRepo.refreshDiscovery()
                landingState = landingState.copy(
                    discovery = discovery,
                    searchPlaceholder = landingState.searchPlaceholder ?: selectSearchPlaceholder(discovery)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "搜索发现刷新失败" }
                landingState = landingState.copy(discoveryError = "刷新失败，请重试")
            } finally {
                landingState = landingState.copy(isRefreshingDiscovery = false)
            }
        }
    }

    fun selectedOf(key: String): Set<String> {
        return sel[key].orEmpty()
    }

    fun updateInput(value: String) {
        input = value
    }

    fun submitSearch(
        value: String = input,
        recordHistory: Boolean = true
    ) {
        val query = value.trim()
        input = query
        if (query.isEmpty() || isLoading) return
        val fresh = query != keyword
        if (fresh) {
            clearSearchState()
        }
        search(query)
        if (recordHistory) {
            viewModelScope.launch {
                try {
                    repo.recordHistory(query)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(TAG, e) { "搜索历史保存失败" }
                }
            }
        }
    }

    fun deleteHistory(keyword: String) {
        viewModelScope.launch {
            repo.deleteHistory(keyword)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearHistory()
        }
    }

    fun applyFilter(key: String, params: Set<String>, nextTime: SearchTime = time) {
        if (filters.none { it.key == key }) return
        val nextSel = sel.toMutableMap().apply {
            if (params.isEmpty()) remove(key) else put(key, params)
        }
        val timeValue = if (key == SINCE_KEY) nextTime else time
        applyFilters(nextSel, timeValue)
    }

    fun applyFilters(nextSel: Map<String, Set<String>>, nextTime: SearchTime = time) {
        val normalizedSel = buildMap {
            filters.forEach { filter ->
                val picked = nextSel[filter.key].orEmpty()
                val valid = filter.ops
                    .asSequence()
                    .filter { !it.isDefault }
                    .map { it.param }
                    .filter { it in picked }
                    .toList()
                if (valid.isEmpty()) return@forEach
                put(filter.key, if (filter.single) setOf(valid.first()) else valid.toSet())
            }
        }
        val timeValue = if (normalizedSel[SINCE_KEY]?.singleOrNull() == CUSTOM_TIME) {
            nextTime
        } else {
            SearchTime()
        }
        val nextOrder = SearchOrder.fromParam(normalizedSel[SORT_KEY]?.firstOrNull())
        if (sel == normalizedSel && time == timeValue && order == nextOrder) return
        sel = normalizedSel
        time = timeValue
        order = nextOrder
        if (keyword.isBlank()) return
        search(keyword)
    }

    fun consumeBack(): Boolean {
        val hasState = keyword.isNotBlank() ||
                _authors.value.isNotEmpty() ||
                _videos.value.isNotEmpty() ||
                isLoading ||
                isLoadingMore ||
                errorMessage != null ||
                filters.isNotEmpty() ||
                sel.isNotEmpty() ||
                time.isActive
        if (!hasState) return false
        clearSearchState()
        return true
    }

    fun loadMore() {
        if (keyword.isBlank() || next.isBlank() || isLoading || isLoadingMore) return
        val generation = searchGeneration
        val request = buildReq(keyword, next)
        isLoadingMore = true
        errorMessage = null
        loadMoreJob = viewModelScope.launch {
            try {
                val page = repo.search(request)
                if (generation != searchGeneration) return@launch
                next = page.next.takeUnless { it == request.next }.orEmpty()
                val newAuthors = page.authors.filter { authorIds.add(it.mid) }
                val newVideos = page.videos.filter { videoIds.add(it.aid to it.cid) }
                _authors.value += newAuthors
                _videos.value += newVideos
                if (page.filters.isNotEmpty()) {
                    syncFilters(page.filters)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != searchGeneration) return@launch
                Logger.e(TAG, e) { "搜索翻页失败" }
                errorMessage = e.message
            } finally {
                if (generation == searchGeneration) isLoadingMore = false
            }
        }
    }

    private fun search(query: String) {
        val generation = ++searchGeneration
        searchJob?.cancel()
        loadMoreJob?.cancel()
        isLoading = true
        isLoadingMore = false
        keyword = query
        next = ""
        authorIds.clear()
        videoIds.clear()
        _authors.value = emptyList()
        _videos.value = emptyList()
        errorMessage = null
        val request = buildReq(query, "")
        searchJob = viewModelScope.launch {
            try {
                val page = repo.search(request)
                if (generation != searchGeneration) return@launch
                keyword = page.keyword
                input = page.keyword
                next = page.next
                syncFilters(page.filters)
                _authors.value = page.authors.filter { authorIds.add(it.mid) }
                _videos.value = page.videos.filter { videoIds.add(it.aid to it.cid) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != searchGeneration) return@launch
                Logger.e(TAG, e) { "搜索失败" }
                errorMessage = e.message
            } finally {
                if (generation == searchGeneration) isLoading = false
            }
        }
    }

    private fun buildReq(query: String, next: String): SearchReq {
        val filterMap = buildMap {
            filters.forEach { filter ->
                val picked = sel[filter.key].orEmpty()
                if (picked.isEmpty()) return@forEach
                val value = filter.ops
                    .asSequence()
                    .map { it.param }
                    .filter { it in picked }
                    .joinToString(",")
                if (value.isNotBlank()) {
                    put(filter.key, value)
                }
            }
        }
        val timeValue = if (sel[SINCE_KEY]?.singleOrNull() == CUSTOM_TIME && time.isActive) {
            time
        } else {
            SearchTime()
        }
        return SearchReq(
            keyword = query,
            next = next,
            order = order,
            filterMap = filterMap,
            time = timeValue
        )
    }

    private fun syncFilters(nextFilters: List<SearchFilter>) {
        filters = nextFilters
        val nextSel = buildMap {
            nextFilters.forEach { filter ->
                val picked = sel[filter.key].orEmpty()
                val valid = filter.ops
                    .asSequence()
                    .map { it.param }
                    .filter { it in picked }
                    .toCollection(linkedSetOf())
                if (valid.isEmpty()) return@forEach
                if (filter.single) {
                    put(filter.key, setOf(valid.first()))
                } else {
                    put(filter.key, valid)
                }
            }
        }
        sel = nextSel
        if (sel[SINCE_KEY]?.singleOrNull() != CUSTOM_TIME) {
            time = SearchTime()
        }
        order = SearchOrder.fromParam(sel[SORT_KEY]?.firstOrNull())
    }

    private fun clearSearchState() {
        ++searchGeneration
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = null
        loadMoreJob = null
        isLoading = false
        isLoadingMore = false
        keyword = ""
        next = ""
        order = SearchOrder.DEFAULT
        filters = emptyList()
        time = SearchTime()
        sel = emptyMap()
        errorMessage = null
        authorIds.clear()
        videoIds.clear()
        _authors.value = emptyList()
        _videos.value = emptyList()
    }

    private companion object {
        const val TAG = "SearchViewModel"
        const val SORT_KEY = "sort"
        const val SINCE_KEY = "since"
        const val CUSTOM_TIME = "custom"
    }
}
