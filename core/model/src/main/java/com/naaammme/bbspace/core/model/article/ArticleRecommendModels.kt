package com.naaammme.bbspace.core.model.article

import androidx.compose.runtime.Immutable

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
)

@Immutable
data class ArticleRecommendPage(
    val items: List<ArticleRecommendItem>,
    val aidsLength: Int
)
