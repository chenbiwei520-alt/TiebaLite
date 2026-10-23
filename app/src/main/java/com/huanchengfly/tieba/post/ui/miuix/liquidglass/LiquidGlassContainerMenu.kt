package com.huanchengfly.tieba.post.ui.miuix.liquidglass

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

/**
 * 收起动画播完后弹窗层还要留的时长（毫秒）。
 *
 * 容器变换的退出是「面板缩回触发位置的 42dp 玻璃盒再淡出」，由
 * [LiquidGlassDropdownMenu] 自己的动画驱动；这段时间里控件必须
 * 仍然被组合，否则控件会连同弹窗层一起被销毁，退出动画根本看不到。
 */
private const val ContainerMenuExitHoldMillis = 520L

/**
 * 参考仓 `MorePopupMenus` 给全屏层加的固定水平偏移（参考里写死 `offset(x = 9.dp)`）。
 */
private val LiquidGlassMenuHorizontalOffset = 9.dp

/**
 * 面板静止时，右缘离屏幕右缘的距离。
 *
 * 参考仓把控件放在全屏层里 `offset(x = 9.dp)` + `contentAlignment = TopEnd`，控件内部再留
 * [LiquidGlassDropdownShadowPadding]（24dp）的阴影边 —— 面板右缘 = 屏幕右缘 − 24dp + 9dp。
 * 把面板对齐到触发区时要拿这个基准反推平移量。
 */
private val LiquidGlassMenuPanelRightInset =
    LiquidGlassDropdownShadowPadding - LiquidGlassMenuHorizontalOffset

/** 参考 `ShortcutMenu` 判定「面板右缘会超出屏幕」时留的安全边距。 */
private val LiquidGlassMenuSafetyPadding = 4.dp

/**
 * 向上展开时，面板底缘与按压点之间留的间隙。
 *
 * 参考 `ShortcutMenu` 把弹层放在 `y = position.y - height + 4.dp`，这里的 4.dp 与它同源。
 */
private val LiquidGlassMenuAnchorGap = 4.dp

/**
 * 把参考仓的容器变换菜单（[LiquidGlassDropdownMenu]）挂进 Miuix 弹窗层。
 *
 * 控件自身尺寸就是面板尺寸（外层 24dp 阴影留边 + 面板），直接放进页面会把它所在的
 * 触发器撑大 —— 参考仓同样把它放在全屏层里渲染，这里用 Miuix 的 `PopupLayout` 当那个
 * 全屏层：它由最近的 `Scaffold` 弹层承载、画在顶栏之后、不参与页面测量，放哪儿都不影响布局。
 *
 * 定位与参考仓 `MainActivity.MorePopupMenus` 逐字一致：
 *
 * ```
 * Box(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .graphicsLayer { clip = false }
 *         .padding(top = anchorTop)
 *         .offset(x = 9.dp),
 *     contentAlignment = Alignment.TopEnd,
 * )
 * ```
 *
 * 即控件静止时贴在屏幕右上角：面板右缘离屏幕右缘 [LiquidGlassMenuPanelRightInset]，
 * 垂直方向由触发区顶端决定（参考里 `anchorTop` 由顶栏槽位顶端算出，这里由 [anchor] 顶端算出）。
 *
 * 参考的触发按钮由控件自己画、且固定在屏幕右上角，所以参考不需要水平对齐；我们的触发按钮在页面里，
 * 于是按参考 `ShortcutMenu` 的摆放规则把面板平移到触发区上 —— 那条规则原文是「默认向右展开；
 * 可见右边缘超出屏幕安全边距时向左平移，右边缘对齐卡片右边缘」，平移量经控件自带的 `offsetPx`
 * 传给面板节点（与参考用 `offsetPx` 传顶栏跟手位移是同一条通路）：
 *
 * - 触发区右侧放得下面板 ⇒ 面板左缘对齐触发区左缘（向右展开）；
 * - 放不下 ⇒ 整体左移、面板右缘对齐触发区右缘（并夹进屏幕安全边距），贴屏幕右侧那些「更多」按钮
 *   因此仍保持原来的右上角效果；
 * - 触发区本身就比面板宽（整张卡片当触发区，长按菜单没有按压点时的兜底锚点）⇒ 一律向右展开，
 *   面板贴着触发区左缘，行为与引入翻边规则之前一致。
 *
 * 「放不下」的判据是「触发区右缘到屏幕安全边距还有没有面板宽」。曾经用的是「面板左缘 + 面板宽
 * 会不会超出屏幕右安全边距」，那在宽屏上会失效：横屏平板/大屏的页面内容列是居中的，卡片右缘离
 * 屏幕右缘还剩两三百 dp 留白，条件因此成立，面板就在留白里向右长出去、和触发按钮彻底脱开（在
 * 动态页卡片的「不感兴趣」按钮上肉眼可见）。改成看触发区右缘的余量后，手机竖屏这些右侧按钮的
 * 结果不变，宽屏则和竖屏一样朝左展开。
 *
 * @param show 菜单是否展开；由页面的菜单状态驱动。
 * @param anchor 触发区在窗口坐标系里的矩形（页面侧用 `boundsInWindow()` 上报）。
 * @param onDismiss 点击面板外或按返回键时回调（由页面把菜单状态关掉）。
 * @param backdrop 页面捕获层，直接透传给控件。
 * @param triggerIcon 触发按钮的图标；交给控件画进收起态的玻璃圆盒里，页面侧同时要把自己那份隐藏。
 * @param onPopupVisibleChanged 弹窗层真实可见性变化回调（含退出滞留期）。
 * @param flipWhenNoRoomBelow 锚点落在屏幕下半区时改为向上展开（面板底缘压在锚点上方）。
 * @param content 菜单项，直接用 [LiquidGlassDropdownMenuItem] 写。
 */
