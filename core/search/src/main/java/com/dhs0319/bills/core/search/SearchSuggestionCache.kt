package com.dhs0319.bills.core.search

internal class SearchSuggestionCache(
    private val clock: () -> Long,
    private val ttlMillis: Long = 30_000L,
    private val maxEntries: Int = 50
) {
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)

    init {
        require(ttlMillis > 0L)
        require(maxEntries > 0)
    }

    @Synchronized
    fun get(query: String): List<String>? {
        val entry = entries[query] ?: return null
        if (clock() - entry.cachedAt !in 0 until ttlMillis) {
            entries.remove(query)
            return null
        }
        return entry.suggestions
    }

    @Synchronized
    fun put(query: String, suggestions: List<String>) {
        entries[query] = Entry(clock(), suggestions.toList())
        while (entries.size > maxEntries) {
            entries.entries.iterator().run {
                next()
                remove()
            }
        }
    }

    private data class Entry(
        val cachedAt: Long,
        val suggestions: List<String>
    )
}
