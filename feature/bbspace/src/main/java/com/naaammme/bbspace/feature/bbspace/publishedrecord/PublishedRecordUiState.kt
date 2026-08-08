package com.naaammme.bbspace.feature.bbspace.publishedrecord

import androidx.compose.runtime.Immutable
import com.naaammme.bbspace.core.model.PublishedRecord

@Immutable
data class PublishedRecordUiState(
    val keywordInput: String = "",
    val hasQuery: Boolean = false,
    val sortDesc: Boolean = true,
    val items: List<PublishedRecord> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
)
