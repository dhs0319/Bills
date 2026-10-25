package com.dhs0319.bills.core.designsystem.component

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.theme.LocalAnimations
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SlidingTabLayout {
    Content,
    Equal
}

private val TabContentStart = VerticalAlignmentLine { first, second -> min(first, second) }
private val TabContentEnd = VerticalAlignmentLine { first, second -> max(first, second) }
private const val TailPositionThreshold = 0.005f
private const val TailVelocityThreshold = 0.05f

/**
 * [position] supplies the continuous tab index without observing pager frames in composition.
 * When omitted, selection changes animate the indicator for non-paged content.
 * [badges] are optional secondary labels included in the indicator's measured content bounds.
 */
@Composable
fun SlidingTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    position: (() -> Float)? = null,
    tabTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
    layout: SlidingTabLayout = SlidingTabLayout.Equal,
    badges: List<String?> = emptyList()
) {
    if (tabs.isEmpty()) return

    val animations = LocalAnimations.current
    val indicatorPosition = position ?: run {
        val animatedPosition = animateFloatAsState(
            targetValue = selectedIndex.coerceIn(tabs.indices).toFloat(),
            animationSpec = tween(durationMillis = animations.medium, easing = animations.standard),
            label = "Tab indicator position"
        )
        remember(animatedPosition) { { animatedPosition.value } }
    }
    val trailingPosition = rememberTabIndicatorTail(indicatorPosition, animations.medium)
    val geometry = remember { TabIndicatorGeometry() }
    val indicatorColor = MaterialTheme.colorScheme.primary
    val scrollModifier = if (layout == SlidingTabLayout.Content) {
        Modifier.horizontalScroll(rememberScrollState())
    } else {
        Modifier
    }

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(scrollModifier)
            .selectableGroup(),
        content = {
            tabs.forEachIndexed { index, title ->
                SlidingTabLabel(
                    title = title,
                    badge = badges.getOrNull(index),
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    textStyle = tabTextStyle
                )
            }

            Canvas(Modifier) {
                val bounds = geometry.bounds
                if (bounds.isEmpty()) return@Canvas

                // These state reads invalidate drawing only, never text composition or measurement.
                val lastTabIndex = bounds.size / 2 - 1
                val head = indicatorPosition().coerceIn(0f, lastTabIndex.toFloat())
                val tail = if (animations.medium <= 1) head else {
                    trailingPosition.value.coerceIn(0f, lastTabIndex.toFloat())
                }
                val fraction = head - floor(head)
                val stretch = if (animations.medium <= 1) 0f else {
                    (0.24 * sin(PI * fraction).let { it * it }).toFloat()
                }
                val indicatorHeight = 3.dp.toPx()
                val left = bounds.edgeAt(min(head - stretch, tail), end = false)
                val right = bounds.edgeAt(max(head + stretch, tail), end = true)
                val drawLeft = if (layoutDirection == LayoutDirection.Rtl) size.width - right else left

                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(drawLeft, size.height - indicatorHeight - 6.dp.toPx()),
                    size = Size(right - left, indicatorHeight),
                    cornerRadius = CornerRadius(indicatorHeight / 2f)
                )
            }
        }
    ) { measurables, constraints ->
        val equalWidth = layout == SlidingTabLayout.Equal && constraints.hasBoundedWidth
        val tabPlaceables = Array(tabs.size) { index ->
            val cellWidth = if (equalWidth) {
                constraints.maxWidth * (index + 1) / tabs.size - constraints.maxWidth * index / tabs.size
            } else {
                Constraints.Infinity
            }
            measurables[index].measure(
                Constraints(
                    minWidth = if (equalWidth) cellWidth else 48.dp.roundToPx(),
                    maxWidth = cellWidth,
                    minHeight = constraints.minHeight,
                    maxHeight = constraints.maxHeight
                )
            )
        }
        val width = constraints.constrainWidth(tabPlaceables.sumOf { it.width })
        val height = constraints.constrainHeight(tabPlaceables.maxOf { it.height })
        val bounds = FloatArray(tabs.size * 2)
        var start = 0
        tabPlaceables.forEachIndexed { index, tab ->
            bounds[index * 2] = start + tab[TabContentStart].toFloat()
            bounds[index * 2 + 1] = start + tab[TabContentEnd].toFloat()
            start += tab.width
        }
        geometry.update(bounds)
        val indicator = measurables.last().measure(Constraints.fixed(width, height))

        layout(width, height) {
            indicator.place(0, 0)
            var tabStart = 0
            tabPlaceables.forEach { tab ->
                tab.placeRelative(tabStart, (height - tab.height) / 2)
                tabStart += tab.width
            }
        }
    }
}

