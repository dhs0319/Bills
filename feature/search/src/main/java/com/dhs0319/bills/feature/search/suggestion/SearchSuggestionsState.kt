package com.dhs0319.bills.feature.search.suggestion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dhs0319.bills.core.common.log.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

internal class SearchSuggestionsState(
    private val scope: CoroutineScope,
    private val fetch: suspend (String) -> List<String>
) {
    var query by mutableStateOf("")
        private set

    var items by mutableStateOf<List<String>>(emptyList())
        private set

    val isVisible: Boolean
        get() = query.isNotEmpty()

    private var request: Job? = null

    fun update(input: String) {
        val nextQuery = input.trim()
        if (nextQuery == query) return
        clear()
        query = nextQuery
        if (!isVisible) return
        request = scope.launch {
            delay(250)
            try {
                val suggestions = fetch(nextQuery)
                currentCoroutineContext().ensureActive()
                items = suggestions
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("SearchSuggestions") { "关键词联想加载失败: ${e.message}" }
            }
        }
    }

    fun clear() {
        request?.cancel()
        request = null
        query = ""
        items = emptyList()
    }
}
