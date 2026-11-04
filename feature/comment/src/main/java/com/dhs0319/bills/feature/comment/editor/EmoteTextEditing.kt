package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentEmote

/** Replaces the selection without splitting an inline emote. */
fun TextFieldValue.insertEmote(emote: CommentEmote, catalog: List<CommentEmote>): TextFieldValue {
    if (!emote.available) return this
    val range = snapEditorEmoteSelection(selection, findEditorEmotes(text, catalog))
    return replaceEmoteSelection(emote.text, range)
}

/** Removes a selection, a whole emote token, or one Unicode code point. */
fun TextFieldValue.deleteEmoteBeforeCursor(catalog: List<CommentEmote>): TextFieldValue {
    val emotes = findEditorEmotes(text, catalog)
    val range = snapEditorEmoteSelection(selection, emotes)
    if (!range.collapsed) return replaceEmoteSelection("", range)
    val end = range.end
    if (end == 0) return this
    val start = end - Character.charCount(Character.codePointBefore(text, end))
    return replaceEmoteSelection("", expandEditorEmoteRange(TextRange(start, end), emotes))
}

private fun TextFieldValue.replaceEmoteSelection(replacement: String, range: TextRange): TextFieldValue {
    val start = range.min.coerceIn(0, text.length)
    val end = range.max.coerceIn(start, text.length)
    return TextFieldValue(text.replaceRange(start, end, replacement), TextRange(start + replacement.length))
}
