package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalShortNavigationBarOverride
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBarState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.window.embedding.SplitAttributes.LayoutDirection
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.defaultVerticalEnterTransition
import com.huanchengfly.tieba.post.ui.common.defaultVerticalExitTransition
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as kyantRememberLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.nav.LiquidBottomTabItem
import com.huanchengfly.tieba.post.ui.miuix.nav.LiquidBottomTabs
import com.huanchengfly.tieba.post.ui.miuix.nav.LiquidBottomTabsDefaults
import com.huanchengfly.tieba.post.ui.models.settings.NavigationLabel
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.settings.TabletSettingsNavState
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationPosition
import com.huanchengfly.tieba.post.ui.utils.calculateNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.NavigationSuiteScaffoldLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TallNavigationBarHeight
import com.huanchengfly.tieba.post.ui.widgets.compose.isNavigationBar
import com.huanchengfly.tieba.post.ui.widgets.compose.navigationSuiteScaffoldConsumeWindowInsets
import com.huanchengfly.tieba.post.utils.DeviceUtils.vibrateOneShot
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.NavigationRailDefaults
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailState
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Badge as MiuixBadge
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.All
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Messages

@Stable
val MainDestination.titleRes: Int
    @StringRes get() = when(this) {
        MainDestination.Home -> R.string.title_main
        MainDestination.Explore -> R.string.title_explore
        MainDestination.Notification -> R.string.title_notifications
        MainDestination.User -> R.string.title_user
    }

/**
 * Animated vector drawables for the main destinations.
 *
 * Currently unused: the rail/drawer branch of [MainNavigationItems] renders [miuixIcon] instead, so
 * that the landscape rail and the portrait bottom bars share one icon set. Kept as the source of
 * record for the animated assets.
 * */
@Stable
val MainDestination.iconRes: Int
    @DrawableRes get() = when(this) {
        MainDestination.Home -> R.drawable.ic_animated_rounded_inventory_2
        MainDestination.Explore -> R.drawable.ic_animated_toy_fans
        MainDestination.Notification -> R.drawable.ic_animated_rounded_notifications
        MainDestination.User -> R.drawable.ic_animated_rounded_person
    }

/** HyperOS style icons (miuix-icons) used by every navigation surface, bars and rail alike. */
val MainDestination.miuixIcon: ImageVector
    get() = when(this) {
        MainDestination.Home -> MiuixIcons.Home
        MainDestination.Explore -> MiuixIcons.All
        MainDestination.Notification -> MiuixIcons.Messages
        MainDestination.User -> MiuixIcons.Contacts
    }

