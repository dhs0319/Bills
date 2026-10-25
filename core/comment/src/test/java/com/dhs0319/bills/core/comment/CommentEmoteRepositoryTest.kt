package com.dhs0319.bills.core.comment

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CommentEmoteRepositoryTest {
    @Test fun `parses image and kaomoji packages using server tokens`() {
        val packages = parseCommentEmotePackages(JSONObject("""
            {"packages":[
              {"id":1,"text":"小黄脸","url":"//example.com/icon.png","type":1,
               "emote":[{"text":"[doge]","url":"http://example.com/doge.png","meta":{"size":1},"flags":{"unlocked":false}}]},
              {"id":4,"text":"颜文字","type":4,"emote":[{"text":"( ゜- ゜)つロ","url":"( ゜- ゜)つロ"}]}
            ]}
        """))
        assertEquals(2, packages.size)
        assertEquals("https://example.com/icon.png", packages[0].iconUrl)
        assertEquals("https://example.com/doge.png", packages[0].emotes.single().url)
        assertEquals("[doge]", packages[0].emotes.single().text)
        assertTrue(packages[0].emotes.single().available)
        assertTrue(packages[1].isText)
        assertEquals("", packages[1].emotes.single().url)
    }

    @Test fun `paid package permissions apply per item and use package size fallback`() {
        val packages = parseCommentEmotePackages(JSONObject("""
            {"packages":[{"id":100,"text":"贴纸","type":3,"meta":{"size":2},"flags":{"no_access":true},"emote":[
              {"text":"[贴纸_锁定]","url":"https://example.com/locked.png"},
              {"text":"[贴纸_可用]","url":"https://example.com/open.png","flags":{"unlocked":false,"no_access":false}},
              {"text":"[贴纸_受限]","url":"https://example.com/restricted.png","flags":{"unlocked":true,"no_access":true}}
            ]}]}
        """))
        assertFalse(packages.single().emotes[0].available)
        assertTrue(packages.single().emotes[1].available)
        assertFalse(packages.single().emotes[2].available)
        assertEquals(2L, packages.single().emotes[1].size)
    }

    @Test fun `hot word entries without no_access stay available even when unlocked is false`() {
        val packages = parseCommentEmotePackages(JSONObject("""
            {"packages":[{"id":53,"text":"热词系列一","type":1,"flags":{"added":true},"emote":[
              {"text":"[热词系列_再给一集]","url":"https://example.com/free.png","flags":{"unlocked":false}},
              {"text":"[热词系列_健健康康]","url":"https://example.com/locked.png","flags":{"no_access":true,"unlocked":false}},
              {"text":"[热词系列_好耶]","url":"https://example.com/allowed.png","flags":{"no_access":false,"unlocked":false}}
            ]}]}
        """))
        assertEquals(listOf(true, false, true), packages.single().emotes.map { it.available })
    }

    @Test fun `built in packages still respect explicit entry access restrictions`() {
        val packages = parseCommentEmotePackages(JSONObject("""
            {"packages":[{"id":1,"text":"小黄脸","type":1,"emote":[
              {"text":"[限定表情]","url":"https://example.com/limited.png","flags":{"no_access":true,"unlocked":false}}
            ]}]}
        """))
        assertFalse(packages.single().emotes.single().available)
    }

    @Test fun `malformed entries and server recent summaries do not become empty tabs`() {
        val packages = parseCommentEmotePackages(JSONObject("""
            {"packages":[
              {"id":0,"type":7,"text":"最近使用"},
              {"id":8,"type":1,"emote":[{"text":"[坏图]","url":null},{"url":"https://example.com/a.png"}]},
              {"id":9,"type":1,"emote":[{"text":"[好图]","url":"https://example.com/a.png"},{"text":"[好图]","url":"https://example.com/b.png"}]}
            ]}
        """))
        assertEquals(listOf(9L), packages.map { it.id })
        assertEquals(1, packages.single().emotes.size)
        assertTrue(parseCommentEmotePackages(JSONObject("""{"packages":null}""")).isEmpty())
    }
}
