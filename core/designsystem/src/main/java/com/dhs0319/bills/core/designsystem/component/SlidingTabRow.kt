package com.dhs0319.bills.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.theme.LocalAnimations
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * [position] is the continuous tab index (for a pager, currentPage + currentPageOffsetFraction).
 * When omitted, selection changes animate the indicator for non-paged content.
 */
@Composable
fun SlidingTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    position: Float? = null
) {
    if (tabs.isEmpty()) return

    val animations = LocalAnimations.current
    val indicatorPosition = if (position == null) {
        val animatedPosition by animateFloatAsState(
            targetValue = selectedIndex.coerceIn(tabs.indices).toFloat(),
            animationSpec = tween(durationMillis = animations.medium, easing = animations.standard),
            label = "Tab indicator position"
        )
        animatedPosition
    } else {
        position
    }
    val trailingPosition = if (position == null || animations.medium <= 1) {
        indicatorPosition
    } else {
        val animatedTail by animateFloatAsState(
            targetValue = position.coerceIn(0f, tabs.lastIndex.toFloat()),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "Tab indicator tail"
        )
        animatedTail
    }
    val indicatorColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxWidth().height(44.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val tabWidth = size.width / tabs.size
            val indicatorWidth = 32.dp.toPx()
            val indicatorHeight = 3.dp.toPx()
            val head = indicatorPosition.coerceIn(0f, tabs.lastIndex.toFloat())
            val tail = trailingPosition.coerceIn(0f, tabs.lastIndex.toFloat())
            val fraction = head - floor(head)
            val stretch = (0.24 * sin(PI * fraction).let { it * it }).toFloat()
            val left = (min(head - stretch, tail) + 0.5f) * tabWidth - indicatorWidth / 2f
            val right = (max(head + stretch, tail) + 0.5f) * tabWidth + indicatorWidth / 2f
            val drawLeft = if (layoutDirection == LayoutDirection.Rtl) size.width - right else left

            drawRoundRect(
                color = indicatorColor,
                topLeft = Offset(drawLeft, size.height - indicatorHeight - 6.dp.toPx()),
                size = Size(right - left, indicatorHeight),
                cornerRadius = CornerRadius(indicatorHeight / 2f)
            )
        }

        Row(Modifier.fillMaxSize().selectableGroup()) {
            tabs.forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = index == selectedIndex,
                            role = Role.Tab,
                            onClick = { onSelect(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = if (index == selectedIndex) indicatorColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerSlidingTabRow(
    tabs: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onReselect: (Int) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val baseDurationMillis = LocalAnimations.current.medium
    SlidingTabRow(
        tabs = tabs,
        selectedIndex = pagerState.currentPage,
        position = pagerState.currentPage + pagerState.currentPageOffsetFraction,
        onSelect = { page ->
            if (page == pagerState.currentPage && !pagerState.isScrollInProgress &&
                abs(pagerState.currentPageOffsetFraction) < 0.001f
            ) {
                onReselect(page)
            } else {
                scope.launch { pagerState.animateScrollContinuouslyToPage(page, baseDurationMillis) }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun PagerState.animateScrollContinuouslyToPage(page: Int, baseDurationMillis: Int) {
    scroll {
        val startPosition = currentPage + currentPageOffsetFraction
        val pageDistance = page - startPosition
        val pageSizeWithSpacing = layoutInfo.pageSize + layoutInfo.pageSpacing
        if (pageDistance == 0f || pageSizeWithSpacing == 0) return@scroll

        val durationMillis = (baseDurationMillis *
            (1f + (abs(pageDistance) - 1f).coerceAtLeast(0f) * 0.2f))
            .roundToInt()
            .coerceAtLeast(1)
        val targetOffset = pageDistance * pageSizeWithSpacing
        var consumedOffset = 0f
        animate(0f, targetOffset, animationSpec = tween(durationMillis)) { value, _ ->
            consumedOffset += scrollBy(value - consumedOffset)
        }
    }
}
