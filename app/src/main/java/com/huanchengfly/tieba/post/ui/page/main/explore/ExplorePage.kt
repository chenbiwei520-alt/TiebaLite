package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.HotTopicList
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.consumeResult
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResultKey
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.staticTopBarHeight
import com.huanchengfly.tieba.post.utils.BooleanBitSet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixEnterAlwaysScrollBehavior

sealed class ExplorePageItem(val title: Int){
    object Concern : ExplorePageItem(R.string.title_concern)

    object Personalized : ExplorePageItem(R.string.title_personalized)

    object Hot : ExplorePageItem(R.string.title_hot)
}

/**
 * Common [ThreadItem] onClick listeners for [ConcernPage], [PersonalizedPage] and [HotPage]
 * */
@Immutable
class ThreadClickListeners(
    val onClicked: (ThreadItem) -> Unit,
    val onReplyClicked: (ThreadItem) -> Unit,
    val onAuthorClicked: (ThreadItem) -> Unit,
    val onForumClicked: (ThreadItem) -> Unit,
    val onNavigateHotTopicList: () -> Unit // Not a thread click listener, place here just for convenience
)

fun createThreadClickListeners(
    onNavigate: (Destination, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) -> Unit
) = ThreadClickListeners(
    onClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId), null, null)
    },
    onReplyClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId, scrollToReply = true), null, null)
    },
    onAuthorClicked = { thread ->
        val route = thread.run {
            Destination.UserProfile(user = author, transitionKey = this.id.toString())
        }
        onNavigate(route, null, null)
    },
    onForumClicked = { thread ->
        val (_, forumName, forumAvatar) = thread.simpleForum
        val extraKey = thread.id.toString()
        onNavigate(Destination.Forum(forumName, forumAvatar, extraKey), null, null)
    },
    onNavigateHotTopicList = {
        onNavigate(HotTopicList, null, null)
    }
)

@Composable
private fun ExplorePageTab(
    pagerState: PagerState,
    pages: List<ExplorePageItem>,
    backdrop: LayerBackdrop? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    // HyperOS style tab row: outlined unselected pills, selected one filled with the accent color
    MiuixTabRow(
        tabs = pages.map { stringResource(id = it.title) },
        selectedTabIndex = pagerState.currentPage,
        onTabSelected = { index ->
            if (index == pagerState.currentPage) return@MiuixTabRow
            coroutineScope.launch {
                if (abs(pagerState.currentPage - index) > 1) {
                    pagerState.scrollToPage(index)
                } else {
                    pagerState.animateScrollToPage(index)
                }
            }
        },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        backdrop = backdrop,
    )
}