val bottomNavigationPlaceholder: @Composable () -> Unit = {
    val navigationSuiteType = calculateMainNavigationSuiteType()
    if (navigationSuiteType.isNavigationBar) {
        when (navigationSuiteType) {
            MainNavigationSuiteType.FloatingNavigationBar,
            MainNavigationSuiteType.FloatingNavigationBarCompact ->
                // The floating liquid glass bar lifts itself above the navigation-bar inset —
                // `bottomLift` already contains the inset, so no extra inset padding here.
                Spacer(
                    modifier = Modifier.height(
                        LiquidBottomTabsDefaults.barHeight + LiquidBottomTabsDefaults.bottomLift(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                    )
                )

            else ->
                Spacer(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .height(
                            when (navigationSuiteType) {
                                MainNavigationSuiteType.ShortNavigationBarCompact -> NavigationBarHeight
                                else -> TallNavigationBarHeight
                            }
                        )
                )
        }
    } else {
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/**
 * Gets the current navigation [MainDestination] as a [MutableState]. When the given navController
 * changes the back stack due to a [NavController.navigate] or [NavController.popBackStack] this
 * will trigger a recompose and return the top destination on the back stack.
 *
 * @return a mutable state of the current [MainDestination]
 */
@Composable
private fun NavController.currentMainDestinationAsState(destinations: List<MainDestination>): State<MainDestination?> {
    val lifecycle = LocalLifecycleOwner.current
    return produceState(initialValue = null, destinations, currentBackStackEntryFlow, lifecycle) {
        withContext(Dispatchers.Default) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                currentBackStackEntryFlow.collect {
                    value = destinations.fastFirstOrNull { dest -> it.destination.hasRoute(dest::class) }
                }
            }
        }
    }
}

@Composable
fun MainPage(
    navHostController: NavHostController,
    /** 显式指定起始页；为 `null` 时用设置里的「启动首选页」。 */
    startDestination: MainDestination? = null,
    vm: MainPageViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val nestedNavController = rememberNavController()
    val scaffoldState = rememberNavigationSuiteScaffoldState()
    val uiSettings = LocalUISettings.current
    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val navigationSuiteType = calculateMainNavigationSuiteType()

    val loggedIn = LocalAccount.current != null
    val destinations = remember(loggedIn, uiSettings.hideExplore) {
        listOfNotNull(
            MainDestination.Home,
            MainDestination.Explore.takeUnless { uiSettings.hideExplore },
            MainDestination.Notification.takeIf { loggedIn },
            MainDestination.User,
        )
    }

    // 启动首选页：设置里存的是 [MainDestinationOrder] 的下标。当前不可见的入口（隐藏了「动态」、
    // 未登录时的「消息」）不能当起始页——那样 NavHost 的起始目的地不在本次构建的图里——此时回退首页。
    val resolvedStartDestination: MainDestination = startDestination
        ?: remember(uiSettings.defaultStartPage, destinations) {
            MainDestinationOrder.getOrNull(uiSettings.defaultStartPage)
                ?.takeIf { candidate -> destinations.any { it === candidate } }
                ?: MainDestination.Home
        }

    // 平板设置页左侧导航栏点过来的主 tab：消费一次并切过去。
    // 镜像参考实现的 `TabletNavSideState.pendingMainTab`——那边的外壳也是「先记下待切 tab、回到主页后消费」。
    LaunchedEffect(Unit) {
        val pending = TabletSettingsNavState.pendingMainTab
        if (pending >= 0) {
            TabletSettingsNavState.pendingMainTab = -1
            MainDestinationOrder.getOrNull(pending)
                ?.takeIf { dest -> destinations.any { it === dest } }
                ?.let { dest ->
                    nestedNavController.navigate(route = dest) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(route = resolvedStartDestination) {
                            saveState = true
                        }
                    }
                }
        }
    }

    val blurEffect = !MaterialTheme.colorScheme.isTranslucent
    val navigationSuiteColors = mainNavigationSuiteColors(uiSettings.bottomNavFloating, blurEffect)

    val currentDestination by nestedNavController.currentMainDestinationAsState(destinations)
    val messageCount by vm.messageCountFlow.collectAsStateWithLifecycle()
    MainNavigationSuiteScaffold(
        state = scaffoldState,
        destinations = destinations,
        isSelected = { dest -> dest === currentDestination },
        onSelect = { dest ->
            nestedNavController.navigate(route = dest) {
                launchSingleTop = true
                restoreState = true
                popUpTo(route = resolvedStartDestination) {
                    saveState = true
                }
            }
            if (dest == MainDestination.Notification) vm.onNavigateNotification()
        },
        onReSelect = { dest ->
            coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(dest))
        },
        bottomNavLabel = uiSettings.bottomNavLabel,
        messageCount = { messageCount },
        onRailExpandedChange = vm::setRailExpanded,
        mainNavSuiteType = navigationSuiteType,
        navigationSuiteColors = navigationSuiteColors,
        navigationVerticalArrangement = calculateNavigationPosition(windowAdaptiveInfo),
        primaryActionContent = {
            val onLoginClicked: () -> Unit = { navHostController.navigate(Destination.Login) }
            when (navigationSuiteType) {
                // Both rail variants use the same header: the rail's own item axis is what the
                // avatar is aligned to (see MiuixRailNavigationSuite). A second expand/collapse
                // toggle is deliberately not drawn here — Miuix's NavigationRail already has one
                // above the header.
                MainNavigationSuiteType.WideNavigationRailCollapsed,
                MainNavigationSuiteType.WideNavigationRailExpanded,
                MainNavigationSuiteType.NavigationRail -> AccountNavIcon(onLoginClicked)

                MainNavigationSuiteType.NavigationDrawer -> TbDrawerNavigationAction(onLoginClicked)

                MainNavigationSuiteType.FloatingNavigationBarCompact -> {
                    if (uiSettings.hideExplore) return@MainNavigationSuiteScaffold
                    ExplorePrimaryAction(visible = MainDestination.Explore === currentDestination) {
                        coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(MainDestination.Explore))
                    }
                }

                else -> Unit // NavigationBar or None
            }
        }
    ) {
        val parentAnimatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val parentSharedTransitionScope = LocalSharedTransitionScope.current
        val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            mainEnterTransition(navigationSuiteType, destinations)
        }
        val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            mainExitTransition(navigationSuiteType, destinations)
        }

        NavHost(
            navController = nestedNavController,
            startDestination = resolvedStartDestination,

            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = enterTransition,
            popExitTransition = exitTransition
        ) {
            mainNavGraph(
                navController = navHostController,
                nestedNavController = nestedNavController,
                parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
                parentSharedTransitionScope = parentSharedTransitionScope,
            )
        }
    }

    currentDestination?.let {
        BackHandler(it !== resolvedStartDestination) {
            nestedNavController.popBackStack(route = resolvedStartDestination::class, inclusive = false, saveState = true)
        }
    }
}

/**
 * Landscape rail built on Miuix's own [NavigationRail].
 *
 * Preferred over the M3 adaptive rail because Miuix animates the icon→icon+label expansion inside
 * the component: [NavigationRailState] drives the width, the item labels and their alpha together,
 * which is the transition shown in the miuix-ui demo.
 */
