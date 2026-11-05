package com.dhs0319.bills.feature.im.conversation

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.ImMessage
import com.dhs0319.bills.feature.comment.editor.CommentEmotePanelState

@Immutable
internal data class ImConversationUiState(
    val title: String = "",
    val avatar: String? = null,
    val selfAvatar: String? = null,
    val messages: List<ImMessage> = emptyList(),
    val rows: List<ConversationRow> = emptyList(),
    val emotes: List<CommentEmote> = emptyList(),
    val hasMoreHistory: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val lastSentMessageKey: Long? = null
)

@Immutable
internal data class ImConversationComposerState(
    val input: TextFieldValue = TextFieldValue(),
    val selectedImageUri: Uri? = null,
    val emotes: List<CommentEmote> = emptyList(),
    val emotePicker: CommentEmotePanelState = CommentEmotePanelState(),
    val isSending: Boolean = false,
    val isUploadingImage: Boolean = false,
    val sendErrorMessage: String? = null
)
