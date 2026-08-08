package com.dhs0319.bills.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalPullRefreshThreshold = compositionLocalOf<Dp> {
    DEFAULT_PULL_REFRESH_DISTANCE_DP.dp
}

@Composable
fun ProvidePullRefresh(
    distanceDp: Float,
    content: @Composable () -> Unit
) {
    val threshold = remember(distanceDp) { distanceDp.dp }
    CompositionLocalProvider(
        LocalPullRefreshThreshold provides threshold,
        content = content
    )
}
