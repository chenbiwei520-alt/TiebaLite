package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadStaggeredGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import top.yukonga.miuix.kmp.basic.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ConcernPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: ConcernViewModel = hiltViewModel(),
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isRefreshing,
        initial = true
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isEmpty,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::error,
        initial = null
    )

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    LaunchedFabStateEffect(gridState, onHideFab, isRefreshing, isError = error != null)

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigateDebounced)
    }

    ConsumeThreadPageResult<Destination.Main>(navigator, viewModel::onThreadResult)

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing && isEmpty,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isLoadingMore = uiState.isLoadingMore
            val data = uiState.data

            SwipeUpLazyLoadStaggeredGrid(
                modifier = modifier.fillMaxSize(),
                state = gridState,
                columns = StaggeredGridCells.Adaptive(400.dp),
                contentPadding = contentPadding,
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                isLoading = isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                bottomIndicator = defaultBottomIndicator,
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { i, thread ->
                    // One Miuix group card per feed item, no divider lines between them
                    Card(modifier = Modifier.fillMaxWidth()) {
                        FeedCard(
                            thread = thread,
                            onClick = threadClickListeners.onClicked,
                            onLike = viewModel::onThreadLikeClicked,
                            onClickReply = threadClickListeners.onReplyClicked,
                            onClickUser = threadClickListeners.onAuthorClicked,
                            onClickForum = threadClickListeners.onForumClicked,
                        )
                    }
                }
            }
        }
    }
}
