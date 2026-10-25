package com.huanchengfly.tieba.post.ui.page.forum.threadlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.ForumRuleDetail
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListViewModel.Companion.ForumVMFactory
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadBlockedTip
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import java.util.Objects

/**
 * Extra top inset for the thread list so its first card does not touch the tab row above it: the
 * scaffold padding alone only reserves the top bar's own height.
 */
internal val ForumListTopSpacing = 12.dp

@Composable
private fun TopThreadItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: String = stringResource(id = R.string.content_top),
) {
    Row(
        modifier = modifier
            // Card hosts content in a Column, which gives children minWidth = 0; without this the
            // row would shrink to its intrinsic width inside the card.
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            color = MiuixTheme.colorScheme.onPrimary,
            text = type,
            modifier = Modifier
                // Miuix accent blue, and the same squircle corner geometry the surrounding Card
                // uses (Sizes.CornerRadius) so the chip's curvature matches the card it
                // sits inside instead of clashing with it.
                .squircleBackground(
                    color = MiuixTheme.colorScheme.primary,
                    cornerRadius = Sizes.CornerRadius,
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ForumThreadList(
    modifier: Modifier = Modifier,
    threadClickListeners: ThreadClickListeners,
    forumId: Long,
    forumName: String,
    type: ForumType,
    initialSortType: Int,
    forumRuleTitle: String?,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    viewModel: ForumThreadListViewModel = hiltViewModel<ForumThreadListViewModel, ForumVMFactory>(
        key = Objects.hash(forumId, forumName, type).toString()
    ) {
        it.create(forumName, forumId, type, initialSortType)
    }
) {
    val navigator = LocalNavController.current

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    onGlobalEvent<ForumThreadListUiEvent.Refresh>(
        filter = { it.type == type },
    ) {
        viewModel.onRefresh()
    }

    if (type == ForumType.Good) {
        onGlobalEvent<ForumThreadListUiEvent.ClassifyChanged> {
            viewModel.onClassifyIdChanged(classifyId = it.goodClassifyId)
        }
    } else {
        onGlobalEvent<ForumThreadListUiEvent.SortTypeChanged> {
            viewModel.onSortTypeChanged(sortType = it.sortType)
        }
    }

    ConsumeThreadPageResult<Destination.Forum>(navigator, viewModel::onThreadResult)

    val threadList by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::threads,
        initial = emptyList()
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::isRefreshing,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumThreadListUiState::error,
        initial = null
    )

    StateScreen(
        isEmpty = threadList.isEmpty(),
        isLoading = isLoading,
        error = error,
        screenPadding = contentPadding,
    ) {
        val hideBlocked by viewModel.hideBlocked.collectAsStateWithLifecycle()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        Container {
            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                // The scaffold padding only accounts for the top bar's height, so without this extra
                // top inset the first card butts straight up against the tab row.
                contentPadding = contentPadding + PaddingValues(
                    start = 16.dp,
                    top = ForumListTopSpacing,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                isLoading = uiState.isLoadingMore,
                onLoad = viewModel::loadMore,
                onLazyLoad = viewModel::loadMore.takeIf { uiState.hasMore },
                bottomIndicator = {
                    LoadMoreIndicator(noMore = !uiState.hasMore, onThreshold = it)
                }
            ) {
                forumRuleTitle?.let { rule ->
                    if (rule.isEmpty()) return@let
                    item(key = "ForumRule") {
                        // Same Miuix group card as the feed items below
                        Card(modifier = Modifier.fillMaxWidth()) {
                            TopThreadItem(
                                title = rule,
                                onClick = { navigator.navigateDebounced(ForumRuleDetail(forumId)) },
                                type = stringResource(id = R.string.desc_forum_rule)
                            )
                        }
                    }
                }

                forumThreadList(
                    threads = threadList,
                    threadClickListeners = threadClickListeners,
                    onLikeClicked = viewModel::onThreadLikeClicked,
                    onOriginThreadClicked = {
                        val route = Thread(threadId = it.tid.toLong(), forumId = it.fid)
                        navigator.navigateDebounced(route)
                    },
                    hideBlocked = hideBlocked,
                )
            }
        }
    }
}

fun LazyListScope.forumThreadList(
    threads: List<ThreadItem>,
    threadClickListeners: ThreadClickListeners,
    onLikeClicked: (ThreadItem) -> Unit,
    onOriginThreadClicked: (OriginThreadInfo) -> Unit = {},
    hideBlocked: Boolean = false,
) {
    itemsIndexed(threads, key = { _, it -> it.id }, ThreadContentType) { _, thread ->
        // Top threads are non-blockable
        if (thread.isTop) {
            // Same Miuix group card as the feed items below, so pinned threads read as part of the
            // list instead of floating above it. The trailing divider that used to separate the two
            // groups is gone: the card boundary already does that job.
            Card(modifier = Modifier.fillMaxWidth()) {
                TopThreadItem(
                    title = thread.title,
                    onClick = { threadClickListeners.onClicked(thread) }
                )
            }
        } else {
            BlockableContent(
                blocked = thread.blocked,
                blockedTip = { ThreadBlockedTip(verticalPadding = 8.dp) },
                hideBlockedContent = hideBlocked
            ) {
                // One Miuix group card per feed item, no divider lines
                Card(modifier = Modifier.fillMaxWidth()) {
                    FeedCard(
                        thread = thread,
                        onClick = threadClickListeners.onClicked,
                        onLike = onLikeClicked,
                        onClickReply = threadClickListeners.onReplyClicked,
                        onClickUser = threadClickListeners.onAuthorClicked,
                        onClickOriginThread = onOriginThreadClicked,
                    )
                }
            }
        }
    }
}