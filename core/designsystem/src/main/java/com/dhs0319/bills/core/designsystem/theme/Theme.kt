package com.dhs0319.bills.core.designsystem.theme

import android.os.Build
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
            ProvidePullRefresh(config.pullRefreshDistanceDp) {
                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = typography,
                    shapes = shapes,
                    content = content
                )
            }
        }
    }
}

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
