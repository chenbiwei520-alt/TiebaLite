package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.miuix.SquircleShape
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Gap between tabs, matching Miuix `TabRow`'s own default. */
private val TabRowItemSpacing = 9.dp

/** Horizontal padding inside a tab pill, sized so longer CJK labels are not clipped. */
private val TabRowItemPadding = 16.dp

private val TabRowItemBorderWidth = 1.dp

/** Blur radius of a tab pill's liquid glass fill, in dp. */
private const val TabRowGlassBlurRadius = 10f

/** Opacity of the accent color blended over the blurred backdrop of the selected pill. */
private const val TabRowGlassTint = 0.45f

/** Opacity of the neutral tint blended over the blurred backdrop of an unselected pill. */
private const val TabRowGlassIdleTint = 0.10f

/**
 * White alpha of the lit edge on a glass pill, per theme.
 *
 * The values keep the visual weight of the `Highlight.GlassStrokeSmall*` rim this replaces on the
 * accent-filled selected pill.
 */
private const val TabRowGlassEdgeAlphaLight = 0.45f
private const val TabRowGlassEdgeAlphaDark = 0.12f

/**
 * Width of the edge fade, in dp.
 *
 * Roughly half a pill's height: wide enough to dissolve a clipped pill instead of shearing it off,
 * narrow enough to leave a pill that merely sits near the edge almost untouched.
 */
private val TabRowEdgeFadeWidth = 24.dp

/**
 * Fades the row out on the sides where content is really clipped.
 *
 * A [LazyRow] clips to its own bounds, so a tab that does not fit gets a hard vertical cut — which
 * reads as a rendering bug, most visibly when the row is inset to leave room for a button (the
 * forum tab strip reserves 48.dp for the sort trigger, so the last pill was sliced in half). A
 * scrim on top would only tint the pills, so the fade punches alpha out of an offscreen layer with
 * [BlendMode.DstOut]: only where the row is genuinely cropped is the content actually removed.
 *
 * Both flags come from the list's own scroll state, so a row whose content fits keeps every pill at
 * full strength, and the offscreen layer is only allocated while a fade is running.
 */
private fun Modifier.tabRowEdgeFade(fadeStart: Boolean, fadeEnd: Boolean): Modifier = this
    .graphicsLayer {
        compositingStrategy = if (fadeStart || fadeEnd) {
            CompositingStrategy.Offscreen
        } else {
            CompositingStrategy.Auto
        }
    }
    .drawWithContent {
        drawContent()
        if (!fadeStart && !fadeEnd) return@drawWithContent
        val fadeWidth = TabRowEdgeFadeWidth.toPx()
        if (fadeStart) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = 0f,
                    endX = fadeWidth,
                ),
                blendMode = BlendMode.DstOut,
            )
        }
        if (fadeEnd) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = size.width - fadeWidth,
                    endX = size.width,
                ),
                blendMode = BlendMode.DstOut,
            )
        }
    }

/**
 * The app wide HyperOS tab row: outlined pills floating over their host, with the selected tab
 * filled with the Miuix accent color.
 *
 * Every tab row in the app goes through this component so the style stays consistent, and it is
 * composed from Miuix primitives instead of Miuix's own `TabRow`, which cannot express this style:
 *
 * - it lays the tabs out at a **fixed equal width** clamped to `maxWidth` (98.dp by default), so a
 *   longer label gets an ellipsis instead of the width it needs
 * - its selected pill is placed by hand-computed offsets against the scroll offset and is not
 *   clipped to the row, so once the tabs overflow and the row scrolls the pill drifts out of place
 *
 * Sizing each pill to its content keeps long labels intact, and painting the fill on the pill
 * itself (rather than as an overlay) keeps the accent color locked to the selected tab at any
 * scroll position.
 *
 * When the host passes a [backdrop], **every** pill is filled with a Miuix **texture blur**
 * (liquid glass) instead of a flat color: the page content behind the row is blurred, a tint is
 * blended over it with [BlurBlendMode.SrcOver] — the accent color for the selected pill, a neutral
 * one for the rest — and a single 1.dp `squircleBorder` edge is stroked on top (white lit edge on
 * the selected pill, `outline` on the rest). All three layers — blur clip, fill and edge — use the
 * same [TabRowDefaults.TabRowCornerRadius] squircle, so a pill can only ever show one outline.
 * Hosts without a backdrop — and devices that cannot run the shader (below Android 13) — fall back
 * to flat fills (accent color for the selected pill, outline only for the rest), so the tab row
 * never depends on blur support.
 *
 * When the pills overflow, the cropped side(s) get an alpha fade (see [tabRowEdgeFade]) instead of
 * the raw clip edge, which otherwise slices the last pill in half right next to a neighbouring
 * button.
 *
 * @param tabs the tab labels.
 * @param selectedTabIndex index of the selected tab.
 * @param onTabSelected called with the index of the tapped tab.
 * @param modifier the [Modifier] applied to the row.
 * @param backdrop content captured behind the row, used for the selected pill's glass fill. Hosts
 *   that capture their content with `rememberLayerBackdrop` (see `rememberMiuixBarBackdrop`) should
 *   pass it here; `null` keeps the flat fill.
 * @param listState hoisted scroll state, so a host can react to the row being scrolled.
 */
