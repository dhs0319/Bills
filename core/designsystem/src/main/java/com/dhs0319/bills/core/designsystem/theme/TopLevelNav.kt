package com.dhs0319.bills.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Measured height of the floating top level navigation bar that overlays tab content.
 *
 * Screens rendered behind that floating bar should add this value to their bottom content
 * padding so the last item is never covered by the bar. It stays 0.dp for layouts that do
 * not overlay content (for example the landscape side navigation).
 */
val LocalTopLevelNavSpace = compositionLocalOf<Dp> { 0.dp }
