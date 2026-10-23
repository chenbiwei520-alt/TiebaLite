package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.utils.LauncherIcons

enum class DarkPreference {
    FOLLOW_SYSTEM, ALWAYS, DISABLED
}

/**
 * 底部导航栏标签显示模式
 *
 * @since 4.0.0-beta.4.3
 * */
enum class NavigationLabel {
    ALWAYS, SELECTED, NONE
}

/**
 * User UI Settings
 *
 * @param appIcon 应用图标
 * @param appIconThemed 应用图标使用动态取色
 * @param bottomNavFloating 主页底部导航栏悬浮模式
 * @param bottomNavLabel 主页底部导航栏标签显示模式
 * @param darkPreference 夜间模式偏好
 * @param darkenImage 夜间模式压暗缩略图
 * @param defaultStartPage 启动首选页，[MainDestinationOrder] 的下标（0=首页 1=动态 2=消息 3=我的）
 * @param hideExplore 隐藏主页「动态」入口
 * @param reduceMotion 减弱动态效果
 * @param setupFinished 设置向导已完成
 * @param homeForumList 吧列表单列显示
 * @param showHistoryInHome 首页显示最近逛的吧
 * */
@Immutable
data class UISettings(
    val appIcon: LauncherIcons = LauncherIcons.NEW_ICON,
    val appIconThemed: Boolean = false,
    val bottomNavFloating: Boolean = false,
    val bottomNavLabel: NavigationLabel = NavigationLabel.ALWAYS,
    val darkPreference: DarkPreference = DarkPreference.FOLLOW_SYSTEM,
    val darkenImage: Boolean = true,
    val defaultStartPage: Int = 0,
    val hideExplore: Boolean = false,
    val reduceMotion: Boolean = false,
    val setupFinished: Boolean = false,
    val homeForumList: Boolean = false,
    /** 横屏侧边栏（NavigationRail）是否显示文字标签 */
    val railExpanded: Boolean = false,
    val showHistoryInHome: Boolean = true,
)