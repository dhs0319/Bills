package com.dhs0319.bills.core.im

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImMessageContentTest {
    @Test
    fun textPreservesQuotesNewlinesBackslashesAndEmoteTokens() {
        val text = "他说：\"你好\"\nC:\\图片\\[doge] 😀\t\u0000"
        assertEquals(text, JSONObject(encodeImTextContent(text)).getString("content"))
    }

    @Test
    fun imageMessageUsesUploadedDimensionsAndSize() {
        val image = parseImUploadedImage(
            JSONObject("""{"image_url":"//i0.hdslb.com/bfs/im/image.jpg","image_width":640,"image_height":480,"img_size":123.5}"""),
            "image/jpeg"
        )
        val content = JSONObject(image.toMessageContent())
        assertEquals("https://i0.hdslb.com/bfs/im/image.jpg", content.getString("url"))
        assertEquals(640, content.getInt("width"))
        assertEquals(480, content.getInt("height"))
        assertEquals("jpg", content.getString("imageType"))
        assertEquals(1, content.getInt("original"))
        assertEquals(123.5, content.getDouble("size"), 0.0)
    }

    @Test
    fun incompleteUploadResponsesCannotBeSentAsImageMessages() {
        listOf(
            """{}""",
            """{"image_url":null,"image_width":100,"image_height":100}""",
            """{"image_url":"file:///image.jpg","image_width":100,"image_height":100}""",
            """{"image_url":"https://i0.hdslb.com/image.jpg","image_width":0,"image_height":100}"""
        ).forEach { json ->
            assertThrows(IllegalArgumentException::class.java) { parseImUploadedImage(JSONObject(json), "image/png") }
        }
    }
}
