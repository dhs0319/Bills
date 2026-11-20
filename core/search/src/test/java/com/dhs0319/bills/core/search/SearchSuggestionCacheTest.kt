package com.dhs0319.bills.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchSuggestionCacheTest {
    @Test
    fun cachedSuggestionsExpireAfterTheShortTtl() {
        var now = 0L
        val cache = SearchSuggestionCache(clock = { now }, ttlMillis = 1_000L)
        val suggestions = listOf("first", "second")
        cache.put("query", suggestions)

        now = 999L
        assertEquals(suggestions, cache.get("query"))
        now = 1_000L
        assertNull(cache.get("query"))
    }

    @Test
    fun cacheEvictsLeastRecentlyUsedQueriesAtItsCapacity() {
        val cache = SearchSuggestionCache(clock = { 0L }, maxEntries = 2)
        cache.put("first", listOf("one"))
        cache.put("second", listOf("two"))
        cache.get("first")
        cache.put("third", listOf("three"))

        assertEquals(listOf("one"), cache.get("first"))
        assertNull(cache.get("second"))
        assertEquals(listOf("three"), cache.get("third"))
    }
}
