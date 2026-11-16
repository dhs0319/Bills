package com.dhs0319.bills.core.designsystem.theme

import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.google.android.material.color.utilities.DynamicColor
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeTonalSpot

@Composable
fun BiliTheme(
    config: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (config.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val baseDensity = LocalDensity.current
    val uiScale = config.uiScale

    val colorScheme = remember(
        context,
        darkTheme,
        config.seedColor,
        config.useDynamicColor
    ) {
        if (config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            createSeedColorScheme(config.seedColor, darkTheme)
        }
    }

    val density = remember(baseDensity, uiScale) {
        Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale / uiScale
        )
    }
    val typography = remember(config.fontScale) { createTypography(config.fontScale) }
    val shapes = remember(config.cornerStyle) { buildShapes(config.cornerStyle) }

    CompositionLocalProvider(LocalDensity provides density) {
        ProvideAnimations(config.animationSpeed) {
            val animatedColorScheme = animateColorScheme(colorScheme)
            ProvidePullRefresh(config.pullRefreshDistanceDp) {
                MaterialTheme(
                    colorScheme = animatedColorScheme,
                    typography = typography,
                    shapes = shapes,
                    content = content
                )
            }
        }
    }
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val animations = LocalAnimations.current
    val transition = updateTransition(targetState = target, label = "Theme color scheme")

    return target.copy(
        primary = transition.animateSchemeColor(animations, "primary") { it.primary },
        onPrimary = transition.animateSchemeColor(animations, "onPrimary") { it.onPrimary },
        primaryContainer = transition.animateSchemeColor(animations, "primaryContainer") { it.primaryContainer },
        onPrimaryContainer = transition.animateSchemeColor(animations, "onPrimaryContainer") { it.onPrimaryContainer },
        inversePrimary = transition.animateSchemeColor(animations, "inversePrimary") { it.inversePrimary },
        secondary = transition.animateSchemeColor(animations, "secondary") { it.secondary },
        onSecondary = transition.animateSchemeColor(animations, "onSecondary") { it.onSecondary },
        secondaryContainer = transition.animateSchemeColor(animations, "secondaryContainer") { it.secondaryContainer },
        onSecondaryContainer = transition.animateSchemeColor(animations, "onSecondaryContainer") { it.onSecondaryContainer },
        tertiary = transition.animateSchemeColor(animations, "tertiary") { it.tertiary },
        onTertiary = transition.animateSchemeColor(animations, "onTertiary") { it.onTertiary },
        tertiaryContainer = transition.animateSchemeColor(animations, "tertiaryContainer") { it.tertiaryContainer },
        onTertiaryContainer = transition.animateSchemeColor(animations, "onTertiaryContainer") { it.onTertiaryContainer },
        background = transition.animateSchemeColor(animations, "background") { it.background },
        onBackground = transition.animateSchemeColor(animations, "onBackground") { it.onBackground },
        surface = transition.animateSchemeColor(animations, "surface") { it.surface },
        onSurface = transition.animateSchemeColor(animations, "onSurface") { it.onSurface },
        surfaceVariant = transition.animateSchemeColor(animations, "surfaceVariant") { it.surfaceVariant },
        onSurfaceVariant = transition.animateSchemeColor(animations, "onSurfaceVariant") { it.onSurfaceVariant },
        surfaceTint = transition.animateSchemeColor(animations, "surfaceTint") { it.surfaceTint },
        inverseSurface = transition.animateSchemeColor(animations, "inverseSurface") { it.inverseSurface },
        inverseOnSurface = transition.animateSchemeColor(animations, "inverseOnSurface") { it.inverseOnSurface },
        error = transition.animateSchemeColor(animations, "error") { it.error },
        onError = transition.animateSchemeColor(animations, "onError") { it.onError },
        errorContainer = transition.animateSchemeColor(animations, "errorContainer") { it.errorContainer },
        onErrorContainer = transition.animateSchemeColor(animations, "onErrorContainer") { it.onErrorContainer },
        outline = transition.animateSchemeColor(animations, "outline") { it.outline },
        outlineVariant = transition.animateSchemeColor(animations, "outlineVariant") { it.outlineVariant },
        scrim = transition.animateSchemeColor(animations, "scrim") { it.scrim },
        surfaceBright = transition.animateSchemeColor(animations, "surfaceBright") { it.surfaceBright },
        surfaceDim = transition.animateSchemeColor(animations, "surfaceDim") { it.surfaceDim },
        surfaceContainer = transition.animateSchemeColor(animations, "surfaceContainer") { it.surfaceContainer },
        surfaceContainerHigh = transition.animateSchemeColor(animations, "surfaceContainerHigh") { it.surfaceContainerHigh },
        surfaceContainerHighest = transition.animateSchemeColor(animations, "surfaceContainerHighest") { it.surfaceContainerHighest },
        surfaceContainerLow = transition.animateSchemeColor(animations, "surfaceContainerLow") { it.surfaceContainerLow },
        surfaceContainerLowest = transition.animateSchemeColor(animations, "surfaceContainerLowest") { it.surfaceContainerLowest },
        primaryFixed = transition.animateSchemeColor(animations, "primaryFixed") { it.primaryFixed },
        primaryFixedDim = transition.animateSchemeColor(animations, "primaryFixedDim") { it.primaryFixedDim },
        onPrimaryFixed = transition.animateSchemeColor(animations, "onPrimaryFixed") { it.onPrimaryFixed },
        onPrimaryFixedVariant = transition.animateSchemeColor(animations, "onPrimaryFixedVariant") { it.onPrimaryFixedVariant },
        secondaryFixed = transition.animateSchemeColor(animations, "secondaryFixed") { it.secondaryFixed },
        secondaryFixedDim = transition.animateSchemeColor(animations, "secondaryFixedDim") { it.secondaryFixedDim },
        onSecondaryFixed = transition.animateSchemeColor(animations, "onSecondaryFixed") { it.onSecondaryFixed },
        onSecondaryFixedVariant = transition.animateSchemeColor(animations, "onSecondaryFixedVariant") { it.onSecondaryFixedVariant },
        tertiaryFixed = transition.animateSchemeColor(animations, "tertiaryFixed") { it.tertiaryFixed },
        tertiaryFixedDim = transition.animateSchemeColor(animations, "tertiaryFixedDim") { it.tertiaryFixedDim },
        onTertiaryFixed = transition.animateSchemeColor(animations, "onTertiaryFixed") { it.onTertiaryFixed },
        onTertiaryFixedVariant = transition.animateSchemeColor(animations, "onTertiaryFixedVariant") { it.onTertiaryFixedVariant }
    )
}

