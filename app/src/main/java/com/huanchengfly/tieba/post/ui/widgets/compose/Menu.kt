package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.OverlayListPopup
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LiquidGlassContainerMenu
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LiquidGlassDropdownMenuItem
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LocalLiquidGlassPopupBackdrop
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.Options
import kotlinx.coroutines.flow.filterIsInstance
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.ListPopupColumn
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.ListPopupDefaults
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Screen margin kept between the menu and the window edge.
 *
 * Miuix's default dropdown position provider uses `horizontalMargin = 0.dp`, so a menu anchored to
 * a top bar action ends up flush against the screen edge. Menus use `Align.TopEnd`, and the
 * provider subtracts this margin from the anchor's trailing edge.
 */
@Composable
private fun rememberMenuPositionProvider(): PopupPositionProvider =
    ListPopupDefaults.dropdownPositionProvider(horizontalMargin = 16.dp)

/**
 * HyperOS style popup menu (Miuix [DropdownImpl] rows on a liquid-glass panel, hosted by the
 * vendored [OverlayListPopup] layout).
 *
 * The Material-style public API ([MenuScope], [ClickMenu], [LongClickMenu]) is preserved so page
 * call sites stay unchanged. Menus anchor to the wrapped content instead of the raw press
 * position, matching Miuix popup behavior.
 *
 * This used to be the `WindowListPopup` (window) variant, for a reason that no longer holds: an
 * Overlay popup needs a Miuix `Scaffold` popup host, and at the time the forum/thread pages still
 * ran a non-Miuix shell, so an Overlay popup was appended to a detached state list and the menu
 * silently never appeared. Every page that opens one of these menus now uses `MyScaffold` (a Miuix
 * `Scaffold`), so the Overlay variant is safe — and it is required for the glass panel, because a
 * window popup is an `AndroidDialog` and the page's captured `GraphicsLayer` cannot be sampled
 * across windows.
 */
