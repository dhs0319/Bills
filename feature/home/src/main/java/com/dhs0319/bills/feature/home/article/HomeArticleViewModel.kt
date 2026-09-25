package com.dhs0319.bills.feature.home.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.article.ArticleRecommendRepository
import com.dhs0319.bills.core.model.article.ArticleRecommendItem
import com.dhs0319.bills.feature.home.paging.HomePage
import com.dhs0319.bills.feature.home.paging.HomePager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeArticleViewModel @Inject constructor(
    private val repository: ArticleRecommendRepository
) : ViewModel() {
    private data class PageKey(val page: Int = 1, val aids: List<Long> = emptyList())

    private val pager = HomePager<ArticleRecommendItem, PageKey>(
        scope = viewModelScope,
        initialKey = { PageKey() },
        itemKey = { it.id },
        loadPage = { key ->
            val page = repository.fetchHomePage(
                page = key.page,
                aids = key.aids.takeIf { it.isNotEmpty() }?.asReversed()?.joinToString(",")
            )
            val nextAids = (key.aids + page.items.map { it.id }).takeLast(page.aidsLength)
            HomePage(page.items, PageKey(key.page + 1, nextAids).takeIf { page.items.isNotEmpty() })
        },
        onError = { Logger.e("HomeArticleViewModel", it) { "加载专栏推荐失败" } }
    )
    val uiState = pager.state

    fun activate(refreshRequest: Int) = pager.activate(refreshRequest)
    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retryLoadMore() = pager.retryLoadMore()
}
