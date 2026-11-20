package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val GRID_COVER_ASPECT_RATIO = 16f / 10f

enum class VideoGridCardSkeletonStyle {
    VIDEO, AUDIO, LIVE, ARTICLE
}

@Composable
private fun skeletonLineHeights(): Pair<Dp, Dp> = with(LocalDensity.current) {
    val titleStyle = MaterialTheme.typography.bodyMedium
    val metadataStyle = MaterialTheme.typography.bodySmall
    maxOf(titleStyle.lineHeight.toDp(), titleStyle.fontSize.toDp()) to
        maxOf(metadataStyle.lineHeight.toDp(), metadataStyle.fontSize.toDp())
}

private fun VideoGridCardSkeletonStyle.verticalPadding(): Dp = when (this) {
    VideoGridCardSkeletonStyle.VIDEO, VideoGridCardSkeletonStyle.AUDIO -> 6.dp
    VideoGridCardSkeletonStyle.LIVE, VideoGridCardSkeletonStyle.ARTICLE -> 8.dp
}

private fun VideoGridCardSkeletonStyle.authorSpacing(): Dp = when (this) {
    VideoGridCardSkeletonStyle.VIDEO, VideoGridCardSkeletonStyle.AUDIO -> 2.dp
    VideoGridCardSkeletonStyle.LIVE -> 4.dp
    VideoGridCardSkeletonStyle.ARTICLE -> 6.dp
}

private fun VideoGridCardSkeletonStyle.authorHeight(metadataLineHeight: Dp): Dp = when (this) {
    VideoGridCardSkeletonStyle.VIDEO -> maxOf(metadataLineHeight, 24.dp)
    VideoGridCardSkeletonStyle.ARTICLE -> maxOf(metadataLineHeight, 20.dp)
    else -> metadataLineHeight
}

/** Also used by the loading grid to estimate how many placeholders fill the viewport. */
@Composable
fun videoGridCardSkeletonHeight(
    coverWidth: Dp,
    style: VideoGridCardSkeletonStyle = VideoGridCardSkeletonStyle.VIDEO
): Dp {
    val (titleLineHeight, metadataLineHeight) = skeletonLineHeights()
    val articleContentHeight = if (style == VideoGridCardSkeletonStyle.ARTICLE) {
        // Two summary lines, statistics, and the two additional row gaps.
        metadataLineHeight * 3 + 12.dp
    } else {
        0.dp
    }
    return coverWidth / GRID_COVER_ASPECT_RATIO + style.verticalPadding() * 2 +
        titleLineHeight * 2 + style.authorSpacing() + style.authorHeight(metadataLineHeight) +
        articleContentHeight
}

@Composable
fun VideoGridCardSkeleton(
    modifier: Modifier = Modifier,
    style: VideoGridCardSkeletonStyle = VideoGridCardSkeletonStyle.VIDEO
) {
    val cardShape = CardDefaults.shape
    val blockShape = MaterialTheme.shapes.extraSmall
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val blockColor = MaterialTheme.colorScheme.surfaceVariant
    val (titleLineHeight, metadataLineHeight) = skeletonLineHeights()
    val contentHeight = videoGridCardSkeletonHeight(0.dp, style)

    // One layout node, with no image requests, text measurement, animation or graphics layer.
    // Rebuild paths only when the size, density, direction, theme or style changes.
    Layout(
        content = {},
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val cardPath = Path().apply {
                    addOutline(cardShape.createOutline(size, layoutDirection, this@drawWithCache))
                }
                val blocks = Path()
                fun block(start: Float, top: Float, width: Float, height: Float, shape: Shape = blockShape) {
                    val safeWidth = width.coerceIn(0f, (size.width - start).coerceAtLeast(0f))
                    if (safeWidth == 0f || height <= 0f) return
                    val left = if (layoutDirection == LayoutDirection.Ltr) start else size.width - start - safeWidth
                    val outline = shape.createOutline(Size(safeWidth, height), layoutDirection, this)
                    blocks.addPath(Path().apply { addOutline(outline) }, Offset(left, top))
                }

                val coverHeight = (size.width / GRID_COVER_ASPECT_RATIO).roundToInt().toFloat()
                val inset = 10.dp.toPx()
                val textWidth = (size.width - inset * 2).coerceAtLeast(0f)
                val titleLine = titleLineHeight.toPx()
                val metadataLine = metadataLineHeight.toPx()
                val titleBarHeight = titleLine * 0.65f
                val metadataBarHeight = metadataLine * 0.65f
                var top = coverHeight + style.verticalPadding().toPx()

                block(inset, top + (titleLine - titleBarHeight) / 2, textWidth * 0.94f, titleBarHeight)
                block(inset, top + titleLine + (titleLine - titleBarHeight) / 2, textWidth * 0.66f, titleBarHeight)
                top += titleLine * 2 + style.authorSpacing().toPx()

                if (style == VideoGridCardSkeletonStyle.ARTICLE) {
                    repeat(2) { line ->
                        block(inset, top + (metadataLine - metadataBarHeight) / 2,
                            textWidth * if (line == 0) 0.92f else 0.76f, metadataBarHeight)
                        top += metadataLine
                    }
                    top += 6.dp.toPx()
                }

                val authorHeight = style.authorHeight(metadataLineHeight).toPx()
                val badgeWidth = if (style == VideoGridCardSkeletonStyle.ARTICLE) 20.dp.toPx() else 16.dp.toPx()
                val badgeHeight = if (style == VideoGridCardSkeletonStyle.ARTICLE) badgeWidth else metadataBarHeight
                block(inset, top + (authorHeight - badgeHeight) / 2, badgeWidth, badgeHeight,
                    if (style == VideoGridCardSkeletonStyle.ARTICLE) CircleShape else blockShape)
                val authorStart = inset + badgeWidth + (if (style == VideoGridCardSkeletonStyle.ARTICLE) 6.dp else 3.dp).toPx()
                block(authorStart, top + (authorHeight - metadataBarHeight) / 2, textWidth * 0.5f, metadataBarHeight)

                if (style == VideoGridCardSkeletonStyle.VIDEO) {
                    val dotSize = 3.dp.toPx()
                    repeat(3) { dot ->
                        block(size.width - inset - 12.dp.toPx(),
                            top + authorHeight / 2 + (dot - 1) * 5.dp.toPx() - dotSize / 2,
                            dotSize, dotSize, CircleShape)
                    }
                } else if (style == VideoGridCardSkeletonStyle.ARTICLE) {
                    top += authorHeight + 6.dp.toPx()
                    block(inset, top + (metadataLine - metadataBarHeight) / 2, textWidth * 0.46f, metadataBarHeight)
                    block(inset + textWidth * 0.72f, top + (metadataLine - metadataBarHeight) / 2,
                        textWidth * 0.28f, metadataBarHeight)
                }

                onDrawBehind {
                    // Clip on the canvas so the cover shares just the card's top corners.
                    clipPath(cardPath) {
                        drawRect(containerColor)
                        drawRect(blockColor, size = Size(size.width, coverHeight))
                        drawPath(blocks, blockColor)
                    }
                }
            }
    ) { _, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val height = (width / GRID_COVER_ASPECT_RATIO).roundToInt() + contentHeight.roundToPx()
        layout(width, constraints.constrainHeight(height)) {}
    }
}
