package com.huanchengfly.tieba.post.ui.page.user.thread

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.PbContentText
import com.huanchengfly.tieba.post.ui.miuix.SquircleShape
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.page.user.thread.UserThreadViewModel.Companion.UserThreadVmFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadStaggeredGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadMedia
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import top.yukonga.miuix.kmp.basic.Card

@Composable
fun UserThreadPage(
    uid: Long,
    fluid: Boolean = false,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: UserThreadViewModel = hiltViewModel<UserThreadViewModel, UserThreadVmFactory> { it.create(uid) },
) {
    val context = LocalContext.current
    val navigator = LocalNavController.current

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::isRefreshing,
        initial = true
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::isLoadingMore,
        initial = false
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::isEmpty,
        initial = false
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = UserThreadUiState::error,
        initial = null
    )

    onGlobalEvent<ThreadLikeUiEvent> {
        context.toastShort(it.toMessage(context))
    }

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = PaddingNone,
    ) {
        val threadClickListeners = remember(navigator) {
            createThreadClickListeners(onNavigate = navigator::navigate)
        }

        Container(fluid = fluid) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val data = uiState.data
            val hasMore = uiState.hasMore

            SwipeUpLazyLoadStaggeredGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = StaggeredGridCells.Adaptive(400.dp),
                // Card grid: 16dp side margins + the list's 16dp rhythm, same as the forum tabs.
                contentPadding = contentPadding + PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                isLoading = isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeIf { hasMore },
                bottomIndicator = defaultBottomIndicator,
            ) {
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { _, thread ->
                    // `UserThread` renders its own card, so no wrapper here.
                    UserThread(
                        modifier = Modifier.fillMaxWidth(),
                        thread = thread,
                        onClick = threadClickListeners.onClicked,
                        onClickForum = threadClickListeners.onForumClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserThread(
    modifier: Modifier = Modifier,
    thread: ThreadItem,
    onClick: (ThreadItem) -> Unit,
    onClickForum: (ThreadItem) -> Unit,
) {
    val context = LocalContext.current
    val (forumId, forumName, _) = thread.simpleForum

    // One real Miuix card per post, same design as the notifications / replies cards: opaque
    // surface, 16dp corners, one inner inset (16/12), 8dp between header, body and media. The
    // former `widgets.compose.Card` has NO background of its own — it expected the theme's global
    // `block` modifier to draw the card surface, which nothing installs anymore, so every post
    // rendered as bare content floating on the page background.
    Card(
        modifier = modifier.fillMaxWidth(),
        showIndication = true,
        onClick = { onClick(thread) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                name = thread.author.name,
                avatar = thread.author.avatarUrl,
                desc = remember { DateTimeUtils.getRelativeTimeString(context, thread.lastTimeMill) },
            ) {
                Surface(
                    onClick = { onClickForum(thread) },
                    // Half the enclosing card's radius, so the chip's corners stay concentric with it.
                    shape = SquircleShape(Sizes.CornerRadius / 2),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(id = R.string.title_forum_name, forumName),
                        modifier = Modifier.padding(4.dp),
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (!thread.content.isNullOrEmpty()) {
                PbContentText(
                    text = thread.content,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 15.sp,
                    lineSpacing = 0.8.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            ThreadMedia(
                forumId = forumId,
                forumName = forumName,
                threadId = thread.id,
                medias = thread.medias ?: emptyList(),
                videoInfo = thread.video,
            )
        }
    }
}
@Preview("UserThreadPreview")
@Composable
private fun UserThreadPreview() = TiebaLiteTheme {
    Surface {
        UserThread(
            thread = ThreadItem(
                author = Author(0, name = "User", avatarUrl = ""),
                title = "预览",
                lastTimeMill = System.currentTimeMillis(),
                simpleForum = SimpleForum(-1, "测试", null)
            ),
            onClick = {},
            onClickForum = {},
        )
    }
}
