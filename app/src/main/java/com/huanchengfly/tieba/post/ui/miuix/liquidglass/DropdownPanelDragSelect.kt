package com.huanchengfly.tieba.post.ui.miuix.liquidglass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

// ── 跟手滑选 ────────────────────────────────────────────────────────
// 两处弹窗共用：右上角「更多」下拉（LiquidGlassDropdownMenu）与 miuix 弹窗面板
// （DropdownImpl + ListPopup）。区别只在状态由谁创建——前者自持，后者在 Popup
// 独立窗口里，必须由调用方创建后下发。
//
// 坐标系统一为**面板局部坐标**：面板整体缩放走 graphicsLayer，不进 layout 坐标，
// 挂在面板上的手势与菜单项天然同一套。

/**
 * ⚠️ 形变参数必须**尺寸无关**。面板宽度随内容自适应（200~288dp）、高度随项数变化，
 * 「绝对 dp / 当前尺寸」的归一化会让效果在大面板上趋近于 0（LiquidTopBarButton 的
 * `2dp / 高度` 就是反例：42dp 上竖拖 40dp 有 4.8% 拉伸，搬到面板上只剩 0.2%）。
 */

/** 沿拖动方向的最大拉伸比例（拖过行程基准时吃满） */
private const val DragStretchRatio = 0.045f

/**
 * 竖向行程基准相对横向的缩放（<1 = 更容易吃满），按展开进度插值。
 * 菜单只有几行高，竖向行程天然比横向短，用同一基准时竖拖明显迟钝。
 */
private const val VerticalDragRefScale = 0.6f

/** 长宽比惩罚下限：以 1:1 为界、偏离时才罚。直接 coerceAtMost(1f) 在扁长面板上会砍掉三成 */
private const val AspectPenaltyFloor = 0.97f

/** 跟手滑选生效的最小展开进度：面板接近满尺寸后菜单项位置不再变动 */
private const val DragSelectReadyFraction = 0.98f

internal data class DragTransform(
    val scaleX: Float,
    val scaleY: Float,
    val translationX: Float,
    val translationY: Float,
)

/**
 * 按压缩放 + 沿拖动方向拉伸 + tanh 阻尼跟手位移。菜单面板与整宽按钮共用这一套。
 *
 * @param shapeAspectRatio 长宽比惩罚的基准，null = 用实测宽高比。
 *   接近方形的面板传 null 即可（实测就是它的正常形状）。
 *   **天生长条**的控件（整宽按钮 360x40）必须传 1f：否则 penaltyY = 40/360 = 0.11，
 *   纵向形变只剩面板的 1/6，等于把这条轴的跟手感关掉。
 *   惩罚是为防「意外扁长」，不该 punish 设计上就长条的形状。
 */
internal fun computeDragTransform(
    width: Float,
    height: Float,
    fraction: Float,
    pressProgress: Float,
    offset: Offset,
    density: Density,
    shapeAspectRatio: Float? = null,
): DragTransform {
    val selfH = height.coerceAtLeast(1f)
    val minDim = minOf(width, selfH).coerceAtLeast(1f)
    val pressScale = 1f + with(density) { 4f.dp.toPx() } / selfH * pressProgress.coerceAtLeast(0f)
    // 竖向行程基准按展开进度收紧（菜单就几行高，竖向行程天然短）
    val refY = minDim * (1f - (1f - VerticalDragRefScale) * fraction.coerceIn(0f, 1f))
    val base = shapeAspectRatio
    val penaltyX = (base ?: width / selfH).coerceAtMost(AspectPenaltyFloor)
    val penaltyY = (base?.let { 1f / it } ?: (selfH / width)).coerceAtMost(AspectPenaltyFloor)
    val angle = atan2(offset.y, offset.x)
    return DragTransform(
        scaleX = pressScale + DragStretchRatio * abs(cos(angle) * offset.x / minDim) * penaltyX,
        scaleY = pressScale + DragStretchRatio * abs(sin(angle) * offset.y / refY) * penaltyY,
        translationX = minDim * tanh(0.08f * offset.x / minDim),
        translationY = minDim * tanh(0.08f * offset.y / minDim),
    )
}

/** 菜单项登记信息：面板局部坐标下的纵向区间 + 点击动作 */
class DropdownPanelEntry internal constructor() {
    internal var top = 0f
    internal var bottom = 0f
    internal var action: (() -> Unit)? = null
    internal var enabled = true
}

