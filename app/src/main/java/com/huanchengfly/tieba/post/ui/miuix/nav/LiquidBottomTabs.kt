// HyperOS-style **liquid glass** bottom bar: a sliding capsule that refracts what is behind it.
//
// Port of Kyant0/AndroidLiquidGlass (Apache-2.0) `LiquidBottomTabs.kt`, running on the upstream's
// own backdrop library (`io.github.kyant0:backdrop`) — which brings the one capability miuix-blur
// lacks: `rememberCombinedBackdrop` multi-layer capture. The capsule samples the page AND an
// invisible, accent-tinted copy of the tab row recorded into a second backdrop, so the glass shows
// a *refracted copy of the icons* — the signature upstream look an earlier revision of this port
// documented as a deliberate deviation.
//
// Three Kyant `drawBackdrop` surfaces, in composition order:
//  1. **slab + tabs** — vibrancy + blur(8.dp) + a constant 24.dp edge lens; the tab row is real,
//     clickable content, and the slab zooms slightly with the press progress (upstream's layer block);
//  2. **capture layer** — the same tab row re-rendered inside its own glass copy, tinted accent and
//     alpha-0 on screen. The `alpha` sits OUTSIDE `layerBackdrop`, so the node composites to nothing
//     while the recording itself stays opaque and readable by the sampler;
//  3. **capsule** — `drawBackdrop(rememberCombinedBackdrop(page, capture))` with the press-driven
//     lens (10.dp/14.dp + chromatic dispersion) and highlight / shadow / inner shadow.
//
// Motion is upstream's `DampedDragAnimation` + `InteractiveHighlight`, ported next to this file
// with their spring constants untouched (the highlight still renders through miuix's AGSL
// RuntimeShader backend — same shader source, no behavioural difference).
//
// Below API 33 (no RuntimeShader) the host passes a `null` backdrop and the bar keeps its flat
// theme surface: the Kyant layers are never created and the geometry stays identical.

package com.huanchengfly.tieba.post.ui.miuix.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Overall height of the glass bar, including its inset. */
private val LiquidBarHeight = 64.dp

/** Height of the sliding capsule — 64.dp bar minus two 4.dp insets, as upstream. */
private val LiquidPillHeight = 56.dp

/** Inset between the bar's outer edge and both the items and the capsule. */
private val LiquidBarInset = 4.dp

/**
 * Horizontal margin that lifts the bar off the screen edges, so it floats as a capsule instead of
 * being a docked strip. The bar's own size, the tab width and the capsule geometry all derive from
 * the width left after this margin.
 */
private val LiquidBarFloatingMargin = 8.dp

/**
 * Elevation of the flat fallback bar's drop shadow. The glass capsule uses the backdrop's own
 * press-driven `Shadow` instead.
 */
private val LiquidBarShadow = 6.dp

/** How far the whole panel drifts while dragging, at full drag fraction. */
private val LiquidBarParallax = 4.dp

/** Scale the capsule animates to while pressed: the reference's `78f / 56f`. */
private const val LiquidPressedScale = 78f / 56f

/** Opacity of the container colour laid over the blur — the reference's `0.4f`. */
private const val LiquidContainerAlpha = 0.4f

/**
 * Opacity of the capsule's rest tint. The reference lays `Black 10%` (light) / `White 10%` (dark)
 * on the capsule and fades it out as the press builds, so the capsule is a *visible* slab of glass
 * at rest and turns into a pure lens while dragging.
 */
private const val LiquidPillRestTint = 0.1f

/** Blur radius of the slab's and the capture layer's fill, in dp — the reference's `blur(8f.dp)`. */
private const val LiquidBarBlurRadius = 8f

/** Constant edge lens on the visible slab and the press-driven lens on the capture layer, in dp. */
private val LiquidSlabLensRadius = 24.dp

/**
 * Press-driven lens reach on the capsule: how far the bend travels inward from the capsule's edge,
 * at full press — the reference scales the same 10dp/14dp pair by `pressProgress`.
 */
private val LiquidLensRefractionHeight = 10.dp
private val LiquidLensRefractionAmount = 14.dp

/** Press-driven inner shadow radius on the capsule, in dp. */
private val LiquidInnerShadowRadius = 8.dp

/** Icon size inside a tab. */
private val LiquidIconSize = 24.dp

/** Hairline edge stroke width of the bar and capsule, in dp. */
private val LiquidEdgeWidth = 1.dp

