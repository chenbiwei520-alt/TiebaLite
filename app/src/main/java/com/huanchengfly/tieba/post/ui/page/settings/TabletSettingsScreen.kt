package com.huanchengfly.tieba.post.ui.page.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.miuix.MiuixProgressiveBlurBar
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainDestinationOrder
import com.huanchengfly.tieba.post.ui.page.main.MainPageViewModel
import com.huanchengfly.tieba.post.ui.page.main.MiuixRailNavigationSuite
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.titleRes
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.ForumBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.KeywordBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.blocklist.UserBlockListPage
import com.huanchengfly.tieba.post.ui.page.settings.theme.AppFontPage
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.utils.LocalAccount
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 中间栏宽度占内容区宽度的比例。
 *
 * 与参考实现一致：左栏（这里是最左导航栏右侧的入口栏）宽度**固定**为内容区的 0.39，
 * 不随窗口变宽而分到更多空间，剩余宽度全部给右栏。
 * */
private const val TabletSettingsLeftWidthFraction = 0.39f

/** 入口栏里图标到文字的距离。 */
private val TabletSettingsIconEndPadding = 8.dp

/**
 * 左栏里应该高亮的入口。
 *
 * 二级页（屏蔽吧管理、字体大小、帖子功能栏、Worker Info）不在左栏列表里，但它们被父页打开时，
 * 高亮应该停在父入口上——否则下钻后左栏会一个都不亮。
 * */
private fun TabletSettingsDest.listHighlight(): TabletSettingsDest = when (this) {
    TabletSettingsDest.ForumBlockList,
    TabletSettingsDest.UserBlockList,
    TabletSettingsDest.KeywordBlockList -> TabletSettingsDest.BlockSettings

    TabletSettingsDest.AppFont -> TabletSettingsDest.UI
    TabletSettingsDest.StickyHeader -> TabletSettingsDest.Habit
    TabletSettingsDest.WorkInfo -> TabletSettingsDest.More
    else -> this
}

/**
 * 平板设置页左侧导航栏待切换的主 tab。
 *
 * 镜像参考实现的 `TabletNavSideState.pendingMainTab`：点侧栏里的主 tab 时先记下目标下标，
 * 再退回主页外壳，由 `MainPage` 消费一次并切到该 tab。
 * */
object TabletSettingsNavState {
    var pendingMainTab by mutableStateOf(-1)
}

/** 从侧栏切到某个主 tab：先记下目标下标，再退回主页外壳（与参考实现的 pendingMainTab 同一套做法）。 */
private fun switchToMainTab(navigator: NavController, destination: MainDestination) {
    TabletSettingsNavState.pendingMainTab = MainDestinationOrder.indexOf(destination)
    // 设置是从主页推进来的：直接回退；若没有可回退的（深链直达）就显式回主页。
    if (!navigator.popBackStack()) {
        navigator.navigate(Destination.Main)
    }
}

/**
 * 平板/横屏设置页的入口。
 *
 * 移植自 NexioSchedule 的 `TabletSettingsScreen`（仓库 `com_haooz_account/hyper_schedule`）：
 *
 *  - 最左侧是与参考实现平板外壳同样的**常驻主导航栏**（参考的 `TabletNavSideBar` 由它的外壳渲染，
 *    这里由本页渲染，效果一致：设置页里主导航栏不消失）；
 *  - 设置页的**全部入口（含二级）按分组平铺在中间栏**，分组标题用 `SmallTitle`、组内一张 `Card`
 *    装 `ArrowPreference`；
 *  - **右栏用 `when (selected)` 直接渲染选中项的内容**，选中项只改状态、不做页面跳转；页内的二级
 *    入口也由这里的回调接管（屏蔽设置→三个屏蔽列表、个性化→字体大小、使用习惯→帖子功能栏、
 *    更多→Worker Info），不再跳出两栏；
 *  - 中间栏宽度固定为内容区的 [TabletSettingsLeftWidthFraction]，右栏吃掉剩余宽度，中间一条 1.dp 分隔线；
 *  - 中间栏有自己的一栏标题；**右栏的标题由页面自身的顶栏提供**（页面按手机上原样渲染）——不用外面的
 *    标题栏是因为：页面的 Miuix Scaffold 同时是下拉菜单弹层的宿主（去掉它下拉点不开），而且页面自带
 *    顶栏才能让「关于」的流光背景一直铺到最顶部、并让列表滚动与手机完全一致。
 *
 * 选中态由 [ArrowPreference] 的 `holdDownState` 表达——这正是参考实现里"当前项高亮"的来源。
 * */
