package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.ComposeNavigatorDestinationBuilder
import androidx.navigation.compose.composable
import androidx.navigation.get
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlinx.serialization.Serializable
import kotlin.reflect.KType

@Serializable
sealed interface MainDestination {

    @Serializable
    object Home: MainDestination

    @Serializable
    object Explore: MainDestination

    @Serializable
    object Notification: MainDestination

    @Serializable
    object User: MainDestination
}

/**
 * Bottom-navigation order of [MainDestination].
 *
 * The 「启动首选页」preference stores an index into this list rather than a destination object, so the
 * stored value survives renames. [MainDestination] is a sealed interface of objects, so there is no
 * `entries` to read the order from.
 * */
val MainDestinationOrder: List<MainDestination> = listOf(
    MainDestination.Home,
    MainDestination.Explore,
    MainDestination.Notification,
    MainDestination.User,
)

fun NavGraphBuilder.mainNavGraph(
    navController: NavController,
    nestedNavController: NavController,
    parentAnimatedVisibilityScope: AnimatedVisibilityScope? = null,
    parentSharedTransitionScope: SharedTransitionScope? = null,
) {
    animatedMainComposable<MainDestination.Home>(
        navController = navController,
        parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
        parentSharedTransitionScope = parentSharedTransitionScope,
    ) {
        HomePage(
            onOpenExplore = {
                nestedNavController.navigate(route = MainDestination.Explore) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(MainDestination.Home) {
                        saveState = true
                    }
                }
            },
        )
    }

    animatedMainComposable<MainDestination.Explore>(
        navController = navController,
        parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
        parentSharedTransitionScope = parentSharedTransitionScope,
    ) {
        val loggedIn = LocalAccount.current != null
        key(loggedIn) { // Force recreate
            ExplorePage(loggedIn)
        }
    }

    composable<MainDestination.Notification> { backStackEntry ->
        NotificationsPage(fromHome = true, navigator = navController)
    }

    animatedMainComposable<MainDestination.User>(
        navController = navController,
        parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
        parentSharedTransitionScope = parentSharedTransitionScope,
    ) {
        ProvideNavigator(navigator = navController) {
            UserPage()
        }
    }
}

private inline fun <reified T : Any> NavGraphBuilder.animatedMainComposable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    navController: NavController,
    parentAnimatedVisibilityScope: AnimatedVisibilityScope?,
    parentSharedTransitionScope: SharedTransitionScope?,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    destination(
        ComposeNavigatorDestinationBuilder(
            provider[ComposeNavigator::class],
            T::class,
            typeMap
        ) {
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalAnimatedVisibilityScope provides parentAnimatedVisibilityScope,
                LocalSharedTransitionScope provides parentSharedTransitionScope,
            ) {
                content(it)
            }
        }
        .apply {
            deepLinks.forEach { deepLink -> deepLink(deepLink) }
        }
    )
}