/**
 * One entry of the bar.
 *
 * @param icon the tab's icon; the selected tab is tinted with the theme's accent colour.
 * @param label the tab's label, always shown under the icon.
 * @param contentDescription accessibility label; falls back to [label].
 * @param badge optional overlay drawn at the icon's top-end, e.g. miuix's `Badge`.
 */
@Immutable
data class LiquidBottomTabItem(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String? = null,
    val badge: (@Composable () -> Unit)? = null,
)

/**
 * Geometry the host layout must mirror so page content is never hidden behind the floating bar
 * (`MainPage.bottomNavigationPlaceholder` reserves [barHeight] + [bottomLift]).
 */
object LiquidBottomTabsDefaults {

    /** Overall height of the glass bar, including its inset. */
    val barHeight = LiquidBarHeight

    /**
     * Horizontal share of the screen the bar occupies per tab — NexioSchedule's floating-bar rule
     * (`0.42f` for two tabs, `0.63f` for three), generalised to `0.21f * tab count`. Keeps the bar
     * a centred floating pill instead of a full-width dock.
     */
    const val WidthFractionPerTab = 0.21f

    /** Minimum distance between the floating bar and the screen's bottom edge. */
    val BottomLiftMin = 24.dp

    /** Extra distance added on top of the navigation-bar inset. */
    val BottomLiftExtra = 8.dp

    /**
     * Distance between the floating bar and the screen's bottom edge: at least [BottomLiftMin],
     * otherwise the navigation-bar inset plus [BottomLiftExtra] — NexioSchedule's rule.
     */
    fun bottomLift(navBarBottomInset: Dp): Dp =
        maxOf(BottomLiftMin, navBarBottomInset + BottomLiftExtra)
}

/**
 * Liquid glass bottom bar with a draggable capsule indicator.
 *
 * The capsule can be dragged across the bar; on release it springs to the nearest tab and reports it
 * through [onSelect]. Tapping a tab animates the capsule to it, and a change of [selectedIndex] from
 * outside (back navigation, deep link) animates it just the same — the capsule position is the single
 * source of truth for both directions, exactly like the reference.
 *
 * ## Interaction contract
 *
 * | state | trigger | expected visuals |
 * |---|---|---|
 * | **rest** | nothing pressed | Glass slab (vibrancy + blur + 40% container tint + 1.dp themed edge) floating `8.dp` off the edges; the capsule sits on the selected tab as a 10%-darkened slab of glass in which the page *and the accent-tinted icons* are refracted. No press shaders run. |
 * | **press** | finger down on the capsule, before it moves | `pressProgress` → 1: the capsule scales to `78/56`, the slab and the captured icon copy zoom slightly, the capsule's rest tint fades out while its highlight / shadow / inner shadow fade in. |
 * | **drag** | dragging the capsule past the touch slop | Capsule follows the finger, the whole panel drifts up to `4.dp` the other way (eased parallax), and the capsule squashes/stretches with drag velocity. The **lens** bends the combined sample (page + captured icons) by up to `14.dp` near the capsule's edge, with chromatic dispersion splitting the edge into a rainbow fringe. The touch highlight lights up under the finger. |
 * | **release** | finger up | The capsule springs to the nearest tab (and the host is told through `onSelect`), the lens and the press scale ease back to zero, the panel parallax springs home. |
 * | **tab tap** | tap a tab | The capsule springs to it and `onSelect` fires. Tapping the already-selected tab fires `onReSelect` instead (scroll-to-top behaviour). |
 * | **no shader** (below API 33) | — | Same layout and motion, flat fills, no lens or refraction. |
 *
 * @param items the tabs, left to right.
 * @param selectedIndex index of the selected tab; out-of-range values are clamped.
 * @param onSelect called with the index a drag or tap settled on.
 * @param modifier applied to the bar.
 * @param onReSelect called when the already-selected tab is tapped again (scroll-to-top behaviour).
 * @param backdrop the page capture the glass samples. Must be a capture that is recorded in every
 *   state and covers the bar — the host scaffold's own full-screen capture is the right one. A
 *   `null` backdrop (below API 33, or no capture) keeps the flat theme surface.
 *
 * The bar itself fills whatever width its [modifier] gives it and handles no window insets: hosts
 * apply the NexioSchedule floating placement — a centred width cap
 * ([LiquidBottomTabsDefaults.WidthFractionPerTab] per tab) and a bottom lift
 * ([LiquidBottomTabsDefaults.bottomLift]) — see `MiuixFloatingNavigationSuite`.
 */
