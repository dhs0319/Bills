package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentUser

internal data class CommentMentionQuery(val range: TextRange, val keyword: String)

/** Keep identities attached to the original mention, never to a later matching substring. */
internal fun reconcileCommentMentions(
    previous: String,
    current: String,
    mentions: List<CommentEditorMention>
): List<CommentEditorMention> {
    if (previous == current) return mentions
    val prefix = previous.commonPrefixWith(current).length
    val suffix = previous.substring(prefix).commonSuffixWith(current.substring(prefix)).length
    val oldEnd = previous.length - suffix
    val delta = current.length - previous.length
    return mentions.mapNotNull { mention ->
        val range = when {
            oldEnd <= mention.range.start -> TextRange(mention.range.start + delta, mention.range.end + delta)
            prefix >= mention.range.end -> mention.range
            else -> return@mapNotNull null
        }
        mention.copy(range = range).takeIf { it.isValidIn(current) }
    }
}

internal fun CommentEditorMention.isValidIn(text: String): Boolean =
    range.start >= 0 && range.end <= text.length &&
        text.substring(range.start, range.end) == token &&
        (range.end == text.length || !text[range.end].isMentionNameCharacter())

private fun Char.isMentionNameCharacter(): Boolean = isLetterOrDigit() || this == '_' || this == '-'

internal fun findCommentMentionQuery(
    value: TextFieldValue,
    mentions: List<CommentEditorMention>
): CommentMentionQuery? {
    if (!value.selection.collapsed || value.composition != null) return null
    val end = value.selection.end
    if (end == 0 || mentions.any { end > it.range.start && end <= it.range.end }) return null
    val start = value.text.lastIndexOf('@', startIndex = end - 1)
    if (start < 0 || (start > 0 && value.text[start - 1].isMentionNameCharacter())) return null
    val keyword = value.text.substring(start + 1, end)
    if (keyword.length > 64 || keyword.any { it.isWhitespace() || it == '@' }) return null
    return CommentMentionQuery(TextRange(start, end), keyword)
}

internal fun CommentEditorState.replaceSelection(
    text: String,
    range: TextRange = input.selection
): CommentEditorState {
    val start = range.min.coerceIn(0, input.text.length)
    val end = range.max.coerceIn(start, input.text.length)
    val next = input.text.replaceRange(start, end, text)
    return copy(
        input = TextFieldValue(next, TextRange(start + text.length)),
        mentions = reconcileCommentMentions(input.text, next, mentions)
    )
}

internal fun CommentEditorState.insertMention(user: CommentUser): CommentEditorState {
    val range = mentionPicker.replacement
    val start = range.min.coerceIn(0, input.text.length)
    val token = "@${user.name}"
    val next = replaceSelection("$token ", range)
    return next.copy(
        mentions = next.mentions + CommentEditorMention(user, TextRange(start, start + token.length)),
        panel = CommentEditorPanel.Keyboard,
        mentionPicker = CommentMentionPickerState()
    )
}

internal fun CommentEditorState.deleteBeforeCursor(): CommentEditorState {
    if (!input.selection.collapsed) return replaceSelection("")
    val end = input.selection.end
    if (end == 0) return this
    val mentionStart = mentions.firstOrNull { it.range.end == end }?.range?.start
    val emote = (emotes.recent.asSequence() + emotes.packages.asSequence().flatMap { it.emotes })
        .filter { it.text.startsWith('[') && it.text.endsWith(']') }
        .filter { it.text.length <= end && input.text.regionMatches(end - it.text.length, it.text, 0, it.text.length) }
        .maxByOrNull { it.text.length }
    val start = mentionStart ?: emote?.let { end - it.text.length }
        ?: (end - Character.charCount(Character.codePointBefore(input.text, end)))
    return replaceSelection("", TextRange(start, end))
}

internal fun CommentEditorState.mentionIds(): Map<String, Long> = mentions
    .filter { it.user.mid > 0L && it.isValidIn(input.text) }
    .associate { it.user.name to it.user.mid }
