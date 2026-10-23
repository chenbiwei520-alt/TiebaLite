package com.huanchengfly.tieba.post.ui.page.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.miuix.LocalLiquidGlassBackdrop
import com.huanchengfly.tieba.post.ui.miuix.MiuixProgressiveBlurBar
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.ProvideRecordingBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LocalLiquidGlassPopupBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as kyantRememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop as kyantRememberCombinedBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import com.huanchengfly.tieba.post.ui.page.Destination.Login
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.About
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination.AccountManage
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixBackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedTextPrefsScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SettingsSegmentedPrefsScope
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
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
import com.huanchengfly.tieba.post.plus
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

private val SettingsContentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

private val SettingsGroupSpacing = 12.dp

private val SettingsIconEndPadding = 8.dp

@Composable
fun SettingsPage(navigator: NavController) {
    val account = LocalAccount.current
    val scrollBehavior = MiuixScrollBehavior()
    val snackbarHostState = rememberSnackbarHostState()
    val backdrop = rememberMiuixBarBackdrop()
    // Blur replaces the plain surface background while it is active
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    MiuixScaffold(
        topBar = {
            MiuixProgressiveBlurBar(backdrop = backdrop, scrollBehavior = scrollBehavior) {
                MiuixTopAppBar(
                    title = stringResource(R.string.title_settings),
                    color = barColor,
                    navigationIcon = { MiuixBackNavigationIcon(onBackPressed = navigator::navigateUp) },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = { SwipeToDismissSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .miuixLayerBackdrop(backdrop),
                contentPadding = contentPadding + SettingsContentPadding,
            ) {
                item {
                    Card(modifier = Modifier.padding(bottom = SettingsGroupSpacing)) {
                        if (account != null) {
                            AccountPreferenceRow(
                                account = account,
                                onClick = { navigator.navigateDebounced(route = AccountManage) },
                            )
                        } else {
                            SettingsPreferenceRow(
                                titleRes = R.string.title_account_manage,
                                summaryRes = R.string.summary_not_logged_in,
                                icon = MiuixIcons.Contacts,
                                onClick = { navigator.navigateDebounced(route = Login) },
                            )
                        }

                        SettingsPreferenceRow(
                            titleRes = R.string.title_oksign,
                            summaryRes = R.string.summary_settings_oksign,
                            icon = MiuixIcons.Tasks,
                            enabled = account != null,
                            onClick = { navigator.navigateDebounced(SettingsDestination.OKSign) },
                        )
                    }
                }

                item {
                    Card(modifier = Modifier.padding(bottom = SettingsGroupSpacing)) {
                        SettingsPreferenceRow(
                            titleRes = R.string.title_block_settings,
                            summaryRes = R.string.summary_block_settings,
                            icon = MiuixIcons.Blocklist,
                            onClick = { navigator.navigateDebounced(SettingsDestination.BlockSettings) },
                        )
                    }
                }

                item {
                    Card(modifier = Modifier.padding(bottom = SettingsGroupSpacing)) {
                        SettingsPreferenceRow(
                            titleRes = R.string.title_settings_custom,
                            summaryRes = R.string.summary_settings_custom,
                            icon = MiuixIcons.Theme,
                            onClick = { navigator.navigateDebounced(SettingsDestination.UI) },
                        )
                        SettingsPreferenceRow(
                            titleRes = R.string.title_settings_read_habit,
                            summaryRes = R.string.summary_settings_habit,
                            icon = MiuixIcons.Tune,
                            onClick = { navigator.navigateDebounced(SettingsDestination.Habit) },
                        )
                    }
                }

                item {
                    Card(modifier = Modifier.padding(bottom = SettingsGroupSpacing)) {
                        SettingsPreferenceRow(
                            titleRes = R.string.title_settings_privacy,
                            summaryRes = R.string.summary_settings_privacy,
                            icon = MiuixIcons.Lock,
                            onClick = { navigator.navigateDebounced(SettingsDestination.Privacy) },
                        )
                    }
                }

                item {
                    Card(modifier = Modifier.padding(bottom = SettingsGroupSpacing)) {
                        SettingsPreferenceRow(
                            titleRes = R.string.title_settings_more,
                            summaryRes = R.string.summary_settings_more,
                            icon = MiuixIcons.More,
                            onClick = { navigator.navigateDebounced(SettingsDestination.More) },
                        )
                        SettingsPreferenceRow(
                            titleRes = R.string.title_about,
                            summaryRes = R.string.summary_settings_about,
                            icon = MiuixIcons.Info,
                            onClick = { navigator.navigateDebounced(About) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountPreferenceRow(account: Account, onClick: () -> Unit) {
    val avatarUrl = remember { StringUtil.getAvatarUrl(account.portrait) }
    val name = account.nickname ?: account.name

    ArrowPreference(
        title = stringResource(R.string.title_account_manage),
        summary = stringResource(R.string.summary_now_account, name),
        startAction = {
            Icon(
                imageVector = MiuixIcons.Contacts,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(end = SettingsIconEndPadding),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun SettingsPreferenceRow(
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = stringResource(titleRes),
        summary = stringResource(summaryRes),
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(end = SettingsIconEndPadding),
            )
        },
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
fun <T> SettingsScaffold(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    // Kept for call-site compatibility; the Miuix top app bar always aligns the title to the start
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onBack: () -> Unit,
    /** 面板模式：平板设置页的右栏自己画标题栏，本页的顶栏（含返回键）不再渲染。 */
    paneMode: Boolean = false,
    settings: Settings<T>,
    initialValue: T,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SwipeToDismissSnackbarHost(snackbarHostState) },
    content: SettingsSegmentedPrefsScope<T>.() -> Unit
) {
    MiuixSettingsScaffold(
        modifier = modifier,
        titleRes = titleRes,
        onBack = onBack,
        paneMode = paneMode,
        snackbarHostState = snackbarHostState,
        snackbarHost = snackbarHost,
    ) { contentPadding ->
        SegmentedPrefsScreen(
            modifier = Modifier.fillMaxHeight(),
            settings = settings,
            initialValue = initialValue,
            contentPadding = contentPadding + SettingsContentPadding,
            content = content
        )
    }
}

@Composable
fun SettingsScaffold(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    // Kept for call-site compatibility; the Miuix top app bar always aligns the title to the start
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onBack: () -> Unit,
    /** 面板模式：平板设置页的右栏自己画标题栏，本页的顶栏（含返回键）不再渲染。 */
    paneMode: Boolean = false,
    // Painted inside the scaffold's content box, *behind* the preference list and above the
    // scaffold's own surface. Empty by default, so every page that does not pass one keeps the
    // plain surface background it has today. The `BoxScope` receiver is what lets a layer use
    // `matchParentSize()` and follow the content box without affecting the list's layout.
    background: @Composable BoxScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SwipeToDismissSnackbarHost(snackbarHostState) },
    content: SegmentedPrefsScope.() -> Unit
) {
    MiuixSettingsScaffold(
        modifier = modifier,
        titleRes = titleRes,
        onBack = onBack,
        paneMode = paneMode,
        snackbarHostState = snackbarHostState,
        snackbarHost = snackbarHost,
        background = background,
    ) { contentPadding ->
            SegmentedTextPrefsScreen(
                modifier = Modifier.fillMaxHeight(),
                contentPadding = contentPadding + SettingsContentPadding,
                content = content
            )
        }
}

@Composable
private fun MiuixSettingsScaffold(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    /** 面板模式：不画 Scaffold 顶栏（标题与返回由外层面板负责），只保留内容与玻璃捕获。 */
    paneMode: Boolean = false,
    snackbarHostState: SnackbarHostState,
    snackbarHost: @Composable () -> Unit,
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBarBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    // Kyant 版同样的捕获：参考弹窗走 Kyant 的 drawBackdrop/blur，与 Miuix 捕获是两个层。
    val popupLayerBackdrop = if (isRuntimeShaderSupported()) kyantRememberLayerBackdrop() else null
    // 顶栏的第二层捕获：Scaffold 把 TopBar 与 content 当兄弟节点排布，只覆盖 content 的捕获
    // 在顶栏那一条上是透明的 —— 从顶栏落下的下拉只剩 0.72/0.8 的染色，顶栏原样透出来。
    // Kyant 自带的 rememberCombinedBackdrop 把两层按消费者坐标对齐后合成一张底片。
    val barLayerBackdrop = if (isRuntimeShaderSupported()) kyantRememberLayerBackdrop() else null
    // 与参考一致：直接把 Kyant 的 LayerBackdrop 交给弹窗，它自己记住录制节点的坐标。
    val popupBackdrop = if (popupLayerBackdrop != null && barLayerBackdrop != null) {
        kyantRememberCombinedBackdrop(popupLayerBackdrop, barLayerBackdrop)
    } else {
        popupLayerBackdrop
    }

    // 面板模式下没有 Scaffold 提供的窗口 inset，内容自己吃系统栏。
    val contentBlock: @Composable (PaddingValues) -> Unit = { contentPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .miuixLayerBackdrop(backdrop)
                .then(if (popupLayerBackdrop != null) Modifier.kyantLayerBackdrop(popupLayerBackdrop) else Modifier),
        ) {
            // `backdrop` is recorded by the Box above; publish it so a descendant that tries to
            // capture the same layer is skipped rather than nesting `record()` (which crashes).
            ProvideRecordingBackdrop(backdrop) {
                // Behind the list, above the scaffold surface — see `SettingsScaffold.background`.
                background()
                content(contentPadding)
            }
        }
    }

    // Published around the whole Scaffold — not only around its content — so the dropdown sheet
    // rendered in the popup layer, which is a sibling of the content, sees the captured page.
    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalLiquidGlassBackdrop provides backdrop,
        LocalLiquidGlassPopupBackdrop provides popupBackdrop,
    ) {
        if (paneMode) {
            androidx.compose.foundation.layout.Box(
                modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
            ) {
                contentBlock(PaddingValues.Zero)
            }
        } else {
            MiuixScaffold(
                modifier = modifier,
                topBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (barLayerBackdrop != null) Modifier.kyantLayerBackdrop(barLayerBackdrop) else Modifier),
                    ) {
                        MiuixProgressiveBlurBar(backdrop = backdrop, scrollBehavior = scrollBehavior) {
                            MiuixTopAppBar(
                                title = stringResource(titleRes),
                                color = barColor,
                                navigationIcon = { MiuixBackNavigationIcon(onBackPressed = onBack) },
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    }
                },
                snackbarHost = snackbarHost,
                content = contentBlock,
            )
        }
    }
}
