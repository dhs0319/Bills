package com.dhs0319.bills.feature.search.suggestion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun SearchSuggestionsPanel(
    query: String,
    suggestions: List<String>,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    LazyColumn(
        modifier = modifier.fillMaxSize().navigationBarsPadding().imePadding(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
    ) {
        items(suggestions, key = { it }) { suggestion ->
            val title = remember(suggestion, query, highlightColor) {
                highlightSuggestion(suggestion, query, highlightColor)
            }
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onSearch(suggestion) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun highlightSuggestion(text: String, query: String, color: Color): AnnotatedString {
    val keyword = query.trim()
    return buildAnnotatedString {
        append(text)
        if (keyword.isEmpty()) return@buildAnnotatedString
        var start = text.indexOf(keyword, ignoreCase = true)
        while (start >= 0) {
            val end = start + keyword.length
            addStyle(SpanStyle(color = color), start, end)
            start = text.indexOf(keyword, startIndex = end, ignoreCase = true)
        }
    }
}
