// Corner-radius adapter for the APIs that take a `Shape` instead of a `Modifier`.
//
// miuix-squircle ships *modifiers* (`squircleBackground` / `squircleClip` / `squircleSurface` /
// `squircleBorder`) and a `Path` builder, but no `Shape` — while a few call sites in this app are
// locked to `Shape`:
//
//   * `Modifier.textureBlur(shape = …)` — the blur node clips its texture to that shape;
//   * `Modifier.placeholder(shape = …)` — the shimmer is drawn inside that shape's outline.
//
// [SquircleShape] bridges them with `Outline.Generic` over `Path.addSquircleRect`. Below API 33 the
// squircle modifiers degrade to `RoundedCornerShape` on their own, and this shape degrades to a
// plain rounded rect of the very same radius, so a clip and a fill never disagree about which
// silhouette they are using.

package com.huanchengfly.tieba.post.ui.miuix

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.squircle.SquircleDefaults
import top.yukonga.miuix.kmp.squircle.addSquircleRect

/**
 * A rounded rectangle with miuix-squircle's continuous corners, expressed as a Compose [Shape].
 *
 * @param cornerRadius the radius applied uniformly to all four corners, in the same dp the
 *   squircle modifiers use, so a [SquircleShape] clip and a `squircleBackground` fill of the same
 *   radius line up exactly.
 * @param extension corner-tile size as a multiple of [cornerRadius]; `1.0` is a circular arc, the
 *   default `1.1` is Miuix's continuous-corner look.
 */
@Immutable
class SquircleShape(
    private val cornerRadius: Dp,
    private val extension: Float = SquircleDefaults.Extension,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val path = Path().apply {
            addSquircleRect(
                width = size.width,
                height = size.height,
                cornerRadius = radiusPx,
                extension = extension,
                squircleEnabled = isRuntimeShaderSupported(),
            )
        }
        return Outline.Generic(path)
    }
}

/**
 * Corner radii of the Material shape tokens the app still clips to.
 *
 * `TiebaLiteTheme` never overrides `MaterialTheme.shapes`, so these are Material's own defaults
 * (`extraSmall` 4 / `small` 8 / `medium` 12 / `large` 16). They exist as one place to convert those
 * `clip(MaterialTheme.shapes.*)` call sites to `squircleClip` without changing the radius each site
 * currently renders — the value is what changes shape (circular arc → continuous corner), never the
 * size or the visual hierarchy.
 */
object SquircleCornerRadius {

    /** `MaterialTheme.shapes.extraSmall` — image chips and small thumbnails. */
    val ExtraSmall = 4.dp

    /** `MaterialTheme.shapes.small` — post bodies, quotes, quoted titles, search bars. */
    val Small = 8.dp

    /** `MaterialTheme.shapes.medium` — large media frames. */
    val Medium = 12.dp

    /** `MaterialTheme.shapes.large` — controls taller than a row, e.g. the vertical slider. */
    val Large = 16.dp
}