internal enum class TabletSettingsDest(
    @StringRes val groupRes: Int,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    /**
     * 是否出现在中间栏的入口列表里。
     *
     * 二级入口（屏蔽吧管理/字体大小/帖子功能栏…）本身就在父页里，父页在面板模式下由回调切换右栏，
     * 所以**不在左栏重复列一遍**；它们仍然作为 [TabletSettingsDest] 存在，用于右栏标题与内容。
     * */
    val inList: Boolean = true,
) {
    AccountManage(R.string.settings_group_tablet_account, R.string.title_account_manage, MiuixIcons.Contacts),
    OKSign(R.string.settings_group_tablet_account, R.string.title_oksign, MiuixIcons.Tasks),

    BlockSettings(R.string.settings_group_tablet_content, R.string.title_block_settings, MiuixIcons.Blocklist),

    UI(R.string.settings_group_tablet_appearance, R.string.title_settings_custom, MiuixIcons.Theme),
    Habit(R.string.settings_group_tablet_appearance, R.string.title_settings_read_habit, MiuixIcons.Tune),

    Privacy(R.string.settings_group_tablet_other, R.string.title_settings_privacy, MiuixIcons.Lock),
    More(R.string.settings_group_tablet_other, R.string.title_settings_more, MiuixIcons.More),
    About(R.string.settings_group_tablet_other, R.string.title_about, MiuixIcons.Info),

    // ── 二级入口：只作右栏目标，不出现在左栏 ──────────────────────────────────────────
    WorkInfo(R.string.settings_group_tablet_account, R.string.title_settings_worker, MiuixIcons.Tasks, inList = false),
    ForumBlockList(R.string.settings_group_tablet_content, R.string.settings_block_forum, MiuixIcons.Blocklist, inList = false),
    UserBlockList(R.string.settings_group_tablet_content, R.string.settings_block_user, MiuixIcons.Blocklist, inList = false),
    KeywordBlockList(R.string.settings_group_tablet_content, R.string.settings_block_keyword, MiuixIcons.Blocklist, inList = false),
    AppFont(R.string.settings_group_tablet_appearance, R.string.title_custom_font_size, MiuixIcons.Theme, inList = false),
    StickyHeader(R.string.settings_group_tablet_appearance, R.string.title_settings_sticky_header, MiuixIcons.Tune, inList = false),
}

