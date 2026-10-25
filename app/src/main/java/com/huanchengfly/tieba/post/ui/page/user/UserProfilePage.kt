package com.huanchengfly.tieba.post.ui.page.user

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.models.database.UserProfile
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.miuix.MiuixProgressiveBlurBar
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isLooseWindowWidth
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.models.user.PermissionList
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.page.user.edit.EditProfileActivity
import com.huanchengfly.tieba.post.ui.page.user.likeforum.UserLikeForumPage
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostPage
import com.huanchengfly.tieba.post.ui.page.user.thread.UserThreadPage
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.MoreMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.DefaultLoadingScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreenScope
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Tab(val titleRes: Int) {
    THREADS(R.string.title_profile_threads),

    POSTS(R.string.title_profile_posts),

    FORUMS(R.string.title_profile_forums);

    fun formatTitle(number: Int, context: Context): String {
        return context.getString(titleRes, if (number > 0) number.getShortNumString() else " ")
    }
}

private typealias TabWithTitle = Pair<Tab, String>

/** The default size of an avatar in [UserProfileDetail] */
private val AvatarLandscapeSize: Dp
    @Composable @ReadOnlyComposable get() = if (isWindowHeightCompact()) 96.dp else 144.dp

/** The avatar size in the [ProfileHeaderBlock] */
private val UserAvatarSize = 88.dp

/** Whether to use landscape layout or not */
private val ContentLandscapeLayout: Boolean
    @Composable @ReadOnlyComposable get() = isLooseWindowWidth()

