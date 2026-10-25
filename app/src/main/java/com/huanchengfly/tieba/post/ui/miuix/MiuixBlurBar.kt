package com.huanchengfly.tieba.post.ui.miuix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_3BUTTON
import com.huanchengfly.tieba.post.utils.DisplayUtil.GESTURE_DEFAULT
import com.huanchengfly.tieba.post.utils.DisplayUtil.gestureType
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixTopBarRowHeight
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState
import top.yukonga.miuix.kmp.basic.TopAppBarState as MiuixTopAppBarState
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
 * device cannot render shader-backed blur: RuntimeShader requires Android 13 / API 33, and even
 * on API 33+ some Adreno Vulkan drivers silently fail to compile the AGSL pipeline (the blur
 * draw is dropped mid-session — see [ShaderBlurProbe]). Callers must keep a readable opaque bar
 * when null is returned.
 */
@Composable
fun rememberMiuixBarBackdrop(): LayerBackdrop? {
    val context = LocalContext.current
    ShaderBlurProbe.warmUp(context.applicationContext)
    val supported = isRuntimeShaderSupported() && ShaderBlurProbe.renderable
    val surfaceColor = MiuixTheme.colorScheme.surface
    return if (supported) {
        rememberLayerBackdrop {
            // layerBackdrop only captures the wrapped node's own drawing; the opaque surface
            // base keeps the blur from sampling into transparent areas (guide: Background Color).
            drawRect(surfaceColor)
            drawContent()
        }
    } else {
        null
    }
}

/**
 * The Miuix scroll behavior the app's collapsing top bars are driven by.
 *
 * Exit-until-collapsed semantics: the bar scrolls away as the content moves up and comes back
 * once the content returns to its top.
 *
 * `initialHeightOffsetLimit = 0f` keeps [MiuixTopAppBarState.collapsedFraction] at 0 until the
 * bar has actually been measured and the limit set from its height.
 */
@Composable
fun rememberMiuixExitUntilCollapsedScrollBehavior(): ScrollBehavior =
    MiuixScrollBehavior(
        state = rememberMiuixTopAppBarState(initialHeightOffsetLimit = 0f),
    )

/**
 * The Miuix counterpart of `TopAppBarDefaults.pinnedScrollBehavior()`: the bar stays
 * where it is and the content scrolls underneath it.
 *
 * Miuix has no dedicated pinned factory, so this is expressed through `canScroll = { false }`.
 */
@Composable
fun rememberMiuixPinnedScrollBehavior(): ScrollBehavior =
    MiuixScrollBehavior(
        state = rememberMiuixTopAppBarState(initialHeightOffsetLimit = 0f),
        canScroll = { false },
    )

/**
 * The Miuix counterpart of `TopAppBarDefaults.enterAlwaysScrollBehavior()`: the bar
 * scrolls away with the content and comes back as soon as the content moves the other way.
 *
 * Miuix's own behavior is exit-until-collapsed, so this reuses that animation pipeline with
 * `canScroll = { true }` — the closest native equivalent, and it keeps a single scroll
 * pipeline per page.
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
 * descendants that try to capture the same backdrop become no-ops instead of nesting two
 * `record()` calls on one graphics layer.
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
 */
private val BlurFadeInDistance = 48.dp

/**
 * 统一的顶部渐进模糊条高度：状态栏内缩 + [MiuixTopBarRowHeight]（64dp）。
 *
 * 状态栏高度必须取 [android.view.View.getRootWindowInsets]（窗口级、不受 Compose insets
 * 消费链影响）：`WindowInsets.statusBars` 在 Scaffold 的 topBar 槽内读出来已被上游消费成 0，
 * 条带会矮一截、渐变提前淡完，看起来就是"模糊消失"。
 */
@Composable
private fun UnifiedBlurBandHeight(): Dp {
    val density = LocalDensity.current
    val statusBarTop = LocalView.current.rootWindowInsets?.let {
        @Suppress("DEPRECATION")
        it.systemWindowInsetTop
    } ?: 0
    return with(density) { statusBarTop.toDp() } + MiuixTopBarRowHeight
}