@Composable
fun TabletSettingsScreen(
    navigator: NavController,
    settingsRepo: SettingsRepository,
) {
    // 选中项只改状态，不导航；旋转/重建后保留选中项。
    var selected by rememberSaveable { mutableStateOf(TabletSettingsDest.AccountManage) }
    // 只列一级入口：二级入口在父页里（父页在面板模式下会把右栏切过去），不在左栏重复。
    val groups = remember { TabletSettingsDest.entries.filter { it.inList }.groupBy { it.groupRes } }
    val leftListState = rememberLazyListState()
    val colors = MiuixTheme.colorScheme
    val dividerColor =
        if (colors.surface.luminance() < 0.5f) Color.White.copy(alpha = 0.1f)
        else Color.Black.copy(alpha = 0.06f)

    // 常驻导航栏里的主入口：与主页外壳同一套目的地与可用性规则。
    val uiSettings = LocalUISettings.current
    val loggedIn = LocalAccount.current != null
    val railDestinations = remember(loggedIn, uiSettings.hideExplore) {
        listOfNotNull(
            MainDestination.Home,
            MainDestination.Explore.takeUnless { uiSettings.hideExplore },
            MainDestination.Notification.takeIf { loggedIn },
            MainDestination.User,
        )
    }
    // 侧栏用与主页（以及其他所有页面）**完全相同**的那个 rail：同一个组件、同一条 railExpanded 设置、
    // 同一个头像头部、同样的角标。此前这里自己手搓了一个 NavigationRail，外观与行为都和别处不一致。
    val mainViewModel: MainPageViewModel = hiltViewModel()
    val messageCount by mainViewModel.messageCountFlow.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize()) {
        // ── 最左侧：常驻主导航栏（与主页同一个 MiuixRailNavigationSuite）──────────────────
        MiuixRailNavigationSuite(
            mainNavigationSuiteType = calculateMainNavigationSuiteType(),
            destinations = railDestinations,
            // 设置是从「我」进来的，所以侧栏里「我」保持选中。
            isSelected = { it === MainDestination.User },
            onSelect = { destination -> switchToMainTab(navigator, destination) },
            onReSelect = { destination -> switchToMainTab(navigator, destination) },
            messageCount = { messageCount },
            primaryActionContent = {
                AccountNavIcon(onLoginClicked = { navigator.navigate(Destination.Login) })
            },
            onExpandedChange = mainViewModel::setRailExpanded,
        )

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val density = LocalDensity.current
            val leftWidth = maxWidth * TabletSettingsLeftWidthFraction
            val rightWidth = maxWidth - leftWidth

            Row(modifier = Modifier.fillMaxSize()) {
                // ── 中间栏：设置入口（一级 + 二级平铺，按分组）──────────────────────────────
                // 中间栏的顶栏与手机上设置首页同一套：滚动内容从顶栏下穿过时做**顶部渐变模糊**。
                val paneScrollBehavior = MiuixScrollBehavior()
                val paneBackdrop = rememberMiuixBarBackdrop()
                val paneBarColor = if (paneBackdrop != null) Color.Transparent else colors.surface
                MiuixScaffold(
                    modifier = Modifier.width(leftWidth).fillMaxHeight(),
                    topBar = {
                        MiuixProgressiveBlurBar(
                            backdrop = paneBackdrop,
                            scrollBehavior = paneScrollBehavior,
                        ) {
                            // 标题栏只有标题：离开设置交给左侧导航栏或系统返回。
                            MiuixTopAppBar(
                                title = stringResource(R.string.title_settings),
                                color = paneBarColor,
                                scrollBehavior = paneScrollBehavior,
                            )
                        }
                    },
                ) { contentPadding ->
                    LazyColumn(
                        state = leftListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(paneScrollBehavior.nestedScrollConnection)
                            .miuixLayerBackdrop(paneBackdrop),
                        contentPadding = contentPadding + PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        groups.forEach { (groupRes, dests) ->
                            item(key = "group_$groupRes") {
                                Column {
                                    SmallTitle(text = stringResource(groupRes))
                                    Card(insideMargin = PaddingValues(0.dp)) {
                                        dests.forEach { dest ->
                                            ArrowPreference(
                                                title = stringResource(dest.titleRes),
                                                startAction = {
                                                    Icon(
                                                        imageVector = dest.icon,
                                                        contentDescription = null,
                                                        tint = colors.onBackground,
                                                        modifier = Modifier.padding(end = TabletSettingsIconEndPadding),
                                                    )
                                                },
                                                holdDownState = dest === selected.listHighlight(),
                                                onClick = { selected = dest },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 右栏：选中项内容 ────────────────────────────────────────────────────────
                // 页面按**手机上的原样**渲染。这一点不能改：页面的 Miuix Scaffold 是下拉菜单弹层的
                // 宿主（LiquidGlassDropdown 需要最近的 Scaffold），去掉它下拉就点不开；页面自带顶栏
                // 也才能让「关于」的流光背景一直铺到最顶部，并让列表滚动/窗口 inset 与手机一致。
                val onBack: () -> Unit = navigator::navigateUp
                Column(modifier = Modifier.width(rightWidth).fillMaxHeight().background(colors.surface)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (selected) {
                            TabletSettingsDest.AccountManage -> AccountManagePage(
                                myLittleTailSettings = settingsRepo.myLittleTail,
                                navigator,
                            )

                            TabletSettingsDest.OKSign -> OKSignSettingsPage(
                                settings = settingsRepo.signConfig,
                                onBack = onBack,
                            )

                            TabletSettingsDest.WorkInfo -> WorkInfoPage(
                                onBack = onBack,
                            )

                            TabletSettingsDest.BlockSettings -> BlockSettingsPage(
                                settings = settingsRepo.blockSettings,
                                navigator,
                                onOpenForumBlockList = { selected = TabletSettingsDest.ForumBlockList },
                                onOpenUserBlockList = { selected = TabletSettingsDest.UserBlockList },
                                onOpenKeywordBlockList = { selected = TabletSettingsDest.KeywordBlockList },
                            )

                            TabletSettingsDest.ForumBlockList -> ForumBlockListPage(
                                onBack = onBack,
                            )

                            TabletSettingsDest.UserBlockList -> UserBlockListPage(
                                onBack = onBack,
                            )

                            TabletSettingsDest.KeywordBlockList -> KeywordBlockListPage(
                                onBack = onBack,
                            )

                            TabletSettingsDest.UI -> UISettingsPage(
                                settings = settingsRepo.uiSettings,
                                navigator,
                                onOpenAppFont = { selected = TabletSettingsDest.AppFont },
                            )

                            TabletSettingsDest.AppFont -> AppFontPage(
                                onBack = onBack,
                            )

                            TabletSettingsDest.Habit -> HabitSettingsPage(
                                habitSettings = settingsRepo.habitSettings,
                                onStickyHeaderClicked = { selected = TabletSettingsDest.StickyHeader },
                                onBack = onBack,
                            )

                            TabletSettingsDest.StickyHeader -> StickyHeaderSettingsPage(
                                settingsRepo.habitSettings,
                                onBack = onBack,
                            )

                            TabletSettingsDest.Privacy -> PrivacySettingsPage(
                                settingsRepo.privacySettings,
                                onBack = onBack,
                            )

                            TabletSettingsDest.More -> MoreSettingsPage(
                                navigator,
                                onOpenWorkInfo = { selected = TabletSettingsDest.WorkInfo },
                            )

                            TabletSettingsDest.About -> AboutPage(onBackClicked = onBack)
                        }
                    }
                }
            }

            // 左右栏分隔线：与参考实现同样的 1.dp 细线，深色白 10% / 浅色黑 6%。
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(with(density) { leftWidth.roundToPx() }, 0) }
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(dividerColor),
            )
        }
    }
}
