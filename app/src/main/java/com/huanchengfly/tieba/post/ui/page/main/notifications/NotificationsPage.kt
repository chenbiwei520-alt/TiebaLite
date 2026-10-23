package com.huanchengfly.tieba.post.ui.page.main.notifications

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.miuix.MiuixBlurNavigationBarPlaceHolder
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import kotlinx.coroutines.launch
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixEnterAlwaysScrollBehavior

@Composable
fun NotificationsPage(
    initialPage: NotificationsType = NotificationsType.ReplyMe,
    fromHome: Boolean = false,
    navigator: NavController = LocalNavController.current
) {
    val pages = NotificationsType.entries
    val pagerState = rememberPagerState(initialPage = initialPage.ordinal, pageCount = { pages.size })
    val listStates = rememberPagerListStates(pages.size)
    val scrollBehavior = rememberMiuixEnterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()

    OnMainNavigationScrollTopEvent<MainDestination.Notification>(
        coroutineScope = coroutineScope,
        topAppBarState = scrollBehavior.state,
        listState = { listStates.getOrNull(pagerState.currentPage) }
    )
    // Feeds the liquid glass fill of the tab row's pills
    val tabBackdrop = rememberMiuixBarBackdrop()

    MyScaffold(
        useMD2Layout = true,
        topBar = {
            NotificationsToolBar(
                navigator = navigator,
                fromHome = fromHome,
                scrollBehavior = scrollBehavior,
                canScrollBackward = {
                    listStates[pagerState.currentPage].canScrollBackward
                },
                backdrop = tabBackdrop,
            ) {
                MiuixTabRow(
                    tabs = pages.map { type ->
                        stringResource(
                            when (type) {
                                NotificationsType.ReplyMe -> R.string.title_reply_me

                                NotificationsType.AtMe -> R.string.title_at_me
                            }
                        )
                    },
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    backdrop = tabBackdrop,
                )
            }
        },
        bottomBar = {
            if (fromHome) bottomNavigationPlaceholder else MiuixBlurNavigationBarPlaceHolder(tabBackdrop)
        },
        bottomBarAtop = calculateMainNavigationSuiteType().isFloatingNavigationBar,
    ) { contentPadding ->
        ProvideNavigator(navigator = navigator) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.miuixLayerBackdrop(tabBackdrop),
                key = { pages[it] }
            ) {
                NotificationsListPage(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    type = NotificationsType.entries[it],
                    listState = listStates[pagerState.currentPage],
                    contentPadding = contentPadding
                )
            }
        }
    }
}

@Composable
private fun NotificationsToolBar(
    navigator: NavController,
    fromHome: Boolean,
    scrollBehavior: ScrollBehavior?,
    canScrollBackward: () -> Boolean,
    backdrop: LayerBackdrop? = null,
    // Occupies the title slot so the tab row shares the first row with the search action, rather
    // than rendering on a second row below it. The page name is intentionally dropped.
    content: @Composable () -> Unit
) {
    if (fromHome) {
        TopAppBarPaged(
            title = content,
            // Keep the tab row centred like it was in the `content` slot: that slot rendered inside
            // a Column with horizontalAlignment = CenterHorizontally, but the title slot defaults
            // to CenterStart.
            titleHorizontalAlignment = Alignment.CenterHorizontally,
            navigationIcon = {
                AccountNavIconIfCompact(onLoginClicked = { navigator.navigate(Destination.Login) })
            },
            actions = {
                ActionItem(
                    icon = MiuixIcons.Search,
                    contentDescription = stringResource(id = R.string.title_search),
                    onClick = { navigator.navigateDebounced(Search) }
                )
            },
            scrollBehavior = scrollBehavior,
            canScrollBackward = canScrollBackward,
            backdrop = backdrop,
        )
    } else {
        TopAppBarPaged(
            title = content,
            titleHorizontalAlignment = Alignment.CenterHorizontally,
            navigationIcon = {
                BackNavigationIcon(onBackPressed = navigator::navigateUp)
            },
            scrollBehavior = scrollBehavior,
            canScrollBackward = canScrollBackward,
            backdrop = backdrop,
        )
    }
}