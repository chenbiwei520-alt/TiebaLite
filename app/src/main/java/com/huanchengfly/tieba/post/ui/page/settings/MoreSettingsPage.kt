package com.huanchengfly.tieba.post.ui.page.settings

import android.annotation.SuppressLint
import android.os.Build
import android.text.format.Formatter
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.Icon
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PreferenceGroupPosition
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.TiebaWebView.Companion.dumpWebViewVersion
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.utils.ImageCacheUtil
import com.huanchengfly.tieba.post.utils.buildAppSettingsIntent
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("WebViewApiAvailability")
@Composable
fun MoreSettingsPage(
    navigator: NavController,
    /** 面板模式：平板设置页的右栏自己画标题栏，本页不再画自己的顶栏。 */
    paneMode: Boolean = false,
    /** 面板模式下由外层接管子页（平板两栏不跳转）；为 null 时保持原来的页面跳转。 */
    onOpenWorkInfo: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    SettingsScaffold(
        titleRes = R.string.title_settings_more,
        onBack = navigator::navigateUp,
        paneMode = paneMode,
    ) {
        group(title = R.string.summary_settings_more) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                preference(
                    title = { Text(text = stringResource(R.string.title_use_webview)) },
                    summary = {
                        val version = remember { dumpWebViewVersion(context) }
                        Text(text = version ?: stringResource(id = R.string.toast_load_failed))
                    },
                    onClick = {
                        runCatching {
                            val pkg = WebView.getCurrentWebViewPackage() ?: return@runCatching
                            context.startActivity(buildAppSettingsIntent(pkg.packageName))
                        }
                    },
                    icon = {
                        Icon(ImageVector.vectorResource(id = R.drawable.ic_chrome), contentDescription = null)
                    },
                )
            }

            preference(
                title = R.string.title_settings_worker,
                onClick = onOpenWorkInfo ?: {
                    navigator.navigate(route = SettingsDestination.WorkInfo)
                },
                leadingIcon = Icons.Outlined.Analytics,
            )

            // 运行日志：手机与平板都跳到独立页面（同一条导航图，两栏布局下也是整页覆盖）。
            preference(
                title = R.string.title_settings_log,
                summary = R.string.summary_settings_log,
                onClick = {
                    navigator.navigate(route = SettingsDestination.LogViewer)
                },
                leadingIcon = MiuixIcons.File,
            )
        }

        group(title = R.string.settings_group_cache) {
            customPreference {
                ImageCachePreference(position = it)
            }
        }
    }
}

@Composable
private fun ImageCachePreference(modifier: Modifier = Modifier, position: PreferenceGroupPosition) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    var diskCacheJob: Job? by retain { mutableStateOf(null) }
    BackHandler(diskCacheJob != null) {} // 硬控用户直到清除完成

    var cacheSize: String? by rememberSaveable { mutableStateOf(null) }
    if (cacheSize == null) {
        LaunchedEffect(Unit) {
            val size = ImageCacheUtil.getCacheSize(context.applicationContext)
            val formattedSize = Formatter.formatShortFileSize(context, size)
            cacheSize = context.getString(R.string.tip_cache, formattedSize)
        }
    }

    SegmentedPreference(
        modifier = modifier,
        title = stringResource(id = R.string.title_clear_picture_cache),
        position = position,
        leadingIcon = Icons.Rounded.DeleteForever,
        summary = cacheSize ?: stringResource(id = R.string.text_loading),
        onClick = {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.tip_clearing_cache),
                    duration = SnackbarDuration.Indefinite
                )
            }
            diskCacheJob = coroutineScope
                .launch {
                    ImageCacheUtil.clearImageAllCache(context.applicationContext)
                }
                .also {
                    it.invokeOnCompletion {
                        diskCacheJob = null
                        cacheSize = context.getString(R.string.tip_cache, "0B")
                        coroutineScope.launch {
                            snackbarHostState.newestSnackbarData()?.dismiss()
                            delay(300)
                            snackbarHostState.showSnackbar(context.getString(R.string.toast_clear_picture_cache_success))
                        }
                    }
                }
        },
        enabled = cacheSize != null && diskCacheJob == null,
    )
}
