package com.dhs0319.bills.core.model.im

import androidx.compose.runtime.Immutable

@Immutable
data class NewFollowerItem(
    val mid: Long,
    val name: String,
    val avatar: String?,
    val followedAt: Long,
    val isFollowing: Boolean
)

@Immutable
data class NewFollowersPage(
    val items: List<NewFollowerItem>,
    val hasMore: Boolean
)
