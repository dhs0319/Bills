package com.dhs0319.bills.core.model

import androidx.compose.runtime.Immutable

/** items 发布后不再修改；更新内容时创建新的分段，供加载层和渲染层共享。 */
@Immutable
data class DanmakuWindow(
    val id: Long,
    val items: List<DanmakuItem>
)

@Immutable
data class DanmakuSessionState(
    val sourceKey: String? = null,
    /** 最近就绪的当前分段；新分段加载期间保留旧值，渲染前需核对 id。 */
    val window: DanmakuWindow? = null,
    /** 下一分段预取结果，命中后可在分段边界直接切换，避免等待网络。 */
    val prefetchWindow: DanmakuWindow? = null,
    val lastError: String? = null
) {
    fun windowAt(windowId: Long): DanmakuWindow? {
        return window?.takeIf { it.id == windowId }
            ?: prefetchWindow?.takeIf { it.id == windowId }
    }
}