class MenuScope(
    private val menuState: MenuState,
    private val onDismiss: (() -> Unit)? = null,
) {
    private var itemIndex = 0

    fun dismiss() {
        onDismiss?.invoke()
        menuState.dismiss()
    }

    @Composable
    fun TextMenuItem(modifier: Modifier = Modifier, @StringRes text: Int, onClick: () -> Unit) =
        TextMenuItem(modifier, stringResource(id = text), onClick)

    /** Simple text menu item, auto closes the menu after the onClick event is triggered. */
    @Composable
    fun TextMenuItem(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
        MenuItem(item = DropdownItem(text = text), onClick = {
            onClick()
            dismiss()
        })
    }

    /** Text menu item with a leading icon, auto closes the menu after the onClick event. */
    @Composable
    fun TextIconMenuItem(
        modifier: Modifier = Modifier,
        text: String,
        icon: ImageVector,
        enabled: Boolean = true,
        onClick: () -> Unit
    ) {
        MenuItem(
            item = DropdownItem(
                text = text,
                enabled = enabled,
                icon = { iconModifier ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = iconModifier,
                    )
                },
            ),
            onClick = {
                onClick()
                dismiss()
            },
        )
    }

    @Composable
    fun ListPickerMenuItem(
        text: String,
        modifier: Modifier = Modifier,
        picked: Boolean,
        pickedIndicator: @Composable (() -> Unit)? = null,
        onClick: () -> Unit
    ) {
        MenuItem(
            item = DropdownItem(text = text, selected = picked),
            onClick = {
                if (!picked) {
                    onClick()
                }
                dismiss()
            },
        )
    }

    @Composable
    fun ListPickerMenuItem(
        @StringRes textRes: Int,
        modifier: Modifier = Modifier,
        picked: Boolean,
        pickedIndicator: @Composable (() -> Unit)? = null,
        onClick: () -> Unit
    ) = ListPickerMenuItem(
        text = stringResource(textRes),
        modifier = modifier,
        picked = picked,
        pickedIndicator = pickedIndicator,
        onClick = onClick,
    )

    @Composable
    fun <Option> ListPickerMenuItems(
        items: Options<Option>,
        picked: Option,
        onItemPicked: (item: Option) -> Unit,
        pickedIndicator: @Composable () -> Unit = {
            Icon(
                imageVector = MiuixIcons.Basic.Check,
                contentDescription = stringResource(id = R.string.desc_checked),
            )
        }
    ) {
        items.forEach { (option, title) ->
            ListPickerMenuItem(
                textRes = title,
                picked = option == picked,
                onClick = {
                    onItemPicked(option)
                },
                pickedIndicator = pickedIndicator.takeIf { option == picked }
            )
        }
    }

    @Composable
    private fun MenuItem(item: DropdownItem, onClick: () -> Unit) {
        // 图标/更多菜单整块已换成参考仓的容器变换菜单（[LiquidGlassDropdownMenu]），因此行也换成
        // 它自己的 [LiquidGlassDropdownMenuItem]：字号、行高、内边距、跟手高亮都与参考一致。
        // 选中态仍是贴吧原有的「蓝色文字 + 勾选图标」，靠本地扩展的 selected / trailing 参数。
        LiquidGlassDropdownMenuItem(
            text = item.text,
            onClick = onClick,
            icon = item.icon?.let { icon -> { icon(Modifier.size(24.dp)) } },
            enabled = item.enabled,
            selected = item.selected,
            trailing = if (item.selected) {
                {
                    Icon(
                        imageVector = MiuixIcons.Basic.Check,
                        contentDescription = stringResource(id = R.string.desc_checked),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            } else {
                null
            },
        )
    }
}

/**
 * HyperOS style sort picker trigger: a Miuix [Icon] that opens a single choice list menu.
 *
 * Shared by the pages whose list can be sorted (forum thread list, search thread results).
 *
 * @param sortTypes available sort types with their label resources.
 * @param currentSortType the currently picked sort type.
 * @param onSortTypeChanged called with the newly picked sort type.
 * @param modifier the [Modifier] applied to the trigger icon.
 * @param enabled whether the trigger reacts to input; a disabled trigger is dimmed.
 */
@Composable
fun SortMenuButton(
    sortTypes: Options<Int>,
    currentSortType: () -> Int,
    onSortTypeChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // 页面侧与胶囊侧共用同一个 tint：胶囊接管触发图标的瞬间不会有明暗跳变。
    val tint = if (enabled) {
        MiuixTheme.colorScheme.onBackground
    } else {
        MiuixTheme.colorScheme.disabledOnSecondaryVariant
    }

    ClickMenu(
        modifier = modifier,
        menuContent = {
            ListPickerMenuItems(
                items = sortTypes,
                picked = currentSortType(),
                onItemPicked = onSortTypeChanged,
            )
        },
        enabled = enabled,
        triggerIcon = MiuixIcons.Sort,
        // 与页面图标同尺寸同色：胶囊接管/交还的那一帧看不出替换。
        triggerIconSize = 24.dp,
        triggerContentDescription = stringResource(id = R.string.title_sort_type),
        triggerIconTint = tint,
    ) {
        Icon(
            imageVector = MiuixIcons.Sort,
            contentDescription = stringResource(id = R.string.title_sort_type),
            tint = tint,
            modifier = Modifier.padding(SortMenuButtonPadding),
        )
    }
}

private val SortMenuButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

@Composable
fun ClickMenu(
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    triggerShape: Shape? = null,
    // 触发按钮的图标（可选）。容器变换菜单的收起态是一颗 42dp 玻璃圆盒，控件会在盒里画这个
    // 图标、随展开淡出；不传的话盒子里是空的，页面此刻还画着自己的图标，看上去就是
    // 「胶囊图层盖住了图标」。传了之后页面那份会在胶囊在场期间隐藏。
    triggerIcon: ImageVector? = null,
    triggerIconSize: Dp = 23.dp,
    triggerContentDescription: String? = null,
    triggerIconTint: Color? = null,
    onDismiss: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition
            }
    }

    // Read the glass sampling layer HERE, in the trigger's composition, and capture it as a value.
    // A CompositionLocal read inside the popup's content lambda would resolve against the popup
    // host's composition instead of this page's, and came back null — which is why the panel
    // rendered as flat `surfaceContainer` with no blur at all. `LiquidGlassDropdownPreference`
    // gets this for free because its `backdrop` is a default *parameter*, evaluated at the call
    // site; this is the same idea applied by hand.
