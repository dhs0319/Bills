package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import com.dhs0319.bills.core.model.CommentEmote
import org.junit.Assert.*
import org.junit.Test

class CommentEditorEmotesTest {
    private val catalog = listOf(
        CommentEmote("[doge]", "https://example.com/doge.png"),
        CommentEmote("[tv_白眼]", "https://example.com/tv.png"),
        CommentEmote("( ゜- ゜)つロ", "")
    )

    @Test fun `adjacent and repeated image tokens retain their full original ranges`() {
        val text = "前[doge][tv_白眼][doge]后"
        val emotes = findEditorEmotes(text, catalog)
        assertEquals(listOf("[doge]", "[tv_白眼]", "[doge]"), emotes.map { text.substring(it.range.start, it.range.end) })
        assertEquals(TextRange(1, 7), emotes.first().range)
        assertEquals(TextRange(14, 20), emotes.last().range)
    }

    @Test fun `unknown tokens and kaomoji stay ordinary text`() {
        val text = "[未知]( ゜- ゜)つロ[doge]"
        assertEquals(listOf("[doge]"), findEditorEmotes(text, catalog).map { it.emote.text })
        assertTrue(findEditorEmotes("[doge\n]", catalog).isEmpty())
    }

    @Test fun `cursor snaps to an image edge while text positions remain unchanged`() {
        val emotes = findEditorEmotes("前[doge]后", catalog)
        assertEquals(TextRange(1), snapEditorEmoteSelection(TextRange(2), emotes))
        assertEquals(TextRange(7), snapEditorEmoteSelection(TextRange(5), emotes))
        assertEquals(TextRange(7), snapEditorEmoteSelection(TextRange(7), emotes))
        assertEquals(TextRange(0), snapEditorEmoteSelection(TextRange(0), emotes))
    }

    @Test fun `keyboard delete expands one character to the full token`() {
        val emotes = findEditorEmotes("前[doge]后", catalog)
        assertEquals(TextRange(1, 7), expandEditorEmoteRange(TextRange(6, 7), emotes))
        assertEquals(TextRange(1, 7), expandEditorEmoteRange(TextRange(1, 2), emotes))
        assertEquals(TextRange(7, 8), expandEditorEmoteRange(TextRange(7, 8), emotes))
    }

    @Test fun `selection and reverse selection include entire images without touching neighbors`() {
        val emotes = findEditorEmotes("[doge]字[tv_白眼]", catalog)
        assertEquals(TextRange(0, 7), snapEditorEmoteSelection(TextRange(2, 7), emotes))
        assertEquals(TextRange(7, 0), snapEditorEmoteSelection(TextRange(7, 2), emotes))
        assertEquals(TextRange(6, 14), snapEditorEmoteSelection(TextRange(6, 9), emotes))
    }
}
