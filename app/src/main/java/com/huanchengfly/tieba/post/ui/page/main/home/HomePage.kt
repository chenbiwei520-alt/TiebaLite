package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.placeholder.PlaceholderDefaults
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.isOverlapping
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.SearchToolbarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.explore.topAppBarBlurEffect
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.miuix.SquircleShape
import com.huanchengfly.tieba.post.ui.miuix.LocalLiquidGlassBackdrop
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.widgets.compose.ListItemCard
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.color
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.MiuixLoadingIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.ui.miuix.rememberSearchBarGlass
import kotlin.random.Random
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixEnterAlwaysScrollBehavior
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView

private val FORUM_AVATAR_SIZE = 40.dp
@Preview("HomeSearchBar")
@Composable
private fun HomeSearchBarPreview() {
    Column {
        TiebaLiteTheme {
            HomeSearchBar(onClick = {})
        }

        TiebaLiteTheme(colorSchemeExt = DefaultDarkColors) {
            HomeSearchBar(onClick = {})
        }
    }
}

/**
 * Home search entry built on the Miuix [SearchBar] / [InputField] pair.
 *
 * The home page only routes to the dedicated search screen (`Destination.Search` carries no query),
 * so the field is display-only: a transparent layer above it swallows the tap before the field can
 * take focus, which keeps the IME closed and sends every tap to [onClick]. Because the entry never
 * expands, [SearchBar]'s results slot stays empty.
 *
 * With a non-null [backdrop] the capsule is glass: the shared [rememberSearchBarGlass] treatment —
 * the same one `MiuixSearchTopBar` uses on the search page — blurs the content recorded by the host
 * Scaffold's full-screen capture (also the top bar's progressive-blur source). That capture is
 * recorded on every frame, which is what keeps the capsule readable while the page loads: a
 * page-level capture only exists in `StateScreen`'s content branch, and blurring an unrecorded layer
 * yields transparent black. Null (below API 33) falls back to the flat `surfaceContainerHigh`
 * capsule.
 *
 * @param backdrop the host capture recording the content behind the capsule (the Scaffold's, read in
 *   the `topBar` slot), or null to keep the flat capsule.
 */
@Composable
private fun HomeSearchBar(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    onClick: () -> Unit,
) {
    val glass = rememberSearchBarGlass(backdrop)

    Box(
        // SearchBar already insets its content by 12dp; add the remainder to line up with the
        // page's 16dp gutter.
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        SearchBar(
            inputField = {
                InputField(
                    query = "",
                    onQueryChange = {/* Display-only entry */ },
                    onSearch = {/* Handled by the search screen */ },
                    expanded = false,
                    onExpandedChange = {/* Never expands */ },
                    label = stringResource(id = R.string.hint_search),
                    modifier = glass.modifier,
                    color = glass.capsuleColor,
                )
            },
            onExpandedChange = {/* Never expands */ },
            expanded = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Unreachable: this entry routes to Destination.Search instead of searching in place.
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickableNoIndication(onClick = onClick)
        )
    }
}

@Composable
private fun Header(text: String, modifier: Modifier = Modifier, invert: Boolean = false) {
    Box(modifier = modifier) {
        Chip(text = text, modifier = Modifier.padding(start = 16.dp), invertColor = invert)
    }
}

