package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.testColumn
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import top.yukonga.miuix.kmp.basic.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadBlockedTip
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadStaggeredGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PersonalizedPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: PersonalizedViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()

    var refreshCount by remember { mutableIntStateOf(0) }

    viewModel.uiEvent.collectUiEventWithLifecycle {
        when (it) {
            is PersonalizedUiEvent.RefreshSuccess -> coroutineScope.launch {
                gridState.scrollToItem(0, 0)
                refreshCount = it.count // Show refresh tip
                delay(2000)
                refreshCount = 0        // Hide refresh tip
            }

            is PersonalizedUiEvent.BlockRuleUpdated -> toastShort(R.string.toast_block_rule_updated)

            is PersonalizedUiEvent.DislikeFailed -> if (it.e !is TiebaNotLoggedInException) {
                toastShort(R.string.toast_exception, it.e.getErrorMessage())
            }

            is CommonUiEvent.ToastError -> toastShort(R.string.toast_exception, it.message)
        }
    }

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigateDebounced)
    }

    ConsumeThreadPageResult<Destination.Main>(navigator, viewModel::onThreadResult)

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isRefreshing,
        initial = true
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::isEmpty,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = PersonalizedUiState::error,
        initial = null
    )
    val isError = error != null

    LaunchedFabStateEffect(gridState, onHideFab, isRefreshing, isError)

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing && isEmpty, // Only initial load, allow browse existing content on refresh
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val hideBlockedContent by viewModel.hideBlockedContent.collectAsStateWithLifecycle()

            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val data = uiState.data
            val isLoadingMore = uiState.isLoadingMore
            val itemPlacementSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()

            SwipeUpLazyLoadStaggeredGrid(
                modifier = modifier.fillMaxSize().testColumn(),
                state = gridState,
                // Adaptive: one column on phones, a multi-column waterfall on tablets and wider
                // windows.
                columns = StaggeredGridCells.Adaptive(400.dp),
                contentPadding = contentPadding,
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                isLoading = isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeUnless { isRefreshing },
                bottomIndicator = defaultBottomIndicator,
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { index, thread ->
                    val isHidden = thread.blocked && hideBlockedContent
                    trace(MacrobenchmarkConstant.TRACE_FEED_CARD) {
                        BlockableContent(
                            blocked = thread.blocked,
                            blockedTip = { ThreadBlockedTip() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(fadeInSpec = null, placementSpec = itemPlacementSpec, fadeOutSpec = null),
                            hideBlockedContent = hideBlockedContent
                        ) {
                            // One Miuix group card per feed item, no divider lines between them
                            Card(modifier = Modifier.fillMaxWidth()) {
                            FeedCard(
                                thread = thread,
                                onClick = threadClickListeners.onClicked,
                                onLike = viewModel::onThreadLikeClicked,
                                onClickReply = threadClickListeners.onReplyClicked,
                                onClickUser = threadClickListeners.onAuthorClicked,
                                onClickForum = threadClickListeners.onForumClicked,
                                dislikeAction = {
                                    // Single-action popup: submit the dislike without a reason
                                    // checklist (the reason toggles never affected the submission).
                                    Dislike {
                                        viewModel.onThreadDislike(thread, emptyList())
                                    }
                                },
                            )
                            }
                        }
                    }
                }
            }

            // 提示条要贴在整页顶部居中。原来 align 写在 AnimatedVisibility 上，而外层
            // StrongBox 是个只包内容宽度的小 Box（默认 TopStart 摆放），居中只在小盒子内部生效，
            // 于是气泡一直停在左上角。对齐要提到 StrongBox 这一层。
            StrongBox(modifier = Modifier.align(Alignment.TopCenter)) {
                AnimatedVisibility(
                    visible = refreshCount > 0,
                    enter = fadeIn() + slideInVertically(),
                    exit = slideOutVertically() + fadeOut(),
                ) {
                    RefreshTip(
                        modifier = Modifier
                            .padding(contentPadding)
                            .padding(top = 12.dp),
                        refreshCount = refreshCount
                    )
                }
            }
        }
    }
}

@Composable
private fun RefreshTip(modifier: Modifier = Modifier, refreshCount: Int) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Text(
            text = stringResource(id = R.string.toast_feed_refresh, refreshCount),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview("RefreshTip", backgroundColor = 0xFFFFFFFF)
@Composable
private fun RefreshTipPreview() = TiebaLiteTheme {
    Surface {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RefreshTip(refreshCount = Int.MAX_VALUE)
        }
    }
}
