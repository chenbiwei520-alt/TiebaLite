package com.huanchengfly.tieba.post.ui.miuix

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.DropdownPanelDragSelectState
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.ListPopupColumn
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.DropdownEntriesPopupContent
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LocalLiquidGlassPopupBackdrop
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.OverlayListPopup
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The page layer the liquid-glass dropdowns sample.
 *
 * Provided by `MyScaffold` for every page, and by the two scaffolds that host a dropdown directly
 * (`MiuixSettingsScaffold`, the theme page) from the capture they already run for their top bar.
 *
 * **Placement rule — the capture must never contain the popup.** An `OverlayListPopup` renders in
 * the `Scaffold`'s *popup layer*, which is drawn outside the `Scaffold`'s `content`. So the capture
 * has to sit inside `content`: `MyScaffold` puts it there. Wrapping the whole app instead (e.g.
 * around the `NavHost`) also records the open popup, and since the panel is an opaque
 * `surfaceContainer` the sheet then blurs *itself* — the panel washes out to flat white and the lit
 * edge disappears against its own background. That is the bug this placement rule exists to
 * prevent.
 *
 * Null when the device cannot render shader-backed blur (RuntimeShader needs API 33).
 */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }


/**
 * Item colours for a dropdown that sits on a liquid-glass sheet. Internal so the dislike reason
 * menu can reuse the same fully transparent rows on its own glass panel.
 *
 * miuix's default `containerColor` is an opaque `surfaceContainer`. This follows the reference
 * project (hyper_schedule), which always pairs its liquid-glass panels with
 * `DropdownDefaults.dropdownColors(containerColor = Color.Transparent, selectedContainerColor =
 * Color.Transparent)`: the panel surface belongs to the glass material layer alone, so that the
 * sampled (and blurred) page stays the only thing tinting the sheet. Any alpha here would sit on
 * top of the blur and wash it out again.
 *
 * The readable fallback for the cases where no backdrop is available (API < 33, or a page that
 * provides none) lives in `PopupMaterialLayer`, which paints `backgroundColor` itself in that case.
 */
@Composable
internal fun liquidGlassDropdownColors(): DropdownColors {
    val default = DropdownDefaults.dropdownColors()
    return default.copy(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        selectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    )
}


/**
 * A Miuix list popup whose panel is a liquid-glass sheet sampling [liquidGlassBackdrop].
 *
 * Built on the public [OverlayListPopup] shell, so positioning, dismissal, back handling and the
 * enter/exit reveal animation are all miuix's own; only the panel content is ours. The rows are
 * miuix's [DropdownImpl], so the selected check, padding and typography match the stock dropdown —
 * including the popup-global first/last row padding, which miuix computes because group boundaries
 * must fall back to the middle padding.
 */
/**
 * Compensation added to the trigger's measured size when the popup's collapsed box is placed on
 * it. The row is measured at its text and chevron only, while the box it grows out of has to cover
 * the padding around them; the values are the reference app's `DefaultCollapseExtra`.
 */
private val LiquidGlassCollapseExtra = DpSize(14.dp, 10.dp)