@Composable
private fun ForumItemPlaceholder(showAvatar: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholderColor = PlaceholderDefaults.color()

        if (showAvatar) {
            Box(
                modifier = Modifier
                    .size(FORUM_AVATAR_SIZE)
                    .placeholder(color = placeholderColor, shape = CircleShape),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = "",
            modifier = Modifier
                .weight(1.0f)
                .placeholder(color = placeholderColor),
            fontSize = 15.sp,
        )

        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .padding(vertical = 4.dp)
                .placeholder(color = placeholderColor)
        ) {
            Text(text = "0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryItem(
    modifier: Modifier = Modifier,
    title: String,
    avatar: @Composable RowScope.() -> Unit,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(shape = CircleShape)
            .background(color = color)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        avatar()
        Text(text = title, color = contentColor, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun HistoryRow(modifier: Modifier = Modifier, history: List<History>, onClick: (History) -> Unit) {
    var expandHistoryForum by rememberSaveable { mutableStateOf(true) }

    val degrees by animateFloatAsState(
        targetValue = if (expandHistoryForum) 90f else 0f,
        label = "ExpandRotateAnim"
    )
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickableNoIndication { expandHistoryForum = !expandHistoryForum }
                .padding(vertical = 8.dp)
                .padding(end = 16.dp)
        ) {
            Header(text = stringResource(id = R.string.title_history_forum))

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.desc_show),
                modifier = Modifier.graphicsLayer {
                    rotationZ = degrees
                }
            )
        }

        AnimatedVisibility(visible = expandHistoryForum) {
            LazyRow(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = history, key = { it.id }) {
                    HistoryItem(
                        title = it.name,
                        avatar = { Avatar(data = it.avatar, size = Sizes.Tiny) },
                        color = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurface,
                        onClick = { onClick(it) }
                    )
                }
            }
        }
    }
}

