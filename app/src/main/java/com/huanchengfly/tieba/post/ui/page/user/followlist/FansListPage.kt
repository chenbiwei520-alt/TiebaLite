package com.huanchengfly.tieba.post.ui.page.user.followlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.models.user.ConcernType
import com.huanchengfly.tieba.post.ui.models.user.FollowUser
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MoreMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixPinnedScrollBehavior
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

@Composable
fun FansListPage(
    uid: Long = 0,
    navigator: NavController,
    viewModel: FansListViewModel = hiltViewModel(),
) {
    val showActions = (uid == 0L || uid == LocalAccount.current?.uid)
    val lazyListState = rememberLazyListState()
    val scrollBehavior = rememberMiuixPinnedScrollBehavior()
    val snackbarHostState = rememberSnackbarHostState()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val uiMessage = when (it) {
            is FollowListUiEvent.FollowFailed -> getString(R.string.toast_like_failed, it.message)

            is FollowListUiEvent.UnfollowFailed -> getString(R.string.toast_unlike_failed, it.message)

            is CommonUiEvent.Toast -> it.message.toString()

            else -> Unit
        }
        if (uiMessage is String) {
            snackbarHostState.newestSnackbarData()?.dismiss()
            snackbarHostState.showSnackbar(uiMessage)
        }
    }

    MyScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.title_fans_list))
                },
                navigationIcon = { BackNavigationIcon(onBackPressed = navigator::navigateUp) },
                actions = {
                    ClickMenu(
                        menuContent = {
                            TextIconMenuItem(
                                text = stringResource(R.string.btn_refresh),
                                icon = MiuixIcons.Refresh
                            ) {
                                lazyListState.requestScrollToItem(0)
                                scrollBehavior.state.contentOffset = 0f
                                viewModel.onRefresh()
                            }

                            TextIconMenuItem(
                                text = stringResource(R.string.btn_back_to_top),
                                icon = Icons.Rounded.VerticalAlignTop,
                                onClick = {
                                    lazyListState.requestScrollToItem(0)
                                    scrollBehavior.state.contentOffset = 0f
                                }
                            )
                        },
                        triggerShape = CircleShape,
                        triggerIcon = Icons.Rounded.MoreVert,
                        triggerContentDescription = stringResource(id = R.string.btn_more),
                        content = MoreMenuItem,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
        backgroundColor = MaterialTheme.colorScheme.background,
    ) { contentPaddings ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        StateScreen(
            isEmpty = uiState.users.isEmpty(),
            isLoading = uiState.isRefreshing,
            error = uiState.error,
            onReload = viewModel::onRefresh,
            screenPadding = contentPaddings,
        ) {
            FollowList(
                data = uiState.users,
                tipsText = null,
                countText = stringResource(id = R.string.text_fans_list_count, uiState.totalCount),
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = contentPaddings + PaddingValues(bottom = 16.dp),
                state = lazyListState,
                scrollBehavior = scrollBehavior,
                isLoading = {
                    uiState.isLoadingMore
                },
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                onClick = {
                    navigator.navigate(Destination.UserProfile(uid = it.uid))
                },
                onFollowActionClicked = { user: FollowUser ->
                    when (user.concernType) {
                        ConcernType.FOLLOWING,
                        ConcernType.MUTUAL -> viewModel.onUnfollowClicked(user)
                        ConcernType.NONE,
                        ConcernType.FANS -> viewModel.onFollowClicked(user)
                        else -> Unit
                    }
                }.takeIf { showActions }
            )
        }
    }
}