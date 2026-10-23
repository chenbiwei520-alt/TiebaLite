package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LiquidGlassContainerMenu
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LiquidGlassDropdownMenuItem
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LocalLiquidGlassPopupBackdrop
import com.huanchengfly.tieba.post.ui.miuix.liquidGlassDropdownColors
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.OverlayListPopup
import top.yukonga.miuix.kmp.basic.DropdownImpl
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.ListPopupColumn
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.ListPopupDefaults
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.PopupPositionProvider

/**
 * Dislike action popup.
 *
 * The popup is a **single-action** menu: its only entry is the "不感兴趣" submit row. The old
 * multi-reason checklist (colour/quality/ads/…, block-user, block-forum) is gone — those toggles
 * never changed what the submit actually sent, so the panel only added steps in front of one
 * working action.
 *
 * Conforms to the Miuix dropdown spec the same way the settings pages do:
 *
 *  * the shell is the app's shared [OverlayListPopup] (backdrop blur + lit edge, the same pair
 *    `LiquidGlassDropdownPreference` renders through);
 *  * the one entry is a [DropdownImpl] row — a **direct** child of [ListPopupColumn], so upstream's
 *    own width measurement (children forced to the panel width) and its own `selectable` click
 *    target work exactly as designed; no wrapper hacks are needed for a single-row panel;
 *  * `optionSize = 1` with `index = 0` gives the row the first-and-last vertical padding and fully
 *    rounded corners; `isSelected = false` keeps the check glyph transparent, so the row reads as a
 *    plain menu action.
 *
 * Clicks arrive through [DropdownImpl]'s `onSelectedIndexChange` — that is the callback its
 * internal `selectable` actually invokes (`DropdownItem.onClick` is never wired to the row).
 *
 * @param onDislikeClicked submits the dislike for the current thread.
 */
@Composable
fun Dislike(onDislikeClicked: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // Read the glass sampling layer HERE, in the trigger's composition: a CompositionLocal read
    // inside the popup's content lambda resolves against the popup host's composition and comes
    // back null (same trap `ClickMenu` / `LongClickMenu` document).
val glassBackdrop = LocalLiquidGlassPopupBackdrop.current

    // 触发区在窗口坐标系里的矩形：容器变换菜单靠它把面板摆到触发按钮下方、右缘对齐。
    var triggerBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .onGloballyPositioned { triggerBounds = it.boundsInWindow() }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = { expanded = true },
            ),
    ) {
        Box(
            modifier = Modifier.size(40.dp/** SmallIconButtonTokens.ContainerHeight */),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.btn_more)
            )
        }

        if (glassBackdrop != null) {
            LiquidGlassContainerMenu(
                show = expanded,
                anchor = triggerBounds,
                backdrop = glassBackdrop,
                onDismiss = { expanded = false },
            ) {
                LiquidGlassDropdownMenuItem(
                    text = stringResource(id = R.string.button_submit_dislike),
                    onClick = {
                        onDislikeClicked()
                        expanded = false
                    },
                )
            }
        } else {
            OverlayListPopup(
                show = expanded,
                liquidGlassBackdrop = glassBackdrop,
                alignment = PopupPositionProvider.Align.End,
                popupPositionProvider = ListPopupDefaults.dropdownPositionProvider(horizontalMargin = 16.dp),
                enableWindowDim = false,
                onDismissRequest = { expanded = false },
            ) {
                ListPopupColumn {
                    DropdownImpl(
                        text = stringResource(id = R.string.button_submit_dislike),
                        optionSize = 1,
                        isSelected = false,
                        index = 0,
                        dropdownColors = liquidGlassDropdownColors(),
                        onSelectedIndexChange = {
                            onDislikeClicked()
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