@Composable
fun LiquidBottomTabs(
    items: List<LiquidBottomTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onReSelect: ((Int) -> Unit)? = null,
    backdrop: Backdrop? = null,
) {
    if (items.isEmpty()) return

    val colors = MiuixTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f
    val accentColor = colors.primary
    val idleColor = colors.onSurfaceContainerVariant
    val containerColor = colors.surfaceContainer.copy(alpha = LiquidContainerAlpha)
    val pillRestTint = if (isDark) Color.White else Color.Black
    val outlineColor = colors.outline.copy(alpha = if (isDark) 0.35f else 0.6f)
    val capsuleShape: Shape = remember { ContinuousCapsule() }
    val labelStyle = MiuixTheme.textStyles.body2

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val selectedIndexClamped = selectedIndex.coerceIn(0, items.lastIndex)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LiquidBarFloatingMargin),
        contentAlignment = Alignment.CenterStart,
    ) {
        val itemCount = items.size
        val insetPx = with(density) { LiquidBarInset.toPx() }
        val barWidthPx = constraints.maxWidth.toFloat()
        val tabWidthPx = ((barWidthPx - insetPx * 2f) / itemCount).coerceAtLeast(1f)
        val tabWidth = with(density) { tabWidthPx.toDp() }

        // Rubber-band parallax: while the capsule is dragged the whole panel drifts a few dp the other
        // way, eased out, then springs back. Reads as the bar "giving" under the finger.
        val parallax = remember { Animatable(0f) }
        val panelOffset by remember(barWidthPx, density) {
            derivedStateOf {
                val fraction = (parallax.value / barWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    LiquidBarParallax.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        // The capsule's position, in tab units, is the ONLY source of truth: it is animated towards
        // whatever the selection is, and a drag reports the tab it settled on. There is deliberately
        // no mirrored index state — a second copy of "where the capsule is" can disagree with the
        // animation, and a capsule that has drifted off its tab is exactly what that looks like.
        val damped = remember(scope, itemCount) {
            DampedDragAnimation(
                animationScope = scope,
                initialValue = selectedIndexClamped.toFloat(),
                valueRange = 0f..(itemCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = LiquidPressedScale,
                onDragStarted = {},
                onDragStopped = {
                    val target = targetValue.roundToInt().coerceIn(0, itemCount - 1)
                    animateToValue(target.toFloat())
                    onSelect(target)
                    scope.launch { parallax.animateTo(0f, spring(1f, 300f, 0.5f)) }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * (if (isLtr) 1f else -1f))
                            .coerceIn(0f, (itemCount - 1).toFloat())
                    )
                    scope.launch { parallax.snapTo(parallax.value + dragAmount.x) }
                },
            )
        }
        // Follows the selection, whether it changed because of a tap, a re-selection from elsewhere in
        // the app, or back navigation.
        LaunchedEffect(selectedIndexClamped, damped) {
            damped.animateToValue(selectedIndexClamped.toFloat())
        }

        // Touch highlight: a soft radial add-blend that follows the capsule (not the finger — the
        // capsule is what the finger moves) and only lights up while pressed.
        val highlight = remember(scope) {
            InteractiveHighlight(scope) { size, _ ->
                Offset(
                    x = if (isLtr) {
                        (damped.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        size.width - (damped.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    y = size.height / 2f,
                )
            }
        }

        val pillTranslationX = {
            if (isLtr) {
                damped.value * tabWidthPx + panelOffset
            } else {
                barWidthPx - (damped.value + 1f) * tabWidthPx - insetPx * 2f + panelOffset
            }
        }

        val onTabClick: (Int) -> Unit = { index ->
            if (index == selectedIndexClamped) {
                onReSelect?.invoke(index)
            } else {
                scope.launch { damped.animateToValue(index.toFloat()) }
                onSelect(index)
            }
        }

        if (backdrop == null) {
            // ── Flat fallback (below API 33): no RuntimeShader, so no backdrop layers exist. Same
            //    geometry and colours; the slab is a plain themed capsule with the app's hairline
            //    edge, the capsule its 10%-tinted slab. ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = LiquidBarInset)
                    // No backdrop sampling here, so the node itself carries the pressed scale and the
                    // drag-velocity squash — the same 78/56 motion the glass path applies in its
                    // layer block.
                    .graphicsLayer {
                        translationX = pillTranslationX()
                        val velocity = damped.velocity / 10f
                        scaleX = damped.scaleX / (1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f))
                        scaleY = damped.scaleY * (1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f))
                    }
                    .background(color = containerColor, shape = capsuleShape)
                    .squircleBorder(
                        width = LiquidEdgeWidth,
                        color = outlineColor,
                        cornerRadius = LiquidPillHeight / 2,
                    )
                    .height(LiquidPillHeight)
                    .fillMaxWidth(1f / itemCount),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawRect(
                                color = pillRestTint,
                                alpha = LiquidPillRestTint * (1f - damped.pressProgress),
                            )
                        },
                )
            }

            Row(
                Modifier
                    .graphicsLayer { translationX = panelOffset }
                    .shadow(elevation = LiquidBarShadow, shape = capsuleShape)
                    .background(color = containerColor, shape = capsuleShape)
                    .squircleBorder(
                        width = LiquidEdgeWidth,
                        color = outlineColor,
                        cornerRadius = LiquidBarHeight / 2,
                    )
                    .height(LiquidBarHeight)
                    .fillMaxWidth()
                    .padding(LiquidBarInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    LiquidTab(
                        item = item,
                        selected = index == selectedIndexClamped,
                        accentColor = accentColor,
                        idleColor = idleColor,
                        labelStyle = labelStyle,
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight(),
                        onClick = { onTabClick(index) },
                    )
                }
            }
        } else {
            // ── Upstream glass path: three drawBackdrop surfaces over the Kyant backdrop. ─────────
            val tabsBackdrop = rememberLayerBackdrop()
            val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

            // 1/3 — the visible slab with the real, clickable tab row. The lens bends the page near
            // the slab's edge (the reference runs it constantly), and the whole slab zooms slightly
            // with the press progress so the glass "lifts" toward the finger.
            Row(
                Modifier
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule() },
                        effects = {
                            vibrancy()
                            blur(LiquidBarBlurRadius.dp.toPx())
                            lens(LiquidSlabLensRadius.toPx(), LiquidSlabLensRadius.toPx())
                        },
                        layerBlock = {
                            val progress = damped.pressProgress
                            val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .squircleBorder(
                        width = LiquidEdgeWidth,
                        color = outlineColor,
                        cornerRadius = LiquidBarHeight / 2,
                    )
                    .height(LiquidBarHeight)
                    .fillMaxWidth()
                    .padding(LiquidBarInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    LiquidTab(
                        item = item,
                        selected = index == selectedIndexClamped,
                        accentColor = accentColor,
                        idleColor = idleColor,
                        labelStyle = labelStyle,
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight(),
                        onClick = { onTabClick(index) },
                    )
                }
            }

            // 2/3 — the capture layer: the same tab row re-rendered inside its own glass copy,
            // tinted accent and alpha-0 on screen. `alpha` is OUTSIDE `layerBackdrop` on purpose:
            // the recording stays opaque (the sampler must see the icons) while the node composites
            // to nothing. The identical tab layout keeps the copy pixel-aligned with the real row,
            // so the capsule refracts each icon exactly where the real one sits.
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule() },
                        effects = {
                            val progress = damped.pressProgress
                            vibrancy()
                            blur(LiquidBarBlurRadius.dp.toPx())
                            lens(LiquidSlabLensRadius.toPx() * progress, LiquidSlabLensRadius.toPx() * progress)
                        },
                        highlight = { Highlight.Default.copy(alpha = damped.pressProgress) },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .height(LiquidPillHeight)
                    .fillMaxWidth()
                    .padding(horizontal = LiquidBarInset)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    LiquidTab(
                        item = item,
                        selected = index == selectedIndexClamped,
                        accentColor = accentColor,
                        idleColor = idleColor,
                        labelStyle = labelStyle,
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight(),
                        // An invisible clickable copy would steal every tap from the real row below.
                        interactive = false,
                    )
                }
            }

            // 3/3 — the capsule, drawn OVER the real icons (upstream order): what it shows is its
            // combined backdrop sample — the page with the accent-tinted icon copy refracted by the
            // press-driven lens — while its rest tint fades out as the press builds.
            //
            // Geometry: `padding` (not `offset`) applies the 4.dp inset *before*
            // `fillMaxWidth(1f/n)`, so the capsule is `(barWidth - 8.dp) / n` wide — exactly one
            // tab. An offset instead leaves it `barWidth / n` wide, which drifts further off with
            // every tab to the right.
            Box(
                modifier = Modifier
                    .padding(horizontal = LiquidBarInset)
                    // Only the travelled position lives on the node. The press scale is a *layer
                    // block* (below), never an outer graphicsLayer: the library inverse-transforms
                    // the sampled layers with that same block, so the glass grows while the refracted
                    // content stays screen-aligned. Scaling the node instead magnifies the refracted
                    // icons and drags them off the tabs they belong to.
                    .graphicsLayer { translationX = pillTranslationX() }
                    .drawBackdrop(
                        backdrop = combinedBackdrop,
                        shape = { ContinuousCapsule() },
                        effects = {
                            val progress = damped.pressProgress
                            lens(
                                LiquidLensRefractionHeight.toPx() * progress,
                                LiquidLensRefractionAmount.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = damped.pressProgress) },
                        shadow = { Shadow(alpha = damped.pressProgress) },
                        innerShadow = {
                            val progress = damped.pressProgress
                            InnerShadow(radius = LiquidInnerShadowRadius * progress, alpha = progress)
                        },
                        // Pressed scale (78/56), plus the reference's velocity squash: dragged fast
                        // the capsule stretches sideways and flattens, so it looks like it trails the
                        // finger. Applying it here — not on the node — is what keeps the refracted
                        // copy locked to the tab underneath, because the backdrop draw compensates
                        // the sample with the inverse of this block (see `InverseLayerScope`).
                        layerBlock = {
                            scaleX = damped.scaleX
                            scaleY = damped.scaleY
                            val velocity = damped.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = damped.pressProgress
                            // `pillRestTint` is an opaque Black/White; the 10% lives in the alpha
                            // argument. Passing the colour alone paints a solid black capsule at rest.
                            drawRect(pillRestTint, alpha = LiquidPillRestTint * (1f - progress))
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        },
                    )
                    .height(LiquidPillHeight)
                    .fillMaxWidth(1f / itemCount),
            )

            // ── The input overlay — composed LAST so it is the topmost node for the pointer. It
            //    carries the drag and the touch highlight; it draws nothing.
            //
            //    Why the split: `zIndex` (used before) reorders a node for *hit testing* as well as
            //    for drawing, so putting the tab row on top of the capsule also handed it every
            //    pointer event — the capsule could not be dragged at all. A plain tap still lands on
            //    the tab underneath, because `detectDragGestures` only starts consuming after the
            //    touch slop. ─────────────────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = LiquidBarInset)
                    .graphicsLayer { translationX = pillTranslationX() }
                    .then(highlight.gestureModifier)
                    .then(damped.modifier)
                    .height(LiquidPillHeight)
                    .fillMaxWidth(1f / itemCount),
            )
        }
    }
}

