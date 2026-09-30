package com.dhs0319.bills.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class DanmakuWindow(
    val id: Long,
    val items: List<DanmakuItem>
)

@Immutable
data class DanmakuSessionState(
    val sourceKey: String? = null,
    /** 当前播放位置所在分段。 */
    val window: DanmakuWindow? = null,
    /** 下一分段预取结果，命中后可在分段边界直接切换，避免等待网络。 */
    val prefetchWindow: DanmakuWindow? = null,
    val lastError: String? = null
)
