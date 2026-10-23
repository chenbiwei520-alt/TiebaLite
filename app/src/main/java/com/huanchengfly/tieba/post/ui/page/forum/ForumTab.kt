package com.huanchengfly.tieba.post.ui.page.forum

import android.util.SparseIntArray
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.util.getOrDefault
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.FrsTabInfo
import com.huanchengfly.tieba.post.arch.unsafeLazy
import com.huanchengfly.tieba.post.ui.models.settings.ForumSortType
import com.huanchengfly.tieba.post.ui.widgets.compose.SortMenuButton
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.Options
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

const val TAB_FORUM_LATEST = 0
const val TAB_FORUM_GOOD = 1

/**
 * Horizontal room reserved on BOTH sides of the tab strip.
 *
 * Reserving it symmetrically is what makes the centred pills land on the true screen centre: with
 * the sort button occupying only the trailing side, centring inside the leftover strip would push
 * the pills left by half the button's width. The leading reserve is invisible but keeps the
 * geometry symmetric, and it also stops a scrolling tab list from sliding under the button.
 *
 * Matches the sort button's own footprint: a 24.dp (Sizes.Tiny) icon plus 12.dp of
 * SortMenuButtonPadding on each side. Keep in sync with SortMenuButton.
 */
private val TabStripSidePadding = 48.dp

private val TabSortTypes: Options<Int> by unsafeLazy {
    persistentMapOf(
        ForumSortType.BY_REPLY to R.string.title_sort_by_reply,
        ForumSortType.BY_SEND to R.string.title_sort_by_send
    )
}

@Composable
fun ForumTab(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    tabs: List<FrsTabInfo>,
    sortTypes: SparseIntArray,
    onSortTypeChanged: (sortType: Int) -> Unit,
    backdrop: LayerBackdrop? = null,
) {
    val currentPage = pagerState.currentPage
    val coroutineScope = rememberCoroutineScope()
    val defaultSortType = LocalHabitSettings.current.forumSortType
    val tabNames = remember(tabs) { tabs.map { it.tabName } }

    Box(modifier = modifier.fillMaxWidth()) {
        // The strip is inset on both sides, so centring inside it equals centring on the screen.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TabStripSidePadding),
            contentAlignment = Alignment.Center,
        ) {
            // HyperOS tab row: unselected pills are outlined by Miuix and float over the blurred app
            // bar, the selected one is filled with the Miuix accent color.
            // No weight()/fillMaxWidth() here: the row has to wrap its content to be centreable.
            MiuixTabRow(
                tabs = tabNames,
                selectedTabIndex = currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
                backdrop = backdrop,
            )
        }

        // Sorting only applies to the "latest" tab.
        //
        // The trigger sits on the trailing edge, like the search tab row's: the glass dropdown is a
        // container transform that grows out of the trigger's top-end corner — LiquidGlassDropdownMenu
        // aligns its collapsed 42.dp box at TopEnd, and LiquidGlassContainerMenu pins the panel's right
        // edge to the trigger's. A leading trigger would therefore make the panel morph out of a point
        // a whole panel width (200.dp) to the right of the icon it is supposed to open from.
        SortMenuButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            sortTypes = TabSortTypes,
            currentSortType = {
                val currentTabId = tabs[pagerState.currentPage].tabId
                sortTypes.getOrDefault(currentTabId, defaultSortType)
            },
            onSortTypeChanged = onSortTypeChanged,
            enabled = remember { derivedStateOf { pagerState.currentPage != TAB_FORUM_GOOD } }.value,
        )
    }
}
