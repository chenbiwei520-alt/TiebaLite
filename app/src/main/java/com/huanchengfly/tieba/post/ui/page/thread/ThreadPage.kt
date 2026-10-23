package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalHabitSettings
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.theme.LikeRed
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.LikeZero
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.page.Destination.Forum
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.setResult
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStoreUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultFloatingActionButtonMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultToggleFloatingActionButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PromptDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.StickyHeaderOverlay
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.fixedTopBarPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PreferenceGroupPosition
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.useStickyHeaderWorkaround
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.trace
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixPinnedScrollBehavior
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixEnterAlwaysScrollBehavior
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Reply
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

/**
 * 底部余量，避免最后一条回复被右下角的 FAB 压住（FAB 60dp + 16dp 外边距 + 阴影）。
 * */
private val ThreadFabClearance = 88.dp

const val ThreadResultKey = "THREAD_PAGE"

private fun createResult(threadId: Long, like: Like?, markedPostId: Long?): ThreadResult? {
    return if (like != null) {
        ThreadResult(threadId, liked = like.liked, likes = like.count, markedPostId = markedPostId)
    } else {
        null
    }
}

private fun LazyListState.middleVisiblePost(uiState: ThreadUiState): PostData? = layoutInfo.run {
    var postItem = visibleItemsInfo.getOrNull(visibleItemsInfo.size / 2)
    if (postItem == null || postItem.contentType !== Type.Post) {
        // Not found, search last visible post
        postItem = visibleItemsInfo.lastOrNull { it.contentType === Type.Post } ?: return uiState.firstPost
    }
    // item key is Post ID
    val postId = postItem.key as Long
    return uiState.data.fastFirstOrNull { p -> p.id == postId } ?: uiState.firstPost
}