@Composable
internal fun MiuixRailNavigationSuite(
    mainNavigationSuiteType: MainNavigationSuiteType,
    modifier: Modifier = Modifier,
    destinations: List<MainDestination>,
    isSelected: (MainDestination) -> Boolean,
    onSelect: (MainDestination) -> Unit,
    onReSelect: (MainDestination) -> Unit,
    messageCount: () -> String?,
    primaryActionContent: @Composable (() -> Unit) = {},
    onExpandedChange: (Boolean) -> Unit = {},
) {
    val uiSettings = LocalUISettings.current
    val expanded = uiSettings.railExpanded &&
            mainNavigationSuiteType != MainNavigationSuiteType.NavigationRail
    val railState = remember {
        NavigationRailState(
            if (expanded) NavigationRailValue.Expanded else NavigationRailValue.Collapsed
        )
    }

    // Settings are the single source of truth (persisted across rotation); the rail state follows.
    LaunchedEffect(expanded) {
        if (expanded) railState.expand() else railState.collapse()
    }

    // Miuix's built-in toggle only moves the rail state, so mirror it back into settings — without
    // this the mode was lost on rotation. The guard keeps the two-way sync from looping.
    LaunchedEffect(railState, expanded) {
        snapshotFlow { railState.isExpanded }
            .collect { if (it != expanded) onExpandedChange(it) }
    }

    NavigationRail(
        state = railState,
        modifier = modifier,
        header = {
            // Align the avatar on the same vertical centre line as the nav item icons.
            //
            // Where the item icons actually sit (verified by decompiling
            // `NavigationRailItemExpandable` from the pinned miuix-ui artifact, and by measuring the
            // device screenshot):
            //
            //   collapsed rail (80.dp = MinWidth)
            //     item content is centred in the rail  -> icon centre = MinWidth / 2 = 40.dp
            //   expanded rail (240.dp = ExpandedWidth)
            //     horizontal margin 12.dp (ExpandedItemHorizontalMargin)
            //   + content padding  14.dp (ExpandedItemContentHorizontalPadding)
            //   + icon 28.dp        (IconSize)
            //                                      -> icon centre = 12 + 14 + 14 = 40.dp
            //
            // So the icon centre is 40.dp from the rail's left edge in BOTH states — the rail never
            // re-centres its items when it expands, it only grows to the right.
            //
            // Three things previously pushed the avatar off that line:
            //   1. the header content was wrapped in `Modifier.size(NavigationRailDefaults.IconSize)`
            //      (28.dp), so the 36.dp avatar was constrained down to 28.dp and shrank;
            //   2. it carried `padding(start = ExpandedItemContentHorizontalPadding)` (14.dp), which
            //      an item gets from its own container but the header does not;
            //   3. the slot was sized with `Modifier.width(MinWidth)` while the rail's internal
            //      Column centres its children, so in the 240.dp expanded rail the whole slot was
            //      centred (starting at 80.dp) instead of starting at 0 — this is what moved the
            //      avatar to ~99.dp expanded vs ~28.dp collapsed on device.
            //
            // The fix is a full-width child whose content is centred in a MinWidth-wide slot pinned
            // to the left. `fillMaxWidth()` removes any dependence on the parent Column's alignment,
            // so the 40.dp centre holds in both states.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier.width(NavigationRailDefaults.MinWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    primaryActionContent()
                }
            }
        },
    ) {
        destinations.fastForEach { destination ->
            val selected = isSelected(destination)
            NavigationRailItem(
                selected = selected,
                onClick = { if (selected) onReSelect(destination) else onSelect(destination) },
                icon = destination.miuixIcon,
                label = stringResource(destination.titleRes),
                badge = destination.miuixNotificationBadge(messageCount),
            )
        }
    }
}

@Composable
private fun MainNavigationSuite(
    mainNavigationSuiteType: MainNavigationSuiteType,
    modifier: Modifier = Modifier,
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    verticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    primaryActionContent: @Composable (() -> Unit) = {},
    destinations: List<MainDestination> = emptyList(),
    isSelected: (MainDestination) -> Boolean = { false },
    onSelect: (MainDestination) -> Unit = {},
    onReSelect: (MainDestination) -> Unit = {},
    bottomNavLabel: NavigationLabel = NavigationLabel.ALWAYS,
    messageCount: () -> String? = { null },
    onExpandedChange: (Boolean) -> Unit = {},
    backdrop: Backdrop? = null,
    content: @Composable () -> Unit,
) {
    when {
        // HyperOS bottom bars (miuix): standard bottom bar and floating capsule bar
        mainNavigationSuiteType == MainNavigationSuiteType.FloatingNavigationBar ||
                mainNavigationSuiteType == MainNavigationSuiteType.FloatingNavigationBarCompact -> {
            MiuixFloatingNavigationSuite(
                mainNavigationSuiteType = mainNavigationSuiteType,
                destinations = destinations,
                isSelected = isSelected,
                onSelect = onSelect,
                onReSelect = onReSelect,
                messageCount = messageCount,
                backdrop = backdrop,
            )
        }

        mainNavigationSuiteType == MainNavigationSuiteType.ShortNavigationBarCompact ||
                mainNavigationSuiteType == MainNavigationSuiteType.ShortNavigationBarMedium ||
                mainNavigationSuiteType == MainNavigationSuiteType.NavigationBar -> {
            MiuixBottomNavigationSuite(
                modifier = modifier,
                destinations = destinations,
                isSelected = isSelected,
                onSelect = onSelect,
                onReSelect = onReSelect,
                bottomNavLabel = bottomNavLabel,
                messageCount = messageCount,
            )
        }

        // Landscape rail: Miuix's own NavigationRail, which animates the icon→icon+label
        // expansion inside the component (NavigationRailState.expand()/collapse()). The M3 adaptive
        // rail only animated the container width, so the items jumped — the missing transition.
        mainNavigationSuiteType == MainNavigationSuiteType.NavigationRail ||
                mainNavigationSuiteType == MainNavigationSuiteType.WideNavigationRailCollapsed ||
                mainNavigationSuiteType == MainNavigationSuiteType.WideNavigationRailExpanded -> {
            MiuixRailNavigationSuite(
                mainNavigationSuiteType = mainNavigationSuiteType,
                modifier = modifier,
                destinations = destinations,
                isSelected = isSelected,
                onSelect = onSelect,
                onReSelect = onReSelect,
                messageCount = messageCount,
                primaryActionContent = primaryActionContent,
                onExpandedChange = onExpandedChange,
            )
        }

        else -> {
            val shortNavBarOverride = when (mainNavigationSuiteType) {
                MainNavigationSuiteType.FloatingNavigationBar -> FloatingNavigationBarOverride
                MainNavigationSuiteType.FloatingNavigationBarCompact -> FloatingIconNavigationBarOverride
                MainNavigationSuiteType.NavigationBar -> DefaultNavigationBarOverride
                else -> androidx.compose.material3.DefaultShortNavigationBarOverride
            }
            CompositionLocalProvider(LocalShortNavigationBarOverride provides shortNavBarOverride) {
                NavigationSuite(
                    navigationSuiteType = mainNavigationSuiteType.toNavigationSuiteType(),
                    // 仅图标 ↔ 图标+文字 切换时，导航轨宽度平滑过渡
                    modifier = modifier.animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        )
                    ),
                    colors = colors,
                    verticalArrangement = verticalArrangement,
                    primaryActionContent = primaryActionContent,
                    content = content,
                )
            }
        }
    }
}

