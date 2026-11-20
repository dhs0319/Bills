package com.dhs0319.bills.core.search

import com.dhs0319.bills.core.model.SearchLandingData
import com.dhs0319.bills.core.model.SearchSuggestion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Keeps one account's landing page briefly and joins concurrent loads behind the same lock. */
internal class SearchLandingCache(
    private val clock: () -> Long,
    private val ttlMillis: Long = 60_000L
) {
    private val mutex = Mutex()
    private var cached: Entry? = null

    suspend fun load(
        account: Long,
        forceRefresh: Boolean = false,
        fetch: suspend () -> SearchLandingData
    ): SearchLandingData = mutex.withLock {
        val entry = cached
        if (!forceRefresh && entry?.account == account && clock() - entry.loadedAt in 0 until ttlMillis) {
            return@withLock entry.data
        }
        fetch().also { cached = Entry(account = account, loadedAt = clock(), data = it) }
    }

    suspend fun refreshDiscovery(
        account: Long,
        fetch: suspend () -> List<SearchSuggestion>
    ): List<SearchSuggestion> = mutex.withLock {
        fetch().also { discovery ->
            cached?.takeIf { it.account == account }?.let { entry ->
                // Refreshing discovery must not extend the lifetime of the hot search list.
                cached = entry.copy(data = entry.data.copy(discovery = discovery))
            }
        }
    }

    private data class Entry(
        val account: Long,
        val loadedAt: Long,
        val data: SearchLandingData
    )
}
