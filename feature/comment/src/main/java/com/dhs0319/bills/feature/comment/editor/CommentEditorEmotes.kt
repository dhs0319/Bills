package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import com.dhs0319.bills.core.model.CommentEmote

internal data class CommentEditorEmote(val range: TextRange, val emote: CommentEmote)

private val EDITOR_EMOTE_REGEX = Regex("\\[[^]\\r\\n]+]")

internal fun findEditorEmotes(text: String, catalog: List<CommentEmote>): List<CommentEditorEmote> {
    val byToken = catalog.filter { it.url.isNotBlank() }.associateBy { it.text }
    return EDITOR_EMOTE_REGEX.findAll(text).mapNotNull { match ->
        byToken[match.value]?.let { CommentEditorEmote(TextRange(match.range.first, match.range.last + 1), it) }
    }.toList()
}

/** An image is one editable unit, even though its wire representation has several characters. */
internal fun snapEditorEmoteSelection(selection: TextRange, emotes: List<CommentEditorEmote>): TextRange {
    return snapEditorTokenSelection(selection, emotes.map { it.range })
}

internal fun snapEditorTokenSelection(selection: TextRange, ranges: List<TextRange>): TextRange {
    if (!selection.collapsed) return expandEditorTokenRange(selection, ranges)
    val offset = selection.start
    val range = ranges.firstOrNull { offset > it.start && offset < it.end } ?: return selection
    return TextRange(if (offset - range.start < range.end - offset) range.start else range.end)
}

internal fun expandEditorEmoteRange(range: TextRange, emotes: List<CommentEditorEmote>): TextRange {
    return expandEditorTokenRange(range, emotes.map { it.range })
}

private fun expandEditorTokenRange(range: TextRange, ranges: List<TextRange>): TextRange {
    if (range.collapsed) return range
    var start = range.min
    var end = range.max
    ranges.sortedBy { it.start }.forEach { token ->
        if (start < token.end && end > token.start) {
            start = minOf(start, token.start)
            end = maxOf(end, token.end)
        }
    }
    return if (range.reversed) TextRange(end, start) else TextRange(start, end)
}
