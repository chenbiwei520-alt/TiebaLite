package com.huanchengfly.tieba.post.ui.miuix

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_3BUTTON
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_DEFAULT
import com.huanchengfly.tieba.post.utils.DisplayUtil.gestureType
import com.huanchengfly.tieba.post.utils.ThemeUtil
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.basic.TopAppBarState as MiuixTopAppBarState
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS style progressive-blur top bar support.
 *
 * Returns a [LayerBackdrop] that captures the page content for the blur pass, or null when the
 * device cannot render shader-backed blur (RuntimeShader requires Android 13 / API 33). Callers
 * must keep a readable opaque bar when null is returned.
 */
@Composable
fun rememberMiuixBarBackdrop(): LayerBackdrop? {
    val supported = isRuntimeShaderSupported()
    val surfaceColor = MiuixTheme.colorScheme.surface
    return if (supported) {
        rememberLayerBackdrop {
            // LayerBackdrop captures the wrapped node only; draw an opaque surface first so the
            // blur does not sample into transparent areas.
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }
}

/**
 * The single Miuix scroll behavior the app's collapsing top bars are driven by.
 *
 * Exit-until-collapsed semantics: the bar scrolls away as the content moves up and comes back
 * once the content returns to its top. This replaces Material3's
 * `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()` so that a page never mixes two scroll
 * pipelines — Miuix's `ScrollBehavior` is the only one attached to the list.
 *
 * `initialHeightOffsetLimit = 0f` keeps [MiuixTopAppBarState.collapsedFraction] at 0 until the
 * bar has actually been measured and the limit set from its height (see the bar layouts in
 * `TopAppBar.kt`); without it the fraction starts at 1 and the bar would render pre-collapsed.
 */
@Composable
fun rememberMiuixExitUntilCollapsedScrollBehavior(): ScrollBehavior =
    MiuixScrollBehavior(
        state = rememberMiuixTopAppBarState(initialHeightOffsetLimit = 0f),
    )

/**
 * The Miuix counterpart of Material3's `TopAppBarDefaults.pinnedScrollBehavior()`: the bar stays
 * where it is and the content scrolls underneath it.
 *
 * Miuix has no dedicated pinned factory, so this is expressed through `canScroll = { false }` —
 * the behavior stops translating the bar's height offset, which is exactly what "pinned" means.
 */
@Composable
fun rememberMiuixPinnedScrollBehavior(): ScrollBehavior =
    MiuixScrollBehavior(
        state = rememberMiuixTopAppBarState(initialHeightOffsetLimit = 0f),
        canScroll = { false },
    )

/**
 * The Miuix counterpart of Material3's `TopAppBarDefaults.enterAlwaysScrollBehavior()`: the bar
 * scrolls away with the content and comes back as soon as the content moves the other way.
 *
 * Miuix's own behavior is exit-until-collapsed (the bar only returns once the content reaches its
 * top), so this reuses that animation pipeline with `canScroll = { true }`; it is the closest
 * native equivalent and, critically, keeps a single scroll pipeline per page.
 */
@Composable
fun rememberMiuixEnterAlwaysScrollBehavior(): ScrollBehavior =
    MiuixScrollBehavior(
        state = rememberMiuixTopAppBarState(initialHeightOffsetLimit = 0f),
        canScroll = { true },
    )

/**
 * Captures this node's drawing into [backdrop] for the bar blur; no-op when backdrop is null.
 *
 * Also publishes [backdrop] as the active recording layer for the subtree that uses this modifier,
 * so a nested [miuixLayerBackdrop] with the *same* backdrop can detect the re-entrancy and skip
 * itself. `LayerBackdrop.graphicsLayer.record()` is not reentrant: nesting two recordings of one
 * layer throws `IllegalStateException: Recording currently in progress` from
 * `RenderNode.beginRecording`, which crashes the page on its first draw.
 */
@Composable
fun Modifier.miuixLayerBackdrop(backdrop: LayerBackdrop?): Modifier {
    if (backdrop == null) return this
    // A container above us (e.g. `MyScaffold`) may already be recording this exact backdrop;
    // capturing it again on a descendant nests two `record()` calls on one graphics layer.
    if (LocalRecordingBackdrop.current === backdrop) return this
    return this.layerBackdrop(backdrop)
}

/**
 * The [LayerBackdrop] a surrounding container is already recording, or null. See
 * [miuixLayerBackdrop] for why this must be tracked.
 */
private val LocalRecordingBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Wraps [content] so that any [miuixLayerBackdrop] inside it sees [backdrop] as already recording.
 *
 * Use this around a container that records [backdrop] itself (like `MyScaffold`'s content box):
 * descendants that try to capture the same backdrop become no-ops instead of crashing.
 */
@Composable
fun ProvideRecordingBackdrop(
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRecordingBackdrop provides backdrop, content = content)
}

/**
 * How far the list has to scroll (in dp) before the blur reaches full strength when no explicit
 * [MiuixProgressiveBlurBar.blurProgress] is supplied.
 *
 * 48.dp matches the ramp Miuix's own `TopAppBarLayout` uses, so a bar hosting this composable
 * without a custom curve fades in at the platform's rate.
 */
private val BlurFadeInDistance = 48.dp

/**
 * Height of the blurred strip measured from the top of the bar.
 *
 * ## Why the blur layer must NOT be as tall as the bar
 *
 * `ProgressiveBlur` describes its ramp in **normalised layer coordinates**: `startFraction` and
 * `endFraction` are fractions of the layer's own extent along `angle`
 * (`ProgressiveBlur.Top` = `angle 90°, startFraction 0f, endFraction 1f`). The blur is therefore at
 * full strength on the layer's **first row of pixels** and decays to zero on its **last** one.
 *
 * Decompiling `createProgressiveBlurEffect` in `miuix-blur` confirms the mapping — for
 * `angle = 90°` the gradient axis is `(0, 1)`, so the shader's ramp position is
 * `p = dot(xy, (0,1)) = xy.y`, and the band is
 *
 * ```
 * in_gradBand = ( (a1 + startFraction * extent + proj) / exp ,
 *                 (a1 + endFraction   * extent + proj) / exp )
 * ```
 *
 * with `extent = layerHeight`. The band is **proportional to the layer's height**, so a layer that
 * is 180.dp tall spends its whole ramp across those 180.dp, while a layer that is 64.dp tall spends
 * it across 64.dp.
 *
 * Handing the composable a bar-sized layer (the previous `Modifier.matchParentSize()` behaviour)
 * therefore broke in both directions:
 *
 *  - **`CollapsingTopAppBar` (topic page, `expandedHeight = 180.dp`)**: the ramp was spread over
 *    180.dp of *expanded header* — but the header is an image that is painted over the blur, so by
 *    the time the bar collapses to its 64.dp strip the entire visible strip sits in the band's top
 *    fraction, i.e. `raw ≈ 0` and `intensity ≈ 1`. The blur became a flat full-strength sheet with
 *    no visible gradient, and the page looked like it had no progressive blur at all.
 *  - **`TopAppBarPaged` (explore/dynamic page, a short static bar)**: the layer was only as tall as
 *    the bar, so the ramp was squeezed into ~64.dp. Combined with the shader's
 *    `clamp(xy ± offset, 0.5, in_maxCoord)` sample clamp — where `in_maxCoord = layerSize / exp`
 *    and `exp` is the downsample factor — samples near the band's clear end were clamped to the
 *    last texel row, which is why the blur terminated on a hard horizontal seam instead of fading
 *    out.
 *
 * The fix is to stop deriving the ramp from the bar's height and pin it to a **fixed strip** at the
 * top of the bar instead. The blur layer keeps the bar's full width, but its height is clamped to
 * this value, so:
 *
 *  - the ramp always spans the same physical distance regardless of how tall the bar currently is,
 *    which is what makes the effect identical on static and collapsing pages;
 *  - the clear end always lands inside the layer, so the shader's sample clamp never kicks in and
 *    no boundary line can appear;
 *  - scrolling / collapsing / re-measuring only changes the *alpha* of the layer, never its
 *    geometry, so nothing can shimmer or jump while the list moves.
 *
 * 64.dp matches `MiuixTopBarRowHeight` in `TopAppBar.kt`, i.e. exactly the height of a resting
 * Miuix top app bar, so the ramp covers the bar and reaches clear right at its bottom edge.
 */
private val BlurBandHeight = 64.dp

/**
 * A top bar container with HyperOS progressive blur: the blur strength ramps from full at the
 * status bar edge to clear at the bar's bottom edge, fading in as content scrolls underneath.
 * The wrapped content should use a transparent background while [backdrop] is non-null.
 *
 * @param backdrop the captured page content, or null when the device cannot render the blur.
 * @param scrollBehavior Miuix scroll behavior; the blur fades in with the bar's scroll offset.
 * @param blurProgress explicit blur strength in `0f..1f`, taking precedence over [scrollBehavior].
 *   Use it for hosts whose fade curve differs from the `contentOffset` ramp, e.g. a bar whose
 *   `collapsedFraction` is already remapped through its own curve.
 * @param bandHeight how tall the blurred strip is, measured down from the top of the bar. The ramp
 *   is at full strength on the strip's first row and fully clear on its last one. Leave it at the
 *   default for a standard bar; raise it for a bar whose resting height is taller than
 *   [BlurBandHeight] so the ramp still reaches clear at the bar's bottom edge. See
 *   [BlurBandHeight] for why this is a fixed strip rather than the bar's own height.
 * @param content the bar content, drawn on top of the blur layer.
 */
@Composable
fun MiuixProgressiveBlurBar(
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior? = null,
    blurProgress: (() -> Float)? = null,
    bandHeight: Dp = BlurBandHeight,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier) {
        if (backdrop != null) {
            val fadeInDistance = with(LocalDensity.current) { BlurFadeInDistance.toPx() }
            // The blur layer is a FIXED-SIZE strip anchored to the top of the host box, not a
            // bar-sized sheet. See `BlurBandHeight` for the full reasoning; in short:
            //
            //   * `ProgressiveBlur` expresses its ramp as fractions of the layer's own height, so a
            //     bar-sized layer ties the visible gradient to the bar's current height. That makes a
            //     collapsing bar (180.dp expanded) show a flat saturated sheet when collapsed, and it
            //     squeezes a short static bar's ramp so the shader's sample clamp turns the clear end
            //     into a hard seam.
            //   * Pinning the height keeps the ramp identical on every page, keeps the clear end
            //     inside the layer, and makes the layer's *geometry* constant — so scroll, collapse
            //     and re-measure only ever animate `alpha`.
            //
            // Drawing it as a sibling of `content()` rather than as a wrapper keeps the
            // `progressiveTextureBlur` node's geometry split from the bar's re-measurement: the blur
            // node only needs to re-resolve when the page content behind it moves, not when the bar
            // height changes on every scroll frame.
            //
            // `graphicsLayer` sits OUTSIDE `progressiveTextureBlur` on purpose — the layer is the
            // cheaper of the two, so updating alpha invalidates only the GraphicsLayer compositing
            // step while the (expensive) RuntimeShader output stays cached by the draw node.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bandHeight)
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        val progress = blurProgress?.invoke()
                            ?: scrollBehavior?.state
                                ?.let { (-it.contentOffset / fadeInDistance).coerceIn(0f, 1f) }
                            ?: 1f
                        alpha = progress.coerceIn(0f, 1f)
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

/**
 * Bottom navigation bar placeholder with HyperOS progressive blur — the counterpart of
 * [MiuixProgressiveBlurBar] for the bottom edge. The blur is strongest at the system
 * navigation-bar edge and fades upward over [backdrop].
 *
 * Gesture semantics carried over from the removed haze `BlurNavigationBarPlaceHolder`:
 *  - full-screen gesture navigation: a transparent spacer of the system-bar height (the content
 *    scrolls behind it and the blur lives in the floating bar above)
 *  - 3-button navigation: blurred + tinted backdrop for the button row
 *  - hardware keys: nothing to draw
 *
 * @param backdrop the captured page content, or null when the device cannot render shader-backed
 *   blur; then the tint renders alone, so the bar stays readable.
 */
@Composable
fun MiuixBlurNavigationBarPlaceHolder(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val navBarInsets = WindowInsets.navigationBars
    when (navBarInsets.gestureType(LocalDensity.current)) {
        GESTURE_DEFAULT ->
            Spacer(modifier = modifier.windowInsetsBottomHeight(navBarInsets))

        GESTURE_3BUTTON -> {
            val trackedColorScheme by ThemeUtil.colorState
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(navBarInsets)
            ) {
                if (backdrop != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .progressiveTextureBlur(
                                backdrop = backdrop,
                                shape = RectangleShape,
                                gradient = ProgressiveBlur.Bottom.copy(curve = 2.2f),
                                blurRadius = 10f,
                                colors = BlurDefaults.blurColors(
                                    blendColors = listOf(
                                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.3f)),
                                    ),
                                ),
                            )
                    )
                }
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color = trackedColorScheme.navigationContainer)
                )
            }
        }

        else -> Unit
    }
}
