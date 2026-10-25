package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.consumeResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFrom
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResultKey
import com.huanchengfly.tieba.post.ui.page.thread.ThreadSortType
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import top.yukonga.miuix.kmp.basic.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ThreadStorePage(
    navigator: NavController,
    viewModel: ThreadStoreViewModel = hiltViewModel()
) {
    val barBackdrop = rememberMiuixBarBackdrop()

    MyScaffold(
        topBar = {
            TopAppBarPaged(
                title = {
                    Text(text = stringResource(id = R.string.title_my_collect))
                },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                // Counterweight matching the back button so the centered title stays on the
                // true screen center
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                backdrop = barBackdrop,
            )
        },
    ) { contentPadding ->
        // FeedCard renders its body through PbContentText, which reads LocalNavController.current
        // to resolve @user / @url annotations. Without this provider the collection page throws
        // "No navigator is available" as soon as the first card is composed.
        ProvideNavigator(navigator = navigator) {
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current

        val isRefreshing by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isRefreshing,
            initial = false
        )
        val isEmpty by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::isEmpty,
            initial = true
        )

        val error by viewModel.uiState.collectPartialAsState(
            prop1 = ThreadStoreUiState::error,
            initial = null
        )

        viewModel.uiEvent.collectUiEventWithLifecycle { event ->
            val message = when(event) {
                is ThreadStoreUiEvent -> event.toMessage(context)

                is CommonUiEvent.Toast -> event.message.toString()

                else -> Unit
            }
            if (message is String) {
                snackbarHostState.newestSnackbarData()?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }

        StateScreen(
            isEmpty = isEmpty,
            isLoading = isRefreshing,
            error = error,
            onReload = viewModel::onRefresh,
            screenPadding = contentPadding,
        ) {
            val isLoadingMore by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::isLoadingMore,
                initial = false
            )
            val hasMore by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::hasMore,
                initial = true
            )
            val data by viewModel.uiState.collectPartialAsState(
                prop1 = ThreadStoreUiState::data,
                initial = emptyList()
            )

            val habit = LocalHabitSettings.current

            // Initialize click listeners now
            val onUserClicked: (Author, String) -> Unit = { author, extraKey ->
                val route = author.run { UserProfile(id, avatarUrl, name, transitionKey = extraKey) }
                navigator.navigateDebounced(route)
            }

            val onThreadClicked: (ThreadStore) -> Unit = { thread ->
                navigator.navigateDebounced(
                    route = Thread(
                        threadId = thread.id,
                        postId = thread.markPid,
                        seeLz = habit.favoriteSeeLz,
                        sortType = if (habit.favoriteDesc) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT,
                        from = ThreadFrom.Store(maxPid = thread.maxPid, maxFloor = thread.postNo)
                    )
                )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::onRefresh,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                SwipeUpLazyLoadColumn(
                    modifier = Modifier.fillMaxSize().miuixLayerBackdrop(barBackdrop),
                    // Match the feed pages (ConcernPage / ForumThreadListPage / TopicDetailPage):
                    // 16dp horizontal gutter off the screen edge and 20dp between cards.
                    contentPadding = contentPadding + PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    isLoading = isLoadingMore,
                    onLoad = viewModel::onLoadMore,
                    onLazyLoad = viewModel::onLoadMore.takeIf { hasMore },
                    bottomIndicator = {
                        LoadMoreIndicator(noMore = !hasMore, onThreshold = it)
                    }
                ) {
                    items(items = data, key = { it.id }) { info ->
                        // One Miuix group card per stored thread, rendered like a feed item
                        val threadItem = info.run {
                            ThreadItem(
                                id = id,
                                author = author,
                                isDeleted = isDeleted,
                                title = title,
                                content = AnnotatedString(title),
                                lastTimeMill = lastTimeMill,
                                replyNum = count,
                                simpleForum = SimpleForum(-1L, forumName, null),
                            )
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            // LongClickMenu only supplies the long-press affordance here. Taps are
                            // owned by FeedCard's own Card, so passing onClick to both would fire
                            // onThreadClicked twice per tap.
                            LongClickMenu(
                                menuContent = {
                                    TextMenuItem(
                                        text = R.string.title_collect_on,
                                        onClick = { viewModel.onDelete(info) }
                                    )
                                }
                            ) {
                                FeedCard(
                                    thread = threadItem,
                                    onClick = { onThreadClicked(info) },
                                    onLike = {},
                                    onClickReply = { onThreadClicked(info) },
                                    onClickUser = { onUserClicked(info.author, info.id.toString()) },
                                    onClickForum = {
                                        navigator.navigateDebounced(route = Forum(forumName = info.forumName))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            navigator.consumeResult<Destination.ThreadStore, ThreadResult>(ThreadResultKey)?.run {
                viewModel.onThreadResult(threadId, markedPostId)
            }
        }
        }
    }
}
