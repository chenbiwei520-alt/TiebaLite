package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/**
 * HyperOS palette: the M3 color scheme is derived from the Miuix theme defaults
 * (HyperOS blue 0xFF3482FF light / 0xFF277AF7 dark) so Material components share
 * the Miuix accent.
 */
val HyperOsColorScheme: ColorSchemeDayNight = ColorSchemeDayNight(
    lightColor = miuixToM3ColorScheme(miuixLightColorScheme(), dark = false),
    darkColor = miuixToM3ColorScheme(miuixDarkColorScheme(), dark = true),
)

// Kept as an alias: existing references expect the blue palette
val BlueColorScheme: ColorSchemeDayNight = HyperOsColorScheme

private fun miuixToM3ColorScheme(m: Colors, dark: Boolean): ColorScheme = lightColorScheme(
    primary = m.primary,
    onPrimary = m.onPrimary,
    primaryContainer = m.primaryContainer,
    onPrimaryContainer = m.onPrimaryContainer,
    inversePrimary = m.primaryContainer,
    secondary = m.secondary,
    onSecondary = m.onSecondary,
    secondaryContainer = m.secondaryContainer,
    onSecondaryContainer = m.onSecondaryContainer,
    tertiary = m.primaryVariant,
    onTertiary = m.onPrimaryVariant,
    tertiaryContainer = m.tertiaryContainer,
    onTertiaryContainer = m.onTertiaryContainer,
    background = m.surface,
    onBackground = m.onSurface,
    surface = m.surface,
    onSurface = m.onSurface,
    surfaceVariant = m.surfaceVariant,
    onSurfaceVariant = m.onSurfaceVariantSummary,
    surfaceTint = m.primary,
    inverseSurface = m.onBackground,
    inverseOnSurface = m.background,
    error = m.error,
    onError = m.onError,
    errorContainer = m.errorContainer,
    onErrorContainer = m.onErrorContainer,
    outline = m.outline,
    outlineVariant = m.dividerLine,
    scrim = m.windowDimming,
    surfaceBright = if (dark) m.surfaceContainerHighest else m.surface,
    surfaceDim = if (dark) m.surfaceContainer else m.surfaceContainerHighest,
    surfaceContainer = m.surfaceContainer,
    surfaceContainerHigh = m.surfaceContainerHigh,
    surfaceContainerHighest = m.surfaceContainerHighest,
    surfaceContainerLow = m.surface,
    surfaceContainerLowest = m.background,
)
