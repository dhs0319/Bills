package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentEmote
import org.junit.Assert.assertEquals
import org.junit.Test

class EmoteTextEditingTest {
    private val doge = CommentEmote("[doge]", "https://example.com/doge.png")
    private val catalog = listOf(doge)

    @Test
    fun insertUsesTheCursorAndReplacesAReverseSelection() {
        val inserted = TextFieldValue("你好", TextRange(1)).insertEmote(doge, catalog)
        assertEquals("你[doge]好", inserted.text)
        assertEquals(TextRange(7), inserted.selection)
        val replaced = TextFieldValue("你[doge]好", TextRange(7, 3)).insertEmote(doge, catalog)
        assertEquals("你[doge]好", replaced.text)
        assertEquals(TextRange(7), replaced.selection)
    }

    @Test
    fun backspaceRemovesOneWholeEmoteOrUnicodeCodePoint() {
        val input = TextFieldValue("😀[doge]", TextRange(8))
        val withoutEmote = input.deleteEmoteBeforeCursor(catalog)
        assertEquals("😀", withoutEmote.text)
        assertEquals(TextRange(2), withoutEmote.selection)
        assertEquals("", withoutEmote.deleteEmoteBeforeCursor(catalog).text)
    }

    @Test
    fun selectionDeletionDoesNotLeaveAPartialEmote() {
        val input = TextFieldValue("前[doge]后", TextRange(3, 8))
        assertEquals("前", input.deleteEmoteBeforeCursor(catalog).text)
    }

    @Test
    fun unavailableEmoteDoesNotReplaceTheDraft() {
        val input = TextFieldValue("草稿", TextRange(1))
        assertEquals(input, input.insertEmote(doge.copy(available = false), catalog))
    }
}
