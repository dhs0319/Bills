package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun AvatarImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    BiliAsyncImage(
        url = url,
        contentDescription = contentDescription,
        modifier = modifier.clip(CircleShape),
        variant = BiliImageVariant.Avatar
    )
}