/**
 * HyperOS floating capsule bottom bar.
 *
 * Now the app's own **liquid glass** bar (`LiquidBottomTabs`): a sliding capsule that blurs and
 * refracts the page behind it, with the reference's drag/spring motion. It replaces Miuix's
 * `FloatingNavigationBar`, whose flat `surfaceContainer` capsule this component was modelled on —
 * the item order, selection semantics (`onSelect` / `onReSelect`), badges and labels are identical,
 * and the compact variant ([MainNavigationSuiteType.FloatingNavigationBarCompact]) still renders
 * through the exact same component because the icon-only difference is a width/typography concern
 * the bar does not need to know about.
 *
 * Placement follows NexioSchedule's floating bar: a centred pill capped to
 * [LiquidBottomTabsDefaults.WidthFractionPerTab] of the width per tab and lifted above the
 * navigation-bar inset by [LiquidBottomTabsDefaults.bottomLift], instead of docking to the edges.
 *
 * @param backdrop the Kyant page capture the glass samples, recorded by `MainNavigationSuiteScaffold`.
 */
@Composable
private fun MiuixFloatingNavigationSuite(
    mainNavigationSuiteType: MainNavigationSuiteType,
    destinations: List<MainDestination>,
    isSelected: (MainDestination) -> Boolean,
    onSelect: (MainDestination) -> Unit,
    onReSelect: (MainDestination) -> Unit,
    messageCount: () -> String?,
    backdrop: Backdrop? = null,
) {
    // Floating placement (NexioSchedule): a centred pill capped to `0.21f * tab count` of the
    // width, lifted above the navigation-bar inset by at least 24.dp. `LiquidBottomTabs` fills
    // whatever width its modifier gives it, so the cap survives its internal `fillMaxWidth()`.
    val barWidthFraction = (LiquidBottomTabsDefaults.WidthFractionPerTab * destinations.size)
        .coerceAtMost(1f)
    val barBottomLift = LiquidBottomTabsDefaults.bottomLift(
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        LiquidBottomTabs(
            items = destinations.map { destination ->
                LiquidBottomTabItem(
                    icon = destination.miuixIcon,
                    label = stringResource(destination.titleRes),
                    badge = destination.miuixNotificationBadge(messageCount),
                )
            },
            selectedIndex = destinations.indexOfFirst { isSelected(it) }.coerceAtLeast(0),
            onSelect = { index -> destinations.getOrNull(index)?.let(onSelect) },
            onReSelect = { index -> destinations.getOrNull(index)?.let(onReSelect) },
            backdrop = backdrop,
            modifier = Modifier
                .padding(bottom = barBottomLift)
                .fillMaxWidth(barWidthFraction),
        )
    }
}

/** HyperOS standard bottom bar (miuix [NavigationBar]). */
@Composable
private fun MiuixBottomNavigationSuite(
    modifier: Modifier = Modifier,
    destinations: List<MainDestination>,
    isSelected: (MainDestination) -> Boolean,
    onSelect: (MainDestination) -> Unit,
    onReSelect: (MainDestination) -> Unit,
    bottomNavLabel: NavigationLabel,
    messageCount: () -> String?,
) {
    val displayMode = when (bottomNavLabel) {
        NavigationLabel.ALWAYS -> NavigationBarDisplayMode.IconAndText
        NavigationLabel.SELECTED -> NavigationBarDisplayMode.IconWithSelectedLabel
        NavigationLabel.NONE -> NavigationBarDisplayMode.IconOnly
    }
    NavigationBar(
        modifier = modifier,
        mode = displayMode,
    ) {
        destinations.forEach { destination ->
            val selected = isSelected(destination)
            NavigationBarItem(
                selected = selected,
                onClick = { if (selected) onReSelect(destination) else onSelect(destination) },
                icon = destination.miuixIcon,
                label = stringResource(destination.titleRes),
                badge = destination.miuixNotificationBadge(messageCount),
            )
        }
    }
}

private fun MainDestination.miuixNotificationBadge(messageCount: () -> String?): (@Composable () -> Unit)? {
    if (this !== MainDestination.Notification) return null
    return {
        messageCount()?.let { messageCountText ->
            MiuixBadge { Text(text = messageCountText) }
        }
    }
}

