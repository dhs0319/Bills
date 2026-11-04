package com.dhs0319.bills.feature.comment.component

import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.CommentUser
import org.junit.Assert.*
import org.junit.Test

class CommentRichTextTest {
    @Test fun `emotes and mention annotations preserve original character positions`() {
        val text = "[doge] @小明 [未知表情][doge]"
        val parsed = parseCommentText(text, listOf(CommentEmote("[doge]", "https://example.com/a.png")), listOf(CommentUser(42, "小明")))
        assertEquals(text, parsed.text.text)
        assertEquals(1, parsed.emotes.size)
        val mention = parsed.text.getStringAnnotations("comment_mention", 0, text.length).single()
        assertEquals("42", mention.item)
        assertEquals("@小明", text.substring(mention.start, mention.end))
    }

    @Test fun `a longer username is matched first and literal prefixes do not become mentions`() {
        val parsed = parseCommentText("@小明同学 @小明 @小明123", emptyList(), listOf(CommentUser(1, "小明"), CommentUser(2, "小明同学")))
        val annotations = parsed.text.getStringAnnotations("comment_mention", 0, parsed.text.length)
        assertEquals(listOf("2", "1"), annotations.map { it.item })
    }

    @Test fun `unknown tokens and kaomoji remain readable text`() {
        val text = "[未知] ( ゜- ゜)つロ"
        val parsed = parseCommentText(text, listOf(CommentEmote("( ゜- ゜)つロ", "")))
        assertEquals(text, parsed.text.text)
        assertTrue(parsed.emotes.isEmpty())
    }

    @Test fun `one catalog parses multiple messages without carrying over inline entries`() {
        val doge = CommentEmote("[doge]", "https://example.com/doge.png")
        val smile = CommentEmote("[微笑]", "https://example.com/smile.png")
        val catalog = CommentEmoteCatalog(listOf(doge, smile, CommentEmote("[文字]", "")))
        val first = parseCommentText("[doge][doge] [未知]", catalog)
        val second = parseCommentText("[微笑] [文字]", catalog)
        val plain = parseCommentText("普通消息 @小明", catalog)

        assertEquals("[doge][doge] [未知]", first.text.text)
        assertEquals(listOf(doge), first.emotes.map { it.emote })
        assertEquals("[微笑] [文字]", second.text.text)
        assertEquals(listOf(smile), second.emotes.map { it.emote })
        assertEquals("普通消息 @小明", plain.text.text)
        assertTrue(plain.emotes.isEmpty())
        assertTrue(plain.text.getStringAnnotations("comment_mention", 0, plain.text.length).isEmpty())
    }

    @Test fun `shared catalog keeps mention positions after repeated emotes`() {
        val catalog = CommentEmoteCatalog(listOf(CommentEmote("[doge]", "https://example.com/a.png")))
        val text = "[doge] [doge] @小明"
        val parsed = parseCommentText(text, catalog, listOf(CommentUser(42, "小明")))

        val mention = parsed.text.getStringAnnotations("comment_mention", 0, text.length).single()
        assertEquals(text.indexOf('@'), mention.start)
        assertEquals(text.length, mention.end)
        assertEquals("42", mention.item)
    }
}