@Composable
fun LiquidGlassContainerMenu(
    show: Boolean,
    anchor: Rect,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    // 触发按钮的图标。控件把收起态画成一颗 42dp 玻璃圆盒，图标就长在盒里、随盒子展开淡出；
    // 传 null 时盒子里是空的，页面若还画着自己的图标，就会看到一层玻璃压在那颗图标上。
    triggerIcon: ImageVector? = null,
    triggerIconSize: Dp = 23.dp,
    triggerContentDescription: String? = null,
    triggerIconTint: Color? = null,
    // 弹窗层的**真实**可见性：收起动画播完、[ContainerMenuExitHoldMillis] 滞留期结束后才是 false。
    // 页面侧靠它把触发图标让给胶囊（用 show 判断会让图标在胶囊还在时提前冒出来）。
    onPopupVisibleChanged: ((Boolean) -> Unit)? = null,
    // 锚点落在屏幕下半区时改为向上展开。给「跟随手指落点」的长按菜单用：
    // 按压点靠下时继续向下展开会把面板顶到屏幕外。
    flipWhenNoRoomBelow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visibleState = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    // 面板自然高度（px）。向上展开要靠它把面板底缘压到按压点上方，测出来之前为 0。
    var panelHeightPx by remember { mutableIntStateOf(0) }
    // 摆放坐标：全屏层尺寸与 anchor（boundsInWindow）在同一坐标系，参考 ShortcutMenu 也是这么比较的。
    val windowSize = LocalWindowInfo.current.containerSize
    val screenWidthPx = windowSize.width.toFloat()
    val screenHeightPx = windowSize.height.toFloat()
    val anchorTop = with(density) {
        val shadowPaddingPx = LiquidGlassDropdownShadowPadding.toPx()
        // 参考 ShortcutMenu：面板底缘压在按压点上方 [LiquidGlassMenuAnchorGap] 处。
        // 面板顶 = anchor.top - 高度 - gap，而控件还带 24dp 阴影留边，容器顶要再减这一截。
        val preferred = if (
            flipWhenNoRoomBelow &&
            panelHeightPx > 0 &&
            anchor.top > screenHeightPx / 2f
        ) {
            anchor.top - panelHeightPx - LiquidGlassMenuAnchorGap.toPx() - shadowPaddingPx
        } else {
            anchor.top - shadowPaddingPx
        }
        preferred.coerceAtLeast(0f).toDp()
    }
    val panelRightInsetPx = with(density) { LiquidGlassMenuPanelRightInset.toPx() }
    val panelWidthPx = with(density) { PanelWidth.toPx() }
    val safetyPaddingPx = with(density) { LiquidGlassMenuSafetyPadding.toPx() }
    // 面板静止（offsetPx = 0）时的右缘
    val restRightPx = screenWidthPx - panelRightInsetPx
    // 参考 ShortcutMenu 的规则：优先从触发区左缘向右展开；触发区右侧余量不足时改成右缘对齐触发区右缘。
    // 判据必须落在「触发区右缘」上，而不是「面板左缘 + 宽度」：后者只看屏幕边缘，宽屏里内容列两侧
    // 的留白就足以让条件成立，面板会从触发区一路往右长到屏幕边、悬在卡片外面（见上方 KDoc）。
    val horizontalOffsetPx: () -> Offset = {
        val roomRightPx = screenWidthPx - safetyPaddingPx - anchor.right
        val roomLeftPx = anchor.left - safetyPaddingPx
        val alignEnd = when {
            // 触发区比面板还宽：面板在触发区自己的范围内，贴左缘展开即可（长按菜单的兜底锚点）。
            anchor.width >= panelWidthPx -> false
            // 触发区右侧放得下面板。
            roomRightPx >= panelWidthPx -> false
            // 右侧放不下但左侧放得下：翻到左侧（宽屏小按钮、手机右侧按钮都走这里）。
            roomLeftPx >= panelWidthPx -> true
            // 两侧都放不下：贴屏幕右安全边距，至少保证整块面板在屏幕内。
            else -> true
        }
        if (alignEnd) {
            Offset(x = minOf(anchor.right, screenWidthPx - safetyPaddingPx) - restRightPx, y = 0f)
        } else {
            Offset(x = anchor.left - (restRightPx - panelWidthPx), y = 0f)
        }
    }

    LaunchedEffect(show) {
        if (show) {
            visibleState.value = true
        } else {
            delay(ContainerMenuExitHoldMillis)
            visibleState.value = false
        }
    }

    // 如实上报可见性：页面侧据此在「胶囊在场」期间隐藏自己的图标，等到胶囊真正消失再露出来。
    LaunchedEffect(visibleState.value) {
        onPopupVisibleChanged?.invoke(visibleState.value)
    }

    PopupLayout(
        visible = visibleState,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableWindowDim = false,
        // 控件的返回处理已经由 LiquidGlassDropdownMenu 内部的 BackHandler 承担，
        // 它走 onDismiss => 页面菜单状态，交给弹窗层只会把状态弄成两份。
        enableBackHandler = false,
        renderInRootScaffold = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 面板外的点击关掉菜单；画在控件之前，因此不会挡到面板自己的手势。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = false }
                    .padding(top = anchorTop)
                    .offset(x = LiquidGlassMenuHorizontalOffset),
                contentAlignment = Alignment.TopEnd,
            ) {
                LiquidGlassDropdownMenu(
                    show = show,
                    backdrop = backdrop,
                    onDismiss = onDismiss,
                    triggerIcon = triggerIcon,
                    // 只有在控件自己画触发图标时，收起态的玻璃圆盒才代表那颗按钮；没有图标时
                    // 它只是一枚凭空出现的玻璃圆点（长按菜单、DislikeBtn 都是这种），索性让它
                    // 从 0 开始随展开淡入：顺带避免向上展开时定位修正把圆点整块挪走的那一帧。
                    materialAlpha = if (triggerIcon != null) 1f else 0f,
                    triggerIconSize = triggerIconSize,
                    triggerContentDescription = triggerContentDescription,
                    triggerIconTint = triggerIconTint,
                    onPanelHeightChanged = { panelHeightPx = it },
                    offsetPx = horizontalOffsetPx,
                    content = content,
                )
            }
        }
    }
}
