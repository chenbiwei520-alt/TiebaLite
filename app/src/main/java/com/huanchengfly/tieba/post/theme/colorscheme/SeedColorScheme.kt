package com.huanchengfly.tieba.post.theme.colorscheme

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.Variant
import com.huanchengfly.tieba.post.theme.DynamicSchemes

/**
 * Creating a Material color scheme from a seed color. Kept for the translucent theme's
 * seed-based palette; the Monet (wallpaper) theme itself has been removed.
 *
 * @param seed source color in ARGB
 * @param variant scheme variant
 * @param dark whether to generate the dark scheme
 * @param contrastLevel (-1 to 1, currently contrast ratio 3.0 and 7.0)
 */
fun generateColorSchemeFromSeed(
    seed: Int,
    variant: Variant = Variant.CONTENT,
    dark: Boolean,
    @FloatRange(-1.0, 1.0) contrastLevel: Double = 0.0
): ColorScheme {
    val scheme = DynamicSchemes.from(Hct.fromInt(seed), variant, dark, contrastLevel)

    val dynamicColors = MaterialDynamicColors()
    return ColorScheme(
        primary = dynamicColors.primary().getArgb(scheme).toComposeColor(),
        onPrimary = dynamicColors.onPrimary().getArgb(scheme).toComposeColor(),
        primaryContainer = dynamicColors.primaryContainer().getArgb(scheme).toComposeColor(),
        onPrimaryContainer = dynamicColors.onPrimaryContainer().getArgb(scheme).toComposeColor(),
        inversePrimary = dynamicColors.inversePrimary().getArgb(scheme).toComposeColor(),
        secondary = dynamicColors.secondary().getArgb(scheme).toComposeColor(),
        onSecondary = dynamicColors.onSecondary().getArgb(scheme).toComposeColor(),
        secondaryContainer = dynamicColors.secondaryContainer().getArgb(scheme).toComposeColor(),
        onSecondaryContainer = dynamicColors.onSecondaryContainer().getArgb(scheme).toComposeColor(),
        tertiary = dynamicColors.tertiary().getArgb(scheme).toComposeColor(),
        onTertiary = dynamicColors.onTertiary().getArgb(scheme).toComposeColor(),
        tertiaryContainer = dynamicColors.tertiary().getArgb(scheme).toComposeColor(),
        onTertiaryContainer = dynamicColors.onTertiaryContainer().getArgb(scheme).toComposeColor(),
        background = dynamicColors.background().getArgb(scheme).toComposeColor(),
        onBackground = dynamicColors.onBackground().getArgb(scheme).toComposeColor(),
        surface = dynamicColors.surface().getArgb(scheme).toComposeColor(),
        onSurface = dynamicColors.onSurface().getArgb(scheme).toComposeColor(),
        surfaceVariant = dynamicColors.surfaceVariant().getArgb(scheme).toComposeColor(),
        onSurfaceVariant = dynamicColors.onSurfaceVariant().getArgb(scheme).toComposeColor(),
        surfaceTint = dynamicColors.surfaceTint().getArgb(scheme).toComposeColor(),
        inverseSurface = dynamicColors.inverseSurface().getArgb(scheme).toComposeColor(),
        inverseOnSurface = dynamicColors.inverseOnSurface().getArgb(scheme).toComposeColor(),
        error = dynamicColors.error().getArgb(scheme).toComposeColor(),
        onError = dynamicColors.onError().getArgb(scheme).toComposeColor(),
        errorContainer = dynamicColors.errorContainer().getArgb(scheme).toComposeColor(),
        onErrorContainer = dynamicColors.onErrorContainer().getArgb(scheme).toComposeColor(),
        outline = dynamicColors.outline().getArgb(scheme).toComposeColor(),
        outlineVariant = dynamicColors.outlineVariant().getArgb(scheme).toComposeColor(),
        scrim = Color.Black,
        surfaceBright = dynamicColors.surfaceBright().getArgb(scheme).toComposeColor(),
        surfaceDim = dynamicColors.surfaceDim().getArgb(scheme).toComposeColor(),
        surfaceContainer = dynamicColors.surfaceContainer().getArgb(scheme).toComposeColor(),
        surfaceContainerHigh = dynamicColors.surfaceContainerHigh().getArgb(scheme).toComposeColor(),
        surfaceContainerHighest = dynamicColors.surfaceContainerHighest().getArgb(scheme).toComposeColor(),
        surfaceContainerLow = dynamicColors.surfaceContainerLow().getArgb(scheme).toComposeColor(),
        surfaceContainerLowest = dynamicColors.surfaceContainerLowest().getArgb(scheme).toComposeColor(),
    )
}

private inline fun Int.toComposeColor(): Color = Color(this)
