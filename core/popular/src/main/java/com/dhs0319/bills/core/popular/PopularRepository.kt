package com.dhs0319.bills.core.popular

import com.bapis.bilibili.app.card.v1.Base
import com.bapis.bilibili.app.card.v1.Card
import com.bapis.bilibili.app.card.v1.SmallCoverV5
import com.bapis.bilibili.app.show.popular.v1.PopularReply
import com.bapis.bilibili.app.show.popular.v1.PopularResultReq
import com.dhs0319.bills.core.common.AuthProvider
import com.dhs0319.bills.core.common.media.httpsImageUrl
import com.dhs0319.bills.core.model.PopularVideoItem
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.VideoTargetTool
import com.dhs0319.bills.infra.grpc.BiliGrpcClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PopularCursor(
    val idx: Long = 0L,
    val lastParam: String = "",
    val ver: String = ""
)

data class PopularPage(
    val items: List<PopularVideoItem>,
    val nextCursor: PopularCursor?
)

@Singleton
class PopularRepository @Inject constructor(
    private val grpcClient: BiliGrpcClient,
    private val authProvider: AuthProvider
) {
    suspend fun fetchPage(cursor: PopularCursor): PopularPage {
        val request = PopularResultReq.newBuilder()
            .setIdx(cursor.idx)
            .setLastParam(cursor.lastParam)
            .setVer(cursor.ver)
            .setLoginEvent(when {
                cursor.idx != 0L -> 0
                authProvider.accessToken.isNotBlank() -> 2
                else -> 1
            })
            .build()
        val reply = grpcClient.call(
            endpoint = "bilibili.app.show.v1.Popular/Index",
            requestBytes = request.toByteArray(),
            parser = PopularReply.parser()
        )
        return withContext(Dispatchers.Default) {
            reply.toPopularPage(cursor)
        }
    }
}

internal fun PopularReply.toPopularPage(cursor: PopularCursor): PopularPage {
    // The last server card owns the cursor even when it is not a video card.
    val lastBase = itemsList.asReversed().firstNotNullOfOrNull { card ->
        card.baseOrNull()?.takeIf { it.idx > 0L && it.param.isNotBlank() }
    }
    val nextCursor = lastBase?.takeIf { it.idx > cursor.idx }?.let {
        PopularCursor(idx = it.idx, lastParam = it.param, ver = ver.ifBlank { cursor.ver })
    }
    return PopularPage(
        items = itemsList.mapNotNull { card ->
            if (card.itemCase == Card.ItemCase.SMALLCOVERV5) {
                card.smallCoverV5.toVideoItem()
            } else {
                null
            }
        },
        nextCursor = nextCursor
    )
}

private fun SmallCoverV5.toVideoItem(): PopularVideoItem? {
    if (base.goto != "av" || base.adInfo.isAd) return null
    val share = base.threePointV4.sharePlane
    val aid = base.param.toLongOrNull()?.takeIf { it > 0L }
        ?: share.aid.takeIf { it > 0L }
        ?: return null
    val title = base.title.trim().takeIf(String::isNotBlank) ?: return null
    val cover = base.cover.trim().httpsImageUrl()
    val ownerMid = share.authorId.takeIf { it > 0L }
        ?: up.id.takeIf { it > 0L }
        ?: base.args.upId
    val ownerName = share.author.ifBlank { up.name }
        .ifBlank { base.args.upName }
        .ifBlank { rightDesc1 }
        .trim()
    val cid = share.firstCid.takeIf { it > 0L }
        ?: base.playerArgs.cid.takeIf { it > 0L }
        ?: VideoTargetTool.cid(base.uri)
        ?: 0L
    val bvid = share.bvid.takeIf(String::isNotBlank)
        ?: VideoTargetTool.bvid(base.uri)
    val target = VideoTarget.Ugc(
        aid = aid,
        cid = cid,
        bvid = bvid,
        src = VideoTargetTool.feed(
            trackId = base.trackId,
            titleHint = title,
            ownerNameHint = ownerName,
            ownerMidHint = ownerMid.takeIf { it > 0L },
            coverHint = cover.takeIf(String::isNotBlank)
        )
    )
    return PopularVideoItem(
        aid = aid,
        bvid = bvid,
        title = title,
        cover = cover,
        ownerMid = ownerMid,
        ownerName = ownerName,
        durationText = coverRightText1,
        viewCountText = share.playNumber.ifBlank {
            rightDesc2.substringBefore('·').trim().removeSuffix("播放").trim()
        },
        publishTimeText = rightDesc2.substringAfter('·', "").trim(),
        target = target
    )
}

private fun Card.baseOrNull(): Base? = when (itemCase) {
    Card.ItemCase.SMALLCOVERV5 -> smallCoverV5.base
    Card.ItemCase.LARGECOVERV1 -> largeCoverV1.base
    Card.ItemCase.THREEITEMALLV2 -> threeItemAllV2.base
    Card.ItemCase.THREEITEMV1 -> threeItemV1.base
    Card.ItemCase.HOTTOPIC -> hotTopic.base
    Card.ItemCase.THREEITEMHV5 -> threeItemHV5.base
    Card.ItemCase.MIDDLECOVERV3 -> middleCoverV3.base
    Card.ItemCase.LARGECOVERV4 -> largeCoverV4.base
    Card.ItemCase.POPULARTOPENTRANCE -> popularTopEntrance.base
    Card.ItemCase.RCMDONEITEM -> rcmdOneItem.base
    Card.ItemCase.SMALLCOVERV5AD -> smallCoverV5Ad.base
    Card.ItemCase.TOPICLIST -> topicList.base
    else -> null
}
