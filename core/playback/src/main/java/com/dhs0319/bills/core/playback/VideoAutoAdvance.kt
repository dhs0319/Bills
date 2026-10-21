package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.model.PlayBiz
import com.dhs0319.bills.core.model.PlaybackState
import com.dhs0319.bills.core.model.VideoPlaybackState
import com.dhs0319.bills.core.model.VideoTarget

internal fun nextVideoTarget(
    target: VideoTarget,
    state: VideoPlaybackState
): VideoTarget.Ugc? {
    if (
        target !is VideoTarget.Ugc || state.biz != PlayBiz.UGC ||
        state.playbackState != PlaybackState.Ended || !state.playWhenReady ||
        state.isPreparing || state.waitingForPlay || state.detailLoading ||
        state.error != null || state.playerError != null || state.playbackSource == null
    ) return null

    val detail = state.detail ?: return null
    val ids = state.ids
    if (ids.aid <= 0L || ids.cid <= 0L) return null
    if (target.aid > 0L && target.aid != ids.aid) return null
    if (target.cid > 0L && target.cid != ids.cid) return null

    val pageIndex = detail.pages.indexOfFirst { it.cid == ids.cid }
    if (pageIndex < 0) return null
    val nextPage = detail.pages.getOrNull(pageIndex + 1) ?: return null
    if (nextPage.cid <= 0L || nextPage.cid == ids.cid) return null
    return target.copy(
        aid = ids.aid,
        cid = nextPage.cid,
        bvid = ids.bvid ?: target.bvid
    )
}
