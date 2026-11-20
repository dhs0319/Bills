package com.dhs0319.bills.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** A looping three-bar live indicator, sized to 16.dp unless overridden with [modifier]. */
@Composable
fun AnimatedLiveBadge(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = "直播中"
) {
    val transition = rememberInfiniteTransition(label = "Live badge")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = FullCircle,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Live bars"
    )

    val descriptionModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    val color = tint.takeOrElse { Color.Black }
    Canvas(modifier = modifier.size(16.dp).then(descriptionModifier)) {
        // Draw Material Equalizer's original bar positions and widths directly, once per bar.
        val side = size.minDimension
        val left = (size.width - side) / 2f
        val baseline = (size.height - side) / 2f + side * 800f / 960f
        val barWidth = side * 160f / 960f
        val currentPhase = phase.value
        LiveBars.forEachIndexed { index, bar ->
            val height = side * (bar.middleHeight + bar.amplitude *
                sin(currentPhase + FullCircle * index / LiveBars.size))
            drawRect(
                color = color,
                topLeft = Offset(left + side * (160f + 240f * index) / 960f, baseline - height),
                size = Size(barWidth, height)
            )
        }
    }
}

private data class LiveBar(
    val middleHeight: Float,
    val amplitude: Float
)

private val FullCircle = (2 * PI).toFloat()

private val LiveBars = listOf(
    LiveBar(middleHeight = 0.45f, amplitude = 0.22f),
    LiveBar(middleHeight = 0.55f, amplitude = 0.23f),
    LiveBar(middleHeight = 0.42f, amplitude = 0.19f)
)
