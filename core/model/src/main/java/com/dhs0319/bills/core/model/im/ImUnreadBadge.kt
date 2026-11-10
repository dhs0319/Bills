package com.dhs0319.bills.core.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ImUnreadBadge {
    data object None : ImUnreadBadge
    data object Dot : ImUnreadBadge
    data class Count(val count: Long, val text: String) : ImUnreadBadge
}
