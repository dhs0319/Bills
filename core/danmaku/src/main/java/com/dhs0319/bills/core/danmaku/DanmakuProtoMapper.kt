package com.dhs0319.bills.core.danmaku

import com.bapis.bilibili.community.service.dm.v1.DanmakuElem
import com.dhs0319.bills.core.model.DanmakuItem

fun DanmakuElem.toDanmakuItem(): DanmakuItem {
    return DanmakuItem(
        id = id,
        idStr = idStr,
        progressMs = progress,
        mode = mode,
        fontSize = fontsize,
        color = color,
        midHash = midHash,
        content = content,
        createdAtEpochSecond = ctime,
        weight = weight,
        action = action,
        pool = pool,
        attr = attr,
        likeCount = likeCount,
        animation = animation,
        extra = extra,
        colorfulType = colorfulValue,
        type = type,
        oid = oid,
        dmFromType = dmFromValue
    )
}