@Composable
fun ThreadPage(
    threadId: Long,
    postId: Long = 0,
    extra: ThreadFrom? = null,
    navigator: NavController,
    viewModel: ThreadViewModel,
) = trace(MacrobenchmarkConstant.TRACE_THREAD) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = rememberSnackbarHostState()
    val useStickyHeader = LocalHabitSettings.current.stickyHeader
    val useStickyHeaderWorkaround = useStickyHeaderWorkaround()

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isEmpty by remember {
        derivedStateOf { state.data.isEmpty() && state.firstPost == null }
    }

    val lazyListState = rememberLazyListState()
    val topAppBarScrollBehavior = if (useStickyHeader) {
        rememberMiuixPinnedScrollBehavior()
    } else {
        rememberMiuixEnterAlwaysScrollBehavior()
    }

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val openBottomSheet: () -> Unit = { showBottomSheet = true }
    val closeBottomSheet: () -> Unit = { showBottomSheet = false }

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val message = when (it) {
            is CommonUiEvent.Toast -> it.message.toString()

            is CommonUiEvent.NavigateUp -> navigator.navigateUp()

            is ThreadUiEvent.DeletePostFailed -> getString(R.string.toast_delete_failure, it.message)

            is ThreadUiEvent.DeletePostSuccess -> getString(R.string.toast_delete_success)

            is ThreadUiEvent.ScrollToFirstReply -> lazyListState.scrollToItem(1)

            is ThreadUiEvent.ScrollToLatestReply -> {
                if (state.sortType != ThreadSortType.BY_DESC) {
                    lazyListState.animateScrollToItem(2 + state.data.size)
                } else {
                    lazyListState.animateScrollToItem(1)
                }
            }

            // Workaround for broken scroll position preservation
            is ThreadUiEvent.LoadPreviousSuccess -> {
                val nonDataItems = if (state.pageData.hasPrevious) 3 else 2 // FirstPost + StickyHeader + PreviousButton
                lazyListState.scrollToItem(nonDataItems + it.previousIndex, it.offset)
            }

            is ThreadUiEvent.LoadSuccess -> {
                if (it.postId != 0L || it.page > 1) {
                    lazyListState.animateScrollToItem(1)
                } else {
                    // Scroll to bottom when sorting by DESC
                    val index = if (state.sortType != ThreadSortType.BY_DESC) 1 else 2 + state.data.size
                    lazyListState.animateScrollToItem(index)
                }
            }

            is ThreadUiEvent.ToReplyDestination -> navigator.navigateDebounced(it.direction)

            is ThreadUiEvent.ToSubPostsDestination -> navigator.navigateDebounced(it.direction)

            is ThreadLikeUiEvent -> it.toMessage(context)

            is ThreadStoreUiEvent -> it.toMessage(context)

            else -> Unit
        }
        if (message is String) {
            snackbarHostState.newestSnackbarData()?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    onGlobalEvent<GlobalEvent.ReplySuccess>(filter = { it.threadId == threadId }) { event ->
        viewModel.requestLoadMyLatestReply(event.newPostId)
    }

    if (extra != null && extra is ThreadFrom.Store && extra.maxPid != postId) {
        CollectionsUpdatedSnack(snackbarHostState, extra) {
            viewModel.requestLoad(page = 0, postId = extra.maxPid)
        }
    }

    var newMarkedCollectionPost: PostData? by remember { mutableStateOf(null) }

    newMarkedCollectionPost?.let {
        CollectionsUpdateDialog(
            markedPost = it,
            onUpdate = viewModel::updateCollections,
            onBack = navigator::navigateUp
        )
    }

    val markedDeletionPost: PostData? by viewModel.deletePost.collectAsStateWithLifecycle()
    ThreadOrPostDeleteDialog(
        deletePost = markedDeletionPost,
        firstPost = state.firstPost,
        onConfirm = viewModel::onDeleteConfirmed,
        onCancel = viewModel::onDeleteCancelled
    )

    val jumpToPageDialogState = rememberDialogState()
    PromptDialog(
        onConfirm = {
            viewModel.requestLoad(it.toInt())
        },
        dialogState = jumpToPageDialogState,
        keyboardType = KeyboardType.Number,
        isError = {
            it.isEmpty() || (it.toIntOrNull() ?: -1) !in 1..state.pageData.total
        },
        title = { Text(text = stringResource(id = R.string.title_jump_page)) },
        content = {
            with(state.pageData) {
                Text(text = stringResource(R.string.tip_jump_page, current, total))
            }
        }
    )

    val onRefreshClicked: () -> Unit = {
        viewModel.requestLoad(0, postId)
    }

    state.thread?.let { thread ->
        LaunchedEffect(thread.like, thread.collectMarkPid, newMarkedCollectionPost?.id) {
            val markedPostId = newMarkedCollectionPost?.id ?: thread.collectMarkPid
            navigator.setResult(ThreadResultKey, createResult(threadId, thread.like, markedPostId))
        }
    }

    val onBackPressedCallback: () -> Unit = {
        if (showBottomSheet) {
            closeBottomSheet()
        } else {
            val lastVisiblePost = lazyListState.middleVisiblePost(state)
            // 更新收藏楼层
            val collectMarkPid: Long? = state.thread?.collectMarkPid
            val newCollectMarkPid: Long? = lastVisiblePost?.id
            if (collectMarkPid != null && collectMarkPid != newCollectMarkPid) {
                // Show CollectionsUpdateDialog now
                newMarkedCollectionPost = lastVisiblePost
            } else {
                navigator.navigateUp()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val lastVisiblePost = lazyListState.middleVisiblePost(state)
            viewModel.onSaveHistory(lastVisiblePost)
        }
    }

    state.thread?.collectMarkPid?.let { collectMarkPid ->
        StrongBox {
            val interceptBack by remember {
                derivedStateOf {
                    showBottomSheet || collectMarkPid != lazyListState.middleVisiblePost(state)?.id
                }
            }
            BackHandler(enabled = interceptBack, onBack = onBackPressedCallback)
        }
    }

    StateScreen(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        isEmpty =  isEmpty,
        isLoading = state.isRefreshing,
        error = state.error,
        onReload = onRefreshClicked,
    ) {
        val barBackdrop = rememberMiuixBarBackdrop()

        MyScaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        state.forum?.let { forum ->
                            ForumTitleChip(forum = forum) {
                                navigator.navigateDebounced(route = Forum(forumName = forum.second))
                            }
                        }
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = onBackPressedCallback)
                    },
                    actions = {
                        val scrollToTopVisible by remember { // Not on top
                            derivedStateOf { lazyListState.canScrollBackward }
                        }
                        FadedVisibility(visible = scrollToTopVisible) {
                            ActionItem(
                                icon = Icons.Rounded.VerticalAlignTop,
                                contentDescription = R.string.btn_back_to_top
                            ) {
                                if (scrollToTopVisible) {
                                    coroutineScope.launch { lazyListState.scrollToItem(0) }
                                    topAppBarScrollBehavior.state.contentOffset = 0f
                                }
                            }
                        }
                    },
                    scrollBehavior = topAppBarScrollBehavior,
                    backdrop = barBackdrop
                ) {
                    if (useStickyHeaderWorkaround && state.thread?.replyNum != null) {
                        Container {
                            StickyHeaderOverlay(state = lazyListState) {
                                ThreadHeader(uiState = state, viewModel = viewModel)
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                ThreadFabMenu(
                    // 列表在滚（拖动或惯性）时把 FAB 收起来，停下再回来：它正好压在最后一条回复上。
                    visible = !lazyListState.isScrollInProgress,
                    onClickReply = viewModel::onReplyThread.takeUnless { viewModel.hideReply },
                    onClickMore = openBottomSheet,
                    onJumpPage = jumpToPageDialogState::show,
                    like = state.thread?.like ?: LikeZero,
                    onLiked = viewModel::onThreadLikeClicked,
                )
            },
            snackbarHostState = snackbarHostState,
            snackbarHost = { SwipeToDismissSnackbarHost(hostState = snackbarHostState) },
            backgroundColor = Color.Transparent,
        ) { padding ->
            // Ignore Scaffold padding top changes if workaround enabled
            // 底部原来由 bottomBar 的工具栏占位；换成 FAB 后 Scaffold 不再为它留高度，
            // 这里补一段等同于 FAB 的余量，否则最后一条回复会被 FAB 压住。
            val contentPadding = padding.fixedTopBarPadding() + PaddingValues(bottom = ThreadFabClearance)

            // The blur capture must be FULL WIDTH.
            //
            // `Container` narrows its child to a centred column on wide screens (75% once the window
            // is at least `WIDTH_DP_EXPANDED_LOWER_BOUND` — i.e. any landscape phone). The app bar's
            // progressive-blur band, on the other hand, spans the whole screen width. Attaching the
            // capture to `ThreadContent` therefore made the captured layer narrower than the blur
            // band, and `progressiveTextureBlur`'s shader clamps every out-of-bounds sample to the
            // capture's edge (`clamp(xy, 0.5, in_maxCoord)`): the column's edge colour got smeared
            // across both side margins as flat slabs with hard vertical seams. Portrait was never
            // affected because the fraction there is 1f and the two widths agree.
            // The capture also needs to own the page background: on wide screens `Container` leaves
            // the side margins empty, and a capture with nothing in them makes the blur band's tint
            // the only thing drawn there — a tonal step against the blurred content right at the
            // column edge. Painting the page's own surface into the capture gives the band a real
            // background to blur in the margins, so the ramp stays continuous across the width.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .miuixLayerBackdrop(barBackdrop)
            ) {
                Container(modifier = Modifier.clipToBounds()) {
                    ProvideNavigator(navigator = navigator) {
                        ThreadContent(
                            modifier = Modifier
                                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                            viewModel = viewModel,
                            lazyListState = lazyListState,
                            contentPadding = contentPadding,
                            topAppBarScrollBehavior = topAppBarScrollBehavior,
                            useStickyHeader = useStickyHeader && !useStickyHeaderWorkaround
                        )
                    }
                }
            }

            val isMyThread by remember(state.lz) {
                derivedStateOf { state.user != null && state.lz?.id == state.user?.id }
            }
            val isDesc by remember { derivedStateOf { state.sortType == ThreadSortType.BY_DESC } }

            OverlayBottomSheet(
                show = showBottomSheet,
                onDismissRequest = closeBottomSheet,
            ) {
                ThreadMenu(
                    isSeeLz = state.seeLz,
                    isCollected = state.thread?.collected == true,
                    isImmersiveMode = viewModel.isImmersiveMode,
                    isDesc = isDesc,
                    onSeeLzClick = viewModel::onSeeLzChanged,
                    onCollectClick = {
                        if (state.user == null) {
                            context.toastShort(R.string.title_not_logged_in)
                        } else if (state.thread!!.collected) {
                            viewModel.removeFromCollections()
                        } else {
                            lazyListState.middleVisiblePost(state)?.let { post ->
                                viewModel.updateCollections(markedPost = post)
                            }
                        }
                    },
                    onImmersiveModeClick = {
                        if (!viewModel.isImmersiveMode && !state.seeLz) {
                            viewModel.onSeeLzChanged()
                        }
                        viewModel.onImmersiveModeChanged()
                    },
                    onDescClick = {
                        val notDesc = state.sortType != ThreadSortType.BY_DESC
                        val sortType = if (notDesc) ThreadSortType.BY_DESC else ThreadSortType.DEFAULT
                        viewModel.onSortChanged(sortType)
                    },
                    onShareClick = viewModel::onShareThread,
                    onCopyLinkClick = viewModel::onCopyThreadLink,
                    onReportClick = {
                        coroutineScope.launch {
                            TiebaUtil.reportPost(context, navigator, state.firstPost!!.id.toString())
                        }
                    },
                    onDeleteClick = viewModel::onDeleteThread.takeIf { isMyThread },
                    closeMenu = closeBottomSheet,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ForumTitleChip(forum: SimpleForum, onForumClick: () -> Unit) {
    Surface(
        onClick = onForumClick,
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = forum.second
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .height(intrinsicSize = IntrinsicSize.Min)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = forum.third,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )

            val forumStyle = MaterialTheme.typography.titleMedium
            Text(
                text = stringResource(id = R.string.title_forum, forum.second),
                modifier = Modifier.padding(horizontal = 8.dp),
                autoSize = TextAutoSize.StepBased(8.sp, forumStyle.fontSize),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = forumStyle
            )
        }
    }
}

@Composable
private fun ThreadMenu(
    isSeeLz: Boolean,
    isCollected: Boolean,
    isImmersiveMode: Boolean,
    isDesc: Boolean,
    onSeeLzClick: () -> Unit,
    onCollectClick: () -> Unit,
    onImmersiveModeClick: () -> Unit,
    onDescClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    closeMenu: () -> Unit,
    modifier: Modifier = Modifier,
    ) {
    Column(
        modifier = modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 两处细节决定了「对齐」：一是所有行都用同一套 SegmentedPreference（同一行高、同一图标槽、
        // 同一文字样式），二是去掉外面那层 Card —— PreferenceGroupPosition 自己会按组首/组尾倒圆角，
        // 一组行读起来就是一张连续卡片。以前开关组用 miuix SwitchPreference（自己画 container，
        // 行高 71dp、底色和 Card 不同）、操作组用 BasicComponent（行高 66dp），两套节奏放在一个
        // 面板里，看着就是图标/文字/开关没对上。
        Column {
            ThreadMenuSwitch(
                title = R.string.title_see_lz,
                icon = MiuixIcons.Filter,
                checked = isSeeLz,
                position = PreferenceGroupPosition.First,
                onClick = onSeeLzClick,
            )
            ThreadMenuSwitch(
                title = if (isCollected) R.string.title_collected else R.string.title_uncollected,
                icon = MiuixIcons.Favorites,
                checked = isCollected,
                position = PreferenceGroupPosition.Middle,
                onClick = onCollectClick,
            )
            ThreadMenuSwitch(
                title = R.string.title_pure_read,
                icon = MiuixIcons.Notes,
                checked = isImmersiveMode,
                position = PreferenceGroupPosition.Middle,
                onClick = onImmersiveModeClick,
            )
            ThreadMenuSwitch(
                title = R.string.title_sort,
                icon = MiuixIcons.Sort,
                checked = isDesc,
                position = PreferenceGroupPosition.Last,
                onClick = onDescClick,
            )
        }
        Column {
            ThreadMenuAction(
                title = R.string.title_share,
                icon = MiuixIcons.Share,
                position = PreferenceGroupPosition.First,
                onClick = {
                    closeMenu()
                    onShareClick()
                },
            )
            ThreadMenuAction(
                title = R.string.title_copy_link,
                icon = MiuixIcons.Copy,
                position = PreferenceGroupPosition.Middle,
                onClick = {
                    closeMenu()
                    onCopyLinkClick()
                },
            )
            ThreadMenuAction(
                title = R.string.title_report,
                icon = MiuixIcons.Report,
                position = if (onDeleteClick == null) {
                    PreferenceGroupPosition.Last
                } else {
                    PreferenceGroupPosition.Middle
                },
                onClick = {
                    closeMenu()
                    onReportClick()
                },
            )
            if (onDeleteClick != null) {
                ThreadMenuAction(
                    title = R.string.title_delete,
                    icon = MiuixIcons.Delete,
                    position = PreferenceGroupPosition.Last,
                    onClick = {
                        closeMenu()
                        onDeleteClick()
                    },
                )
            }
        }
    }
}

/**
 * 开关型菜单项：右侧开关只作展示，整行都可以点。
 *
 * 走 [SegmentedPreference] 而不是 miuix 的 SwitchPreference，是为了和设置页共用同一套行版式 —— 图标槽、
 * 标题基线、右侧控件位置都由同一个 BasicComponent 决定，多行并排时自然对齐。
 */
@Composable
private fun ThreadMenuSwitch(
    @StringRes title: Int,
    icon: ImageVector,
    checked: Boolean,
    position: PreferenceGroupPosition,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    SegmentedPreference(
        title = title,
        position = position,
        leadingIcon = icon,
        interactionSource = interactionSource,
        onClick = onClick,
        trailingContent = {
            MiuixSwitch(
                checked = checked,
                enabled = true,
                onCheckedChange = null,
            )
        },
    )
}

@Composable
private fun ThreadMenuAction(
    @StringRes title: Int,
    icon: ImageVector,
    position: PreferenceGroupPosition,
    onClick: () -> Unit,
) {
    SegmentedPreference(
        title = title,
        position = position,
        leadingIcon = icon,
        onClick = onClick,
    )
}

@Composable
private fun CollectionsUpdatedSnack(
    snackbarHostState: SnackbarHostState,
    extra: ThreadFrom.Store,
    onLoadLatest: () -> Unit
    ) {
    var showed by rememberSaveable { mutableStateOf(false) }
    if (showed) return // Display only once

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.message_store_thread_update, extra.maxFloor),
            context.getString(R.string.button_load_new),
            true,
            SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            onLoadLatest()
        }
        showed = true
    }
}

