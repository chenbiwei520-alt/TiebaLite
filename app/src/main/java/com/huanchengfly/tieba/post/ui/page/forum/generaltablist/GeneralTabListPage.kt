package com.huanchengfly.tieba.post.ui.page.forum.generaltablist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.api.models.protos.FrsTabInfo
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.forum.generaltablist.GeneralTabListViewModel.Companion.GeneralTabListVMFactory
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.forumThreadList
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumListTopSpacing
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadStaggeredGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentListOf
import java.util.Objects

@Composable
fun GeneralTabListPage(
    forumId: Long,
    forumName: String,
    initialSortType: Int,
    navTabInfo: FrsTabInfo,
    threadClickListeners: ThreadClickListeners,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues.Zero,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    viewModel: GeneralTabListViewModel = hiltViewModel<GeneralTabListViewModel, GeneralTabListVMFactory>(
        key = Objects.hash(forumId, forumName, navTabInfo.tabId).toString()
    ) {
        it.create(forumName, forumId, navTabInfo, initialSortType)
    },
) {
    val navigator = LocalNavController.current

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::isRefreshing,
        initial = false
    )
    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::threads,
        initial = persistentListOf()
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = GeneralTabListUiState::error,
        initial = null
    )

    onGlobalEvent<GeneralTabListUiEvent.SortTypeChanged>(filter = { it.tabId == navTabInfo.tabId }) {
        viewModel.onSortTypeChanged(sortType = it.sortType)
    }

    onGlobalEvent<GeneralTabListUiEvent.Refresh>(filter = { it.tabId == navTabInfo.tabId }) {
        viewModel.onRefresh()
    }

    ConsumeThreadPageResult<Destination.Forum>(navigator, viewModel::onThreadResult)

    StateScreen(
        isEmpty = threadList.isEmpty(),
        isLoading = isRefreshing,
        error = error,
        screenPadding = contentPadding,
    ) {
        val hideBlocked by viewModel.hideBlocked.collectAsStateWithLifecycle()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val isWideWindow = LocalWindowAdaptiveInfo.current.windowSizeClass
            .isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

        Container(fluid = isWideWindow) {
            SwipeUpLazyLoadStaggeredGrid(
                modifier = modifier.fillMaxSize(),
                state = gridState,
                columns = StaggeredGridCells.Adaptive(400.dp),
                // Same card grid metrics as the 最新/精华 tab: 16dp side margins, a top inset so the
                // first card clears the tab row, and 16dp between cards. Those three were missing
                // here, which is why every custom tab's cards hugged the screen edge and each other.
                contentPadding = contentPadding + PaddingValues(
                    start = 16.dp,
                    top = ForumListTopSpacing,
                    end = 16.dp,
                ),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                isLoading = uiState.isLoadingMore,
                onLoad = viewModel::loadMore,
                onLazyLoad = viewModel::loadMore.takeIf { uiState.hasMore },
                bottomIndicator = {
                    LoadMoreIndicator(noMore = !uiState.hasMore, onThreshold = it)
                }
            ) {
                if (navTabInfo.sub_tab_list.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        LazyRow(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items = navTabInfo.sub_tab_list, key = { it.class_id }) { menu ->
                                Chip(text = menu.class_name)
                            }
                        }
                    }
                }

                forumThreadList(
                    threads = threadList,
                    threadClickListeners = threadClickListeners,
                    onLikeClicked = viewModel::onThreadLikeClicked,
                    onOriginThreadClicked = {
                        val route = Destination.Thread(threadId = it.tid.toLong(), forumId = it.fid)
                        navigator.navigateDebounced(route)
                    },
                    hideBlocked = hideBlocked,
                )
            }
        }
    }
}
