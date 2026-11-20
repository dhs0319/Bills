package com.dhs0319.bills.feature.video.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlinedfilled.Close
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** An overlay bounded by the detail pane, so playback stays visible and interactive. */
@Composable
internal fun DetailSelectionPanel(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable (close: (() -> Unit) -> Unit) -> Unit
) {
    val state = remember { AnchoredDraggableState(PanelPosition.Hidden) }
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var heightPx by remember { mutableIntStateOf(0) }
    var opening by remember { mutableStateOf(true) }
    var closing by remember { mutableStateOf(false) }
    val close: (() -> Unit) -> Unit = { afterClose ->
        if (!closing) {
            closing = true
            scope.launch {
                state.animateTo(PanelPosition.Hidden, tween(180))
                currentOnDismiss()
                afterClose()
            }
        }
    }
    val dismiss = { close {} }

    LaunchedEffect(state) {
        snapshotFlow { heightPx }.first { it > 0 }
        try {
            state.animateTo(PanelPosition.Expanded, tween(240))
        } finally {
            opening = false
        }
    }
    LaunchedEffect(state) {
        snapshotFlow {
            !opening && !closing && state.settledValue == PanelPosition.Hidden
        }.first { it }
        currentOnDismiss()
    }
    BackHandler(onBack = dismiss)

    val thresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = remember(thresholdPx) { { distance -> minOf(thresholdPx, distance / 2) } },
        animationSpec = tween(180)
    )
    val nestedScrollConnection = remember(state, flingBehavior) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (!opening && !closing && source == NestedScrollSource.UserInput && available.y < 0f) {
                    Offset(0f, state.dispatchRawDelta(available.y))
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return if (!opening && !closing && source == NestedScrollSource.UserInput) {
                    Offset(0f, state.dispatchRawDelta(available.y))
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (!opening && !closing && available.y < 0f && state.requireOffset() > 0f) {
                    state.settleWithVelocity(available.y, flingBehavior)
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (!opening && !closing) {
                    state.settleWithVelocity(available.y, flingBehavior)
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }
    val scrimColor = MaterialTheme.colorScheme.scrim

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { size ->
                state.updateAnchors(
                    DraggableAnchors {
                        PanelPosition.Expanded at 0f
                        PanelPosition.Hidden at size.height.toFloat()
                    },
                    newTarget = state.targetValue
                )
                heightPx = size.height
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val offset = state.offset.takeUnless(Float::isNaN) ?: size.height
                    val progress = if (size.height > 0f) (1f - offset / size.height).coerceIn(0f, 1f) else 0f
                    drawRect(scrimColor.copy(alpha = 0.32f * progress))
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "关闭面板",
                    onClick = dismiss
                )
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                // Read the offset in the layer block to avoid relayout on every animation frame.
                .graphicsLayer {
                    translationY = (state.offset.takeUnless(Float::isNaN) ?: size.height).coerceIn(0f, size.height)
                }
                .nestedScroll(nestedScrollConnection)
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Vertical,
                    enabled = !opening && !closing,
                    flingBehavior = flingBehavior
                )
                .semantics {
                    paneTitle = title
                    dismiss {
                        dismiss()
                        true
                    }
                },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = containerColor
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(48.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .size(width = 32.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(2.dp))
                    )
                    IconButton(onClick = dismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(MaterialSymbols.OutlinedFilled.Close, contentDescription = "关闭面板")
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    content(close)
                }
            }
        }
    }
}

private enum class PanelPosition { Hidden, Expanded }

private suspend fun AnchoredDraggableState<PanelPosition>.settleWithVelocity(
    velocity: Float,
    flingBehavior: FlingBehavior
) {
    anchoredDrag {
        val scrollScope = object : ScrollScope {
            override fun scrollBy(pixels: Float): Float {
                val previous = requireOffset()
                val next = (previous + pixels).coerceIn(anchors.minPosition(), anchors.maxPosition())
                dragTo(next)
                return next - previous
            }
        }
        with(flingBehavior) { scrollScope.performFling(velocity) }
    }
}