@Composable
fun UserProfilePage(
    uid: Long,
    avatar: String?,
    nickname: String?,
    username: String?,
    transitionKey: String?,
    navigator: NavController,
    showPostsTab: Boolean = false,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val onBack: () -> Unit = navigator::navigateUp
    val snackbarHostState = rememberSnackbarHostState()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val uiMessage = when (it) {
            is UserProfileUiEvent.FollowSuccess -> it.message

            is UserProfileUiEvent.FollowFailed -> getString(R.string.toast_like_failed, it.message)

            is UserProfileUiEvent.UnfollowFailed -> getString(R.string.toast_unlike_failed, it.message)

            is UserProfileUiEvent.PermissionListException -> getString(R.string.toast_ban_interact_failed, it.message)

            is CommonUiEvent.Toast -> it.message.toString()

            else -> Unit
        }
        if (uiMessage is String) {
            snackbarHostState.newestSnackbarData()?.dismiss()
            snackbarHostState.showSnackbar(uiMessage)
        }
    }

    StateScreen(
        isLoading = uiState.isRefreshing || uiState.userProfile == null,
        error = uiState.error,
        onReload = viewModel::onRefresh,
        loadingScreen = {
            LoadingScreen(
                modifier = Modifier,
                onBack = onBack,
            )
        }
    ) {
        val userProfile = uiState.userProfile ?: return@StateScreen
        val account = LocalAccount.current
        val isSelf = account?.uid == uid
        val blockState by viewModel.blockState.collectAsStateWithLifecycle()
        val onAppealClicked: (() -> Unit)? = {
            navigator.navigate(Destination.WebView("https://c.tieba.baidu.com/mo/q/userappeal"))
        }.takeIf { isSelf }
        val onNavigateFollowList: () -> Unit = {
            navigator.navigate(Destination.UserFollowList(uid))
        }
        val onNavigateFansList: () -> Unit = {
            navigator.navigate(Destination.UserFansList(uid))
        }

        val permList = uiState.permList
        val permissionDialogState = rememberDialogState()
        if (permissionDialogState.show && permList != null) {
            PermissionSettingDialog(
                dialogState = permissionDialogState,
                initialPermissionList = permList,
                isLoading = uiState.isLoadingPermList,
                onConfirm = { updatedBean ->
                    viewModel.setUserBlack(updatedBean)
                        .invokeOnCompletion { permissionDialogState.show = false }
                }
            )
        }

        val tabs: List<TabWithTitle> = remember {
            Tab.entries.mapNotNull {
                when(it) {
                    Tab.THREADS -> it to it.formatTitle(userProfile.thread, context)

                    Tab.POSTS -> (it to it.formatTitle(userProfile.post, context)).takeIf { isSelf }

                    Tab.FORUMS -> it to it.formatTitle(userProfile.forum, context)
                }
            }
        }

        // Land on the 回贴 tab when requested; falls back to the first tab for other users
        // where the posts tab is not part of [tabs]
        val initialPage = if (showPostsTab) {
            tabs.indexOfFirst { it.first == Tab.POSTS }.coerceAtLeast(0)
        } else {
            0
        }
        val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
        val lazyListStates = rememberPagerListStates(tabs.size)
        val pagerContents: List<@Composable (Boolean, PaddingValues) -> Unit> = tabs.mapIndexed { i, (tab, _) ->
            { fluid: Boolean, contentPadding: PaddingValues ->
                val lazyListState = lazyListStates[i]
                when (tab) {
                    Tab.THREADS -> UserThreadPage(uid, fluid, lazyListState, contentPadding)

                    Tab.POSTS -> UserPostPage(uid, fluid, lazyListState, contentPadding)

                    Tab.FORUMS -> when {
                        !isSelf && userProfile.privateForum -> UserPageHide(hiddenTab = tab)

                        userProfile.forum == 0 -> UserPageEmpty()

                        else -> UserLikeForumPage(uid, fluid, lazyListState, contentPadding)
                    }
                }
            }
        }

        // Captured pager content feeds the top bar's progressive blur and the tab pills' glass fill
        val barBackdrop = rememberMiuixBarBackdrop()
        val movablePager = movableContentOf<Modifier, Boolean, PaddingValues> { modifier, fluid, contentPadding ->
            HorizontalPager(
                pagerState,
                (Modifier.fillMaxSize() then modifier).miuixLayerBackdrop(barBackdrop),
                key = { it },
            ) {
                ProvideNavigator(navigator) {
                    pagerContents[it](fluid, contentPadding)
                }
            }
        }

        val contentLandscapeLayout = ContentLandscapeLayout
        // Exit-until-collapsed behavior: the header cards slide away as the pager lists scroll
        // down, and expand back when the list reaches its top
        val scrollBehavior = MiuixScrollBehavior(
            state = rememberTopAppBarState(initialHeightOffsetLimit = 0f),
        )
        val onActionClicked = {
            when {
                isSelf -> context.goToActivity<EditProfileActivity>()

                userProfile.following -> viewModel.onUnFollowClicked()

                else -> viewModel.onFollowClicked()
            }
        }.takeUnless { uiState.isRefreshing || account == null }

        MyScaffold(
            topBar = {
                TopAppBarPaged(
                    title = {},
                    navigationIcon = { AnimatedBackButton(onBackPressed = onBack) },
                    actions = {
                        if (uiState.isRequestingFollow) {
                            CircularProgressIndicator(
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                strokeWidth = 2.5.dp
                            )
                        } else if (onActionClicked != null) {
                            TextButton(onClick = onActionClicked) {
                                Text(
                                    text = when {
                                        isSelf -> stringResource(id = R.string.menu_edit_info)
                                        userProfile.following -> stringResource(id = R.string.button_unfollow)
                                        else -> stringResource(id = R.string.button_follow)
                                    }
                                )
                            }
                        }

                        ClickMenu(
                            menuContent = {
                                TextMenuItem(
                                    text = R.string.btn_refresh,
                                    onClick = viewModel::onRefresh
                                )
                                if (!isSelf) {
                                    val isInBlackList = blockState == UserBlockState.Blacklisted
                                    val isInWhiteList = blockState == UserBlockState.Whitelisted
                                    TextMenuItem (
                                        text = if (isInBlackList) R.string.title_remove_black else R.string.title_add_black,
                                        onClick = viewModel::onUserBlacklisted
                                    )
                                    TextMenuItem(
                                        text = if (isInWhiteList) R.string.title_remove_white else R.string.title_add_white,
                                        onClick = viewModel::onUserWhitelisted
                                    )
                                    if (permList != null) {
                                        TextMenuItem(text = R.string.menu_ban_user, onClick = permissionDialogState::show)
                                    }
                                }
                            },
                            triggerShape = CircleShape,
                            triggerIcon = Icons.Rounded.MoreVert,
                            triggerContentDescription = stringResource(id = R.string.btn_more),
                            content = MoreMenuItem,
                        )
                    },
                    backdrop = barBackdrop,
                ) {
                    if (!contentLandscapeLayout) {
                        // The header cards sit between the title row and the tab row, inside the
                        // blurred bar region, and collapse away as a list scrolls up
                        Box(
                            modifier = Modifier
                                // clip wraps the layout node: the clip region follows the
                                // shrunken bounds, so slid-away content is cut at the bar edge
                                .clipToBounds()
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val collapsed = (-scrollBehavior.state.heightOffset)
                                        .roundToInt()
                                        .coerceIn(0, placeable.height)
                                    layout(placeable.width, placeable.height - collapsed) {
                                        placeable.placeRelative(0, -collapsed)
                                    }
                                }
                        ) {
                            // onSizeChanged observes the NATURAL height here (inside the collapsing
                            // node): observing the shrunken outer size would feed back into
                            // heightOffsetLimit and make the header jitter mid-collapse
                            Column(
                                modifier = Modifier.onSizeChanged {
                                    scrollBehavior.state.heightOffsetLimit = -it.height.toFloat()
                                },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ProfileHeaderBlock(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    profile = userProfile,
                                    block = blockState,
                                    transitionKey = transitionKey,
                                    onNavigateFollowList = onNavigateFollowList,
                                    onNavigateFansList = onNavigateFansList,
                                    onAppealClicked = onAppealClicked,
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Inside the collapsing header so it scrolls away with it
                                // instead of floating over the feed. Landscape renders its own
                                // copy in the content column.
                                UserProfileTabRow(tabs, pagerState, backdrop = barBackdrop)
                            }
                        }
                    }
                }
            },
            snackbarHostState = snackbarHostState,
        ) { paddingValues ->
            if (contentLandscapeLayout) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Reserve the bar's height here: unlike portrait, the landscape header lives
                        // in this column instead of inside the top bar, so without the top inset the
                        // cards and the tab row get drawn underneath the bar. The bottom inset is
                        // handled per pane so both scroll behind the floating gesture handle.
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f)
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    start = 16.dp,
                                    top = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp + paddingValues.calculateBottomPadding(),
                                )
                        ) {
                            // Reuse the portrait header so both orientations share one look:
                            // identity card, stat card, intro card.
                            ProfileHeaderBlock(
                                profile = userProfile,
                                block = blockState,
                                transitionKey = transitionKey,
                                onNavigateFollowList = onNavigateFollowList,
                                onNavigateFansList = onNavigateFansList,
                                onAppealClicked = onAppealClicked,
                            )
                        }

                        // Feed pane: the pager runs full-height behind the tab row so list
                        // content scrolls under the pills, where the progressive blur band
                        // softens it. The pager's own top content padding keeps the first
                        // item below the pills at rest.
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            movablePager(
                                Modifier.fillMaxSize(),
                                true,
                                PaddingValues(
                                    top = TabRowDefaults.TabRowHeight,
                                    bottom = paddingValues.calculateBottomPadding(),
                                ),
                            )

                            MiuixProgressiveBlurBar(
                                backdrop = barBackdrop,
                                blurProgress = { 1f },
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    UserProfileTabRow(
                                        tabs = tabs,
                                        pagerState = pagerState,
                                        modifier = Modifier.align(Alignment.Center),
                                        backdrop = barBackdrop,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Fullscreen pager: the lists start below the (dynamically padded) bar and slide
                // under it once the header has collapsed, where the bar blurs them. The pager
                // itself runs to the bottom edge so content scrolls behind the translucent
                // navigation bar; the lists' content padding keeps the last row clear of it.
                Box(modifier = Modifier.fillMaxSize()) {
                    movablePager(
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        false,
                        paddingValues,
                    )
                }
            }
        }
    }
}

