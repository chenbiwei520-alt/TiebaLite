package com.huanchengfly.tieba.post.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.Typography
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.theme.colorscheme.BlueColorScheme
import com.huanchengfly.tieba.post.utils.ColorUtils
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalExtendedColorScheme = staticCompositionLocalOf { DefaultColors }

val DefaultColors = ExtendedColorScheme(BlueColorScheme.lightColor, darkTheme = false)

val DefaultDarkColors = ExtendedColorScheme(BlueColorScheme.darkColor, darkTheme = true)

@Composable
fun TiebaLiteTheme(
    colorSchemeExt: ExtendedColorScheme = if (isSystemInDarkTheme()) DefaultDarkColors else DefaultColors,
    motionScheme: MotionScheme = MotionScheme.expressive(),
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = MaterialTheme.typography,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalExtendedColorScheme provides colorSchemeExt,
        LocalWindowAdaptiveInfo provides currentWindowAdaptiveInfo(),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorSchemeExt.colorScheme,
            motionScheme = motionScheme,
            shapes = shapes,
            typography = typography,
            content = {
                // Miuix owns the HyperOS color system (its own light/dark schemes);
                // Material keeps the app palettes for Material components.
                // The controller follows the app-resolved dark state (dark preference +
                // overrides) instead of the raw system config, so Miuix surfaces match the
                // Material side in every dark/light mode. In the translucent background theme,
                // Miuix surfaces stay transparent so the wallpaper remains visible.
                val darkTheme = colorSchemeExt.darkTheme
                val translucent = colorSchemeExt.colorScheme.isTranslucent
                val controller = remember(darkTheme, translucent) {
                    ThemeController(
                        colorSchemeMode = if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
                        lightColors = if (translucent) {
                            lightColorScheme(
                                background = Color.Transparent,
                                surface = Color.Transparent,
                            )
                        } else {
                            lightColorScheme()
                        },
                        darkColors = if (translucent) {
                            darkColorScheme(
                                background = Color.Transparent,
                                surface = Color.Transparent,
                            )
                        } else {
                            darkColorScheme()
                        },
                    )
                }
                MiuixTheme(controller = controller) {
                    // MaterialTheme does not provide LocalContentColor, so a Material Text/Icon
                    // without an explicit color falls back to Color.Black. That is invisible on
                    // dark surfaces, and pages hosted by the Miuix Scaffold never get a Material
                    // Surface to supply it either. Provide it once from the app palette.
                    CompositionLocalProvider(
                        androidx.compose.material3.LocalContentColor provides
                                colorSchemeExt.colorScheme.onBackground,
                    ) {
                        content()
                    }
                }
            }
        )
    }
}

val ColorScheme.isTranslucent: Boolean
    get() = surface == Color.Transparent

val ColorScheme.isDarkScheme: Boolean
    get() = ColorUtils.isColorLight(onSurface.toArgb())

/**
 * Contains functions to access the current theme values provided at the call site's position in the
 * hierarchy.
 */
object TiebaLiteTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current.colorScheme

    val extendedColorScheme: ExtendedColorScheme
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current

    val topAppBarColors: TopAppBarColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColorScheme.current.appBarColors

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
}