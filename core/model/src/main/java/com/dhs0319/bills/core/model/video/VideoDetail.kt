package com.dhs0319.bills.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class VideoDetail(
    val title: String,
    val cover: String? = null,
    val owner: VideoOwner? = null,
    val stat: VideoStat? = null,
    val pubTs: Long? = null,
    val tags: List<String> = emptyList(),
    val desc: String = "",
    val staffs: List<VideoStaff> = emptyList(),
    val season: VideoSeason? = null,
    val pages: List<VideoPagePart> = emptyList(),
    val relates: List<VideoRelate> = emptyList(),
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isFavorited: Boolean = false,
    val userCoinCount: Int = 0
)

@Immutable
data class VideoDetailResult(
    val detail: VideoDetail,
    val ids: ResolvedVideoIds,
    val biz: PlayBiz
)

@Immutable
data class VideoOwner(
    val mid: Long,
    val name: String,
    val fansText: String?,
    val arcCountText: String?,
    val face: String?,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false
)

@Immutable
data class VideoStat(
    val view: String,
    val danmaku: String,
    val reply: String,
    val like: String,
    val coin: String,
    val fav: String,
    val share: String
)

@Immutable
data class VideoStaff(
    val role: String,
    val name: String
)

@Immutable
data class VideoSeason(
    val title: String,
    val subTitle: String?,
    val sections: List<VideoSeasonSection>,
    val id: Long = 0L
) {
    private val index = VideoSeasonIndex(sections)

    fun containsEntry(target: VideoTarget): Boolean {
        if (target is VideoTarget.Ugc && target.aid > 0L) {
            if (target.aid in index.ugcAids) return true
            if (target.bvid.isNullOrBlank()) return false
        }
        return sections.any { section ->
            section.eps.any { episode -> episode.target.isSameEntry(target) }
        }
    }

    fun sheetItemIndex(ids: ResolvedVideoIds): Int? {
        if (ids.epId <= 0L) return index.byCid[ids.cid]
        val episodeIndex = index.byEpisodeId[ids.epId]
        val cidIndex = index.withoutEpisodeIdByCid[ids.cid]
        return when {
            episodeIndex == null -> cidIndex
            cidIndex == null -> episodeIndex
            else -> minOf(episodeIndex, cidIndex)
        }
    }
}

private class VideoSeasonIndex(sections: List<VideoSeasonSection>) {
    val ugcAids = mutableSetOf<Long>()
    val byCid = mutableMapOf<Long, Int>()
    val byEpisodeId = mutableMapOf<Long, Int>()
    val withoutEpisodeIdByCid = mutableMapOf<Long, Int>()

    init {
        var itemIndex = 1 // The sheet starts with a title, then a header for each section.
        sections.forEach { section ->
            itemIndex++
            section.eps.forEach { episode ->
                val target = episode.target
                if (target is VideoTarget.Ugc && target.aid > 0L) ugcAids.add(target.aid)
                val episodeId = episode.episodeId
                if (episodeId > 0L) byEpisodeId.putIfAbsent(episodeId, itemIndex)
                if (episode.cid > 0L) {
                    byCid.putIfAbsent(episode.cid, itemIndex)
                    if (episodeId <= 0L) withoutEpisodeIdByCid.putIfAbsent(episode.cid, itemIndex)
                }
                itemIndex++
            }
        }
    }
}

@Immutable
data class VideoSeasonSection(
    val title: String,
    val eps: List<VideoSeasonEpisode>
)

@Immutable
data class VideoSeasonEpisode(
    val target: VideoTarget,
    val cid: Long,
    val title: String,
    val subTitle: String?,
    val cover: String?,
    val publishTimeText: String? = null,
    val durationText: String? = null,
    val viewText: String? = null,
    val danmakuText: String? = null,
    val pubTs: Long? = null
) {
    internal val episodeId: Long
        get() = when (val target = target) {
            is VideoTarget.Pgc -> target.epId
            is VideoTarget.Pugv -> target.epId
            is VideoTarget.Ugc -> 0L
        }

    fun isCurrent(ids: ResolvedVideoIds): Boolean {
        val epId = episodeId
        return if (epId > 0L && ids.epId > 0L) epId == ids.epId else cid > 0L && cid == ids.cid
    }
}

@Immutable
data class VideoPagePart(
    val cid: Long,
    val part: String,
    val durationSec: Long
)

@Immutable
data class VideoRelate(
    val target: VideoTarget.Ugc,
    val title: String,
    val cover: String,
    val author: String?,
    val durationText: String?,
    val viewText: String?,
    val danmakuText: String?,
    val reason: String?
)