@Composable
private fun SlidingTabLabel(
    title: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
    textStyle: TextStyle
) {
    Layout(
        modifier = Modifier
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        content = {
            Text(
                text = title,
                color = if (selected) MaterialTheme.colorScheme.primary else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!badge.isNullOrBlank()) {
                Text(
                    text = badge,
                    color = if (selected) MaterialTheme.colorScheme.primary else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) { measurables, constraints ->
        val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val badgeMaxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth / 2 else Constraints.Infinity
        val badge = measurables.getOrNull(1)?.measure(contentConstraints.copy(maxWidth = badgeMaxWidth))
        val gap = if (badge == null) 0 else min(4.dp.roundToPx(), constraints.maxWidth)
        // Infinity is a sentinel, not a pixel budget; subtracting from it creates invalid constraints.
        val titleMaxWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - (badge?.width ?: 0) - gap).coerceAtLeast(0)
        } else {
            Constraints.Infinity
        }
        val title = measurables[0].measure(contentConstraints.copy(maxWidth = titleMaxWidth))
        val contentWidth = title.width + gap + (badge?.width ?: 0)
        val baseline = max(title[FirstBaseline], badge?.get(FirstBaseline) ?: 0)
        val contentHeight = baseline + max(
            title.height - title[FirstBaseline],
            badge?.let { it.height - it[FirstBaseline] } ?: 0
        )
        val width = constraints.constrainWidth(contentWidth)
        val height = constraints.constrainHeight(contentHeight + 16.dp.roundToPx())
        val contentStart = (width - contentWidth) / 2
        val contentTop = ((height - contentHeight - 8.dp.roundToPx()) / 2).coerceAtLeast(0)

        layout(
            width,
            height,
            mapOf(TabContentStart to contentStart, TabContentEnd to contentStart + contentWidth)
        ) {
            title.placeRelative(contentStart, contentTop + baseline - title[FirstBaseline])
            badge?.placeRelative(contentStart + title.width + gap, contentTop + baseline - badge[FirstBaseline])
        }
    }
}

/** Interleaved content starts and ends are updated together and reused by every animation frame. */
private class TabIndicatorGeometry {
    var bounds by mutableStateOf(FloatArray(0))
        private set

    fun update(newBounds: FloatArray) {
        Snapshot.withoutReadObservation {
            if (!bounds.contentEquals(newBounds)) bounds = newBounds
        }
    }
}

private fun FloatArray.edgeAt(position: Float, end: Boolean): Float {
    val lastTabIndex = size / 2 - 1
    val boundedPosition = position.coerceIn(0f, lastTabIndex.toFloat())
    val start = boundedPosition.toInt()
    val next = min(start + 1, lastTabIndex)
    val edge = if (end) 1 else 0
    val startValue = this[start * 2 + edge]
    val nextValue = this[next * 2 + edge]
    return startValue + (nextValue - startValue) * (boundedPosition - start)
}

/** A single frame loop follows a moving target, without restarting an animation on every pager frame. */
@Composable
private fun rememberTabIndicatorTail(position: () -> Float, durationMillis: Int): State<Float> {
    val enabled = durationMillis > 1
    val tail = remember(position, enabled) {
        mutableFloatStateOf(Snapshot.withoutReadObservation { position() })
    }
    val springSpec = remember(durationMillis) {
        val speed = 200f / durationMillis.coerceAtLeast(1)
        FloatSpringSpec(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow * speed * speed,
            visibilityThreshold = TailPositionThreshold
        )
    }
    LaunchedEffect(position, springSpec, enabled) {
        if (!enabled) return@LaunchedEffect

        val durationScale = coroutineContext[MotionDurationScale]
        coroutineScope {
            var follower: Job? = null
            snapshotFlow { position() }.collect { target ->
                if (follower?.isActive == true || abs(target - tail.floatValue) <= TailPositionThreshold) {
                    return@collect
                }
                follower = launch {
                    var velocity = 0f
                    var previousFrame = withFrameNanos { it }
                    var moving = true
                    while (moving) {
                        withFrameNanos { frame ->
                            val targetPosition = position()
                            val scale = durationScale?.scaleFactor ?: 1f
                            if (scale <= 0f) {
                                tail.floatValue = targetPosition
                                moving = false
                            } else {
                                val delta = ((frame - previousFrame) / scale).toLong()
                                val initialValue = tail.floatValue
                                val value = springSpec.getValueFromNanos(delta, initialValue, targetPosition, velocity)
                                velocity = springSpec.getVelocityFromNanos(delta, initialValue, targetPosition, velocity)
                                moving = abs(targetPosition - value) > TailPositionThreshold ||
                                    abs(velocity) > TailVelocityThreshold
                                tail.floatValue = if (moving) value else targetPosition
                            }
                            previousFrame = frame
                        }
                    }
                }
            }
        }
    }
    return tail
}

@Composable
fun AdaptiveSlidingTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    layout: SlidingTabLayout = SlidingTabLayout.Content
) {
    AdaptiveTabLayout(layout, modifier) { tabModifier ->
        SlidingTabRow(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            layout = layout,
            modifier = tabModifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdaptivePagerSlidingTabRow(
    tabs: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    layout: SlidingTabLayout = SlidingTabLayout.Content
) {
    AdaptiveTabLayout(layout, modifier) { tabModifier ->
        PagerSlidingTabRow(
            tabs = tabs,
            pagerState = pagerState,
            layout = layout,
            modifier = tabModifier
        )
    }
}

@Composable
private fun AdaptiveTabLayout(
    layout: SlidingTabLayout,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        content(if (layout == SlidingTabLayout.Equal) Modifier.widthIn(max = 360.dp) else Modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerSlidingTabRow(
    tabs: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onReselect: (Int) -> Unit = {},
    tabTextStyle: TextStyle = MaterialTheme.typography.titleMedium,
    layout: SlidingTabLayout = SlidingTabLayout.Equal,
    badges: List<String?> = emptyList()
) {
    val scope = rememberCoroutineScope()
    val baseDurationMillis = LocalAnimations.current.medium
    val currentOnReselect = rememberUpdatedState(onReselect)
    val position = remember(pagerState) { { pagerState.currentPage + pagerState.currentPageOffsetFraction } }
    val onSelect: (Int) -> Unit = remember(pagerState, scope, baseDurationMillis) {
        var scrollJob: Job? = null
        var requestedPage = -1
        select@{ page: Int ->
            if (scrollJob?.isActive == true && requestedPage == page) return@select
            if (page == pagerState.currentPage && !pagerState.isScrollInProgress &&
                abs(pagerState.currentPageOffsetFraction) < 0.001f
            ) {
                currentOnReselect.value(page)
            } else {
                scrollJob?.cancel()
                requestedPage = page
                scrollJob = scope.launch { pagerState.animateScrollContinuouslyToPage(page, baseDurationMillis) }
            }
        }
    }
    SlidingTabRow(
        tabs = tabs,
        selectedIndex = pagerState.currentPage,
        position = position,
        onSelect = onSelect,
        tabTextStyle = tabTextStyle,
        layout = layout,
        badges = badges,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun PagerState.animateScrollContinuouslyToPage(page: Int, baseDurationMillis: Int) {
    if (baseDurationMillis <= 1) {
        scrollToPage(page)
        return
    }
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
