package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.common.theme.compose.animateBackground
import com.huanchengfly.tieba.post.ui.miuix.LiquidGlassDropdownPreference
import com.huanchengfly.tieba.post.ui.miuix.LocalLiquidGlassBackdrop
import com.huanchengfly.tieba.post.ui.miuix.MiuixProgressiveBlurBar
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.miuix.SquircleShape
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LocalLiquidGlassPopupBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as kyantRememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop as kyantRememberCombinedBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.subposts.PostLikeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixBackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.StringUtil
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppThemePage(
    navigator: NavController = LocalNavController.current,
    viewModel: AppThemeViewModel = hiltViewModel()
) {
    val darkPreference by viewModel.darkPreferenceFlow.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val snackbarHostState = rememberSnackbarHostState()
    val backdrop = rememberMiuixBarBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    // Kyant twin of the capture above: the vendored reference dropdown paints its glass through
    // Kyant's drawBackdrop/blur, and a Kyant Backdrop is a SEPARATE recording layer from the Miuix
    // one. This page builds `MiuixScaffold` directly instead of `MyScaffold`, so it has to publish
    // the capture itself — without it the dropdown has no glass at all.
    var popupCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val popupLayerBackdrop = if (isRuntimeShaderSupported()) kyantRememberLayerBackdrop() else null
    // 顶栏的第二层捕获，理由同 MyScaffold：Scaffold 的 TopBar 与 content 是兄弟节点，只覆盖
    // content 的捕获在顶栏那一条上是透明的。用 Kyant 自带的 rememberCombinedBackdrop 合成。
    val barLayerBackdrop = if (isRuntimeShaderSupported()) kyantRememberLayerBackdrop() else null
    val popupBackdrop = if (popupLayerBackdrop != null && barLayerBackdrop != null) {
        kyantRememberCombinedBackdrop(popupLayerBackdrop, barLayerBackdrop)
    } else {
        popupLayerBackdrop
    }

    // Ordered lightest -> darkest, matching the HyperOS theme mode picker
    val modeOptions = listOf(
        DarkPreference.FOLLOW_SYSTEM to R.string.summary_theme_mode_system,
        DarkPreference.DISABLED to R.string.summary_theme_mode_light,
        DarkPreference.ALWAYS to R.string.summary_theme_mode_dark,
    )
    val modeItems = modeOptions.map { stringResource(it.second) }
    val selectedIndex = modeOptions.indexOfFirst { it.first == darkPreference }.coerceAtLeast(0)

    MiuixScaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (barLayerBackdrop != null) Modifier.kyantLayerBackdrop(barLayerBackdrop) else Modifier),
            ) {
                MiuixProgressiveBlurBar(backdrop = backdrop, scrollBehavior = scrollBehavior) {
                    MiuixTopAppBar(
                        title = stringResource(R.string.title_theme),
                        color = barColor,
                        navigationIcon = { MiuixBackNavigationIcon(onBackPressed = navigator::navigateUp) },
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
        snackbarHost = { SwipeToDismissSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            // This page uses `MiuixScaffold` directly rather than `MyScaffold`, so it has to publish
            // its capture itself. The capture is the list below — i.e. page content, which the
            // Scaffold's popup layer sits outside of, so the glass samples the page and not its own
            // panel.
            LocalLiquidGlassBackdrop provides backdrop,
            LocalLiquidGlassPopupBackdrop provides popupBackdrop,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .miuixLayerBackdrop(backdrop)
                    .then(
                        if (popupLayerBackdrop != null) {
                            Modifier.kyantLayerBackdrop(popupLayerBackdrop)
                        } else {
                            Modifier
                        }
                    )
                    .onGloballyPositioned { popupCoordinates = it },
                contentPadding = contentPadding + PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // Miuix dark / light color scheme mode, selected through a popup menu
                item {
                    SmallTitle(text = stringResource(R.string.settings_group_theme))
                    Card {
                        LiquidGlassDropdownPreference(
                            items = modeItems,
                            selectedIndex = selectedIndex,
                            title = stringResource(R.string.title_theme_mode),
                            summary = modeItems[selectedIndex],
                            onSelectedIndexChange = { index ->
                                viewModel.onDarkPreferencePicked(modeOptions[index].first)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TranslucentThemeBackground(modifier: Modifier = Modifier, file: java.io.File?) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(file)
            .diskCachePolicy(coil3.request.CachePolicy.DISABLED)
            .build(),
        contentDescription = null,
        modifier = modifier,
        error = androidx.compose.ui.res.painterResource(R.drawable.user_header),
        contentScale = ContentScale.Crop,
    )
}

// Used by StickyHeaderSettingsPage to preview the sticky header widget
@Composable
fun UserPostCardWidget(modifier: Modifier = Modifier, account: com.huanchengfly.tieba.post.models.database.Account?, postText: Int) {
    val colorScheme = androidx.compose.material3.MaterialTheme.colorScheme

    CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
        Column(
            modifier = modifier
                .animateBackground(color = colorScheme.surface, shape = SquircleShape(SquircleCornerRadius.Small))
                .padding(all = 12.dp)
                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
        ) {
            UserHeader(
                avatar = {
                    if (account?.portrait.isNullOrEmpty()) {
                        Avatar(R.drawable.ic_launcher_new_round, size = com.huanchengfly.tieba.post.ui.widgets.compose.Sizes.Small)
                    } else {
                        Avatar(remember { StringUtil.getAvatarUrl(account.portrait) }, size = com.huanchengfly.tieba.post.ui.widgets.compose.Sizes.Small)
                    }
                },
                name = {
                    Text(text = account?.name ?: stringResource(R.string.app_name))
                },
                desc = {
                    Text(text = stringResource(R.string.relative_date_minute, 1), maxLines = 1)
                },
            ) {
                var like by remember { mutableStateOf(Like(true, 99999)) }
                PostLikeButton(like, onClick = { like = !like })
            }

            Text(
                text = stringResource(id = postText),
                modifier = Modifier.padding(start = 44.dp, top = 8.dp)
            )
        }
    }
}
