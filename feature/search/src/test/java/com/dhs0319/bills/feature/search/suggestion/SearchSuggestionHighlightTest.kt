package com.dhs0319.bills.feature.search.suggestion

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionHighlightTest {
    @Test
    fun highlightsEveryMatchUsingTheThemeColor() {
        val color = Color(0xFFFF6699)
        val title = highlightSuggestion("野火和野孩子", "野", color)

        assertEquals("野火和野孩子", title.text)
        assertEquals(listOf(0 to 1, 3 to 4), title.spanStyles.map { it.start to it.end })
        assertTrue(title.spanStyles.all { it.item.color == color })
    }

    @Test
    fun matchesLiteralTextWithoutCaseSensitivity() {
        val title = highlightSuggestion("A.B a-b a.b", " a.b ", Color.Blue)

        assertEquals(listOf(0 to 3, 8 to 11), title.spanStyles.map { it.start to it.end })
    }

    @Test
    fun blankOrMissingKeywordLeavesTheTextUnstyled() {
        for (query in listOf("", "   ", "missing")) {
            val title = highlightSuggestion("野火", query, Color.Blue)
            assertEquals("野火", title.text)
            assertTrue(title.spanStyles.isEmpty())
        }
    }
}
