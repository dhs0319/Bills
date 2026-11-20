package com.dhs0319.bills.core.search

import com.dhs0319.bills.core.model.SearchLandingData
import com.dhs0319.bills.core.model.SearchSuggestion
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class SearchLandingCacheTest {
    @Test
    fun repeatedEntriesReuseDataUntilExpiry() = runBlocking {
        var now = 0L
        var requests = 0
        val cache = SearchLandingCache(clock = { now }, ttlMillis = 1_000L)
        val first = cache.load(1L) { requests++; landing("first") }

        now = 999L
        repeat(100) {
            assertSame(first, cache.load(1L) { requests++; landing("unexpected") })
        }
        assertEquals(1, requests)

        now = 1_000L
        assertEquals(landing("fresh"), cache.load(1L) { requests++; landing("fresh") })
        assertEquals(2, requests)
    }

    @Test
    fun overlappingLoadsForTheSameAccountMakeOneRequest() = runBlocking {
        var requests = 0
        val cache = SearchLandingCache(clock = { 0L })
        val response = CompletableDeferred<SearchLandingData>()
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            cache.load(1L) { requests++; response.await() }
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            cache.load(1L) { requests++; landing("unexpected") }
        }

        response.complete(landing("shared"))
        assertSame(first.await(), second.await())
        assertEquals(1, requests)
    }

    @Test
    fun changingAccountsDoesNotReusePersonalizedDiscovery() = runBlocking {
        val cache = SearchLandingCache(clock = { 0L })
        cache.load(1L) { landing("account one") }
        assertEquals(landing("account two"), cache.load(2L) { landing("account two") })
        assertEquals(landing("guest"), cache.load(0L) { landing("guest") })
    }

    @Test
    fun explicitReloadBypassesFreshCache() = runBlocking {
        val cache = SearchLandingCache(clock = { 0L })
        cache.load(1L) { landing("old") }
        val fresh = cache.load(1L, forceRefresh = true) { landing("new") }
        assertEquals(landing("new"), fresh)
        assertSame(fresh, cache.load(1L) { error("Expected cached reload result") })
    }

    @Test
    fun discoveryRefreshUpdatesCacheWithoutExtendingTrendingExpiry() = runBlocking {
        var now = 0L
        val cache = SearchLandingCache(clock = { now }, ttlMillis = 1_000L)
        val initial = cache.load(1L) { landing("initial") }

        now = 900L
        val discovery = listOf(suggestion("refreshed discovery"))
        cache.refreshDiscovery(1L) { discovery }
        val cached = cache.load(1L) { error("Expected updated cache") }
        assertEquals(initial.trending, cached.trending)
        assertEquals(discovery, cached.discovery)

        now = 1_000L
        assertEquals(landing("fresh trending"), cache.load(1L) { landing("fresh trending") })
    }

    @Test
    fun anotherAccountsRefreshCannotReplaceCachedDiscovery() = runBlocking {
        val cache = SearchLandingCache(clock = { 0L })
        val initial = cache.load(1L) { landing("account one") }
        cache.refreshDiscovery(2L) { listOf(suggestion("account two")) }
        assertSame(initial, cache.load(1L) { error("Expected original account data") })
    }

    @Test
    fun failedRefreshPreservesUsableCache() = runBlocking {
        val cache = SearchLandingCache(clock = { 0L })
        val initial = cache.load(1L) { landing("initial") }
        try {
            cache.refreshDiscovery(1L) { throw IOException("offline") }
            fail("Expected the refresh error")
        } catch (_: IOException) {
            assertSame(initial, cache.load(1L) { error("Expected retained data") })
        }
    }

    @Test
    fun cancelledLoadReleasesTheLockAndCanBeRetried() = runBlocking {
        val cache = SearchLandingCache(clock = { 0L })
        var requests = 0
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            cache.load(1L) { requests++; awaitCancellation() }
        }
        first.cancelAndJoin()

        assertEquals(landing("retry"), cache.load(1L) { requests++; landing("retry") })
        assertEquals(2, requests)
    }

    private fun landing(name: String) = SearchLandingData(
        trending = listOf(suggestion("trending $name")),
        discovery = listOf(suggestion("discovery $name"))
    )

    private fun suggestion(keyword: String) = SearchSuggestion(keyword = keyword, title = keyword)
}