@Composable
fun LiquidGlassDropdownPopup(
    entries: List<DropdownEntry>,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    maxHeight: Dp?,
    dropdownColors: DropdownColors,
    liquidGlassBackdrop: Backdrop?,
    renderInRootScaffold: Boolean = true,
    collapseOnSelection: Boolean = entries.size <= 1,
    collapseSize: IntSize? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val currentEntries by rememberUpdatedState(entries)
    val currentCollapseOnSelection by rememberUpdatedState(collapseOnSelection)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentHapticFeedback by rememberUpdatedState(hapticFeedback)
    // 跟手滑选：与其它页面的下拉（[LiquidGlassDropdownMenu]）同一套状态与手势。
    // executesTapAction 必须为 false —— 行是 miuix 的 DropdownImpl，自带 clickable，
    // 纯点击交给它，面板手势只在真的拖动时接管（否则一次点击会被两边各执行一遍）。
    val dragSelectState = remember { DropdownPanelDragSelectState() }
    dragSelectState.executesTapAction = false
    val onItemClicked: (entryIdx: Int, itemIdx: Int) -> Unit = remember {
        { entryIdx, itemIdx ->
            currentHapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            currentEntries.getOrNull(entryIdx)?.items?.getOrNull(itemIdx)?.onClick?.invoke()
            if (currentCollapseOnSelection) {
                currentOnDismiss()
            }
        }
    }


    OverlayListPopup(
        show = show,
        liquidGlassBackdrop = liquidGlassBackdrop,
        alignment = PopupPositionProvider.Align.End,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        maxHeight = maxHeight,
        // 下拉不改背景亮度：参考实现的下拉也是直接浮在页面上，压暗整页会让设置项内容看不清。
        enableWindowDim = false,
        renderInRootScaffold = renderInRootScaffold,
        // The panel grows out of the trigger's own box, so it reads as opening from it instead of
        // fading a finished panel in over it.
        collapseSize = collapseSize,
        collapseExtra = LiquidGlassCollapseExtra,
        dragSelectState = dragSelectState,
    ) {
        // onFitsOnScreen 必须写在前面：content 是尾随 lambda，放后面就绑不上了
        ListPopupColumn(
            onFitsOnScreen = { dragSelectState.fitsOnScreen = it },
        ) {
            // 与参考 OverlayDropdownPopup(entries) 完全相同的行渲染：
            // DropdownEntriesPopupContent 就是参考从 miuix 0.9.4 vendor 出来的那一份。
            DropdownEntriesPopupContent(
                entries = entries,
                dropdownColors = dropdownColors,
                onItemClick = onItemClicked,
            )
        }
    }
}

/**
 * A dropdown preference whose options open on a liquid-glass sheet instead of miuix's opaque popup
 * panel.
 *
 * The row is a faithful copy of miuix's `OverlayDropdownPreference` (`BasicComponent` + trailing
 * selected value + chevron), because that component hosts its popup internally and exposes no hook
 * for the panel's background. Everything the row does — haptics, hold-down state, enabled handling,
 * value display — behaves exactly like the stock one.
 *
 * @param liquidGlassBackdrop the page layer the glass samples (a Kyant layer); defaults to the
 */
