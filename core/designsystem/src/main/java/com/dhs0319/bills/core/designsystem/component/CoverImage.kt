package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

@Composable
fun CoverImage(
    url: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape? = MaterialTheme.shapes.medium,
    fallbackContent: (@Composable BoxScope.() -> Unit)? = null,
    placeholderContent: (@Composable BoxScope.() -> Unit)? = null,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val coverModifier = if (shape == null) {
        modifier.background(bgColor)
    } else {
        modifier
            .clip(shape)
            .background(bgColor)
    }

    Box(
        modifier = coverModifier,
        contentAlignment = Alignment.Center
    ) {
        // Keep the placeholder behind the image for loading/error states without subcomposition.
        placeholderContent?.invoke(this)
        if (!url.isNullOrBlank()) {
            BiliAsyncImage(
                url = url,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                variant = BiliImageVariant.CardCover
            )
        } else {
            fallbackContent?.invoke(this)
        }
        content?.invoke(this)
    }
}
