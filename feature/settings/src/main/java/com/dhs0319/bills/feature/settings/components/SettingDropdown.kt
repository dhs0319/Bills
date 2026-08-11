package com.dhs0319.bills.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun <T> SettingDropdown(
    title: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    val menuShape = RoundedCornerShape(24.dp)
    val density = LocalDensity.current
    val screenMargin = with(density) { 8.dp.roundToPx() }
    val menuPositionProvider = remember(pressOffset, screenMargin) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val pointerX = anchorBounds.left + pressOffset.x.roundToInt()
                val pointerY = anchorBounds.top + pressOffset.y.roundToInt()
                val maxX = (windowSize.width - popupContentSize.width - screenMargin)
                    .coerceAtLeast(screenMargin)
                val maxY = (windowSize.height - popupContentSize.height - screenMargin)
                    .coerceAtLeast(screenMargin)
                val menuY = if (pointerY + popupContentSize.height <= windowSize.height - screenMargin) {
                    pointerY
                } else {
                    pointerY - popupContentSize.height
                }

                return IntOffset(
                    x = pointerX.coerceIn(screenMargin, maxX),
                    y = menuY.coerceIn(screenMargin, maxY)
                )
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            pressOffset = down.position
                            waitForUpOrCancellation()
                        }
                    }
                    .clickable { expanded = true },
                headlineContent = {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                },
                supportingContent = {
                    Text(optionLabel(selected))
                },
                trailingContent = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "选择$title")
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (expanded) {
                Popup(
                    popupPositionProvider = menuPositionProvider,
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Surface(
                        modifier = Modifier.width(200.dp),
                        shape = menuShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(optionLabel(option)) },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = selected == option,
                                            onClick = null
                                        )
                                    },
                                    onClick = {
                                        onSelect(option)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
