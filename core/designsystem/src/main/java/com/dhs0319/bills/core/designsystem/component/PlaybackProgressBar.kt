package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProgressBar(
    value: Float,
    bufferedValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = if (enabled) Color.White else Color.White.copy(alpha = 0.28f)
    val inactiveColor = if (enabled) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.14f)
    val bufferedColor = if (enabled) Color.White.copy(alpha = 0.56f) else Color.White.copy(alpha = 0.22f)
    val layoutDirection = LocalLayoutDirection.current
    val visualValue = value.coerceIn(0f, 1f)
    val visualBufferedValue = bufferedValue.coerceIn(visualValue, 1f)
    var dragValue by remember { mutableFloatStateOf(visualValue) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState = rememberUpdatedState(onValueChangeFinished)
    val dragState = rememberDraggableState { delta ->
        if (trackWidthPx > 0f) {
            val directionalDelta = if (layoutDirection == LayoutDirection.Rtl) -delta else delta
            dragValue = (dragValue + directionalDelta / trackWidthPx).coerceIn(0f, 1f)
            onValueChangeState.value(dragValue)
        }
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Slider(
            value = visualValue,
            onValueChange = {},
            enabled = false,
            valueRange = 0f..1f,
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(activeColor, CircleShape)
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            enabled = enabled,
                            startDragImmediately = true,
                            onDragStarted = { dragValue = visualValue },
                            onDragStopped = { onValueChangeFinishedState.value() }
                        )
                )
            },
            track = { sliderState ->
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .onSizeChanged { trackWidthPx = it.width.toFloat() }
                        .pointerInput(enabled, layoutDirection) {
                            if (enabled) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    dragValue = if (layoutDirection == LayoutDirection.Rtl) {
                                        1f - fraction
                                    } else {
                                        fraction
                                    }
                                    onValueChangeState.value(dragValue)
                                    onValueChangeFinishedState.value()
                                }
                            }
                        }
                ) {
                    val startX = if (layoutDirection == LayoutDirection.Rtl) size.width else 0f
                    val endX = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width
                    val bufferedEndX = startX + (endX - startX) * visualBufferedValue
                    val activeEndX = startX + (endX - startX) * sliderState.coercedValueAsFraction
                    val centerY = size.height / 2f

                    drawLine(
                        color = inactiveColor,
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bufferedColor,
                        start = Offset(startX, centerY),
                        end = Offset(bufferedEndX, centerY),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = activeColor,
                        start = Offset(startX, centerY),
                        end = Offset(activeEndX, centerY),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                }
            },
            modifier = modifier.height(16.dp)
        )
    }
}
