package com.huanchengfly.tieba.post.ui.page.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.ForumBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.KeywordBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.UserBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppFontPage
import kotlinx.serialization.Serializable

sealed interface SettingsDestination {

    @Serializable
    object Settings: SettingsDestination

    @Serializable
    object About: SettingsDestination

    @Serializable
    object AccountManage: SettingsDestination

    @Serializable
    object AppFont: SettingsDestination

    @Serializable
    object BlockSettings: SettingsDestination

    @Serializable
    object ForumBlockList: SettingsDestination

    @Serializable
    object KeywordBlockList: SettingsDestination

    @Serializable
    object UserBlockList: SettingsDestination

    @Serializable
    object UI: SettingsDestination

    @Serializable
    object Habit: SettingsDestination

    @Serializable
    object Privacy: SettingsDestination

    @Serializable
    object StickyHeader: SettingsDestination

    @Serializable
    object More: SettingsDestination

    @Serializable
    object OKSign: SettingsDestination

    @Serializable
    object WorkInfo: SettingsDestination

    @Serializable
    object LogViewer: SettingsDestination
}

fun NavGraphBuilder.settingsGraph(navController: NavController, settingsRepo: SettingsRepository) {
    // 设置分区入口：宽屏（≥600.dp，横屏手机与平板）走两栏的 [TabletSettingsScreen]——左栏平铺入口、
    // 右栏直接渲染内容，全程不做页面跳转；窄屏保持原来的列表逐页导航。
    // 下面其它目的地仍然是窄屏路径的一部分，平板两栏页的右栏通过 `when (selected)` 直接渲染同样的页面。
    composable<SettingsDestination.Settings> {
        if (isWindowWidthCompact()) {
            SettingsPage(navController)
        } else {
            TabletSettingsScreen(navigator = navController, settingsRepo = settingsRepo)
        }
    }

    composable<SettingsDestination.About> {
        AboutPage(navController::navigateUp)
    }

    composable<SettingsDestination.AccountManage> {
        AccountManagePage(myLittleTailSettings = settingsRepo.myLittleTail, navController)
    }

    composable<SettingsDestination.AppFont> {
        AppFontPage(navController::navigateUp)
    }

    composable<SettingsDestination.BlockSettings> {
        BlockSettingsPage(settings = settingsRepo.blockSettings, navController)
    }

    composable<SettingsDestination.ForumBlockList> {
        ForumBlockListPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.KeywordBlockList> {
        KeywordBlockListPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.UserBlockList> {
        UserBlockListPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.UI> {
        UISettingsPage(settings = settingsRepo.uiSettings, navController)
    }

    composable<SettingsDestination.Habit> {
        HabitSettingsPage(
            habitSettings = settingsRepo.habitSettings,
            onStickyHeaderClicked = {
                navController.navigateDebounced(SettingsDestination.StickyHeader)
            },
            onBack = navController::navigateUp
        )
    }

    composable<SettingsDestination.Privacy> {
        PrivacySettingsPage(settingsRepo.privacySettings, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.StickyHeader> {
        StickyHeaderSettingsPage(settingsRepo.habitSettings, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.More> {
        MoreSettingsPage(navController)
    }

    composable<SettingsDestination.OKSign> {
        OKSignSettingsPage(settings = settingsRepo.signConfig, onBack = navController::navigateUp)
    }

    composable<SettingsDestination.WorkInfo> {
        WorkInfoPage(onBack = navController::navigateUp)
    }

    composable<SettingsDestination.LogViewer> {
        LogViewerPage(onBack = navController::navigateUp)
    }
}