@NonRestartableComposable
@Composable
private fun ForumItemContent(forum: LikedForum, showAvatar: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Inset of the content inside the card. Independent from the grid's contentPadding,
            // which only controls the gap between the card and the screen edge — dropping this
            // left the name flush against the card border and the 54.dp level badge colliding with
            // the corner radius.
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar) {
            Avatar(
                data = forum.avatar,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(FORUM_AVATAR_SIZE)
                    .localSharedBounds(key = ForumAvatarSharedBoundsKey(forum.name, null)),
            )
        }

        Box(modifier = Modifier.weight(1.0f)) {  // Boxing for transition animation
            Text(
                text = forum.name,
                modifier = Modifier.onCase(showAvatar) { // Enable transition on List Mode (showAvatar)
                    localSharedBounds(key = ForumTitleSharedBoundsKey(forum.name, null))
                },
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Surface(
            modifier = Modifier.width(54.dp),
            shape = SquircleShape(SquircleCornerRadius.ExtraSmall),
            color = MaterialTheme.colorScheme.secondary,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = forum.level, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                if (forum.signed) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = MiuixIcons.Basic.Check,
                        contentDescription = stringResource(id = R.string.tip_signed),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.forumItems(
    forums: LazyPagingItems<LikedForum>,
    isTopPinnedForum: Boolean,
    showAvatar: Boolean,
    onClick: (forum: LikedForum) -> Unit,
    onUnfollow: (forum: LikedForum) -> Unit,
    onPinnedForumChanged: (forum: LikedForum, isTop: Boolean) -> Unit,
) {
    val contentType: (index: Int) -> Any? = forums.itemContentType {
        if (showAvatar) ForumType.ListItem else ForumType.GridItem
    }

    items(count = forums.itemCount, key = forums.itemKey { it.id }, contentType = contentType) {
        val context = LocalContext.current
        val forum = forums[it]
        if (forum != null) {
            ListItemCard(
                modifier = Modifier.animateItem(
                    placementSpec = ForumReflowPlacementSpec,
                    fadeInSpec = ForumReflowFadeInSpec,
                    fadeOutSpec = ForumReflowFadeOutSpec,
                )
            ) {
                LongClickMenu(
                    menuContent = {
                        TextMenuItem(text = if (isTopPinnedForum) R.string.menu_top_del else R.string.menu_top) {
                            onPinnedForumChanged(forum, !isTopPinnedForum)
                        }
                        TextMenuItem(text = R.string.title_copy_forum_name) {
                            TiebaUtil.copyText(context, forum.name)
                        }
                        TextMenuItem(text = R.string.button_unfollow) {
                            onUnfollow(forum)
                        }
                    },
                    onClick = { onClick(forum) }
                ) {
                    ForumItemContent(forum, showAvatar)
                }
            }
        } else {
            ListItemCard {
                ForumItemPlaceholder(showAvatar)
            }
        }
    }
}

/**
 * Column-count reflow motion for the forum grid.
 *
 * Miuix ships no dedicated single/multi-column transition component — its only grid-related class
 * is an internal `LazyGridAdapter` for scrollbars. The HyperOS launcher effect people recognise is
 * Compose's item placement animation: when [MyLazyVerticalGrid]'s `gridCells` changes, every item
 * gets a new slot, and `Modifier.animateItem` tweens each item from its old offset to the new one
 * with a short cross-fade layered on top.
 *
 * Two things this needs to actually run, both of which were previously wrong:
 *  - `animateItem` must sit on the **item root**. It used to be applied to `LongClickMenu`, a nested
 *    child, so the placement tween never applied and the grid snapped instead of sliding.
 *  - the fade specs must not be null. They were `fadeInSpec = null, fadeOutSpec = null`, so the
 *    cross-fade half of the effect was silently disabled.
 *
 * Durations follow HyperOS motion: ~220 ms decelerate on enter, ~120 ms accelerate on exit, and a
 * soft spring for the positional move so items settle rather than stop dead.
 */
private val ForumReflowPlacementSpec = spring<IntOffset>(
    stiffness = Spring.StiffnessMediumLow,
)

private val ForumReflowFadeInSpec = tween<Float>(
    durationMillis = 220,
    easing = FastOutSlowInEasing,
)

private val ForumReflowFadeOutSpec = tween<Float>(
    durationMillis = 120,
    easing = FastOutLinearInEasing,
)

private val DefaultGridSpan: LazyGridItemSpanScope.() -> GridItemSpan = {
    GridItemSpan(maxLineSpan)
}

private sealed interface ForumType {
    object Header: ForumType
    object History: ForumType
    object ListItem: ForumType
    object GridItem: ForumType
}

// Note: Obtain Root AnimatedVisibilityScope by LocalAnimatedVisibilityScope.current
@Composable
fun AnimatedVisibilityScope.HomePage(
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>(),
    onOpenExplore: () -> Unit = {},
) {
    val loggedIn = LocalAccount.current != null
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val gridState = rememberLazyGridState()
    val scrollBehavior = rememberMiuixEnterAlwaysScrollBehavior()

    var unfollowForum by remember { mutableStateOf<LikedForum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    unfollowForum?.let {
        ConfirmDialog(
            dialogState = confirmUnfollowDialog,
            onConfirm = {
                viewModel.onDislikeForum(forum = it)
            },
            onDismiss = { unfollowForum = null },
            title = { Text(text = stringResource(R.string.button_unfollow)) }
        ) {
            Text(text = stringResource(R.string.title_dialog_unfollow_forum, it.name))
        }
    }

    // Read once, above the scaffold: the toggle lives in the top bar while the grid lives in the
    // content lambda, and both need the same value to stay in sync.
    val listSingle = LocalUISettings.current.homeForumList

    MyScaffold(
        topBar = {
            // Both glass surfaces in this bar sample the **Scaffold's** full-screen capture, read
            // here inside the `topBar` slot: `MyScaffold` provides `LocalLiquidGlassBackdrop` around
            // the Miuix Scaffold, so a read in the page body always comes back null.
            //
            // The page used to keep its own `rememberMiuixBarBackdrop()` capture, recorded by a Box
            // inside `StateScreen`'s content lambda. `StateScreen` composes exactly one of error /
            // loading / empty / content, so on the initial load and on every refresh that Box left
            // the composition and the layer stayed unrecorded. `textureBlur` samples the layer
            // unconditionally and blurring an unrecorded layer yields transparent black, so the 42%
            // light tint composited over it rendered the search entry as a flat dark slab while
            // refreshing. The Scaffold's capture is recorded on every frame and — because Miuix's
            // Scaffold lays the body out from (0, 0) — also covers the strip the bar floats over,
            // including the pull-to-refresh container's displaced content.
            val glassBackdrop = LocalLiquidGlassBackdrop.current
            TopAppBarPaged(
                modifier = Modifier
                    .topAppBarBlurEffect(
                        sharedTransitionScope = sharedTransitionScope,
                        rootAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current,
                        blurEnabled = { gridState.canScrollBackward || scrollBehavior.isOverlapping },
                    ),
                // No page title: the account avatar and the action icons already identify the page.
                title = {},
                navigationIcon = {
                    AccountNavIconIfCompact(onLoginClicked = { navigator.navigate(Destination.Login) })
                },
                actions = {
                    if (loggedIn) {
                        val isSinging by viewModel.isOkSignWorkerRunning.collectAsStateWithLifecycle(true)
                        ActionItem(
                            icon = ImageVector.vectorResource(id = R.drawable.ic_oksign),
                            contentDescription = R.string.title_oksign,
                            enabled = !isSinging
                        ) {
                            TiebaUtil.startSign(context)
                        }

                        ActionItem(
                            // Icon doubles as the state indicator: list view while in single-column
                            // mode, grid view while in two-column mode.
                            icon = if (listSingle) MiuixIcons.ListView else MiuixIcons.GridView,
                            contentDescription = R.string.title_switch_list_single,
                            onClick = viewModel::onListModeChanged
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                canScrollBackward = {
                    sharedTransitionScope?.isTransitionActive != true && !transition.isRunning && gridState.canScrollBackward
                },
                backdrop = glassBackdrop,
            ) {
                HomeSearchBar(
                    modifier = Modifier.localSharedBounds(key = SearchToolbarSharedBoundsKey, zIndexInOverlay = 2.0f),
                    // The same capture the top bar's progressive blur samples: the capsule blurs
                    // the page content that scrolls underneath it, in every page state.
                    backdrop = glassBackdrop,
                    onClick = { navigator.navigateDebounced(route = Destination.Search) },
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        bottomBar = bottomNavigationPlaceholder, // MainPage BottomNavBar placeholder
        bottomBarAtop = calculateMainNavigationSuiteType().isFloatingNavigationBar,
    ) { contentPaddings ->
        val coroutineScope = rememberCoroutineScope()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val forums = viewModel.forums.collectAsLazyPagingItems()
        val pinnedForums = viewModel.pinnedForums.collectAsLazyPagingItems()
        val historyForums by viewModel.historyFlow.collectAsStateWithLifecycle()

        val isEmpty = pinnedForums.itemCount == 0 && forums.itemCount == 0
        val isPinnedNotEmpty = pinnedForums.itemCount > 0
        val isLoading = uiState.isLoading || historyForums == null

        val gridCells = remember(listSingle) {
            // Fixed(2), not Adaptive(180.dp): the content column is only ~308.dp wide on a 360.dp
            // phone, so a 180.dp minimum yields floor(308 / 180) = 1 column — identical to the
            // single-column layout, which is why toggling appeared to do nothing.
            if (listSingle) GridCells.Fixed(1) else GridCells.Fixed(2)
        }

        // Initialize click listeners now
        val onForumClickedListener: (LikedForum) -> Unit = {
            navigator.navigateDebounced(route = Destination.Forum(forumName = it.name, avatar = it.avatar))
        }
        val onHistoryClickedListener: (History) -> Unit = {
            navigator.navigateDebounced(route = Destination.Forum(forumName = it.name))
        }

        val onUnfollow: (LikedForum) -> Unit = {
            unfollowForum = it
            confirmUnfollowDialog.show()
        }

        OnMainNavigationScrollTopEvent<MainDestination.Home>(
            coroutineScope = coroutineScope,
            topAppBarState = scrollBehavior.state,
            gridState = gridState,
            listState = { null }
        )

        StateScreen(
            isEmpty = isEmpty,
            isError = uiState.error != null,
            isLoading = uiState.isLoading,
            onReload = viewModel::onRefresh.takeIf { loggedIn },
            emptyScreen = {
                EmptyScreen(onExploreClicked = onOpenExplore)
            },
            loadingScreen = {
                HomePageSkeletonScreen(modifier = Modifier.fillMaxSize())
            },
            errorScreen = {
                if (uiState.error is TiebaNotLoggedInException) {
                    GuestScreen(onExploreClicked = onOpenExplore) {
                        navigator.navigateDebounced(Destination.Login)
                    }
                } else  {
                    ErrorScreen(error = uiState.error)
                }
            },
            screenPadding = contentPaddings
        ) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = viewModel::onRefresh,
                contentPadding = contentPaddings
            ) {
                MyLazyVerticalGrid(
                    columns = gridCells,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    state = gridState,
                    // Gutters and rhythm match the other card lists; the item's own horizontal
                    // padding was removed so the cards line up with them.
                    contentPadding = contentPaddings + PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    historyForums?.takeUnless { it.isEmpty() }?.let {
                        item(key = ForumType.History.hashCode(), DefaultGridSpan, { ForumType.History }) {
                            HistoryRow(history = it, onClick = onHistoryClickedListener)
                        }
                    }

                    if (isPinnedNotEmpty) {
                        item(key = R.string.title_top_forum, DefaultGridSpan, { ForumType.Header }) {
                            Header(
                                text = stringResource(id = R.string.title_top_forum),
                                modifier = Modifier.padding(vertical = 8.dp),
                                invert = true
                            )
                        }
                        forumItems(
                            forums = pinnedForums,
                            isTopPinnedForum = true,
                            showAvatar = listSingle,
                            onClick = onForumClickedListener,
                            onUnfollow = onUnfollow,
                            onPinnedForumChanged = viewModel::onPinnedForumChanged,
                        )
                    }

                    if (!historyForums.isNullOrEmpty() || isPinnedNotEmpty) {
                        item(key = R.string.forum_list_title, DefaultGridSpan, { ForumType.Header }) {
                            Header(text = stringResource(id = R.string.forum_list_title))
                        }
                    }
                    forumItems(
                        forums = forums,
                        isTopPinnedForum = false,
                        showAvatar = listSingle,
                        onClick = onForumClickedListener,
                        onUnfollow = onUnfollow,
                        onPinnedForumChanged = viewModel::onPinnedForumChanged,
                    )
                }
            }
        }

        ReportDrawnWhen { !uiState.isLoading }
    }
}

@NonRestartableComposable
@Composable
private fun ExploreButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    PositiveButton(
        textRes = R.string.button_go_to_explore,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(),
        onClick = onClick,
    )
}

@Composable
private fun HomePageSkeletonScreen(modifier: Modifier = Modifier) {
    // Was a 24-cell grid of shimmer placeholders; now the shared Miuix ring, centred in the same
    // area. The parameters went away with the grid — the ring does not care about column count.
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MiuixLoadingIndicator()
    }
}

@Composable
private fun GuestScreen(
    modifier: Modifier = Modifier,
    onExploreClicked: () -> Unit,
    onLoginClicked: () -> Unit
) {
    TipScreen(
        title = {
            Text(text = stringResource(id = R.string.title_not_logged_in))
        },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            Text(text = stringResource(R.string.home_empty_login), textAlign = TextAlign.Center)
        },
        actions = {
            PositiveButton(R.string.button_login, Modifier.fillMaxWidth(), onClick = onLoginClicked)

            ExploreButton(modifier = Modifier.fillMaxWidth(), onClick = onExploreClicked)
        }
    )
}

@Composable
private fun EmptyScreen(modifier: Modifier = Modifier, onExploreClicked: () -> Unit) {
    TipScreen(
        title = {
            Text(text = stringResource(id = R.string.title_empty))
        },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        actions = { ExploreButton(modifier = Modifier.fillMaxWidth(), onClick = onExploreClicked) },
    )
}

@Preview("HomePageSkeletonScreen")
@Composable
private fun HomePageSkeletonScreenPreview() = TiebaLiteTheme {
    Surface {
        HomePageSkeletonScreen()
    }
}

@Preview("ForumItemContent")
@Composable
private fun ForumItemContentPreview() = TiebaLiteTheme {
    val forums = (0..15).map { i ->
        LikedForum(id = i.toLong(), name = "Forum $i", level = "Lv.${Random.nextInt(1, 99)}")
    }
    Surface {
        Column {
            forums.forEach {
                ForumItemContent(it, showAvatar = false)
            }
        }
    }
}