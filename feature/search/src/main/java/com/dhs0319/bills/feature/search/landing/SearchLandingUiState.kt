package com.dhs0319.bills.feature.search.landing

import com.dhs0319.bills.core.model.SearchSuggestion

data class SearchLandingUiState(
    val trending: List<SearchSuggestion> = emptyList(),
    val discovery: List<SearchSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshingDiscovery: Boolean = false,
    val trendingError: String? = null,
    val discoveryError: String? = null,
    val searchPlaceholder: String? = null
)

internal fun selectSearchPlaceholder(discovery: List<SearchSuggestion>): String? = discovery
    .minByOrNull { updateAgeSeconds(it.subtitle) }
    ?.let { suggestion ->
        val updatedAt = suggestion.subtitle?.takeIf { "更新" in it }
        if (updatedAt == null) suggestion.title else "${suggestion.title} · $updatedAt"
    }

private val relativeUpdatePattern = Regex("""(\d+)\s*(秒|分钟|小时|天|周|个月|月|年)前\s*更新""")

private fun updateAgeSeconds(subtitle: String?): Double {
    if (subtitle == null || "更新" !in subtitle) return Double.POSITIVE_INFINITY
    val match = relativeUpdatePattern.find(subtitle)
    if (match != null) {
        val count = match.groupValues[1].toDoubleOrNull() ?: return Double.MAX_VALUE
        val unitSeconds = when (match.groupValues[2]) {
            "秒" -> 1.0
            "分钟" -> 60.0
            "小时" -> 3_600.0
            "天" -> 86_400.0
            "周" -> 604_800.0
            "个月", "月" -> 2_592_000.0
            else -> 31_536_000.0
        }
        return count * unitSeconds
    }
    return when {
        "刚刚" in subtitle || "今天" in subtitle -> 0.0
        "昨天" in subtitle -> 86_400.0
        "前天" in subtitle -> 172_800.0
        // Keep the server's update label even when it uses an unfamiliar format.
        else -> Double.MAX_VALUE
    }
}