/**
 * [NavigationSuiteScaffold] with Haze blur support
 *
 * @see NavigationSuiteScaffoldLayout
 * @see Modifier.navigationSuiteScaffoldConsumeWindowInsets
 * */
@Composable
private fun MainNavigationSuiteScaffold(
    destinations: List<MainDestination>,
    isSelected: (MainDestination) -> Boolean,
    onSelect: (MainDestination) -> Unit,
    onReSelect: (MainDestination) -> Unit,
    bottomNavLabel: NavigationLabel,
    messageCount: () -> String?,
    onRailExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    mainNavSuiteType: MainNavigationSuiteType = calculateMainNavigationSuiteType(),
    navigationBarAtop: Boolean = true,
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    navigationVerticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    primaryActionContent: @Composable (() -> Unit) = {},
    primaryActionContentHorizontalAlignment: Alignment.Horizontal =
        NavigationSuiteScaffoldDefaults.primaryActionContentAlignment,
    content: @Composable () -> Unit = {},
) {
    val navigationSuiteType = mainNavSuiteType.toNavigationSuiteType()
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    // Override navbar container color when there is ongoing transition
    val colorsOnTransition = if (navigationSuiteType.isNavigationBar && animatedVisibilityScope != null) {
        animatedVisibilityScope.navigationSuiteTransitionColors(mainNavSuiteType, navigationSuiteColors).value
    } else {
        null
    }
    // The liquid-glass bottom bar's backdrop (Kyant `io.github.kyant0:backdrop`).
    //
    // It is recorded HERE, around the pages, rather than inside the bar: the bar occupies a sibling
    // slot of this content, so a capture made here can never contain the bar itself (no feedback),
    // and it is recorded in every state (a backdrop nothing records blurs to transparent black —
    // see `rememberSearchBarGlass`). `NavigationSuiteScaffoldLayout` places this content at
    // (0, 0) at full size, so the capture also covers the strip the bar floats over. Created only
    // when the device can run AGSL (API 33+); below that the bar falls back to its flat surface.
    val barBackdrop = if (isRuntimeShaderSupported()) kyantRememberLayerBackdrop() else null

    NavigationSuiteScaffoldLayout(
        modifier = modifier,
        navigationSuite = {
            MainNavigationSuite(
                mainNavigationSuiteType = mainNavSuiteType,
                modifier = Modifier
                    .onNotNull(colorsOnTransition) {
                        animateEnterExit(
                            animatedVisibilityScope = animatedVisibilityScope,
                            sharedTransitionScope = LocalSharedTransitionScope.current,
                            enter = defaultVerticalEnterTransition(topToBottom = false),
                            exit = defaultVerticalExitTransition(topToBottom = false)
                        )
                    },
                colors = colorsOnTransition ?: navigationSuiteColors,
                verticalArrangement = navigationVerticalArrangement,
                primaryActionContent = primaryActionContent,
                destinations = destinations,
                isSelected = isSelected,
                onSelect = onSelect,
                onReSelect = onReSelect,
                bottomNavLabel = bottomNavLabel,
                messageCount = messageCount,
                onExpandedChange = onRailExpandedChange,
                backdrop = barBackdrop,
                // Rendered only by the Material branch (rails / drawer)
                content = {
                    MainNavigationItems(
                        items = destinations,
                        isSelected = isSelected,
                        onSelect = onSelect,
                        onReSelect = onReSelect,
                        mainNavigationSuiteType = mainNavSuiteType,
                        bottomNavLabel = bottomNavLabel,
                        messageCount = messageCount,
                    )
                },
            )
        },
        state = state,
        navigationSuiteType = navigationSuiteType,
        navigationBarAtop = navigationBarAtop,
        primaryActionContent = primaryActionContent,
        primaryActionContentHorizontalAlignment = primaryActionContentHorizontalAlignment,
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (barBackdrop != null) {
                            Modifier.kyantLayerBackdrop(barBackdrop)
                        } else {
                            Modifier
                        }
                    )
                    .navigationSuiteScaffoldConsumeWindowInsets(
                        navigationSuiteType,
                        navigationBarAtop,
                        state,
                    ),
            ) {
                content()
            }
        },
    )
}

@Stable
private fun NavigationLabel.visible(selected: Boolean): Boolean {
    return when(this) {
        NavigationLabel.ALWAYS -> true
        NavigationLabel.SELECTED -> selected
        NavigationLabel.NONE -> false
    }
}

