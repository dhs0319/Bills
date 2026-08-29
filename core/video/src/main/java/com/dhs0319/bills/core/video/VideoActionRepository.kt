package com.dhs0319.bills.core.video

import com.dhs0319.bills.core.auth.AuthStore
import com.dhs0319.bills.core.common.BiliConstants
import com.dhs0319.bills.infra.network.BiliRestClient
import com.dhs0319.bills.infra.network.BiliRestParamBuilder
import com.dhs0319.bills.infra.network.BiliRestProfile
import javax.inject.Inject
import javax.inject.Singleton

data class VideoFavoriteFolder(
    val id: Long,
    val title: String,
    val mediaCount: Int,
    val selected: Boolean
)

@Singleton
class VideoActionRepository @Inject constructor(
    private val restClient: BiliRestClient,
    private val restParamBuilder: BiliRestParamBuilder,
    private val authStore: AuthStore
) {
    suspend fun setLiked(aid: Long, liked: Boolean): String? {
        requireAidAndToken(aid)
        val json = restClient.postSigned(
            url = "${BiliConstants.BASE_URL_APP}$LIKE_ENDPOINT",
            params = appParams() + mapOf(
                "aid" to aid.toString(),
                "like" to if (liked) "0" else "1"
            ),
            profile = BiliRestProfile.APP
        )
        return json.optJSONObject("data")
            ?.optString("toast")
            ?.takeIf(String::isNotBlank)
    }

    suspend fun addCoins(aid: Long, amount: Int) {
        requireAidAndToken(aid)
        require(amount in 1..2) { "投币数量无效" }
        restClient.postSigned(
            url = "${BiliConstants.BASE_URL_APP}$COIN_ENDPOINT",
            params = appParams() + mapOf(
                "aid" to aid.toString(),
                "multiply" to amount.toString(),
                "select_like" to "0"
            ),
            profile = BiliRestProfile.APP
        )
    }

    suspend fun fetchFavoriteFolders(aid: Long): List<VideoFavoriteFolder> {
        requireAidAndToken(aid)
        val json = restClient.getSigned(
            url = "${BiliConstants.BASE_URL_API}$FAVORITE_FOLDERS_ENDPOINT",
            params = appParams() + mapOf(
                "up_mid" to authStore.mid.toString(),
                "type" to VIDEO_RESOURCE_TYPE,
                "rid" to aid.toString()
            ),
            profile = BiliRestProfile.APP
        )
        val list = json.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
        return buildList {
            for (index in 0 until list.length()) {
                val item = list.optJSONObject(index) ?: continue
                val id = item.optLong("id")
                val title = item.optString("title")
                if (id <= 0L || title.isBlank()) continue
                add(
                    VideoFavoriteFolder(
                        id = id,
                        title = title,
                        mediaCount = item.optInt("media_count"),
                        selected = item.optInt("fav_state") > 0
                    )
                )
            }
        }
    }

    suspend fun updateFavorites(
        aid: Long,
        addFolderIds: Set<Long>,
        removeFolderIds: Set<Long>
    ) {
        requireAidAndToken(aid)
        if (addFolderIds.isEmpty() && removeFolderIds.isEmpty()) return
        restClient.postSigned(
            url = "${BiliConstants.BASE_URL_API}$FAVORITE_DEAL_ENDPOINT",
            params = appParams() + buildMap {
                put("rid", aid.toString())
                put("type", VIDEO_RESOURCE_TYPE)
                if (addFolderIds.isNotEmpty()) {
                    put("add_media_ids", addFolderIds.joinToString(","))
                }
                if (removeFolderIds.isNotEmpty()) {
                    put("del_media_ids", removeFolderIds.joinToString(","))
                }
            },
            profile = BiliRestProfile.APP
        )
    }

    private fun requireAidAndToken(aid: Long) {
        check(aid > 0L) { "视频信息无效" }
        check(authStore.accessToken.isNotBlank()) { "请先登录" }
    }

    private fun appParams(): Map<String, String> {
        return restParamBuilder.app(
            profile = BiliRestProfile.APP,
            ts = System.currentTimeMillis() / 1000,
            accessKey = authStore.accessToken
        )
    }

    private companion object {
        const val LIKE_ENDPOINT = "/x/v2/view/like"
        const val COIN_ENDPOINT = "/x/v2/view/coin/add"
        const val FAVORITE_FOLDERS_ENDPOINT = "/x/v3/fav/folder/created/list-all"
        const val FAVORITE_DEAL_ENDPOINT = "/medialist/gateway/coll/resource/deal"
        const val VIDEO_RESOURCE_TYPE = "2"
    }
}