val glassBackdrop = LocalLiquidGlassPopupBackdrop.current

    // 触发区在窗口坐标系里的矩形：容器变换菜单靠它把面板摆到触发按钮下方、右缘对齐。
    var triggerBounds by remember { mutableStateOf(Rect.Zero) }

    // 胶囊（收起态玻璃圆盒）当前是否在场。含退出滞留：控件要等收回动画播完才真正消失。
    var capsuleVisible by remember { mutableStateOf(false) }
    // 只有「控件会自己画图标」时才能藏页面那份，否则触发按钮会整块消失。
    val iconOwnedByCapsule = glassBackdrop != null && triggerIcon != null

    Box(
        // `modifier` must be applied here: callers use it to place the trigger (e.g.
        // BoxScope.align), and a dropped modifier silently centres the trigger instead. Mirrors
        // LongClickMenu below, which already applies its own modifier.
        modifier = modifier
            .onNotNull(triggerShape) { clip(shape = it) }
            .onGloballyPositioned { triggerBounds = it.boundsInWindow() }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onClick = menuState::show
            )
    ) {
        // 用 alpha 隐藏而不是把 content 移除：移掉会让触发区尺寸变 0，boundsInWindow 跟着变，
        // 面板会在展开途中跳位。alpha 不参与测量，几何保持不变。
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = if (iconOwnedByCapsule && capsuleVisible) 0f else 1f
            }
        ) {
            content()
        }

        // 有采样层时走参考仓的容器变换菜单；API<33（拿不到采样层）时退回原来的列表弹窗。
        if (glassBackdrop != null) {
            LiquidGlassContainerMenu(
                show = menuState.expanded,
                anchor = triggerBounds,
                backdrop = glassBackdrop,
                triggerIcon = triggerIcon,
                triggerIconSize = triggerIconSize,
                triggerContentDescription = triggerContentDescription,
                triggerIconTint = triggerIconTint,
                onPopupVisibleChanged = { capsuleVisible = it },
                onDismiss = {
                    menuState.dismiss()
                    onDismiss?.invoke()
                },
            ) {
                MenuScope(menuState, onDismiss).menuContent()
            }
        } else {
            OverlayListPopup(
                show = menuState.expanded,
                liquidGlassBackdrop = glassBackdrop,
                alignment = PopupPositionProvider.Align.TopEnd,
                popupPositionProvider = rememberMenuPositionProvider(),
                onDismissRequest = {
                    menuState.dismiss()
                    onDismiss?.invoke()
                },
            ) {
                ListPopupColumn {
                    MenuScope(menuState, onDismiss).menuContent()
                }
            }
        }
    }
}

@Composable
fun LongClickMenu(
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    onClick: (() -> Unit)? = null,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition
            }
    }

    // Captured here for the same reason as in `ClickMenu`: a CompositionLocal read inside the
    // popup content resolves against the popup host, not this page.
val glassBackdrop = LocalLiquidGlassPopupBackdrop.current

    // 触发区在窗口坐标系里的矩形：容器变换菜单靠它把面板摆到触发按钮下方、右缘对齐。
    var triggerBounds by remember { mutableStateOf(Rect.Zero) }

    // 长按菜单跟随手指落点：把「触发区的窗口矩形」与「按下的局部坐标」合成一个点锚，
    // 面板因此从手指处展开，而不是从整块卡片的顶边弹出来（参考 ShortcutMenu 走的也是按压点）。
    // 没有按下坐标时（状态被恢复等）退回原来的卡片矩形。
    val pressAnchor = remember(triggerBounds, menuState.offset) {
        val press = menuState.offset
        if (press == Offset.Zero) {
            triggerBounds
        } else {
            val x = triggerBounds.left + press.x
            val y = triggerBounds.top + press.y
            Rect(x, y, x, y)
        }
    }

    Box(
        modifier = modifier
            .onNotNull(shape) { clip(shape = it) }
            .onGloballyPositioned { triggerBounds = it.boundsInWindow() }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                onLongClick = menuState::show,
                onClick = onClick ?: {}
            )
    ) {
        content()

        if (glassBackdrop != null) {
            LiquidGlassContainerMenu(
                show = menuState.expanded,
                anchor = pressAnchor,
                backdrop = glassBackdrop,
                // 按压点落在屏幕下半区时向上展开，否则贴底的卡片会把面板顶出屏幕。
                flipWhenNoRoomBelow = true,
                onDismiss = menuState::dismiss,
            ) {
                MenuScope(menuState).menuContent()
            }
        } else {
            OverlayListPopup(
                show = menuState.expanded,
                liquidGlassBackdrop = glassBackdrop,
                alignment = PopupPositionProvider.Align.TopEnd,
                popupPositionProvider = rememberMenuPositionProvider(),
                onDismissRequest = menuState::dismiss,
            ) {
                ListPopupColumn {
                    MenuScope(menuState).menuContent()
                }
            }
        }
    }
}

@Composable
fun rememberMenuState(): MenuState {
    return rememberSaveable(
        saver = MenuState.Saver,
        init = { MenuState() }
    )
}

/** The State object of the popup menu */
@Stable
class MenuState internal constructor() {

    private var _expanded by mutableStateOf(false)
    /** Whether the menu is expanded */
    val expanded: Boolean
        get() = _expanded

    // 按下位置（触发器本地坐标）。LongClickMenu 用它把容器变换面板锚到手指落点；
    // 点击类菜单不用它（那颗按钮本身就是锚点，胶囊要从图标处长出来）。
    private var _offset by mutableStateOf(Offset.Zero)
    var offset: Offset
        get() = _offset
        set(value) {
            if (value != _offset) {
                _offset = value
            }
        }

    fun toggle() {
        _expanded = !expanded
    }

    fun show() {
        _expanded = true
    }

    fun dismiss() {
        _expanded = false
    }

    companion object {
        val Saver: Saver<MenuState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.expanded,
                    it.offset.packedValue
                )
            },
            restore = {
                MenuState().apply {
                    _expanded = it[0] as Boolean
                    offset = Offset(it[1] as Long)
                }
            }
        )
    }
}