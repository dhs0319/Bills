package com.naaammme.bbspace.feature.home.article

import androidx.compose.runtime.Immutable
import com.naaammme.bbspace.core.model.article.ArticleRecommendItem

@Immutable
data class HomeArticleUiState(
    val items: List<ArticleRecommendItem> = emptyList(),
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)
