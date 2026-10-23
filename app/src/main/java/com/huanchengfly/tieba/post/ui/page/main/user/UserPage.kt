package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.core.layout.WindowSizeClass
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.settings.SettingsDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
// Miuix Text defaults to the Miuix main style; aliased to coexist with the material3 Text
// used inside the TopAppBarPaged title slot (which relies on the bar-provided title style).
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val UserPageHorizontalPadding = 16.dp

private val UserMenuIconEndPadding = 8.dp

private val UserAvatarSize = 88.dp

@Composable
private fun UserHeaderPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(UserAvatarSize)
                .placeholder(shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiuixText(
                text = "",
                style = MiuixTheme.textStyles.title2,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(),
            )
            MiuixText(
                text = "",
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(),
            )
        }
    }
}

@Composable
private fun StatCardPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .height(76.dp)
                .fillMaxWidth()
                .placeholder()
        )
    }
}

/**
 * Large avatar header: avatar at the start, name / colored intro / UID stacked next to it.
 */
@Composable
private fun UserHeader(
    modifier: Modifier = Modifier,
    userName: String,
    userIntro: String?,
    avatar: String?,
    uid: Long,
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            data = avatar,
            size = UserAvatarSize,
            contentDescription = stringResource(id = R.string.desc_user_avatar),
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MiuixText(
                text = userName,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            MiuixText(
                text = userIntro ?: stringResource(id = R.string.tip_no_intro),
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            MiuixText(
                text = "UID $uid",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * Grouped stat card: three columns of big numbers, HyperOS group-card style.
 */
@Composable
private fun StatCard(
    posts: String?,
    fans: String?,
    concerned: String?,
    modifier: Modifier = Modifier,
    onPostsClick: (() -> Unit)? = null,
    onFansClick: (() -> Unit)? = null,
    onFollowClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCardItem(
                modifier = Modifier.onNotNull(onPostsClick) { clickable(onClick = it) },
                title = stringResource(R.string.title_stat_posts_num),
                stat = posts,
            )

            StatCardItem(
                modifier = Modifier.onNotNull(onFansClick) { clickable(onClick = it) },
                title = stringResource(R.string.text_stat_fans),
                stat = fans,
            )

            StatCardItem(
                modifier = Modifier.onNotNull(onFollowClick) { clickable(onClick = it) },
                title = stringResource(R.string.text_stat_follow),
                stat = concerned,
            )
        }
    }
}

@Composable
private fun RowScope.StatCardItem(title: String, stat: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MiuixText(text = stat ?: 0.toString(), style = MiuixTheme.textStyles.title2)

        Spacer(modifier = Modifier.height(4.dp))

        MiuixText(
            text = title,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun LoginTipCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        MenuRow(
            icon = MiuixIcons.Contacts,
            text = stringResource(id = R.string.tip_login),
            onClick = onClick,
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = text,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.padding(end = UserMenuIconEndPadding),
            )
        },
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
fun UserPage(viewModel: UserViewModel = viewModel()) {
    val navigator = LocalNavController.current
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val windowSizeClass = LocalWindowAdaptiveInfo.current.windowSizeClass
    val isWindowHeightExpanded = windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND)
    // Same dual-path top bar blur as the Home/Explore tabs: Miuix progressive blur when the
    // device supports RuntimeShader (API 33+), flat Haze blur as the fallback on older devices,
    // plain opaque bar when neither is available. LocalHazeState comes from animatedMainComposable.
    val barBackdrop = rememberMiuixBarBackdrop()
    val scrollState = rememberScrollState()

    MyScaffold(
        modifier = Modifier.fillMaxSize(),
        useMD2Layout = true,
        topBar = {
            TopAppBarPaged(
                modifier = Modifier,
                title = { Text(text = stringResource(R.string.my_page_title)) },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                // Counterweight matching the end action (40 dp button + 4 dp end padding) so the
                // centered title stays on the true screen center like the reference design.
                navigationIcon = { Spacer(modifier = Modifier.width(44.dp)) },
                actions = {
                    IconButton(
                        onClick = { navigator.navigateDebounced(SettingsDestination.Settings) },
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Settings,
                            contentDescription = stringResource(R.string.title_settings),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                backdrop = barBackdrop,
            )
        },
        bottomBar = bottomNavigationPlaceholder, // MainPage BottomNavBar placeholder
        bottomBarAtop = calculateMainNavigationSuiteType().isFloatingNavigationBar,
    ) { contentPaddings ->
        val account = LocalAccount.current

        // Blur captures must WRAP every node that moves during a gesture: the scroll node and the
        // pull-to-refresh container, which offsets its content while the user drags. A capture
        // nested inside them records the undisplaced layout, so the bar's blur keeps showing the
        // content at its old offset — the ghosted/duplicated header seen while pulling 我的.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .miuixLayerBackdrop(barBackdrop),
        ) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = viewModel::onRefresh,
                contentPadding = contentPaddings,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .onCase(!isWindowHeightExpanded) { verticalScroll(scrollState) }
                        .padding(contentPaddings)
                        .padding(horizontal = UserPageHorizontalPadding)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (account != null) {
                        UserHeader(
                            modifier = Modifier.clickable {
                                navigator.navigateDebounced(Destination.UserProfile(uid = account.uid))
                            },
                            userName = account.nickname ?: account.name,
                            userIntro = account.intro?.takeUnless { it.isEmpty() },
                            avatar = remember { StringUtil.getAvatarUrl(account.portrait) },
                            uid = account.uid,
                        )
                    } else if (isLoading) {
                        UserHeaderPlaceholder()
                    } else {
                        LoginTipCard(onClick = { navigator.navigateDebounced(Destination.Login) })
                    }

                    if (account != null) {
                        StatCard(
                            modifier = Modifier.padding(top = 8.dp),
                            posts = account.posts,
                            fans = account.fans,
                            concerned = account.concerned,
                            onPostsClick = {
                                navigator.navigateDebounced(
                                    Destination.UserProfile(uid = account.uid, showPostsTab = true)
                                )
                            },
                            onFansClick = {
                                navigator.navigateDebounced(Destination.UserFansList(uid = account.uid))
                            },
                            onFollowClick = {
                                navigator.navigateDebounced(Destination.UserFollowList(uid = account.uid))
                            },
                        )
                    } else if (isLoading) {
                        StatCardPlaceholder(modifier = Modifier.padding(top = 8.dp))
                    }

                    SmallTitle(text = stringResource(R.string.my_section_content))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        MenuRow(
                            icon = MiuixIcons.Favorites,
                            text = stringResource(id = R.string.title_my_collect),
                            enabled = account != null,
                            onClick = { navigator.navigateDebounced(Destination.ThreadStore) },
                        )
                        MenuRow(
                            icon = MiuixIcons.Recent,
                            text = stringResource(id = R.string.title_history),
                            onClick = { navigator.navigateDebounced(Destination.History) },
                        )
                        MenuRow(
                            icon = MiuixIcons.Help,
                            text = stringResource(id = R.string.my_info_service_center),
                            enabled = account != null,
                            onClick = {
                                navigator.navigateDebounced(
                                    Destination.WebView(
                                        initialUrl = "https://tieba.baidu.com/mo/q/hybrid-main-service/uegServiceCenter?cuid=${CuidUtils.getNewCuid()}&cuid_galaxy2=${CuidUtils.getNewCuid()}&cuid_gid=&timestamp=${System.currentTimeMillis()}&_client_version=${ClientVersion.TIEBA_V12.version}&nohead=1"
                                    )
                                )
                            },
                        )
                        MenuRow(
                            icon = MiuixIcons.Ok,
                            text = stringResource(id = R.string.title_oksign),
                            enabled = account != null,
                            onClick = { navigator.navigateDebounced(SettingsDestination.OKSign) },
                        )
                    }

                    SmallTitle(text = stringResource(R.string.my_section_other))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        MenuRow(
                            icon = MiuixIcons.Contacts,
                            text = stringResource(id = R.string.title_account_manage),
                            onClick = {
                                if (account != null) {
                                    navigator.navigateDebounced(SettingsDestination.AccountManage)
                                } else {
                                    navigator.navigateDebounced(Destination.Login)
                                }
                            },
                        )
                        MenuRow(
                            icon = MiuixIcons.Info,
                            text = stringResource(id = R.string.my_info_about),
                            onClick = { navigator.navigateDebounced(SettingsDestination.About) },
                        )
                    }

                    if (isWindowHeightExpanded) {
                        Spacer(modifier = Modifier.weight(0.9f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
