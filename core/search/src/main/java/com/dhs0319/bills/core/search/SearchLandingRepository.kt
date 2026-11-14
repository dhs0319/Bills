package com.dhs0319.bills.core.search

import android.os.SystemClock
import com.dhs0319.bills.core.auth.AuthStore
import com.dhs0319.bills.core.common.BiliConstants
import com.dhs0319.bills.core.model.SearchLandingData
import com.dhs0319.bills.core.model.SearchSuggestion
import com.dhs0319.bills.core.model.SearchSuggestionBadge
import com.dhs0319.bills.infra.network.BiliRestClient
import com.dhs0319.bills.infra.network.BiliRestParamBuilder
import com.dhs0319.bills.infra.network.BiliRestProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Singleton
class SearchLandingRepository @Inject constructor(
    private val restClient: BiliRestClient,
    private val restParamBuilder: BiliRestParamBuilder,
    private val authStore: AuthStore
) {
    private val cache = SearchLandingCache(clock = SystemClock::elapsedRealtime)

    suspend fun load(forceRefresh: Boolean = false): SearchLandingData {
        val account = authStore.mid
        val accessToken = authStore.accessToken
        return cache.load(account, forceRefresh) {
            withContext(Dispatchers.IO) { fetchLanding(accessToken) }
        }
    }

    private suspend fun fetchLanding(accessToken: String): SearchLandingData {
        val json = restClient.getSigned(
            url = "${BiliConstants.BASE_URL_APP}/x/v2/search/square",
            params = params(accessToken) + ("limit" to "10"),
            profile = BiliRestProfile.APP
        )
        val sections = json.getJSONArray("data")
        var trending = emptyList<SearchSuggestion>()
        var discovery = emptyList<SearchSuggestion>()
        for (index in 0 until sections.length()) {
            val section = sections.optJSONObject(index) ?: continue
            when (section.optString("type")) {
                "trending" -> trending = parseSuggestions(section.optJSONObject("data"), limit = 10, trending = true)
                "recommend" -> discovery = parseSuggestions(section.optJSONObject("data"), limit = 12)
            }
        }
        return SearchLandingData(trending = trending, discovery = discovery)
    }

    suspend fun refreshDiscovery(): List<SearchSuggestion> {
        val account = authStore.mid
        val accessToken = authStore.accessToken
        return cache.refreshDiscovery(account) {
            withContext(Dispatchers.IO) {
                val json = restClient.getSigned(
                    url = "${BiliConstants.BASE_URL_APP}/x/v2/search/recommend",
                    params = params(accessToken) + ("from" to "2"),
                    profile = BiliRestProfile.APP
                )
                parseSuggestions(json.getJSONObject("data"), limit = 12)
            }
        }
    }

    private fun params(accessToken: String): Map<String, String> {
        return restParamBuilder.app(
            profile = BiliRestProfile.APP,
            ts = System.currentTimeMillis() / 1_000,
            accessKey = accessToken
        )
    }

    private fun parseSuggestions(
        data: JSONObject?,
        limit: Int,
        trending: Boolean = false
    ): List<SearchSuggestion> {
        val list = data?.optJSONArray("list") ?: return emptyList()
        val suggestions = ArrayList<SearchSuggestion>(minOf(limit, list.length()))
        val keywords = HashSet<String>(limit)
        for (index in 0 until list.length()) {
            if (suggestions.size == limit) break
            val item = list.optJSONObject(index) ?: continue
            val keyword = item.text("keyword")
                .ifBlank { item.text("title") }
                .ifBlank { item.text("show_name") }
                .takeIf(String::isNotBlank) ?: continue
            if (!keywords.add(keyword)) continue
            val title = item.text("show_name")
                .ifBlank { item.text("title") }
                .ifBlank { keyword }
            suggestions += SearchSuggestion(
                keyword = keyword,
                title = title,
                subtitle = if (trending) {
                    null
                } else {
                    item.text("recommend_reason")
                        .ifBlank { item.text("pub_time") }
                        .removePrefix("·")
                        .trim()
                        .takeIf(String::isNotBlank)
                },
                badge = if (trending) badge(item) else null
            )
        }
        return suggestions
    }

    private fun JSONObject.text(key: String): String {
        return if (isNull(key)) "" else optString(key).trim()
    }

    private fun badge(item: JSONObject): SearchSuggestionBadge? {
        if (item.optBoolean("show_live_icon")) return SearchSuggestionBadge.LIVE
        return when (item.optInt("word_type")) {
            4 -> SearchSuggestionBadge.NEW
            5 -> SearchSuggestionBadge.HOT
            12 -> SearchSuggestionBadge.EXCLUSIVE
            15 -> SearchSuggestionBadge.PROMOTION
            else -> null
        }
    }
}
