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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
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
 * 单行顶栏（动态页同款）的渐进模糊条带高度：状态栏内缩 + [MiuixTopBarRowHeight]。
 *
 * 状态栏高度必须取 [android.view.View.getRootWindowInsets]（窗口级、不受 Compose insets
 * 消费链影响）：`WindowInsets.statusBars` 在 Scaffold 的 topBar 槽内读出来可能已被上游
 * 消费成 0，条带会矮一截。
 */
@Composable
fun MiuixTopBarBlurBandHeight(): Dp {
    val density = LocalDensity.current
    val statusBarTop = LocalView.current.rootWindowInsets?.let {
        @Suppress("DEPRECATION")
        it.systemWindowInsetTop
    } ?: 0
    return with(density) { statusBarTop.toDp() } + MiuixTopBarRowHeight
}

/**
 * Scaffold 全屏捕获的渐进模糊 backdrop：在 topBar 槽内读取 [LocalLiquidGlassBackdrop]，
 * 并按 AGSL 探针结论（[ShaderBlurProbe]）门控——不可渲染时返回 null，调用方走平面栏。
 *
 * MyScaffold 自己把内容录入该层（内容侧无需再加 [miuixLayerBackdrop]），所以页面被导航
 * 重建后捕获仍然有效——这是折叠顶栏页面（吧页、话题页）与动态页共用同一 miuix 渐进模糊
 * 实现的关键：页面自建的 [rememberMiuixBarBackdrop] 在页面重建后录制层失效。
 */
@Composable
fun rememberMiuixScaffoldBackdrop(): LayerBackdrop? {
    val context = LocalContext.current
    ShaderBlurProbe.warmUp(context.applicationContext)
    val supported = isRuntimeShaderSupported() && ShaderBlurProbe.renderable
    return if (supported) LocalLiquidGlassBackdrop.current else null
}

/**
 * A top bar container with the miuix-blur standard progressive blur — `ProgressiveBlur.Top` at
 * `blurRadius = 20f`（guide: Progressive Blur）: full strength at the top edge, pixel-sharp
 * clear at the bottom edge of the bar. The wrapped content should use a transparent background
 * while [backdrop] is non-null.
 *
 * 统一规范（miuix-blur guide）：模糊层铺满宿主栏自身边界，渐变在栏下缘收干净。单行栏
 * （大多数页面）由此自然同高（状态栏 + 行高），两行栏（吧页的吧信息行 + 标签行）整栏覆盖。
 * 这是全项目唯一的顶部渐进模糊实现（动态页同款）。
 *
 * backdrop 来源要求：会被导航重建的页面（吧页、话题页等）必须采样 Scaffold 的全屏捕获
 * （[rememberMiuixScaffoldBackdrop]），不能用页面自建的 [rememberMiuixBarBackdrop]——页面
 * 重建后自建捕获的录制层不再被录制，模糊会采样到透明凭空消失（HomePage/TopicDetailPage
 * 都踩过并记有注释）；常驻主页（动态页）不受影响。
 *
 * @param backdrop the captured page content, or null when the device cannot render the blur.
 * @param scrollBehavior Miuix scroll behavior; the blur fades in with the bar's scroll offset.
 * @param blurProgress explicit blur strength in `0f..1f`, taking precedence over [scrollBehavior].
 *   Use it for hosts whose fade curve differs from the `contentOffset` ramp.
 * @param blurBandHeight 可选的条带高度钳制。null（默认）＝铺满宿主栏（规范形态，动态页/
 *   吧页等高或整栏）；当栏带有 content 槽额外内容（帖子页吸顶的 ThreadHeader 会把栏撑高）
 *   又希望模糊带与单行动态页等高时，传 [MiuixTopBarBlurBandHeight]。
 * @param content the bar content, drawn on top of the blur layer.
 */
@Composable
fun MiuixProgressiveBlurBar(
    backdrop: LayerBackdrop?,
    scrollBehavior: ScrollBehavior? = null,
    blurProgress: (() -> Float)? = null,
    blurBandHeight: Dp? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier) {
        if (backdrop != null) {
            val fadeInDistance = with(LocalDensity.current) { BlurFadeInDistance.toPx() }
            val blurLayerModifier = if (blurBandHeight != null) {
                Modifier
                    .fillMaxWidth()
                    .height(blurBandHeight)
                    .align(Alignment.TopStart)
            } else {
                Modifier.matchParentSize()
            }
            // `graphicsLayer` sits OUTSIDE `progressiveTextureBlur`: updating alpha invalidates
            // only the compositing step while the shader output stays cached by the draw node.
            Box(
                modifier = blurLayerModifier
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
