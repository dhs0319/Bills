package com.dhs0319.bills.feature.im.conversation

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.ImMessage

@Immutable
data class ImConversationUiState(
    val title: String = "",
    val avatar: String? = null,
    val selfAvatar: String? = null,
    val messages: List<ImMessage> = emptyList(),
    val emotes: List<CommentEmote> = emptyList(),
    val hasMoreHistory: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val sendErrorMessage: String? = null,
    val lastSentMessageKey: Long? = null
)
