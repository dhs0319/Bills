package com.dhs0319.bills.feature.comment.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.CommentEmotePackage
import com.dhs0319.bills.core.model.CommentUser
import org.junit.Assert.*
import org.junit.Test

class CommentEditorTextTest {
    private val alice = CommentUser(42L, "小明")

    @Test fun `emote insertion uses cursor and replaces selected text`() {
        val editor = CommentEditorState(input = TextFieldValue("前文后文", TextRange(2)))
        val inserted = editor.replaceSelection("[doge]")
        assertEquals("前文[doge]后文", inserted.input.text)
        assertEquals(TextRange(8), inserted.input.selection)
        val replaced = editor.copy(input = TextFieldValue("前文后文", TextRange(4, 2))).replaceSelection("[笑哭]")
        assertEquals("前文[笑哭]", replaced.input.text)
    }

    @Test fun `mention picker replaces typed query and stores UID with exact range`() {
        val editor = CommentEditorState(
            input = TextFieldValue("你好 @小，后文", TextRange(5)),
            mentionPicker = CommentMentionPickerState(replacement = TextRange(3, 5))
        ).insertMention(alice)
        assertEquals("你好 @小明 ，后文", editor.input.text)
        assertEquals(TextRange(7), editor.input.selection)
        assertEquals(listOf(CommentEditorMention(alice, TextRange(3, 6))), editor.mentions)
        assertEquals(mapOf("小明" to 42L), editor.mentionIds())
        assertEquals(CommentEditorPanel.Keyboard, editor.panel)
    }

    @Test fun `inserting before a mention shifts its identity range`() {
        val mentions = listOf(CommentEditorMention(alice, TextRange(0, 3)))
        val shifted = reconcileCommentMentions("@小明 你好", "前文 @小明 你好", mentions)
        assertEquals(TextRange(3, 6), shifted.single().range)
    }

    @Test fun `partial edit removes mention identity even when another literal matches`() {
        val mentions = listOf(CommentEditorMention(alice, TextRange(0, 3)))
        val next = reconcileCommentMentions("@小明 和 @小明", "@小 和 @小明", mentions)
        assertTrue(next.isEmpty())
        assertTrue(CommentEditorState(input = TextFieldValue("@小 和 @小明"), mentions = next).mentionIds().isEmpty())
    }

    @Test fun `appending a name character invalidates the previous mention`() {
        val mentions = listOf(CommentEditorMention(alice, TextRange(0, 3)))
        assertTrue(reconcileCommentMentions("@小明 ", "@小明同学 ", mentions).isEmpty())
    }

    @Test fun `deleting a mention keeps other mentions and deduplicates submitted identities`() {
        val bob = CommentUser(99L, "小红")
        val editor = CommentEditorState(
            input = TextFieldValue("@小明 @小红 @小红 ", TextRange(0, 4)),
            mentions = listOf(
                CommentEditorMention(alice, TextRange(0, 3)),
                CommentEditorMention(bob, TextRange(4, 7)),
                CommentEditorMention(bob, TextRange(8, 11))
            )
        ).deleteBeforeCursor()
        assertEquals("@小红 @小红 ", editor.input.text)
        assertEquals(mapOf("小红" to 99L), editor.mentionIds())
    }

    @Test fun `typed at query opens picker but emails and existing mentions do not`() {
        assertEquals(CommentMentionQuery(TextRange(3, 6), "小明"), findCommentMentionQuery(TextFieldValue("你好 @小明", TextRange(6)), emptyList()))
        assertNull(findCommentMentionQuery(TextFieldValue("mail@example", TextRange(12)), emptyList()))
        assertNull(findCommentMentionQuery(TextFieldValue("@小明 ", TextRange(3)), listOf(CommentEditorMention(alice, TextRange(0, 3)))))
        assertNull(findCommentMentionQuery(TextFieldValue("@小", TextRange(2), TextRange(1, 2)), emptyList()))
    }

    @Test fun `panel delete removes an entire emote token or Unicode code point`() {
        val emote = CommentEmote("[doge]", "https://example.com/doge.png")
        val editor = CommentEditorState(
            input = TextFieldValue("前文[doge]后文", TextRange(8)),
            emotes = CommentEmotePanelState(packages = listOf(CommentEmotePackage(1, "默认", "", listOf(emote))))
        ).deleteBeforeCursor()
        assertEquals("前文后文", editor.input.text)
        assertEquals(TextRange(2), editor.input.selection)
        assertEquals("前文", CommentEditorState(input = TextFieldValue("前文😀", TextRange(4))).deleteBeforeCursor().input.text)
        assertEquals("", CommentEditorState().deleteBeforeCursor().input.text)
    }
}