/**
 * One tab: icon (+ optional badge) over its label, tinted by selection.
 *
 * @param interactive whether this row handles the tap itself. The invisible capture layer renders
 *   the same row with `interactive = false` on purpose: an invisible *but clickable* copy sits above
 *   the real row in the z order, so it would swallow every tap — taking the tab's indication with
 *   it. With no pointer input the tap falls through to the real row, which also owns the semantics.
 */
@Composable
private fun LiquidTab(
    item: LiquidBottomTabItem,
    selected: Boolean,
    accentColor: Color,
    idleColor: Color,
    labelStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onClick: () -> Unit = {},
) {
    val contentColor = if (selected) accentColor else idleColor
    Column(
        modifier = if (interactive) {
            modifier.selectable(
                selected = selected,
                // No indication: the sliding glass capsule *is* the selection indicator. The default
                // ripple paints a rectangle over the tab's bounds — the square box showing through
                // (and around) the glass on every tap. Tabs are still announced with Role.Tab and
                // the selected state, only the pressed visual is gone.
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
        } else {
            modifier
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        // The item is as tall as the bar, so the icon+label pair is centred in it rather than parked
        // against the top edge — the bar's own `CenterVertically` only aligns the items, not their
        // contents.
        verticalArrangement = Arrangement.Center,
    ) {
        LiquidTabIcon(item = item, color = contentColor)
        Text(
            text = item.label,
            color = contentColor,
            style = labelStyle,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Icon plus optional badge, centred in whatever box it is given. Shared by the bar and the capsule. */
@Composable
private fun LiquidTabIcon(
    item: LiquidBottomTabItem,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.contentDescription ?: item.label,
            tint = color,
            modifier = Modifier.size(LiquidIconSize),
        )
        item.badge?.let { badge ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-4).dp),
            ) {
                badge()
            }
        }
    }
}
