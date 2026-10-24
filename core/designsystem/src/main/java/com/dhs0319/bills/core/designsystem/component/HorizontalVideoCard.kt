package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizontalVideoCard(
    title: String,
    coverUrl: String?,
    durationText: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false,
    coverOverlay: (@Composable BoxScope.(coverWidth: Dp) -> Unit)? = null,
    coverBottomContent: (@Composable BoxScope.() -> Unit)? = null,
    metadata: @Composable (cardWidth: Dp) -> Unit
) {
    HorizontalVideoCard(
        title = AnnotatedString(title),
        coverUrl = coverUrl,
        durationText = durationText,
        onClick = onClick,
        modifier = modifier,
        isTablet = isTablet,
        coverOverlay = coverOverlay,
        coverBottomContent = coverBottomContent,
        metadata = metadata
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizontalVideoCard(
    title: AnnotatedString,
    coverUrl: String?,
    durationText: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false,
    coverOverlay: (@Composable BoxScope.(coverWidth: Dp) -> Unit)? = null,
    coverBottomContent: (@Composable BoxScope.() -> Unit)? = null,
    metadata: @Composable (cardWidth: Dp) -> Unit
) {
    val titleStyle = if (isTablet) MaterialTheme.typography.titleSmall
        else MaterialTheme.typography.titleMedium

    val cardContent: @Composable () -> Unit = {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth
            val coverWidth = horizontalVideoCoverWidth(cardWidth, isTablet)
            val coverHeight = coverWidth / horizontalVideoCoverAspectRatio(isTablet)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(
                    url = coverUrl,
                    contentDescription = title.text,
                    modifier = Modifier
                        .width(coverWidth)
                        .height(coverHeight),
                    shape = MaterialTheme.shapes.small
                ) {
                    durationText?.takeIf(String::isNotBlank)?.let { duration ->
                        Text(
                            text = duration,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(Color.Black.copy(alpha = 0.68f))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    coverOverlay?.invoke(this, coverWidth)
                    coverBottomContent?.invoke(this)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = coverHeight),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = titleStyle,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    metadata(cardWidth)
                }
            }
        }
    }

    val cardModifier = modifier.fillMaxWidth()
    val cardShape = MaterialTheme.shapes.medium
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = cardShape,
            colors = cardColors,
            content = { cardContent() }
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = cardShape,
            colors = cardColors,
            content = { cardContent() }
        )
    }
}

@Composable
fun HorizontalVideoCardSkeleton(
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        VideoListCardSkeleton(
            coverWidth = horizontalVideoCoverWidth(maxWidth, isTablet),
            coverAspectRatio = horizontalVideoCoverAspectRatio(isTablet),
            contentPadding = 8.dp
        )
    }
}

private fun horizontalVideoCoverWidth(cardWidth: Dp, isTablet: Boolean): Dp {
    return (cardWidth - 16.dp) * if (isTablet) 0.49f else 0.43f
}

private fun horizontalVideoCoverAspectRatio(isTablet: Boolean): Float {
    return if (isTablet) 16f / 10f else 16f / 9f
}
