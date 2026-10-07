package com.dhs0319.bills.feature.im.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.model.ImSessionItem
import kotlin.math.roundToInt

private enum class SwipeAnchor { Closed, Open }

private val ACTION_BUTTON_WIDTH = 84.dp

@Composable
internal fun ImSessionSwipeItem(
    item: ImSessionItem,
    isOpen: Boolean,
    onSwipeStateChange: (Boolean) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val showPin = if (item.isPinned) item.canUnpin else item.canPin
    if (item.sessionId == null || (!showPin && !item.canDelete)) {
        ImSessionCard(item = item, onClick = onClick, modifier = modifier)
        return
    }

    val state = remember { AnchoredDraggableState(SwipeAnchor.Closed) }

    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { settled ->
            onSwipeStateChange(settled == SwipeAnchor.Open)
        }
    }

    LaunchedEffect(isOpen) {
        val target = if (isOpen) SwipeAnchor.Open else SwipeAnchor.Closed
        if (state.settledValue != target) {
            state.animateTo(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .onSizeChanged { size ->
                        val width = size.width.toFloat()
                        if (width > 0f) {
                            state.updateAnchors(
                                DraggableAnchors {
                                    SwipeAnchor.Closed at 0f
                                    SwipeAnchor.Open at -width
                                }
                            )
                        }
                    },
                horizontalArrangement = Arrangement.End
            ) {
                if (showPin) {
                    SwipeActionButton(
                        text = if (item.isPinned) "取消置顶" else "置顶",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            onSwipeStateChange(false)
                            onTogglePin()
                        },
                        modifier = Modifier.width(ACTION_BUTTON_WIDTH)
                    )
                }
                if (item.canDelete) {
                    SwipeActionButton(
                        text = "删除",
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        onClick = {
                            onSwipeStateChange(false)
                            onDelete()
                        },
                        modifier = Modifier.width(ACTION_BUTTON_WIDTH)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val offset = state.offset
                    IntOffset((if (offset.isNaN()) 0f else offset).roundToInt(), 0)
                }
                .anchoredDraggable(state, Orientation.Horizontal)
        ) {
            ImSessionCard(
                item = item,
                onClick = if (isOpen) {
                    { onSwipeStateChange(false) }
                } else {
                    onClick
                }
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
