package com.dhs0319.bills.core.search

import com.bapis.bilibili.app.interfaces.v1.DefaultWordsReply
import com.dhs0319.bills.core.model.SearchDefaultWord
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchDefaultWordCacheTest {
    @Test
    fun mapperKeepsDisplayAndSearchKeywordSeparate() {
        val word = DefaultWordsReply.newBuilder()
            .setShow("推荐名称")
            .setWord("实际关键词")
            .setRefreshIntervalMilli(1_500L)
            .build()
            .toSearchDefaultWord()

        assertEquals("推荐名称", word?.display)
        assertEquals("实际关键词", word?.keyword)
        assertEquals(1_500L, word?.refreshIntervalMillis)
    }

    @Test
    fun mapperFallsBackToWordAndIgnoresEmptyResponse() {
        val word = DefaultWordsReply.newBuilder()
            .setWord("关键词")
            .build()
            .toSearchDefaultWord()

        assertEquals("关键词", word?.display)
        assertEquals("关键词", word?.keyword)
        assertNull(DefaultWordsReply.getDefaultInstance().toSearchDefaultWord())
    }

    @Test
    fun concurrentLoadsShareTheRequestUntilTheServerIntervalExpires() = runBlocking {
        var now = 0L
        val requests = AtomicInteger()
        val cache = SearchDefaultWordCache(clock = { now }, fallbackTtlMillis = 500L)
        val response = CompletableDeferred<SearchDefaultWord?>()
        val first = async(start = CoroutineStart.UNDISPATCHED) {
            cache.load { requests.incrementAndGet(); response.await() }
        }
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            cache.load { requests.incrementAndGet(); defaultWord("unexpected") }
        }

        response.complete(defaultWord("first", refreshIntervalMillis = 1_000L))
        assertEquals(defaultWord("first", refreshIntervalMillis = 1_000L), first.await())
        assertEquals(defaultWord("first", refreshIntervalMillis = 1_000L), second.await())
        assertEquals(1, requests.get())

        now = 999L
        assertEquals(defaultWord("first", refreshIntervalMillis = 1_000L), cache.load {
            requests.incrementAndGet()
            defaultWord("unexpected")
        })
        now = 1_000L
        assertEquals(defaultWord("fresh"), cache.load {
            requests.incrementAndGet()
            defaultWord("fresh")
        })
        assertEquals(2, requests.get())
    }

    private fun defaultWord(name: String, refreshIntervalMillis: Long = 0L) =
        SearchDefaultWord(show = name, word = name, refreshIntervalMillis = refreshIntervalMillis)
}
