package com.dhs0319.bills.feature.comment.editor

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.CommentEmotePackage
import com.dhs0319.bills.core.model.CommentMentionGroup
import com.dhs0319.bills.core.model.CommentUser

const val COMMENT_EDITOR_MAX_IMAGE_COUNT = 9

@Immutable
data class CommentEditorState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val uploading: Boolean = false,
    val input: TextFieldValue = TextFieldValue(),
    val mentions: List<CommentEditorMention> = emptyList(),
    val selectedImageUris: List<Uri> = emptyList(),
    val target: CommentEditorTarget = CommentEditorTarget(),
    val panel: CommentEditorPanel = CommentEditorPanel.Keyboard,
    val emotes: CommentEmotePanelState = CommentEmotePanelState(),
    val mentionPicker: CommentMentionPickerState = CommentMentionPickerState()
)

enum class CommentEditorPanel { Keyboard, Emotes, Mentions }

internal sealed interface CommentEditorAction {
    data object ShowKeyboard : CommentEditorAction
    data object ToggleEmotes : CommentEditorAction
    data object ReloadEmotes : CommentEditorAction
    data object DeleteText : CommentEditorAction
    data object OpenMentions : CommentEditorAction
    data class SelectPackage(val id: Long) : CommentEditorAction
    data class InsertEmote(val emote: CommentEmote) : CommentEditorAction
    data class SearchMentions(val query: String) : CommentEditorAction
    data class InsertMention(val user: CommentUser) : CommentEditorAction
}

@Immutable
data class CommentEditorMention(val user: CommentUser, val range: TextRange) {
    val token: String get() = "@${user.name}"
}

@Immutable
data class CommentEmotePanelState(
    val loading: Boolean = false,
    val error: String? = null,
    val packages: List<CommentEmotePackage> = emptyList(),
    val recent: List<CommentEmote> = emptyList(),
    val selectedPackageId: Long = 1L
)

@Immutable
data class CommentMentionPickerState(
    val query: String = "",
    val replacement: TextRange = TextRange.Zero,
    val loading: Boolean = false,
    val error: String? = null,
    val groups: List<CommentMentionGroup> = emptyList()
)

@Immutable
data class CommentEditorTarget(
    val rootRpid: Long = 0L,
    val parentRpid: Long = 0L,
    val parentName: String? = null
) {
    val isReply: Boolean
        get() = rootRpid > 0L
}
