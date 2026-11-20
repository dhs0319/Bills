package com.dhs0319.bills.core.search

import com.dhs0319.bills.core.model.SearchDefaultWord
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SearchDefaultWordCache(
    private val clock: () -> Long,
    private val fallbackTtlMillis: Long = 5 * 60 * 1_000L
) {
    private val mutex = Mutex()
    private var cached: Entry? = null

    suspend fun load(fetch: suspend () -> SearchDefaultWord?): SearchDefaultWord? = mutex.withLock {
        val entry = cached
        if (entry != null && clock() - entry.loadedAt in 0 until entry.ttlMillis) {
            return@withLock entry.word
        }

        fetch()
            ?.takeIf { it.display.isNotBlank() }
            ?.also { word ->
                val ttlMillis = word.refreshIntervalMillis.takeIf { it > 0L } ?: fallbackTtlMillis
                cached = Entry(word = word, loadedAt = clock(), ttlMillis = ttlMillis)
            }
            ?: entry?.word
    }

    private data class Entry(
        val word: SearchDefaultWord,
        val loadedAt: Long,
        val ttlMillis: Long
    )
}
