package com.dhs0319.bills.core.comment

import android.content.Context
import androidx.core.content.edit
import com.dhs0319.bills.core.common.AuthProvider
import com.dhs0319.bills.core.common.BiliConstants
import com.dhs0319.bills.core.common.media.httpsImageUrl
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.CommentEmotePackage
import com.dhs0319.bills.infra.network.BiliRestClient
import com.dhs0319.bills.infra.network.BiliRestParamBuilder
import com.dhs0319.bills.infra.network.BiliRestProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val DEFAULT_PACKAGE_IDS = listOf(1L, 2L, 3L, 4L)

@Singleton
class CommentEmoteRepository @Inject constructor(
    @param:ApplicationContext context: Context,
    private val restClient: BiliRestClient,
    private val restParamBuilder: BiliRestParamBuilder,
    private val authProvider: AuthProvider
) {
    private val prefs = context.getSharedPreferences("comment_emotes", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    suspend fun fetchPackages(force: Boolean = false): List<CommentEmotePackage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val mid = authProvider.mid
            val cacheKey = "packages_$mid"
            val cached = runCatching {
                prefs.getString(cacheKey, null)?.let { parseCommentEmotePackages(JSONObject(it)) }
            }.getOrNull().orEmpty()
            val fresh = System.currentTimeMillis() - prefs.getLong("updated_$mid", 0L) < CACHE_TTL_MS
            if (!force && fresh && cached.isNotEmpty()) return@withLock cached
            try {
                val params = restParamBuilder.app(
                    BiliRestProfile.APP, System.currentTimeMillis() / 1000L, authProvider.accessToken
                ) + ("business" to "reply")
                val panel = restClient.getSigned(
                    "${BiliConstants.BASE_URL_API}$PANEL_ENDPOINT", params
                ).optJSONObject("data")?.optJSONArray("packages") ?: JSONArray()
                val packages = linkedMapOf<Long, JSONObject>()
                for (index in 0 until panel.length()) {
                    val item = panel.optJSONObject(index) ?: continue
                    val id = item.optLong("id")
                    if (id > 0L) packages[id] = item
                }
                // The user panel can contain only summaries, or omit the built-in free packages.
                val missing = (DEFAULT_PACKAGE_IDS + packages.keys).distinct().filter { id ->
                    packages[id]?.optJSONArray("emote")?.length().let { it == null || it == 0 }
                }
                if (missing.isNotEmpty()) {
                    val details = restClient.getSigned(
                        "${BiliConstants.BASE_URL_API}$PACKAGE_ENDPOINT",
                        params + ("ids" to missing.joinToString(","))
                    ).optJSONObject("data")?.optJSONArray("packages") ?: JSONArray()
                    for (index in 0 until details.length()) {
                        val item = details.optJSONObject(index) ?: continue
                        packages[item.optLong("id")] = item
                    }
                }
                val data = JSONObject().put("packages", JSONArray(packages.values.toList()))
                val result = parseCommentEmotePackages(data)
                check(result.isNotEmpty()) { "暂无可用表情包" }
                if (authProvider.mid == mid) {
                    prefs.edit {
                        putString(cacheKey, data.toString())
                        putLong("updated_$mid", System.currentTimeMillis())
                    }
                }
                result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                cached.ifEmpty { throw error }
            }
        }
    }

    fun recent(packages: List<CommentEmotePackage>): List<CommentEmote> {
        val catalog = packages.flatMap { it.emotes }.filter { it.available }.associateBy { it.text }
        val tokens = runCatching {
            JSONArray(prefs.getString("recent_${authProvider.mid}", "[]"))
        }.getOrElse { JSONArray() }
        return (0 until tokens.length()).mapNotNull { catalog[tokens.optString(it)] }.distinctBy { it.text }
    }

    fun recordRecent(emote: CommentEmote, packages: List<CommentEmotePackage>): List<CommentEmote> {
        val items = (listOf(emote) + recent(packages)).distinctBy { it.text }.take(RECENT_LIMIT)
        prefs.edit { putString("recent_${authProvider.mid}", JSONArray(items.map { it.text }).toString()) }
        return items
    }

    private companion object {
        const val PANEL_ENDPOINT = "/x/emote/user/panel"
        const val PACKAGE_ENDPOINT = "/x/emote/package"
        const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        const val RECENT_LIMIT = 32
    }
}

internal fun parseCommentEmotePackages(data: JSONObject): List<CommentEmotePackage> {
    val packages = data.optJSONArray("packages") ?: return emptyList()
    return (0 until packages.length()).mapNotNull { index ->
        val item = packages.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optLong("id")
        val isText = item.optInt("type") == 4
        val entries = item.optJSONArray("emote") ?: return@mapNotNull null
        val emotes = (0 until entries.length()).mapNotNull emote@{ emoteIndex ->
            val entry = entries.optJSONObject(emoteIndex) ?: return@emote null
            val text = entry.optString("text").takeIf { it.isNotBlank() } ?: return@emote null
            val rawUrl = entry.optString("url")
            val url = if (isText) "" else rawUrl.takeIf {
                it.startsWith("http://") || it.startsWith("https://") || it.startsWith("//")
            }?.httpsImageUrl() ?: return@emote null
            val flags = entry.optJSONObject("flags")
            CommentEmote(
                text = text,
                url = url,
                size = entry.optJSONObject("meta")?.optLong("size", 1L)?.coerceAtLeast(1L) ?: 1L,
                available = id in DEFAULT_PACKAGE_IDS ||
                    (flags?.optBoolean("unlocked", true) != false && flags?.optBoolean("no_access", false) != true)
            )
        }.distinctBy { it.text }
        if (id <= 0L || emotes.isEmpty()) return@mapNotNull null
        CommentEmotePackage(
            id = id,
            name = item.optString("text").ifBlank { "表情包" },
            iconUrl = item.optString("url").httpsImageUrl(),
            emotes = emotes,
            isText = isText
        )
    }.distinctBy { it.id }
}