@Composable
fun MiuixTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    listState: LazyListState = rememberLazyListState(),
) {
    val colors = MiuixTheme.colorScheme
    // The glass highlight has separate light/dark presets; derive it from the scheme's own surface
    val isDark = colors.surface.luminance() < 0.5f
    val glassBackdrop = backdrop?.takeIf { isRuntimeShaderSupported() }
    // 只在「这一侧真的有内容被裁掉」时才淡出；内容装得下时整条胶囊保持原样。
    val fadeStart by remember { derivedStateOf { listState.canScrollBackward } }
    val fadeEnd by remember { derivedStateOf { listState.canScrollForward } }

    // Lit edge of a glass pill. This replaces the white rim `Highlight.GlassStrokeSmall*` used to
    // paint: same white, same 1.dp band — but stroked along the pill's real squircle instead of the
    // capsule `Highlight` silently degraded to (see the note at the blur call below).
    val glassEdge = Color.White.copy(
        alpha = if (isDark) TabRowGlassEdgeAlphaDark else TabRowGlassEdgeAlphaLight,
    )

    // Keep the selected pill on screen; animateScrollToItem scrolls the minimum distance needed
    LaunchedEffect(selectedTabIndex, tabs) {
        listState.animateScrollToItem(selectedTabIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .height(TabRowDefaults.TabRowHeight)
            .tabRowEdgeFade(fadeStart = fadeStart, fadeEnd = fadeEnd),
        horizontalArrangement = Arrangement.spacedBy(TabRowItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
        overscrollEffect = null,
    ) {
        itemsIndexed(tabs) { index, title ->
            val selected = index == selectedTabIndex
            Box(
                modifier = Modifier
                    .height(TabRowDefaults.TabRowHeight)
                    .then(
                        when {
                            glassBackdrop != null -> Modifier.textureBlur(
                                backdrop = glassBackdrop,
                                // Shape-typed slot (blur clipping); matches the `squircleBorder`
                                // stroked below.
                                shape = SquircleShape(TabRowDefaults.TabRowCornerRadius),
                                blurRadius = TabRowGlassBlurRadius,
                                colors = BlurDefaults.blurColors(
                                    blendColors = listOf(
                                        BlendColorEntry(
                                            color = if (selected) {
                                                colors.primary.copy(alpha = TabRowGlassTint)
                                            } else {
                                                colors.onSurface.copy(alpha = TabRowGlassIdleTint)
                                            },
                                            mode = BlurBlendMode.SrcOver,
                                        )
                                    )
                                ),
                                // Deliberately no `highlight` argument. `Highlight`'s bloom shader
                                // takes its corner radii with `shape as? CornerBasedShape`
                                // (`setCornerRadiiUniform`, miuix-blur HighlightStyle.kt) and
                                // `SquircleShape` is a plain `Shape`, so the cast fails and the
                                // shader falls back to `minDimension / 2` — a 21.dp capsule on this
                                // 42.dp pill. It then painted a *second* outline, rounded differently
                                // from the pill's own 12.dp squircle: the double-corner ghost. The
                                // lit edge is stroked below from the same squircle instead.
                            ).then(
                                // Exactly ONE outline per pill, from the same `addSquircleRect`
                                // silhouette as the glass fill: the lit edge on the selected pill,
                                // the neutral outline on the rest. `TabRowCornerRadius` is what the
                                // fill, the blur clip and this stroke all share.
                                Modifier.squircleBorder(
                                    width = TabRowItemBorderWidth,
                                    color = if (selected) glassEdge else colors.outline,
                                    cornerRadius = TabRowDefaults.TabRowCornerRadius,
                                )
                            )

                            selected -> Modifier.squircleBackground(
                                color = colors.primary,
                                cornerRadius = TabRowDefaults.TabRowCornerRadius,
                            )

                            else -> Modifier.squircleBorder(
                                width = TabRowItemBorderWidth,
                                color = colors.outline,
                                cornerRadius = TabRowDefaults.TabRowCornerRadius,
                            )
                        }
                    )
                    // Clip the press ripple to the squircle, like Miuix's own tab items do
                    .squircleClip(TabRowDefaults.TabRowCornerRadius)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onTabSelected(index) },
                    )
                    .padding(horizontal = TabRowItemPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    color = if (selected) colors.onPrimary else colors.onBackground,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}