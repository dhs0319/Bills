package com.dhs0319.bills.core.designsystem.theme

import android.graphics.Color as AndroidColor
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
    val sourceHsv = FloatArray(3)
    AndroidColor.colorToHSV(seedColor.toArgb(), sourceHsv)
    val isNeutralSeed = sourceHsv[1] < 0.05f
    val isSoftSeed = sourceHsv[1] < 0.20f
    val neutralValue = sourceHsv[2]
    val seed = seedColor.tone(
        saturationScale = if (isSoftSeed) 1f else 0.82f,
        value = if (isNeutralSeed) neutralValue else 0.92f
    )
    val containerSaturationScale = if (isSoftSeed) {
        1f
    } else if (isDark) {
        0.45f
    } else {
        0.24f
    }
    val accentSaturationScale = if (isSoftSeed) {
        1f
    } else if (isDark) {
        0.48f
    } else {
        0.20f
    }
    val accentContainerSaturationScale = if (isSoftSeed) {
        1f
    } else if (isDark) {
        0.24f
    } else {
        0.20f
    }
    val primaryValue = if (isNeutralSeed) {
        if (isDark) {
            (0.90f - neutralValue * 0.35f).coerceIn(0.50f, 0.90f)
        } else {
            (0.18f + neutralValue * 0.55f).coerceIn(0.18f, 0.75f)
        }
    } else if (isDark) {
        0.82f
    } else {
        0.64f
    }
    val primaryContainerValue = if (isNeutralSeed) {
        if (isDark) 0.26f + neutralValue * 0.16f else 0.86f + neutralValue * 0.10f
    } else if (isDark) {
        0.26f
    } else {
        0.92f
    }
    val accentValue = if (isNeutralSeed) {
        if (isDark) 0.84f - neutralValue * 0.28f else 0.30f + neutralValue * 0.32f
    } else if (isDark) {
        0.78f
    } else {
        0.50f
    }
    val accentContainerValue = if (isNeutralSeed) {
        if (isDark) 0.28f + neutralValue * 0.12f else 0.88f + neutralValue * 0.08f
    } else if (isDark) {
        0.22f
    } else {
        0.94f
    }
    val primary = seed.tone(value = primaryValue)
    val primaryContainer = seed.tone(
        saturationScale = containerSaturationScale,
        value = primaryContainerValue
    )
    val inversePrimary = seed.tone(value = 0.42f)
    val secondary = seed.tone(
        saturationScale = accentSaturationScale,
        value = accentValue
    )
    val secondaryContainer = seed.tone(
        saturationScale = accentContainerSaturationScale,
        value = accentContainerValue
    )
    val tertiary = seed.tone(
        saturationScale = accentSaturationScale,
        value = accentValue
    )
    val tertiaryContainer = seed.tone(
        saturationScale = accentContainerSaturationScale,
        value = accentContainerValue
    )
    val background = seed.tone(
        saturationScale = if (isDark) 0.06f else 0.08f,
        value = if (isDark) 0.08f else 0.97f
    )
    val surface = seed.tone(
        saturationScale = if (isDark) 0.06f else 0.08f,
        value = if (isDark) 0.10f else 0.98f
    )
    val surfaceTint = primary
    val surfaceVariant = if (isDark) Color(0xFF1C1C1C) else Color(0xFFF0F0F0)
    val onSurfaceVariant = if (isDark) Color(0xFFD0D0D0) else Color(0xFF505050)
    val scrim = seed.tone(saturationScale = 0.04f, value = if (isDark) 0.04f else 0.10f)
    val surfaceBright = if (isDark) seed.tone(saturationScale = 0.06f, value = 0.16f) else Color.White
    val surfaceContainer = seed.tone(
        saturationScale = 0.06f,
        value = if (isDark) 0.12f else 0.96f
    )
    val surfaceContainerHigh = seed.tone(
        saturationScale = 0.06f,
        value = if (isDark) 0.16f else 0.94f
    )
    val surfaceContainerHighest = seed.tone(
        saturationScale = 0.06f,
        value = if (isDark) 0.20f else 0.92f
    )
    val surfaceContainerLow = seed.tone(
        saturationScale = 0.06f,
        value = if (isDark) 0.08f else 0.98f
    )
    val surfaceContainerLowest = if (isDark) seed.tone(saturationScale = 0.04f, value = 0.05f) else Color.White
    val surfaceDim = seed.tone(
        saturationScale = 0.05f,
        value = if (isDark) 0.06f else 0.94f
    )
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color.White,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = Color.White,
            tertiary = tertiary,
            onTertiary = Color.Black,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = Color.White,
            background = background,
            onBackground = Color.White,
            surface = surface,
            onSurface = Color.White,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = Color.White,
            inverseOnSurface = Color.Black,
            outline = Color(0xFF808080),
            outlineVariant = Color(0xFF404040),
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceDim = surfaceDim,
            error = Color(0xFFE0E0E0),
            onError = Color.Black,
            errorContainer = Color(0xFF2A2A2A),
            onErrorContainer = Color.White
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color.Black,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = Color.Black,
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = Color.Black,
            background = background,
            onBackground = Color.Black,
            surface = surface,
            onSurface = Color.Black,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = Color(0xFF1A1A1A),
            inverseOnSurface = Color.White,
            outline = Color(0xFF707070),
            outlineVariant = Color(0xFFC8C8C8),
            scrim = scrim,
            surfaceBright = Color.White,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceDim = surfaceDim,
            error = Color(0xFF2E2E2E),
            onError = Color.White,
            errorContainer = Color(0xFFE2E2E2),
            onErrorContainer = Color.Black
        )
    }
}

private fun Color.tone(
    saturationScale: Float = 1f,
    value: Float
): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    hsv[1] = (hsv[1] * saturationScale).coerceIn(0f, 1f)
    hsv[2] = value.coerceIn(0f, 1f)
    return Color(AndroidColor.HSVToColor(hsv))
}