@Composable
private fun Transition<ColorScheme>.animateSchemeColor(
    animations: BiliAnimations,
    label: String,
    targetValueByState: (ColorScheme) -> Color
): Color = animateColor(
    transitionSpec = { tween(durationMillis = animations.medium, easing = animations.standard) },
    label = label,
    targetValueByState = { targetValueByState(it) }
).value

fun previewThemePrimaryColor(seedColor: Color, isDark: Boolean): Color {
    return createSeedColorScheme(seedColor, isDark).primary
}

private fun createSeedColorScheme(seedColor: Color, isDark: Boolean): ColorScheme {
    val scheme = SchemeTonalSpot(
        Hct.fromInt(seedColor.toArgb()),
        isDark,
        0.0
    )
    val materialColors = MaterialDynamicColors()
    fun colorOf(dynamicColor: DynamicColor): Color = Color(dynamicColor.getArgb(scheme))

    return if (isDark) {
        darkColorScheme(
            primary = colorOf(materialColors.primary()),
            onPrimary = colorOf(materialColors.onPrimary()),
            primaryContainer = colorOf(materialColors.primaryContainer()),
            onPrimaryContainer = colorOf(materialColors.onPrimaryContainer()),
            inversePrimary = colorOf(materialColors.inversePrimary()),
            secondary = colorOf(materialColors.secondary()),
            onSecondary = colorOf(materialColors.onSecondary()),
            secondaryContainer = colorOf(materialColors.secondaryContainer()),
            onSecondaryContainer = colorOf(materialColors.onSecondaryContainer()),
            tertiary = colorOf(materialColors.tertiary()),
            onTertiary = colorOf(materialColors.onTertiary()),
            tertiaryContainer = colorOf(materialColors.tertiaryContainer()),
            onTertiaryContainer = colorOf(materialColors.onTertiaryContainer()),
            background = colorOf(materialColors.background()),
            onBackground = colorOf(materialColors.onBackground()),
            surface = colorOf(materialColors.surface()),
            onSurface = colorOf(materialColors.onSurface()),
            surfaceVariant = colorOf(materialColors.surfaceVariant()),
            onSurfaceVariant = colorOf(materialColors.onSurfaceVariant()),
            surfaceTint = colorOf(materialColors.surfaceTint()),
            inverseSurface = colorOf(materialColors.inverseSurface()),
            inverseOnSurface = colorOf(materialColors.inverseOnSurface()),
            outline = colorOf(materialColors.outline()),
            outlineVariant = colorOf(materialColors.outlineVariant()),
            scrim = colorOf(materialColors.scrim()),
            surfaceBright = colorOf(materialColors.surfaceBright()),
            surfaceContainer = colorOf(materialColors.surfaceContainer()),
            surfaceContainerHigh = colorOf(materialColors.surfaceContainerHigh()),
            surfaceContainerHighest = colorOf(materialColors.surfaceContainerHighest()),
            surfaceContainerLow = colorOf(materialColors.surfaceContainerLow()),
            surfaceContainerLowest = colorOf(materialColors.surfaceContainerLowest()),
            surfaceDim = colorOf(materialColors.surfaceDim()),
            error = colorOf(materialColors.error()),
            onError = colorOf(materialColors.onError()),
            errorContainer = colorOf(materialColors.errorContainer()),
            onErrorContainer = colorOf(materialColors.onErrorContainer())
        )
    } else {
        lightColorScheme(
            primary = colorOf(materialColors.primary()),
            onPrimary = colorOf(materialColors.onPrimary()),
            primaryContainer = colorOf(materialColors.primaryContainer()),
            onPrimaryContainer = colorOf(materialColors.onPrimaryContainer()),
            inversePrimary = colorOf(materialColors.inversePrimary()),
            secondary = colorOf(materialColors.secondary()),
            onSecondary = colorOf(materialColors.onSecondary()),
            secondaryContainer = colorOf(materialColors.secondaryContainer()),
            onSecondaryContainer = colorOf(materialColors.onSecondaryContainer()),
            tertiary = colorOf(materialColors.tertiary()),
            onTertiary = colorOf(materialColors.onTertiary()),
            tertiaryContainer = colorOf(materialColors.tertiaryContainer()),
            onTertiaryContainer = colorOf(materialColors.onTertiaryContainer()),
            background = colorOf(materialColors.background()),
            onBackground = colorOf(materialColors.onBackground()),
            surface = colorOf(materialColors.surface()),
            onSurface = colorOf(materialColors.onSurface()),
            surfaceVariant = colorOf(materialColors.surfaceVariant()),
            onSurfaceVariant = colorOf(materialColors.onSurfaceVariant()),
            surfaceTint = colorOf(materialColors.surfaceTint()),
            inverseSurface = colorOf(materialColors.inverseSurface()),
            inverseOnSurface = colorOf(materialColors.inverseOnSurface()),
            outline = colorOf(materialColors.outline()),
            outlineVariant = colorOf(materialColors.outlineVariant()),
            scrim = colorOf(materialColors.scrim()),
            surfaceBright = colorOf(materialColors.surfaceBright()),
            surfaceContainer = colorOf(materialColors.surfaceContainer()),
            surfaceContainerHigh = colorOf(materialColors.surfaceContainerHigh()),
            surfaceContainerHighest = colorOf(materialColors.surfaceContainerHighest()),
            surfaceContainerLow = colorOf(materialColors.surfaceContainerLow()),
            surfaceContainerLowest = colorOf(materialColors.surfaceContainerLowest()),
            surfaceDim = colorOf(materialColors.surfaceDim()),
            error = colorOf(materialColors.error()),
            onError = colorOf(materialColors.onError()),
            errorContainer = colorOf(materialColors.errorContainer()),
            onErrorContainer = colorOf(materialColors.onErrorContainer())
        )
    }
}
