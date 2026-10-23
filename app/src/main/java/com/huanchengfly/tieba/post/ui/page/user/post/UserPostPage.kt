package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.models.user.PostContent
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostViewModel.Companion.UserPostVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.squircle.squircleClip
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun UserPostPage(
    uid: Long,
    fluid: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: UserPostViewModel = hiltViewModel<UserPostViewModel, UserPostVmFactory> { it.create(uid) },
) {
    val navigator = LocalNavController.current

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isLoadingMore,
        initial = false
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::isEmpty,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserPostUiState::error,
        initial = null
    )

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = PaddingNone,
    ) {
        // initialize onClick listeners
        val onPostContentClicked: (PostListItem, PostContent) -> Unit = { post, content ->
            val threadId = post.threadId
            val forumId = post.forumId
            val route = if (content.isSubPost) {
                SubPosts(threadId, forumId, subPostId = content.postId)
            } else {
                Thread(threadId, forumId, postId = content.postId, scrollToReply = true)
            }
            navigator.navigateDebounced(route)
        }

        val onOriginThreadClicked: (PostListItem) -> Unit = {
            navigator.navigateDebounced(Thread(threadId = it.threadId))
        }

        Container(fluid = fluid) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val data = uiState.data
            val hasMore = uiState.hasMore

            SwipeUpLazyLoadColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = contentPadding + PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                isLoading = isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeIf { hasMore },
                bottomIndicator = defaultBottomIndicator,
            ) {
                itemsIndexed(data, key = { _, it -> it.lazyListKey }) { _, post ->
                    UserPostItem(
                        post = post,
                        onPostContentClicked = onPostContentClicked,
                        onOriginThreadClicked = onOriginThreadClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun UserPostItem(
    modifier: Modifier = Modifier,
    post: PostListItem,
    onPostContentClicked: (PostListItem, PostContent) -> Unit,
    onOriginThreadClicked: (PostListItem) -> Unit,
) {
    // Deliberately the same design as the notifications list: a Miuix card with one inner inset,
    // the reply time inside the header (under the name), then the quoted thread title as a tinted
    // pill. The former layout stacked the time under the title inside a per-row padding, which read
    // as a different component next to every other reply list.
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                name = post.author.name,
                avatar = post.author.avatarUrl,
                desc = remember(post.contents) { post.contents.firstOrNull()?.timeDesc },
            )

            post.contents.fastForEach { content ->
                Text(
                    text = content.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPostContentClicked(post, content) },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Text(
                text = post.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .squircleClip(SquircleCornerRadius.Small)
                    .clickable { onOriginThreadClicked(post) }
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(8.dp),
                textDecoration = if (post.deleted) TextDecoration.LineThrough else null,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 12.sp,
            )
        }
    }
}
