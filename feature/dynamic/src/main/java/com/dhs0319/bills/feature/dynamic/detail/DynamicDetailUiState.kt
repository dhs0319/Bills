package com.dhs0319.bills.feature.dynamic.detail

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.DynamicDetail

@Immutable
data class DynamicDetailUiState(
    val detail: DynamicDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