@Composable
private fun MainNavigationItems(
    items: List<MainDestination>,
    isSelected: (MainDestination) -> Boolean,
    modifier: Modifier = Modifier,
    onSelect: (MainDestination) -> Unit = {},
    onReSelect: (MainDestination) -> Unit = {},
    mainNavigationSuiteType: MainNavigationSuiteType = calculateMainNavigationSuiteType(),
    bottomNavLabel: NavigationLabel = NavigationLabel.ALWAYS,
    messageCount: () -> String? = { null },
) {
    val isNavigationBar = mainNavigationSuiteType.isNavigationBar
    items.fastForEach { destination ->
        val selected = isSelected(destination)
        MainNavigationSuiteItem(
            selected = selected,
            onClick = {
                if (selected) onReSelect(destination) else onSelect(destination)
            },
            icon = {
                // Same source the HyperOS bottom bars use (MainDestination.miuixIcon), so the
                // landscape rail and the portrait bottom bar show one consistent icon set. This
                // branch used to render MainDestination.iconRes, a separate animated drawable set,
                // which is why the rail looked like a different icon family.
                Icon(
                    imageVector = destination.miuixIcon,
                    modifier = Modifier.size(Sizes.Tiny),
                    contentDescription = stringResource(destination.titleRes),
                )
            },
            label = if (mainNavigationSuiteType != MainNavigationSuiteType.NavigationRail &&
                (!isNavigationBar || bottomNavLabel.visible(selected))
            ) {
                { Text(stringResource(id = destination.titleRes)) }
            } else {
                null
            },
            modifier = modifier
                .onCase(mainNavigationSuiteType == MainNavigationSuiteType.NavigationDrawer) {
                    padding(horizontal = 16.dp)
                },
            mainNavigationSuiteType = mainNavigationSuiteType,
            badge = if (destination === MainDestination.Notification) {
                {
                    messageCount()?.let { messageCountText ->
                        Badge {
                            Text(
                                text = messageCountText, // 6.sp ~ BadgeTokens.LargeLabelTextFont.fontSize
                                autoSize = TextAutoSize.StepBased(6.sp, LocalTextStyle.current.fontSize),
                                maxLines = 1
                            )
                        }
                    }
                }
            } else null,
        )
    }
}

// Override NavigationDrawer to use our old custom NavigationDrawerItem
@Composable
private fun MainNavigationSuiteItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    mainNavigationSuiteType: MainNavigationSuiteType,
    enabled: Boolean = true,
    badge: @Composable (() -> Unit)? = null,
    colors: NavigationItemColors? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    if (mainNavigationSuiteType == MainNavigationSuiteType.FloatingNavigationBarCompact) {
        IconNavigationItem(
            selected = selected,
            onClick = onClick,
            icon = {
                if (badge != null) {
                    BadgedBox(badge = { badge.invoke() }, content = { icon() })
                } else {
                    icon()
                }
            },
            colors = colors ?: ShortNavigationBarItemDefaults.colors(),
            enabled = enabled,
            modifier = modifier,
            interactionSource = interactionSource,
        )
    } else if (mainNavigationSuiteType != MainNavigationSuiteType.NavigationDrawer) {
        androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            label = label,
            modifier = modifier,
            navigationSuiteType = mainNavigationSuiteType.toNavigationSuiteType(),
            enabled = enabled,
            badge = badge,
            colors = colors,
            interactionSource = interactionSource,
        )
    } else {
        val actualColors = if (colors != null) {
            NavigationDrawerItemDefaults.colors(
                selectedIconColor = colors.selectedIconColor,
                selectedTextColor = colors.selectedTextColor,
                unselectedIconColor = colors.unselectedIconColor,
                unselectedTextColor = colors.unselectedTextColor,
                selectedContainerColor = colors.selectedIndicatorColor,
            )
        } else {
            NavigationSuiteDefaults.itemColors().navigationDrawerItemColors
        }

        NavigationDrawerItem(
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            icon = icon,
            badge = badge,
            label = { label?.invoke() ?: Text("") },
            colors = actualColors,
            interactionSource = interactionSource,
        )
    }
}

/**
 * @return [NavigationSuiteColors] for background blurring
 *  */
@Composable
private fun mainNavigationSuiteColors(floatingNavBar: Boolean, blur: Boolean): NavigationSuiteColors {
    return TiebaLiteTheme.extendedColorScheme.run {
        NavigationSuiteDefaults.colors(
            shortNavigationBarContainerColor = if (floatingNavBar) {
                colorScheme.vibrantFloatingNavigationBarColor.copy(
                    alpha = when {
                        colorScheme.isTranslucent -> 0.65f
                        blur -> if (darkTheme) 0.86f else 0.74f
                        else -> 1f
                    }
                )
            } else {
                navigationContainer
            },
            shortNavigationBarContentColor = if (floatingNavBar) {
                colorScheme.vibrantFloatingNavigationBarContentColor
            } else {
                ShortNavigationBarDefaults.contentColor
            },
            navigationBarContainerColor = navigationContainer
        )
    }
}

// Override navigation bar container color when there is ongoing transition
@Composable
private fun AnimatedVisibilityScope.navigationSuiteTransitionColors(
    navigationSuiteType: MainNavigationSuiteType,
    defaultColors: NavigationSuiteColors
): State<NavigationSuiteColors> {
    // Replace with NavigationSuiteColors.copy()!!
    val colorsInTransition = NavigationSuiteDefaults.colors(
        navigationBarContainerColor = MaterialTheme.colorScheme.surface,
        shortNavigationBarContainerColor = if (navigationSuiteType.isFloatingNavigationBar) {
            MaterialTheme.colorScheme.vibrantFloatingNavigationBarColor
        } else {
            MaterialTheme.colorScheme.surface
        },
    )
    return remember(defaultColors) {
        derivedStateOf { if (transition.isRunning) colorsInTransition else defaultColors }
    }
}

