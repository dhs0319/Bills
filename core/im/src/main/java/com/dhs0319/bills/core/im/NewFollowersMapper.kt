package com.dhs0319.bills.core.im

import com.dhs0319.bills.core.common.media.httpsImageUrlOrNull
import com.dhs0319.bills.core.model.im.NewFollowerItem
import com.dhs0319.bills.core.model.im.NewFollowersPage
import org.json.JSONObject

internal fun JSONObject.toNewFollowersPage(page: Int, pageSize: Int, ownMid: Long): NewFollowersPage {
    val data = optJSONObject("data") ?: error("粉丝列表数据缺失")
    val list = data.optJSONArray("list")
    val rawCount = list?.length() ?: 0
    val items = buildList {
        for (index in 0 until rawCount) {
            val item = list?.optJSONObject(index) ?: continue
            val mid = item.optLong("mid")
            if (mid <= 0L || mid == ownMid) continue
            add(
                NewFollowerItem(
                    mid = mid,
                    name = item.optString("uname").takeUnless { it.isBlank() || it == "null" } ?: "用户$mid",
                    avatar = item.optString("face")
                        .takeUnless { it.isBlank() || it == "null" }
                        ?.let { if (it.startsWith("//")) "https:$it" else it }
                        .httpsImageUrlOrNull(),
                    followedAt = item.optLong("mtime").coerceAtLeast(0L),
                    isFollowing = isFollowedRelation(item.optInt("attribute"))
                )
            )
        }
    }.distinctBy { it.mid }
    val total = data.optLong("total", -1L)
    return NewFollowersPage(
        items = items,
        hasMore = rawCount > 0 && if (total >= 0L) page.toLong() * pageSize < total else rawCount >= pageSize
    )
}

internal fun isFollowedRelation(attribute: Int): Boolean = attribute == 1 || attribute == 2 || attribute == 6