/**
 * 跟手滑选状态。由面板内容层创建，通过 [LocalDropdownPanelDragSelect] 下发给菜单项。
 */
class DropdownPanelDragSelectState internal constructor() {
    private val entries = mutableStateListOf<DropdownPanelEntry>()

    /** 当前命中的项；null = 松手不执行，菜单保持展开 */
    var selected by mutableStateOf<DropdownPanelEntry?>(null)
        internal set

    /**
     * 列表是否一屏装得下。
     *
     * 装不下时纵向手势归 `verticalScroll`，跟手选择整体禁用 —— 两种手势同时生效
     * 会互相抢，表现为「滑动时菜单乱滚一截才停」。
     */
    var fitsOnScreen by mutableStateOf(true)
        internal set

    /** 面板顶端在 root 里的 y，供菜单项把 boundsInRoot 换算成面板局部坐标 */
    var panelTopInRoot by mutableFloatStateOf(0f)

    /**
     * 面板手势是否连「纯点击」也接管。
     *
     * 菜单项自己不挂 clickable 的弹窗（[LiquidGlassDropdownMenu]）必须为 true —— 那里点击只能靠
     * 面板手势执行。菜单项自带 clickable 的弹窗（miuix 的 `DropdownImpl` 行）必须为 false：
     * 纯点击交给行自己，面板手势只负责拖动，否则一次点击会被两边各执行一遍。
     */
    internal var executesTapAction = true

    internal fun register(entry: DropdownPanelEntry) {
        if (!entries.contains(entry)) entries.add(entry)
    }

    internal fun unregister(entry: DropdownPanelEntry) {
        entries.remove(entry)
        if (selected === entry) selected = null
    }

    internal fun updateBounds(entry: DropdownPanelEntry, top: Float, bottom: Float) {
        entry.top = top
        entry.bottom = bottom
    }

    /** @param y 面板局部 y；@return 命中的项，null = 空白处 */
    internal fun hitTest(y: Float): DropdownPanelEntry? {
        val hit = entries.firstOrNull { it.enabled && y >= it.top && y < it.bottom }
        selected = hit
        return hit
    }

    internal fun clear() {
        selected = null
    }
}

internal val LocalDropdownPanelDragSelect =
    compositionLocalOf<DropdownPanelDragSelectState?> { null }

/**
 * 面板上**唯一**的手势状态：按下 → 命中 → 逐帧改命中 → 松手执行。
 * 菜单项上不能再挂 clickable —— 两个手势状态互相抢事件正是「先按住再滑动会跳状态」的根因。
 *
 * 收起态不接管（面板太小、没有项可选），也不消费 down 事件，所以收起态的点按展开
 * 仍走触发区原有的 clickable。
 *
 * @param fraction 现读展开进度。**不能捕获** —— `pointerInput(Unit)` 的 lambda
 *   只在首次组合跑一次，捕获会得到陈旧值（曾因此导致「直接滑动毫无反应」）。
 */
fun Modifier.dropdownPanelDragSelect(
    state: DropdownPanelDragSelectState,
    fraction: () -> Float,
    hapticFeedback: HapticFeedback,
): Modifier = pointerInput(Unit) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 面板没长开 / 一屏装不下时完全不参与：前者让收起态照常点按，后者归滚动
        if (!state.fitsOnScreen || fraction() < DragSelectReadyFraction) return@awaitEachGesture
        down.consume()
        var lastHit = state.hitTest(down.position.y)
        // 是否真的拖过（超过 touch slop）。行自带 clickable 时靠它区分「点击」与「拖动」
        var dragged = false
        while (true) {
            val change = awaitPointerEvent()
                .changes.fastFirstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            if (!dragged && (change.position - down.position).getDistance() > touchSlop) dragged = true
            val hit = state.hitTest(change.position.y)
            if (hit != null && hit !== lastHit) {
                // 仅「换了一项」才震，按下即命中的那一下不震
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastHit = hit
            // 行自带 clickable 时只吃「拖动」的移动事件：吃掉会取消那次点击，
            // 纯点击留给行自己的 clickable，避免一次点击被执行两遍
            if (state.executesTapAction || dragged) change.consume()
        }
        val hit = state.selected
        state.clear()
        if (state.executesTapAction || dragged) hit?.action?.invoke()
    }
}

/**
 * 面板的按压 / 跟手拉伸 / 阻尼位移形变。
 *
 * 挂在两个槽位的共同父层（玻璃壳与内容层的父节点），玻璃、边光、阴影、内容
 * 才会整体变形；挂到内容层只能让文字动。收起态的形变幅度天然趋近 0。
 */