@Composable
private fun ExplorePrimaryAction(modifier: Modifier = Modifier, visible: Boolean, onClick: () -> Unit) {
    val isTransitionActive = LocalAnimatedVisibilityScope.current?.transition?.isRunning == true
    // Float the FAB alongside the lifted glass bar instead of hugging the screen edge.
    val screenOffset = LiquidBottomTabsDefaults.bottomLift(
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    )
    val visibilityAnimation by animateFloatAsState(
        targetValue = if (visible && !isTransitionActive) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    DefaultBackToTopFAB(
        modifier = modifier
            .graphicsLayer { // SlideIn vertically
                translationY = lerp(size.height * 2, -screenOffset.toPx(), visibilityAnimation)
            },
        visible = visible,
        size = NavigationBarHeight,
        onClick = onClick,
    )
}

// Common ScrollToTop event listener for main destinations
@Composable
inline fun <reified T: MainDestination> OnMainNavigationScrollTopEvent(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    topAppBarState: TopAppBarState? = null,
    gridState: LazyGridState? = null,
    crossinline staggeredGridState: () -> LazyStaggeredGridState? = { null },
    crossinline listState: () -> LazyListState?,
) {
    val context = LocalContext.current
    onGlobalEvent<GlobalEvent.ScrollToTop>(coroutineScope, filter = { it.tag is T }) {
        val listState = listState()
        val staggeredState = staggeredGridState()
        if (listState?.canScrollBackward == true || gridState?.canScrollBackward == true ||
            staggeredState?.canScrollBackward == true
        ) {
            context.vibrateOneShot(milliseconds = 50)
            coroutineScope.launch {
                listState?.run {
                    if (firstVisibleItemIndex > 5) scrollToItem(0) else animateScrollToItem(0)
                }
                gridState?.animateScrollToItem(0)
                staggeredState?.animateScrollToItem(0)
            }
        }
        // Reset TopAppBarState
        topAppBarState?.contentOffset = 0f
        topAppBarState?.heightOffset = 0f
    }
}

/**
 * Creates a list of [TopAppBarState] that is remembered across compositions.
 * */
@Composable
fun rememberTopAppBarScrollBehaviors(
    size: Int,
    init: @Composable (TopAppBarState) -> ScrollBehavior
): List<ScrollBehavior> {
    var result: List<ScrollBehavior> by remember(size) { mutableStateOf(emptyList()) }

    val stateList = rememberSaveable(size, saver = Saver) {
        val states = mutableListOf<TopAppBarState>()
        repeat(size) {
            states.add(TopAppBarState(-Float.MAX_VALUE, 0f, 0f))
        }
        states.toImmutableList()
    }

    if (result.isEmpty()) {
        result = stateList.fastMap { init(it) }
    }
    return result
}

/** The default [Saver] implementation for list of [TopAppBarState]. */
@Suppress("UNCHECKED_CAST")
private val Saver: Saver<List<TopAppBarState>, *> = listSaver(
    save = {
        it.mapIndexed { i, it ->
            mutableListOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset)
        }.reduce { rec, list ->
            rec.apply { addAll(list) }
        }
    },
    restore = {
        assert(it.isNotEmpty())
        val states = mutableListOf<TopAppBarState>()
        var trimList = it
        val saver = (TopAppBarState.Saver as Saver<TopAppBarState, List<Float>>)
        repeat(it.size / 3) { i ->
            trimList = it.subList(i * 3, it.size)
            val state = saver.restore(trimList)!!
            states.add(state)
        }
        states.toImmutableList()
    }
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTransitionDirection(
    navigationSuiteType: MainNavigationSuiteType,
    items: List<MainDestination>,
): LayoutDirection? {
    var from = -1
    var to = -1
    items.fastForEachIndexed { i, dest ->
        if (from == -1 && initialState.destination.hasRoute(dest::class)) {
            from = i
        } else if (to == -1 && targetState.destination.hasRoute(dest::class)) {
            to = i
        }
    }
    return when {
        from == -1 || to == -1 -> null // Edge case: initialize

        navigationSuiteType.isNavigationBar ->
            if (from > to) LayoutDirection.RIGHT_TO_LEFT else LayoutDirection.LEFT_TO_RIGHT

        navigationSuiteType == MainNavigationSuiteType.None -> null

        else -> if (from > to) LayoutDirection.BOTTOM_TO_TOP else LayoutDirection.TOP_TO_BOTTOM
    }
}

// Pager style enter transition
private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainEnterTransition(
    navigationSuiteType: MainNavigationSuiteType,
    items: List<MainDestination>,
): EnterTransition {
    return when(val direction = mainTransitionDirection(navigationSuiteType, items)) {
        LayoutDirection.RIGHT_TO_LEFT, LayoutDirection.LEFT_TO_RIGHT -> {
            slideInHorizontally(
                animationSpec = MAIN_TRANSITION_SPEC,
                initialOffsetX = { if (direction == LayoutDirection.LEFT_TO_RIGHT) it else -it }
            ) + MAIN_FADE_IN_TRANSITION
        }
        LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.BOTTOM_TO_TOP -> {
            slideInVertically(
                animationSpec = MAIN_TRANSITION_SPEC,
                initialOffsetY = { if (direction == LayoutDirection.TOP_TO_BOTTOM) it else -it }
            ) + MAIN_FADE_IN_TRANSITION
        }
        else -> MAIN_FADE_IN_TRANSITION
    }
}

// Pager style exit transition
private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainExitTransition(
    navigationSuiteType: MainNavigationSuiteType,
    items: List<MainDestination>,
): ExitTransition {
    return when(val direction = mainTransitionDirection(navigationSuiteType, items)) {
        LayoutDirection.RIGHT_TO_LEFT, LayoutDirection.LEFT_TO_RIGHT -> {
            slideOutHorizontally(
                animationSpec = MAIN_TRANSITION_SPEC,
                targetOffsetX = { if (direction == LayoutDirection.LEFT_TO_RIGHT) -it else it }
            ) + MAIN_FADE_OUT_TRANSITION
        }
        LayoutDirection.TOP_TO_BOTTOM, LayoutDirection.BOTTOM_TO_TOP -> {
            slideOutVertically(
                animationSpec = MAIN_TRANSITION_SPEC,
                targetOffsetY = { if (direction == LayoutDirection.TOP_TO_BOTTOM) -it else it }
            ) + MAIN_FADE_OUT_TRANSITION
        }
        else -> MAIN_FADE_OUT_TRANSITION
    }
}

private val MAIN_TRANSITION_SPEC: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = 250, easing = FastOutSlowInEasing)