/**
 * Static profile header, matching the "我的" page structure. The information is split across
 * three separate Miuix group cards to keep the layout airy: identity (avatar + names), stats,
 * and intro details.
 */
@Composable
private fun ProfileHeaderBlock(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    block: UserBlockState,
    transitionKey: String?,
    onNavigateFollowList: () -> Unit,
    onNavigateFansList: () -> Unit,
    onAppealClicked: (() -> Unit)?,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Identity card: large avatar with the names beside it
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(
                    modifier = Modifier.size(UserAvatarSize),
                    avatar = remember { StringUtil.getBigAvatarUrl(profile.portrait) },
                    uid = profile.uid,
                    transitionKey = transitionKey,
                )

                Spacer(modifier = Modifier.width(20.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        Nickname(
                            nickname = profile.nickname ?: profile.name,
                            transitionKey = transitionKey,
                            block = block,
                        )
                    }

                    if (profile.nickname != null && profile.name.isNotEmpty()) {
                        Username(username = profile.name, transitionKey = transitionKey, block = block)
                    }
                }
            }
        }

        ProfileStatCard(
            profile = profile,
            onNavigateFollowList = onNavigateFollowList,
            onNavigateFansList = onNavigateFansList,
        )

        // Intro card: signature, verifications, gender / ID / IP chips
        Card(modifier = Modifier.fillMaxWidth()) {
            UserIntro(
                modifier = Modifier.padding(16.dp),
                profile = profile,
                landscape = false,
                onAppealClicked = onAppealClicked,
            )
        }
    }
}