fun Modifier.dropdownPanelDragTransform(
    fraction: () -> Float,
    pressProgress: () -> Float,
    dragOffset: () -> Offset,
): Modifier = composed {
    graphicsLayer {
        val t = computeDragTransform(
            width = size.width,
            height = size.height,
            fraction = fraction(),
            pressProgress = pressProgress(),
            offset = dragOffset(),
            density = this,
        )
        scaleX = t.scaleX
        scaleY = t.scaleY
        translationX = t.translationX
        translationY = t.translationY
    }
}

/** 命中项的高亮底色（与 LiquidGlassDropdownMenu 一致） */
private fun dropdownPanelEntryHighlightColor(isDark: Boolean): Color =
    if (isDark) Color.White.copy(0.11f) else Color.Black.copy(0.075f)

/**
 * 菜单项侧：登记纵向区间 + 绘制命中高亮。供 `DropdownImpl` 等菜单项组件调用。
 *
 * 用 drawBehind 而非 background：`DropdownImpl` 的项自带一层 alpha=1 的不透明白底
 * （`.drawBehind { drawRect(surfaceContainer) }`）。Modifier 链越靠后越晚绘制，调用方
 * 必须把本修饰符放在那行 drawBehind **之后**，否则高亮被白底吞掉。
 *
 * 登记与绘制合在一个修饰符里：两者共用同一个 [DropdownPanelEntry]，拆开会各自
 * remember 出不同实例，选中态永远匹配不上。
 *
 * @param enabled false 的项不参与命中测试（滑过去不高亮、松手也不执行）
 * @param action 命中并松手时执行。每次重组都会更新，可直接传 lambda
 */
@Composable
fun Modifier.dropdownPanelEntry(
    enabled: Boolean,
    action: () -> Unit,
): Modifier = composed {
    val state = LocalDropdownPanelDragSelect.current
    val entry = remember(state) { DropdownPanelEntry() }
    DisposableEffect(state, entry) {
        state?.register(entry)
        onDispose { state?.unregister(entry) }
    }
    SideEffect {
        entry.action = action
        entry.enabled = enabled
    }
    val selected = state != null && state.selected === entry
    // drawBehind 的 lambda 不是 @Composable，主题色必须在这里取好再传进去
    val highlightColor = dropdownPanelEntryHighlightColor(isAppDarkTheme())
    this
        .onGloballyPositioned {
            // boundsInRoot 不含 graphicsLayer 变换，面板与项都取 root 坐标再作差
            val top = it.boundsInRoot().top - (state?.panelTopInRoot ?: 0f)
            state?.updateBounds(entry, top, top + it.size.height)
        }
        .drawBehind {
            if (selected) drawRect(highlightColor)
        }
}

/**
 * 菜单项侧（行没有 modifier 参数可用时的版本）。
 *
 * miuix 的 `DropdownImpl` 不接受 modifier，只能在外面套一层容器：[dropdownPanelEntry] 那种
 * 「登记 + 顺手 drawBehind」的写法在这里画不出高亮 —— 容器的 drawBehind 排在子节点之前，
 * 会被行内那层不透明白底整个吞掉。所以这里把高亮画成**内容之后的兄弟节点**：高亮本身是
 * 半透明黑/白，叠在上面和压在下面观感一致，面板的圆角由内容层统一裁剪。
 *
 * 行自带 clickable 的弹窗（`DropdownImpl`）必须同时把
 * [DropdownPanelDragSelectState.executesTapAction] 置 false，纯点击交给行自己。
 */
@Composable
fun DropdownPanelEntryHost(
    enabled: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = LocalDropdownPanelDragSelect.current
    val entry = remember(state) { DropdownPanelEntry() }
    DisposableEffect(state, entry) {
        state?.register(entry)
        onDispose { state?.unregister(entry) }
    }
    SideEffect {
        entry.action = action
        entry.enabled = enabled
    }
    val selected = state != null && state.selected === entry
    // drawBehind 的 lambda 不是 @Composable，主题色必须在这里取好再传进去
    val highlightColor = dropdownPanelEntryHighlightColor(isAppDarkTheme())
    Box(
        modifier = modifier.onGloballyPositioned {
            val top = it.boundsInRoot().top - (state?.panelTopInRoot ?: 0f)
            state?.updateBounds(entry, top, top + it.size.height)
        }
    ) {
        content()
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind { drawRect(highlightColor) }
            )
        }
    }
}
