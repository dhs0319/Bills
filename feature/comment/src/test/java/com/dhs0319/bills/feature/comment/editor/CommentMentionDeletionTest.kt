package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentUser
import org.junit.Assert.*
import org.junit.Test

class CommentMentionDeletionTest {
    private val user = CommentUser(42L, "猫哥捏食")

    @Test fun `mention remains one unit when its trailing separator is deleted`() {
        val old = CommentEditorState(
            input = TextFieldValue("@猫哥捏食 ", TextRange(6)),
            mentions = listOf(CommentEditorMention(user, TextRange(0, 5)))
        )
        val nextText = old.input.text.removeRange(5, 6)
        val nextMentions = reconcileCommentMentions(old.input.text, nextText, old.mentions)
        assertEquals("@猫哥捏食", nextText)
        assertTrue(nextMentions.single().isValidIn(nextText))
        assertNull(findCommentMentionQuery(TextFieldValue(nextText, TextRange(nextText.length)), nextMentions))
    }

    @Test fun `removing the mention invalidates its identity and does not create a new query`() {
        val old = CommentEditorState(
            input = TextFieldValue("@猫哥捏食", TextRange(5)),
            mentions = listOf(CommentEditorMention(user, TextRange(0, 5)))
        )
        val nextText = old.input.text.removeRange(0, 5)
        val nextMentions = reconcileCommentMentions(old.input.text, nextText, old.mentions)
        assertEquals("", nextText)
        assertTrue(nextMentions.isEmpty())
        assertNull(findCommentMentionQuery(TextFieldValue(nextText, TextRange.Zero), nextMentions))
    }

    @Test fun `typing a fresh at still produces a picker query`() {
        val value = TextFieldValue("你好 @猫", TextRange(5))
        assertEquals(CommentMentionQuery(TextRange(3, 5), "猫"), findCommentMentionQuery(value, emptyList()))
        assertTrue(shouldOpenCommentMentionPicker(TextFieldValue("你好 "), value, emptyList()))
    }

    @Test fun `every backspace within an unresolved query keeps the picker closed`() {
        var previous = TextFieldValue("@猫哥捏食", TextRange(5))
        while (previous.text.isNotEmpty()) {
            val value = TextFieldValue(previous.text.dropLast(1), TextRange(previous.text.length - 1))
            assertFalse(shouldOpenCommentMentionPicker(previous, value, emptyList()))
            previous = value
        }
    }

    @Test fun `backspace includes the inserted separator and panel deletion clears the UID`() {
        val text = "前 @猫哥捏食 "
        val mention = CommentEditorMention(user, TextRange(2, 7))
        assertEquals(TextRange(2, 8), mentionDeletionRange(text, 8, listOf(mention)))
        assertEquals(TextRange(2, 7), mentionDeletionRange(text, 7, listOf(mention)))
        assertEquals(TextRange(2, 7), mentionDeletionRange(text, 4, listOf(mention)))
        val editor = CommentEditorState(input = TextFieldValue(text, TextRange(8)), mentions = listOf(mention)).deleteBeforeCursor()
        assertEquals("前 ", editor.input.text)
        assertTrue(editor.mentionIds().isEmpty())
    }

    @Test fun `space after ordinary text is deleted normally`() {
        assertNull(mentionDeletionRange("普通文字 ", 5, emptyList()))
    }
}