/**
 * A top bar container with the miuix-blur standard progressive blur — `ProgressiveBlur.Top` at
 * `blurRadius = 20f` (guide: Progressive Blur): full strength at the top edge, pixel-sharp clear
 * at the bottom edge of the bar. The wrapped content should use a transparent background while
 * [backdrop] is non-null.
 *
 * NOTE: on some Adreno devices (observed on Android 17 / houji & thyme) this bar's
 * progressiveTextureBlur draws nothing once the page is re-created (e.g. returning from a
 * thread) even though the backdrop layer keeps recording — the same modifier still renders in
 * other contexts. Collapsing-bar pages should prefer [PlatformProgressiveBlurBar].
 *
 * @param backdrop the captured page content, or null when the device cannot render the blur.
 * @param scrollBehavior Miuix scroll behavior; the blur fades in with the bar's scroll offset.
 * @param blurProgress explicit blur strength in `0f..1f`, taking precedence over [scrollBehavior].
 *   Use it for hosts whose fade curve differs from the `contentOffset` ramp.
 * @param content the bar content, drawn on top of the blur layer.
 */
@Composable
fun MiuixProgressiveBlurBar(
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior? = null,
    blurProgress: (() -> Float)? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier) {
        if (backdrop != null) {
            val fadeInDistance = with(LocalDensity.current) { BlurFadeInDistance.toPx() }
            // `graphicsLayer` sits OUTSIDE `progressiveTextureBlur`: updating alpha invalidates
            // only the compositing step while the shader output stays cached by the draw node.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UnifiedBlurBandHeight())
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
                        blurRadius = 20f,
                        gradient = ProgressiveBlur.Top,
                    ),
            )
        }
        content()
    }
}

/**
 * 渐进模糊条的平台模糊实现：内层 GraphicsLayer 挂平台原生 [BlurEffect]（非 AGSL）绘制
 * [backdrop] 的回读层，外层离屏合成后用 DstIn 竖向渐变把模糊从顶部全强淡出到底部全透，
 * 近似 HyperOS 渐进模糊的观感。
 *
 * 为什么需要它：miuix-blur 的 `progressiveTextureBlur`（AGSL RuntimeEffect 链）在部分
 * Adreno 设备上页面重建（导航返回）后在折叠顶栏的上下文里整条链路静默不画——回读层有
 * 内容、滚动状态正确、同一修饰符在别处仍能渲染，唯独顶栏内无输出。平台 BlurEffect
 * （kyant 液态玻璃同一路线）在同样场景存活。折叠类顶栏（话题页、吧页）用它。
 *
 * @param backdrop Scaffold 的全屏回读层（`LocalLiquidGlassBackdrop`，须在 topBar 槽内读取）；
 *   顶栏在屏幕顶部，回读层原点与栏采样原点重合，直接按层原点绘制。
 * @param blurProgress 模糊层透明度 0..1；折叠栏传 `collapsedFraction`，随收起从 0 渐入到 1。
 * @param content the bar content, drawn on top of the blur layer.
 */
@Composable
fun PlatformProgressiveBlurBar(
    backdrop: LayerBackdrop?,
    blurProgress: () -> Float,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier) {
        if (backdrop != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UnifiedBlurBandHeight())
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        alpha = blurProgress().coerceIn(0f, 1f)
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        // 渐进遮罩：顶部不透明（模糊全强）→ 底部透明（露出清晰内容）
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            // ~20dp @3x，对齐 miuix 渐进模糊的默认强度
                            renderEffect = BlurEffect(55f, 55f, TileMode.Clamp)
                        }
                        .drawBehind { drawLayer(backdrop.graphicsLayer) },
                )
            }
        }
        content()
    }
}

/**
 * Bottom navigation bar placeholder with the miuix-blur standard progressive blur — the
 * counterpart of [MiuixProgressiveBlurBar] for the bottom edge. The blur is strongest at the
 * system navigation-bar edge and fades upward over [backdrop].
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
                                blurRadius = 20f,
                                gradient = ProgressiveBlur.Bottom,
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
