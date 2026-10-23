// Shared glass treatment for Miuix search capsules.
//
// One definition for every search bar that sits over scrolling content (home page entry, search
// page field): the blurred backdrop fills the capsule, the capsule's own fill goes transparent —
// the arrangement `InputField.color`'s doc prescribes for a self-drawn backdrop blur — and a
// `Highlight.GlassStroke*` edge keeps it in the same glass language as the tab-row pills.
// Callers only pass the page's content `LayerBackdrop`; null (below API 33, where
// `rememberMiuixBarBackdrop` cannot record) falls back to the flat capsule.

package com.huanchengfly.tieba.post.ui.miuix

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Capsule blur radius, in dp. Matches `BlurDefaults.BlurRadius` — the library's own default. */
val SearchBarGlassBlurRadius = BlurDefaults.BlurRadius

/** Surface blend alpha over the blur: light mode. */
private const val SearchBarGlassTintLight = 0.42f

/** Surface blend alpha over the blur: dark mode. */
private const val SearchBarGlassTintDark = 0.55f

/**
 * The two pieces a caller needs to turn a Miuix search capsule into glass:
 *
 * @property modifier apply to the [top.yukonga.miuix.kmp.basic.InputField] — draws the blurred
 *   backdrop, clipped to the capsule, with the glass edge highlight. Plain [Modifier] when no
 *   backdrop is available.
 * @property capsuleColor pass to `InputField(color = …)`: [Color.Transparent] when the glass is
 *   drawn, the flat `surfaceContainerHigh` fill otherwise.
 */
@Stable
data class SearchBarGlass(
    val modifier: Modifier,
    val capsuleColor: Color,
)

/**
 * Builds the glass treatment for a search capsule from the page's content backdrop.
 *
 * ## The backdrop must be a capture that is recorded in every state
 *
 * `textureBlur` samples the layer unconditionally. A `LayerBackdrop` whose recording node is not
 * composed — e.g. a page-level `rememberMiuixBarBackdrop()` that only its result pager applies
 * `miuixLayerBackdrop` to, before a search has been submitted — contains nothing, and blurring an
 * empty layer yields **transparent black**. On a light theme the 42% light tint is then composited
 * on top of that black: the capsule renders as a flat #616161 dark gray while the label keeps the
 * light-scheme `onSurfaceContainerHigh` (#A2A2A2), i.e. a ~2.4:1 capsule that reads as "broken
 * light mode". Bars hosted in a Scaffold `topBar` should therefore pass the Scaffold's own
 * full-screen capture (`LocalLiquidGlassBackdrop`), which is recorded on every frame and — because
 * Miuix's Scaffold lays the body out from `(0, 0)` — also covers what scrolls under the bar.
 *
 * @param backdrop the [LayerBackdrop] recording the content behind the bar — see above. Null keeps
 *   the flat capsule.
 */
@Composable
fun rememberSearchBarGlass(backdrop: LayerBackdrop?): SearchBarGlass {
    val colors = MiuixTheme.colorScheme
    if (backdrop == null) {
        return SearchBarGlass(modifier = Modifier, capsuleColor = colors.surfaceContainerHigh)
    }
    val isDark = colors.surface.luminance() < 0.5f
    // `blurColors` is itself @Composable and remembers internally — don't wrap it in `remember`.
    val glassColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(
                color = colors.surfaceContainerHigh.copy(
                    alpha = if (isDark) SearchBarGlassTintDark else SearchBarGlassTintLight,
                ),
                mode = BlurBlendMode.SrcOver,
            ),
        ),
    )
    val modifier = Modifier.textureBlur(
        backdrop = backdrop,
        shape = CircleShape,
        blurRadius = SearchBarGlassBlurRadius,
        colors = glassColors,
        highlight = if (isDark) Highlight.GlassStrokeSmallDark else Highlight.GlassStrokeSmallLight,
    )
    return SearchBarGlass(modifier = modifier, capsuleColor = Color.Transparent)
}
