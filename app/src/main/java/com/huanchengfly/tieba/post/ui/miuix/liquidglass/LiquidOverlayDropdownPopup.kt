// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.huanchengfly.tieba.post.ui.miuix.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.Backdrop
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownEntry

/**
 * 单个 [DropdownEntry] 的弹窗。
 *
 * @param entry 下拉条目
 * @param show 是否显示
 * @param onDismiss 关闭回调
 * @param onDismissFinished 关闭动画完成回调
 * @param maxHeight 最大高度
 * @param dropdownColors 下拉颜色
 * @param renderInRootScaffold 是否在根Scaffold中渲染
 * @param collapseOnSelection 选中后是否关闭
 * @param liquidGlassBackdrop 液体玻璃模糊效果的Backdrop
 */
@Composable
fun OverlayDropdownPopup(
    entry: DropdownEntry,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    maxHeight: Dp?,
    dropdownColors: DropdownColors,
    renderInRootScaffold: Boolean,
    collapseOnSelection: Boolean = true,
    liquidGlassBackdrop: Backdrop? = null,
    onFractionProgress: ((Float) -> Unit)? = null,
    collapseSize: IntSize? = null,
    collapseExtra: DpSize = DpSize.Zero,
    collapseContent: (@Composable () -> Unit)? = null,
    /** 跟手滑选状态。为 null 时该弹窗不启用跟手选择。 */
    dragSelectState: DropdownPanelDragSelectState? = null,
) {
    val entries = remember(entry) { listOf(entry) }
    OverlayDropdownPopup(
        entries = entries,
        show = show,
        onDismiss = onDismiss,
        onDismissFinished = onDismissFinished,
        maxHeight = maxHeight,
        dropdownColors = dropdownColors,
        renderInRootScaffold = renderInRootScaffold,
        collapseOnSelection = collapseOnSelection,
        liquidGlassBackdrop = liquidGlassBackdrop,
        onFractionProgress = onFractionProgress,
        collapseSize = collapseSize,
        collapseExtra = collapseExtra,
        collapseContent = collapseContent,
        dragSelectState = dragSelectState,
    )
}

/**
 * 多个 [DropdownEntry] 组的弹窗。
 *
 * @param entries 下拉条目组
 * @param show 是否显示
 * @param onDismiss 关闭回调
 * @param onDismissFinished 关闭动画完成回调
 * @param maxHeight 最大高度
 * @param dropdownColors 下拉颜色
 * @param renderInRootScaffold 是否在根Scaffold中渲染
 * @param collapseOnSelection 选中后是否关闭
 * @param liquidGlassBackdrop 液体玻璃模糊效果的Backdrop
 */
@Composable
fun OverlayDropdownPopup(
    entries: List<DropdownEntry>,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    maxHeight: Dp?,
    dropdownColors: DropdownColors,
    renderInRootScaffold: Boolean,
    collapseOnSelection: Boolean = entries.size <= 1,
    liquidGlassBackdrop: Backdrop? = null,
    onFractionProgress: ((Float) -> Unit)? = null,
    collapseSize: IntSize? = null,
    collapseExtra: DpSize = DpSize.Zero,
    collapseContent: (@Composable () -> Unit)? = null,
    /** 跟手滑选状态。为 null 时该弹窗不启用跟手选择。 */
    dragSelectState: DropdownPanelDragSelectState? = null,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val currentEntries by rememberUpdatedState(entries)
    val currentCollapseOnSelection by rememberUpdatedState(collapseOnSelection)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentHapticFeedback by rememberUpdatedState(hapticFeedback)
    val onItemClicked: (Int, Int) -> Unit = remember {
        { entryIdx, itemIdx ->
            currentHapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            currentEntries.getOrNull(entryIdx)?.let { entry ->
                entry.items.getOrNull(itemIdx)?.onClick?.invoke()
            }
            if (currentCollapseOnSelection) {
                currentOnDismiss()
            }
        }
    }
    OverlayListPopup(
        show = show,
        alignment = PopupPositionProvider.Align.End,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
        maxHeight = maxHeight,
        renderInRootScaffold = renderInRootScaffold,
        liquidGlassBackdrop = liquidGlassBackdrop,
        onFractionProgress = onFractionProgress,
        collapseSize = collapseSize,
        collapseExtra = collapseExtra,
        collapseContent = collapseContent,
        dragSelectState = dragSelectState,
    ) {
        // onFitsOnScreen 必须写在前面：content 是尾随 lambda，放后面就绑不上了
        ListPopupColumn(
            onFitsOnScreen = { dragSelectState?.fitsOnScreen = it },
        ) {
            DropdownEntriesPopupContent(
                entries = entries,
                dropdownColors = dropdownColors,
                onItemClick = onItemClicked,
            )
        }
    }
}