@Composable
private fun CollectionsUpdateDialog(markedPost: PostData, onUpdate: (PostData) -> Unit, onBack: () -> Unit) {
    val updateCollectMarkDialogState = rememberDialogState()
    LaunchedEffect(markedPost) {
        updateCollectMarkDialogState.show()
    }

    if (!updateCollectMarkDialogState.show) return
    ConfirmDialog(
        dialogState = updateCollectMarkDialogState,
        onConfirm = {
            onUpdate(markedPost)
        },
        onDismiss = onBack,
    ) {
        Text(stringResource(R.string.message_update_collect_mark, markedPost.floor))
    }
}

@Composable
private fun ThreadOrPostDeleteDialog(
    deletePost: PostData?,
    firstPost: PostData?,
    onConfirm: () -> Job,
    onCancel: () -> Unit
    ) {
    val dialogState = rememberDialogState()
    LaunchedEffect(deletePost) {
        if (deletePost != null) dialogState.show()
    }

    if (!dialogState.show || firstPost == null) return

    var deleting by remember { mutableStateOf(false) }

    Dialog(
        dialogState = dialogState,
        dialogProperties = AnyPopDialogProperties(
            direction = DirectionState.CENTER,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text(text = stringResource(R.string.title_delete)) },
        buttons = {
            AnimatedVisibility(visible = !deleting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    DialogNegativeButton(text = stringResource(R.string.button_cancel), onClick = onCancel)

                    Button(
                        onClick = {
                            deleting = true
                            onConfirm().invokeOnCompletion {
                                dismiss()
                                deleting = false
                            }
                        },
                        content = { Text(text = stringResource(R.string.button_sure)) }
                    )
                }
            }
        }
    ) {
        if (deletePost == null || deleting) {
            Text(text = stringResource(id = R.string.dialog_content_wait))
        } else {
            val postType = if (deletePost.id == firstPost.id) {
                stringResource(id = R.string.this_thread)
            } else {
                stringResource(R.string.tip_post_floor, deletePost.floor)
            }
            Text(text = stringResource(id = R.string.message_confirm_delete, postType))
        }
    }
}