private data class ProfileStatEntry(val label: String, val value: String, val onClick: (() -> Unit)?)

/**
 * Grouped Miuix stat card: 关注 / 粉丝 / 获赞 / 吧龄 in four columns, mirroring the "我的" page
 * stat card. 关注 and 粉丝 navigate to their list pages.
 */
@Composable
private fun ProfileStatCard(
    profile: UserProfile,
    onNavigateFollowList: () -> Unit,
    onNavigateFansList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val stats = remember(profile.follow, profile.fans, profile.agree, profile.tbAge) {
        listOf(
            ProfileStatEntry(
                label = context.getString(R.string.text_stat_follow),
                value = profile.follow.getShortNumString(),
                onClick = onNavigateFollowList,
            ),
            ProfileStatEntry(
                label = context.getString(R.string.text_stat_fans),
                value = profile.fans.getShortNumString(),
                onClick = onNavigateFansList,
            ),
            ProfileStatEntry(
                label = context.getString(R.string.text_stat_agrees),
                value = profile.agree.getShortNumString(),
                onClick = null,
            ),
            ProfileStatEntry(
                label = context.getString(R.string.text_stat_tbage),
                value = context.getString(R.string.text_profile_tb_age, profile.tbAge),
                onClick = null,
            ),
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            stats.forEach { (label, value, onClick) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .onNotNull(onClick) { clickable(onClick = it) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = value,
                        style = MiuixTheme.textStyles.title3,
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileTabRow(
    tabs: List<TabWithTitle>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    MiuixTabRow(
        tabs = tabs.map { it.second },
        selectedTabIndex = pagerState.currentPage,
        onTabSelected = { i ->
            coroutineScope.launch {
                if (abs(pagerState.currentPage - i) > 1) {
                    pagerState.scrollToPage(i)
                } else {
                    pagerState.animateScrollToPage(i)
                }
            }
        },
        // No fillMaxWidth: the row is sized to its content so the surrounding centred parent can
        // centre the tabs. Forcing it to fill made the pills hug the leading edge instead.
        modifier = modifier
            .padding(horizontal = 16.dp),
        backdrop = backdrop,
    )
}

@Composable
private fun UserAvatar(modifier: Modifier = Modifier, avatar: String?, uid: Long, transitionKey: String?) {
    if (avatar.isNullOrEmpty()) {
        Box(modifier = modifier.placeholder(shape = CircleShape))
    } else {
        val context = LocalContext.current
        Avatar(
            modifier = modifier
                .clickableNoIndication {
                    PhotoViewActivity.launchSinglePhoto(context, url = avatar)
                }
                .onNotNull(avatar) { sharedUserAvatar(uid = uid, extraKey = transitionKey) },
            data = avatar
        )
    }
}

@Composable
private fun AnimatedBackButton(modifier: Modifier = Modifier, onBackPressed: () -> Unit) {
    BackNavigationIcon(
        modifier = modifier.animateEnterExit(
            zIndexInOverlay = 0f,
            animatedVisibilityScope = LocalAnimatedVisibilityScope.current,
            sharedTransitionScope = LocalSharedTransitionScope.current,
            enter = fadeIn(),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 150)
            ) + slideOutHorizontally { -it }
        ),
        onBackPressed = onBackPressed
    )
}

@NonRestartableComposable
@Composable
private fun NameText(
    modifier: Modifier = Modifier,
    name: String,
    block: UserBlockState = UserBlockState.None,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = name,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface,
        autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = style.fontSize),
        maxLines = 1,
        overflow = TextOverflow.MiddleEllipsis,
        textDecoration = TextDecoration.LineThrough.takeIf { block == UserBlockState.Blacklisted },
        style = style
    )
}

@Composable
private fun Nickname(
    modifier: Modifier = Modifier,
    nickname: String?,
    transitionKey: String? = null,
    block: UserBlockState = UserBlockState.None
) {
    NameText(
        modifier = modifier.block {
            if (nickname.isNullOrEmpty()) {
                placeholder()
            } else {
                sharedUserNickname(nickname = nickname, extraKey = transitionKey)
            }
        },
        name = nickname ?: stringResource(R.string.app_name),
        block = block,
    )
}

@Composable
private fun Username(
    modifier: Modifier = Modifier,
    username: String,
    transitionKey: String? = null,
    block: UserBlockState = UserBlockState.None,
) {
    NameText(
        modifier = modifier.sharedUsername(username, extraKey = transitionKey),
        name = remember { "(${username})" },
        block = block,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun StatText(modifier: Modifier = Modifier, title: String, num: String) {
    Column (
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(text = num, color = LocalContentColor.current, fontWeight = FontWeight.Bold)
    }
}

/**
 * 显示吧主认证, 大神认证
 * */
@Composable
private fun VerifiedText(modifier: Modifier = Modifier, verify: String) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val tintColor = MaterialTheme.colorScheme.primary
        Icon(
            imageVector = Icons.Rounded.Verified,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tintColor,
        )
        Text(
            text = verify,
            style = MaterialTheme.typography.bodyMedium,
            color = tintColor
        )
    }
}

@Composable
private fun UserProfileDetail(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    block: UserBlockState = UserBlockState.None,
    transitionKey: String? = null,
    landscape: Boolean,
    onNavigateFollowList: () -> Unit = {},
) {
    val isWindowHeightCompact = isWindowHeightCompact()
    val context = LocalContext.current

    val userStats = remember {
        listOf(
            R.string.text_stat_follow to profile.follow.getShortNumString(), // 关注
            R.string.text_stat_fans to profile.fans.getShortNumString(),     // 粉丝
            R.string.text_stat_agrees to profile.agree.getShortNumString(),  // 赞
            R.string.text_stat_tbage to context.getString(R.string.text_profile_tb_age, profile.tbAge), // 吧龄
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = if (isWindowHeightCompact) Alignment.Top else Alignment.CenterVertically
        ),
    ) {
        if (landscape) {
            UserAvatar(
                modifier = Modifier
                    .size(AvatarLandscapeSize)
                    .align(Alignment.CenterHorizontally),
                avatar = remember { StringUtil.getBigAvatarUrl(profile.portrait) },
                uid = profile.uid,
                transitionKey = transitionKey,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (landscape) {
                val nickname = profile.nickname ?: profile.name
                Nickname(nickname = nickname, transitionKey = transitionKey, block = block)
            }

            if (profile.nickname != null && profile.name.isNotEmpty()) {
                Username(username = profile.name, transitionKey = transitionKey, block = block)
            }

            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = if (landscape) Alignment.CenterHorizontally else Alignment.Start
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clipToBounds()
                        .horizontalScroll(rememberScrollState())
                ) {
                    userStats.fastForEachIndexed { i, (titleRes, number) ->
                        StatText(
                            modifier = Modifier.onCase(titleRes == R.string.text_stat_follow) {
                                clickable(onClick = onNavigateFollowList)
                            },
                            title = stringResource(id = titleRes),
                            num = number,
                        )
                        if (i != userStats.lastIndex) {
                            VerticalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAccountBlockedTip(
    modifier: Modifier = Modifier,
    days: Int,
    onAppealClicked: (() -> Unit)? = null
) {
    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Red,
            )
            Text(
                text = if (days < 36500) {
                    stringResource(R.string.profile_account_blocked, days)
                } else {
                    stringResource(R.string.profile_account_blocked_forever)
                },
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
                color = MaterialTheme.colorScheme.error,
            )
            if (onAppealClicked != null) {
                Text(
                    text = "申诉",
                    modifier = Modifier.clickableNoIndication(onClick = onAppealClicked),
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun UserIntro(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    landscape: Boolean,
    onAppealClicked: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val flowArrangement = Arrangement.spacedBy(6.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (profile.blockDays > 0) {
            UserAccountBlockedTip(days = profile.blockDays, onAppealClicked = onAppealClicked)
        }

        Text(
            text = profile.intro ?: stringResource(id = R.string.tip_no_intro),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (landscape) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )

        FlowRow (
            horizontalArrangement = flowArrangement,
            verticalArrangement = flowArrangement
        ) {
            profile.bazuDesc?.let {
                VerifiedText(verify = it)
            }

            profile.newGod?.let {
                VerifiedText(verify = stringResource(id = R.string.text_god_verify, it))
            }

            if (profile.isOfficial) {
                VerifiedText(verify = stringResource(id = R.string.text_official_account))
            }
        }

        FlowRow(
            horizontalArrangement = flowArrangement,
            verticalArrangement = flowArrangement
        ) {
            Chip(text = profile.sex, invertColor = true)
            Chip(
                text = stringResource(id = R.string.text_profile_user_id, profile.tiebaUid),
                appendIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(R.string.menu_copy)
                    )
                },
                onClick = { TiebaUtil.copyText(context, profile.tiebaUid) }
            )
            profile.address?.let {
                Chip(text = stringResource(id = R.string.text_profile_ip_location, it))
            }
        }
    }
}

@Composable
private fun StateScreenScope.LoadingScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {},
            navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DefaultLoadingScreen()
        }
    }
}

@Composable
private fun UserPageHide(hiddenTab: Tab) {
    val context = LocalContext.current
    TipScreen(
        title = {
            val hiddenText = remember {
                context.getString(R.string.profile_title_hidden, hiddenTab.formatTitle(0, context))
            }
            Text(text = hiddenText)
        },
        modifier = Modifier.fillMaxSize(),
        image = {
            // MIUI-style outline glyph, matching `DefaultEmptyScreen`. The Lottie illustration was
            // not a Miuix asset, so it read as a foreign graphic in the middle of a Miuix page.
            Icon(
                imageVector = MiuixIcons.Hide,
                contentDescription = null, // Decorative: the title below already says it
                modifier = Modifier.size(48.dp),
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        },
    )
}

@Composable
private fun UserPageEmpty() {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_empty)) },
        modifier = Modifier.fillMaxSize(),
        image = {
            // Same treatment as `UserPageHide` / `DefaultEmptyScreen`: a Miuix outline glyph
            // instead of the Lottie box illustration, which is not a Miuix asset.
            Icon(
                imageVector = MiuixIcons.ListView,
                contentDescription = null, // Decorative: the title below already says it
                modifier = Modifier.size(48.dp),
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        },
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    checked: Boolean,
    @StringRes title: Int,
    @StringRes summary: Int? = null,
    leadingIcon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    SegmentedPreference(
        modifier = modifier,
        title = title,
        onClick = {
            onCheckedChange(!checked)
        },
        leadingIcon = leadingIcon,
        trailingContent = {
            MiuixSwitch(
                checked = checked,
                enabled = checked,
                onCheckedChange = null,
            )
        },
        containerColor = Color.Transparent,
        summary = summary,
        interactionSource = interactionSource,
    )
}

@Composable
private fun PermissionSettingDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState,
    initialPermissionList: PermissionList,
    isLoading: Boolean,
    onConfirm: (PermissionList) -> Unit
) {
    // 维护对话框内部的临时状态
    var current by remember { mutableStateOf(initialPermissionList.copy()) }

    AlertDialog(
        modifier = modifier,
        dialogState = dialogState,
        dialogProperties = AnyPopDialogProperties(
            direction = DirectionState.CENTER,
            dismissOnBackPress = !isLoading, // 强控用户直到请求完成
            dismissOnClickOutside = !isLoading,
        ),
        title = {
            val titleRes = if (isLoading) R.string.dialog_ban_requesting else R.string.dialog_ban
            Text(text = stringResource(titleRes))
        },
        content = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                Text(stringResource(id = R.string.dialog_content_wait))
            }

            AnimatedVisibility(
                visible = !isLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    // 1. 禁止关注
                    SwitchPreference(
                        checked = current.follow,
                        title = R.string.ban_follow,
                        leadingIcon = Icons.Outlined.Block,
                        onCheckedChange = { current = current.copy(follow = it) }
                    )
                    // 2. 禁止互动
                    SwitchPreference(
                        checked = current.interact,
                        title = R.string.ban_interact,
                        summary = R.string.ban_interact_summary,
                        leadingIcon = Icons.Outlined.Block,
                        onCheckedChange = { current = current.copy(interact = it) }
                    )
                    // 3. 禁止私信
                    SwitchPreference(
                        checked = current.chat,
                        title = R.string.ban_chat,
                        leadingIcon = Icons.Outlined.Block,
                        onCheckedChange = { current = current.copy(chat = it) }
                    )
                }
            }
        },
        buttons = {
            if (isLoading) return@AlertDialog

            DialogNegativeButton(text = stringResource(R.string.button_cancel))

            PositiveButton(
                text = stringResource(R.string.button_sure),
                enabled = current != initialPermissionList,
                onClick = { onConfirm(current) }
            )
        },
    )
}


@Preview("UserProfileDetail", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun UserProfileDetailPreview() = TiebaLiteTheme {
    UserProfileDetail(
        modifier = Modifier.padding(16.dp),
        profile = UserProfile(
            uid = -1,
            name = "我是0812",
            nickname = "我是谁",
            tbAge = "12.4",
            address = "第二经济开发区",
            bazuDesc = "吃瓜吧吧主",
            newGod = "吃瓜"
        ),
        landscape = false
    )
}

@Preview("LoadingScreen")
@Composable
private fun LoadingScreenPreview() = TiebaLiteTheme {
    Surface {
        with(StateScreenScope()) {
            LoadingScreen()
        }
    }
}

@Preview("LoadingScreenLandscape", device = Devices.PIXEL_TABLET)
@Composable
private fun LoadingScreenLandscapePreview() = TiebaLiteTheme {
    Surface {
        with(StateScreenScope()) {
            LoadingScreen()
        }
    }
}
