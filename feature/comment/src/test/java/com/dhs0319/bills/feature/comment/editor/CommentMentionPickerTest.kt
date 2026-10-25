package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentUser
import org.junit.Assert.*
import org.junit.Test

class CommentMentionPickerTest {
    @Test fun `toolbar starts a query in the existing editor`() {
        val editor = CommentEditorState(input = TextFieldValue("你好", TextRange(2))).startMentionPicker()
        assertEquals("你好 @", editor.input.text)
        assertEquals(TextRange(4), editor.input.selection)
        assertEquals(TextRange(3, 4), editor.mentionPicker.replacement)
        assertEquals(CommentEditorPanel.Mentions, editor.panel)
    }

    @Test fun `opening an existing query does not insert another at`() {
        val editor = CommentEditorState(input = TextFieldValue("你好 @猫", TextRange(5))).startMentionPicker()
        assertEquals("你好 @猫", editor.input.text)
        assertEquals("猫", editor.mentionPicker.query)
        assertEquals(TextRange(3, 5), editor.mentionPicker.replacement)
    }

    @Test fun `search follows the editor composition without moving its cursor`() {
        val value = TextFieldValue("你好 @mao", TextRange(7), TextRange(4, 7))
        val editor = CommentEditorState(input = value, panel = CommentEditorPanel.Mentions).updateMentionQuery()
        assertEquals(value, editor.input)
        assertEquals("mao", editor.mentionPicker.query)
        assertEquals(TextRange(3, 7), editor.mentionPicker.replacement)
    }

    @Test fun `selection replaces the complete query and keeps the editor value`() {
        val user = CommentUser(42L, "猫哥捏食")
        val editor = CommentEditorState(input = TextFieldValue("你好 @猫", TextRange(5)))
            .startMentionPicker().insertMention(user)
        assertEquals("你好 @猫哥捏食 ", editor.input.text)
        assertEquals(TextRange(editor.input.text.length), editor.input.selection)
        assertEquals(mapOf("猫哥捏食" to 42L), editor.mentionIds())
        assertEquals(CommentEditorPanel.Keyboard, editor.panel)
    }

    @Test fun `deleting the query closes the list without replacing the input`() {
        val value = TextFieldValue("你好 ", TextRange(3))
        val editor = CommentEditorState(input = value, panel = CommentEditorPanel.Mentions).updateMentionQuery()
        assertEquals(value, editor.input)
        assertEquals(CommentEditorPanel.Keyboard, editor.panel)
    }
}