/**
 * 帖子页右下角的 miuix FAB：点开是「评论 / 跳页 / 点赞 / 更多」四项。
 *
 * 原来底部是一条 Material3 的 FloatingToolbar 长胶囊（头像 + 回复提示 + 三个图标），
 * 与吧内页的圆形 miuix FAB 不是同一套视觉语言，还占掉一整条列表高度。这里换成同一枚
 * [DefaultToggleFloatingActionButton]，把长胶囊上的按钮收进它的展开菜单。
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadFabMenu(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    onClickReply: (() -> Unit)? = null,
    onClickMore: () -> Unit = {},
    onJumpPage: () -> Unit = {},
    like: Like = LikeZero,
    onLiked: () -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // 展开的四项画在 FAB 之外的浮层里，不会跟着按钮一起淡出，所以一滚动就先收起菜单。
    LaunchedEffect(visible) { if (!visible) expanded = false }
    BackHandler(expanded) { expanded = false }

    val visibility by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "threadFabVisibility",
    )

    // 点击菜单项时先收起再执行动作，避免收起动画期间的重复点击。
    val collapse: (() -> Unit) -> Unit = { action ->
        expanded = false
        action()
    }

    FloatingActionButtonMenu(
        // 缩到 70% 再下移一点，收起/恢复共用同一条 spring；scaffold 的槽位高度不变，
        // 所以列表不会被这条动画推来推去。
        modifier = modifier.graphicsLayer {
            alpha = visibility
            scaleX = 0.7f + 0.3f * visibility
            scaleY = scaleX
            translationY = (1f - visibility) * 32.dp.toPx()
        },
        expanded = expanded,
        button = {
            // 和吧内页 FAB 同一枚按钮：+ 号随展开旋转 45° 变成 ×，缩放同步脉冲。
            DefaultToggleFloatingActionButton(
                checked = expanded,
                // 收起状态下按钮只剩 0 透明度，仍可能吃到手指；这一下不展开，等恢复显示再响应。
                onCheckedChange = { if (visible) expanded = it },
                contentDescription = stringResource(id = R.string.tip_reply_thread),
            )
        },
    ) {
        if (onClickReply != null) {
            DefaultFloatingActionButtonMenuItem(
                onClick = { collapse(onClickReply) },
                icon = {
                    Icon(
                        painter = rememberVectorPainter(MiuixIcons.Reply),
                        contentDescription = null,
                    )
                },
                text = { Text(text = stringResource(id = R.string.tip_reply_thread)) },
            )
        }

        DefaultFloatingActionButtonMenuItem(
            onClick = { collapse(onJumpPage) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                )
            },
            text = { Text(text = stringResource(id = R.string.title_jump_page)) },
        )

        DefaultFloatingActionButtonMenuItem(
            onClick = { collapse(onLiked) },
            icon = {
                Icon(
                    imageVector = if (like.liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    // 已经赞过就用红的爱心，和列表卡片、楼中楼保持一致。
                    tint = if (like.liked) LikeRed else LocalContentColor.current,
                )
            },
            text = {
                val likeLabel = stringResource(id = R.string.button_like)
                Text(
                    text = if (like.count > 0) "$likeLabel " + like.count.getShortNumString() else likeLabel
                )
            },
        )

        DefaultFloatingActionButtonMenuItem(
            onClick = { collapse(onClickMore) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                )
            },
            text = { Text(text = stringResource(id = R.string.btn_more)) },
        )
    }
}