@Composable
fun LiquidGlassDropdownPreference(
    items: List<String>,
    selectedIndex: Int,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors = liquidGlassDropdownColors(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    renderInRootScaffold: Boolean = true,
    liquidGlassBackdrop: Backdrop? = LocalLiquidGlassPopupBackdrop.current,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onSelectedIndexChange: ((Int) -> Unit)? = null,
) {
    val entry = remember(items, selectedIndex, onSelectedIndexChange) {
        DropdownEntry(
            items = items.mapIndexed { index, item ->
                DropdownItem(
                    text = item,
                    selected = index == selectedIndex,
                    onClick = { onSelectedIndexChange?.invoke(index) },
                )
            },
        )
    }
    LiquidGlassDropdownPreference(
        entry = entry,
        title = title,
        modifier = modifier,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        dropdownColors = dropdownColors,
        startAction = startAction,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        maxHeight = maxHeight,
        enabled = enabled,
        showValue = showValue,
        renderInRootScaffold = renderInRootScaffold,
liquidGlassBackdrop = liquidGlassBackdrop,
        onExpandedChange = onExpandedChange,
    )
}

/** Entry overload, mirroring miuix's `OverlayDropdownPreference(entry = ...)` single-group form. */
@Composable
fun LiquidGlassDropdownPreference(
    entry: DropdownEntry,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors = liquidGlassDropdownColors(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    renderInRootScaffold: Boolean = true,
    liquidGlassBackdrop: Backdrop? = LocalLiquidGlassPopupBackdrop.current,
    collapseOnSelection: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    LiquidGlassDropdownPreference(
        entries = listOf(entry),
        title = title,
        modifier = modifier,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        dropdownColors = dropdownColors,
        startAction = startAction,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        maxHeight = maxHeight,
        enabled = enabled,
        showValue = showValue,
        renderInRootScaffold = renderInRootScaffold,
liquidGlassBackdrop = liquidGlassBackdrop,
        collapseOnSelection = collapseOnSelection,
        onExpandedChange = onExpandedChange,
    )
}

/**
 * Entry-group overload, mirroring miuix's `OverlayDropdownPreference(entries = ...)`: several
 * [DropdownEntry] groups render as separate sections and the trailing value joins every selected
 * label.
 */
@Composable
fun LiquidGlassDropdownPreference(
    entries: List<DropdownEntry>,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    dropdownColors: DropdownColors = liquidGlassDropdownColors(),
    startAction: @Composable (() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    maxHeight: Dp? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    renderInRootScaffold: Boolean = true,
    liquidGlassBackdrop: Backdrop? = LocalLiquidGlassPopupBackdrop.current,
    collapseOnSelection: Boolean = entries.size <= 1,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDropdownExpanded = remember { mutableStateOf(false) }
    val isHoldDown = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val currentHapticFeedback by rememberUpdatedState(hapticFeedback)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val setExpanded: (Boolean) -> Unit = remember {
        { expanded ->
            if (isDropdownExpanded.value != expanded) {
                isDropdownExpanded.value = expanded
                currentOnExpandedChange?.invoke(expanded)
            }
        }
    }

    // Measured size of the trailing value + chevron; the popup's collapsed box is exactly this
    // box, so the panel appears to grow out of the row's own contents.
    var triggerSize by remember { mutableStateOf(IntSize.Zero) }

    val nonEmptyEntries = entries.filter { it.items.isNotEmpty() }
    val hasEntries = nonEmptyEntries.isNotEmpty()
    val actualEnabled = enabled && hasEntries

    val actionColor = if (actualEnabled) {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    } else {
        MiuixTheme.colorScheme.disabledOnSecondaryVariant
    }

    val handleClick = remember(actualEnabled) {
        {
            if (actualEnabled) {
                setExpanded(!isDropdownExpanded.value)
                if (isDropdownExpanded.value) {
                    isHoldDown.value = true
                    currentHapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            }
        }
    }

    BasicComponent(
        modifier = modifier,
        interactionSource = interactionSource,
        insideMargin = insideMargin,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = startAction,
        endActions = {
            val selectedValueText = nonEmptyEntries
                .asSequence()
                .flatMap { group -> group.items }
                .filter { it.selected }
                .map { it.text }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .ifBlank { null }
            Row(
                modifier = Modifier.onSizeChanged { triggerSize = it },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showValue && hasEntries && !selectedValueText.isNullOrBlank()) {
                    Text(
                        text = selectedValueText,
                        modifier = Modifier.padding(end = 8.dp),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = actionColor,
                        textAlign = TextAlign.End,
                    )
                }
                DropdownArrowEndAction(actionColor = actionColor)
            }
            if (hasEntries) {
                LiquidGlassDropdownPopup(
                    entries = nonEmptyEntries,
                    show = isDropdownExpanded.value,
                    onDismiss = { setExpanded(false) },
                    onDismissFinished = { isHoldDown.value = false },
                    maxHeight = maxHeight,
                    dropdownColors = dropdownColors,
liquidGlassBackdrop = liquidGlassBackdrop,
                    renderInRootScaffold = renderInRootScaffold,
                    collapseOnSelection = collapseOnSelection,
                    collapseSize = triggerSize,
                )
            }
        },
        bottomAction = bottomAction,
        onClick = handleClick,
        role = Role.DropdownList,
        holdDownState = isHoldDown.value,
        enabled = actualEnabled,
    )
}
