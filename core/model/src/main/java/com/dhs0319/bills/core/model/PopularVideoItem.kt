package com.dhs0319.bills.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class PopularVideoItem(
    val aid: Long,
    val bvid: String?,
    val title: String,
    val cover: String,
    val ownerMid: Long,
    val ownerName: String,
    val durationText: String,
    val viewCountText: String,
    val publishTimeText: String,
    val target: VideoTarget.Ugc
) {
    val identityKey: String = aid.toString()

    val spaceRoute: SpaceRoute?
        get() = ownerMid.takeIf { it > 0L }?.let {
            SpaceRoute(mid = it, name = ownerName, fromViewAid = aid)
        }
}
