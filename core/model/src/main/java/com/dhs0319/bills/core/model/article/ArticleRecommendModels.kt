package com.dhs0319.bills.core.model.article

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.SpaceRoute

@Immutable
data class ArticleRecommendItem(
    val id: Long,
    val title: String,
    val summary: String?,
    val cover: String?,
    val authorMid: Long?,
    val authorName: String?,
    val authorFace: String?,
    val categoryName: String?,
    val publishTimeText: String?,
    val viewCount: Long,
    val likeCount: Long,
    val replyCount: Long
) {
    val spaceRoute: SpaceRoute? = authorMid?.let { SpaceRoute(mid = it, name = authorName) }
    val statLine: String? = buildString {
        if (viewCount > 0L) append(viewCount).append(" 阅读")
        if (likeCount > 0L) {
            if (isNotEmpty()) append("  ")
            append(likeCount).append(" 点赞")
        }
        if (replyCount > 0L) {
            if (isNotEmpty()) append("  ")
            append(replyCount).append(" 评论")
        }
    }.takeIf(String::isNotEmpty)
}

@Immutable
data class ArticleRecommendPage(
    val items: List<ArticleRecommendItem>,
    val aidsLength: Int
)
