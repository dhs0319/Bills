package com.naaammme.bbspace.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class FeedItem(
    val cardType: String,
    val cardGoto: String,
    val goto: String,
    val param: String,
    val uri: String,
    val title: String,
    val cover: String,
    val coverLeftText1: String?,
    val coverLeftText2: String?,
    val coverRightText: String?,
    val idx: Long,
    val target: VideoTarget?,
    val liveRoute: LiveRoute?,
    val descButton: DescButton?,
    val rcmdReason: RcmdReason?,
    val args: FeedArgs?,
    val threePointV2: List<ThreePointItem>?,
    val dislikeContext: FeedDislikeContext?
)

@Immutable
data class DescButton(
    val text: String,
    val uri: String
)

@Immutable
data class RcmdReason(
    val text: String,
    val textColor: String?,
    val bgColor: String?,
    val textColorNight: String?,
    val bgColorNight: String?
)

@Immutable
data class FeedArgs(
    val upId: Long,
    val upName: String?,
    val tid: Int,
    val tname: String?,
    val aid: Long
)

@Immutable
data class ThreePointItem(
    val title: String,
    val subtitle: String?,
    val type: String,
    val reasons: List<ThreePointReason>?,
    val feedbacks: List<ThreePointReason>?
)

@Immutable
data class ThreePointReason(
    val id: Int,
    val name: String,
    val toast: String,
    val extra: String?,
    val kind: ThreePointReasonKind
)

enum class ThreePointReasonKind {
    DISLIKE,
    FEEDBACK
}

@Immutable
data class FeedDislikeContext(
    val id: String,
    val goto: String,
    val useHdProfile: Boolean,
    val spmid: String,
    val fromSpmid: String,
    val fromModule: String?,
    val trackId: String?,
    val reportData: String?,
    val mid: Long?,
    val rid: Long?,
    val tagId: Long?
)

data class FeedToast(
    val hasToast: Boolean,
    val message: String
)