// Note: Obtain Root AnimatedVisibilityScope by LocalAnimatedVisibilityScope.current
@Composable
fun AnimatedVisibilityScope.ExplorePage(loggedIn: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavController.current
    val navigationSuiteType = calculateMainNavigationSuiteType()
    // Hide FAB on FloatingNavigationBarCompact
    val isFloatingNavBarCompat = navigationSuiteType === MainNavigationSuiteType.FloatingNavigationBarCompact
    val sharedTransitionScope = LocalSharedTransitionScope.current

    val pages = remember(loggedIn) {
        listOfNotNull(
            ExplorePageItem.Concern.takeIf { loggedIn },
            ExplorePageItem.Personalized,
            ExplorePageItem.Hot
        )
    }
    val pagerState = rememberPagerState(initialPage = if (loggedIn) 1 else 0) { pages.size }
    val listStates = rememberPagerListStates(pages.size)

    val scrollOrientationConnection = rememberScrollOrientationConnection()
    val scrollBehavior = rememberMiuixEnterAlwaysScrollBehavior()

    // FAB visibility of each page
    var fabHideStates by remember(pages) { mutableStateOf(BooleanBitSet()) }

    // Like event from explorePages
    onGlobalEvent<ThreadLikeUiEvent>(coroutineScope) {
        context.toastShort(it.toMessage(context))
    }

    OnMainNavigationScrollTopEvent<MainDestination.Explore>(
        coroutineScope = coroutineScope,
        topAppBarState = scrollBehavior.state,
        listState = { listStates.getOrNull(pagerState.currentPage) }
    )

    // Feeds the liquid glass fill of the tab row's pills
    val tabBackdrop = rememberMiuixBarBackdrop()

    MyScaffold(
        useMD2Layout = true,
        topBar = {
            TopAppBarPaged(
                modifier = Modifier
                    .topAppBarBlurEffect(
                        sharedTransitionScope = sharedTransitionScope,
                        rootAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current,
                                blurEnabled = { !fabHideStates[pagerState.currentPage] || pagerState.isScrolling },
                        // The Miuix progressive blur below replaces the flat haze blur
                    ),
                // The tab row takes over the title slot so it sits on the same row as the search
                // action instead of dropping to a second row. The page name is dropped: the tabs
                // already identify the page.
                title = {
                    ExplorePageTab(
                        pagerState = pagerState,
                        pages = pages,
                        backdrop = tabBackdrop,
                    )
                },
                // Keep the tab row centred like it was in the `content` slot: that slot rendered
                // inside a Column with horizontalAlignment = CenterHorizontally, but the title slot
                // defaults to CenterStart.
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    AccountNavIconIfCompact(onLoginClicked = { navigator.navigate(Destination.Login) })
                },
                actions = {
                    ActionItem(
                        icon = MiuixIcons.Search,
                        contentDescription = R.string.title_search,
                        onClick = { navigator.navigateDebounced(route = Search) }
                    )
                },
                scrollBehavior = scrollBehavior,
                canScrollBackward = { // No transition running && canScrollBackward
                    sharedTransitionScope?.isTransitionActive != true && !transition.isRunning &&
                            listStates[pagerState.currentPage].canScrollBackward
                },
                backdrop = tabBackdrop,
            )
        },
        bottomBar = bottomNavigationPlaceholder, // MainPage BottomNavBar placeholder
        bottomBarAtop = navigationSuiteType.isFloatingNavigationBar,
        floatingActionButton = {
            if (isFloatingNavBarCompat) return@MyScaffold
            // FAB visibility: scrolling forward, pager !scrolling, list !scrolling, not refreshing
            val visible by remember {
                derivedStateOf {
                    !transition.isRunning && scrollOrientationConnection.isScrollingForward &&
                            !pagerState.isScrolling && !listStates[pagerState.currentPage].isScrollInProgress &&
                            !fabHideStates[pagerState.currentPage]
                }
            }
            DefaultBackToTopFAB(visible = visible) {
                coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(MainDestination.Explore))
            }
        },
        floatingActionButtonPosition = if (isFloatingNavBarCompat) FabPosition.EndOverlay else FabPosition.End,
    ) { contentPadding ->
        // The scaffold's padding is the *measured* top bar height, which can come back one status
        // bar short for a frame or two — the whole feed (and the refresh tip anchored on the same
        // padding) then slides up under the bar and snaps back. The bar's resting height is
        // deterministic, so hold the top inset at that value: a no-op while the measurement is
        // right, and no jump while it is not.
        val topBarHeight = staticTopBarHeight()
        val stableContentPadding =
            if (contentPadding.calculateTopPadding() >= topBarHeight) {
                contentPadding
            } else {
                PaddingValues(top = topBarHeight, bottom = contentPadding.calculateBottomPadding())
            }
        // Feed cards need their own screen margins; the scaffold padding is inset-only
        val listContentPadding = stableContentPadding + PaddingValues(horizontal = 16.dp)
        // The blur capture must be FULL WIDTH — same rule as `ThreadPage`. `Container` narrows its
        // child to a centred column on wide screens (75% once the window is expanded, i.e. any
        // landscape phone), while the app bar's progressive-blur band spans the whole screen width.
        // A capture attached inside `Container` would be narrower than the band, and
        // `progressiveTextureBlur`'s shader clamps out-of-bounds samples to the capture's edge,
        // smearing the column's edge colour across the side margins as flat slabs with hard
        // vertical seams.
        Box(modifier = Modifier.fillMaxWidth().miuixLayerBackdrop(tabBackdrop)) {
            Container {
                HorizontalPager(
                    state = pagerState,
                    key = { pages[it].title },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollOrientationConnection),
                    verticalAlignment = Alignment.Top,
                    flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f)
                ) { index ->
                    // Attach ScrollBehavior connections
                    val modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    val onHideFab: (Boolean) -> Unit = { hideFab ->
                        fabHideStates = fabHideStates.set(index, hideFab)
                    }
                    val listState = listStates[index]

                    when (pages[index]) {
                        ExplorePageItem.Concern -> {
                            ConcernPage(modifier, listContentPadding, listState, navigator, onHideFab)
                        }

                        ExplorePageItem.Personalized -> {
                            PersonalizedPage(modifier, listContentPadding, listState, navigator, onHideFab)
                        }

                        ExplorePageItem.Hot -> {
                            HotPage(modifier, listContentPadding, listState, navigator, onHideFab)
                        }
                    }
                }
            }
        }
    }
}

context(mainAnimatedContentScope: AnimatedVisibilityScope)
fun Modifier.topAppBarBlurEffect(
    sharedTransitionScope: SharedTransitionScope?,
    rootAnimatedVisibilityScope: AnimatedVisibilityScope?,
    blurEnabled: () -> Boolean,
): Modifier {
    val animated = this.onNotNull(rootAnimatedVisibilityScope, sharedTransitionScope) { (rootAnimatedVisibilityScope, sharedTransitionScope) ->
        animateEnterExit(
            zIndexInOverlay = 1.0f,
            animatedVisibilityScope = rootAnimatedVisibilityScope,
            sharedTransitionScope = sharedTransitionScope
        )
    }
    // hazeBlur = false when the bar already draws the Miuix progressive blur: stacking the flat
    // haze blur on top of it would wash the ramp out into a uniform smear.
    return animated
}

@Composable
fun LaunchedFabStateEffect(
    listState: LazyListState,
    onHideFab: (Boolean) -> Unit,
    isRefreshing: Boolean,
    isError: Boolean
) {
    val noScrollBackward by remember { derivedStateOf { !listState.canScrollBackward } }

    LaunchedEffect(noScrollBackward, onHideFab, isRefreshing, isError) {
        onHideFab(noScrollBackward || isRefreshing || isError)
    }
}

@Composable
inline fun <reified Route : Any> ConsumeThreadPageResult(
    navigator: NavController,
    crossinline onThreadResult: (threadId: Long, Like) -> Unit
) {
    LaunchedEffect(Unit) {
        navigator.consumeResult<Route, ThreadResult>(ThreadResultKey)?.run {
            onThreadResult(threadId, Like(liked, likes))
        }
    }
}