private val MAIN_FADE_IN_TRANSITION: EnterTransition =
    fadeIn(tween(durationMillis = 300, easing = FastOutSlowInEasing))

private val MAIN_FADE_OUT_TRANSITION: ExitTransition =
    fadeOut(tween(durationMillis = 300, easing = FastOutSlowInEasing))

@ReadOnlyComposable
@Composable
fun calculateMainNavigationSuiteType(): MainNavigationSuiteType {
    val uiSettings = LocalUISettings.current
    val type = MainNavigationSuiteType.fromNavigationSuiteType(
        type = calculateNavigationType(LocalWindowAdaptiveInfo.current),
        floating = uiSettings.bottomNavFloating,
        noLabel = uiSettings.bottomNavLabel == NavigationLabel.NONE
    )
    // 横屏侧边栏显示模式：用户切到「图标+文字」时，把竖排导航轨升级为展开态宽轨。
    // 只在 rail 类型下生效，底栏类型不受影响。
    return if (uiSettings.railExpanded && (
            type == MainNavigationSuiteType.NavigationRail ||
                type == MainNavigationSuiteType.WideNavigationRailCollapsed
            )
    ) {
        MainNavigationSuiteType.WideNavigationRailExpanded
    } else {
        type
    }
}

private val MainNavigationSuiteType.isNavigationBar
    get() = when (this) {
        MainNavigationSuiteType.FloatingNavigationBar,
        MainNavigationSuiteType.FloatingNavigationBarCompact,
        MainNavigationSuiteType.ShortNavigationBarCompact,
        MainNavigationSuiteType.ShortNavigationBarMedium,
        MainNavigationSuiteType.NavigationBar -> true
        else -> false
    }

@Stable
private fun MainNavigationSuiteType.toNavigationSuiteType(): NavigationSuiteType = when (this) {
    MainNavigationSuiteType.FloatingNavigationBar,
    MainNavigationSuiteType.FloatingNavigationBarCompact,
    MainNavigationSuiteType.ShortNavigationBarCompact -> NavigationSuiteType.ShortNavigationBarCompact
    MainNavigationSuiteType.ShortNavigationBarMedium -> NavigationSuiteType.ShortNavigationBarMedium
    MainNavigationSuiteType.WideNavigationRailCollapsed -> NavigationSuiteType.WideNavigationRailCollapsed
    MainNavigationSuiteType.WideNavigationRailExpanded -> NavigationSuiteType.WideNavigationRailExpanded
    MainNavigationSuiteType.NavigationBar -> NavigationSuiteType.NavigationBar
    MainNavigationSuiteType.NavigationRail -> NavigationSuiteType.NavigationRail
    MainNavigationSuiteType.NavigationDrawer -> NavigationSuiteType.NavigationDrawer
    MainNavigationSuiteType.None -> NavigationSuiteType.None
}

@Preview("MainNavigationItems", device = Devices.PIXEL_TABLET)
@Composable
private fun MainNavigationItemsPreview() = TiebaLiteTheme {
    val destinations = listOf(MainDestination.Home, MainDestination.Explore, MainDestination.Notification, MainDestination.User)
    val isSelected: (MainDestination) -> Boolean = { it == MainDestination.Home }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            MainNavigationSuiteType.WideNavigationRailCollapsed,
            MainNavigationSuiteType.WideNavigationRailExpanded,
            MainNavigationSuiteType.NavigationRail,
            MainNavigationSuiteType.NavigationDrawer,
        )
        .forEach { type ->
            MainNavigationSuite(mainNavigationSuiteType = type, verticalArrangement = Arrangement.Center) {
                MainNavigationItems(destinations, isSelected, mainNavigationSuiteType = type)
            }
        }
    }
}

@Preview("MainBottomNavigationItems", device = Devices.PIXEL_9)
@Composable
private fun MainBottomNavigationItemsPreview() = TiebaLiteTheme {
    val destinations = listOf(MainDestination.Home, MainDestination.Explore, MainDestination.Notification, MainDestination.User)
    val isSelected: (MainDestination) -> Boolean = { it == MainDestination.Home }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            MainNavigationSuiteType.FloatingNavigationBar,
            MainNavigationSuiteType.FloatingNavigationBarCompact,
            MainNavigationSuiteType.ShortNavigationBarCompact,
            MainNavigationSuiteType.ShortNavigationBarMedium,
            MainNavigationSuiteType.NavigationBar,
        )
        .forEach { type ->
            MainNavigationSuite(mainNavigationSuiteType = type, verticalArrangement = Arrangement.Center) {
                MainNavigationItems(destinations, isSelected, mainNavigationSuiteType = type)
            }
        }
    }
}
