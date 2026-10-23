package com.huanchengfly.tieba.post.ui.miuix.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 参考 hyper_schedule 的 `com.haooz.chedule.ui.utils.isAppDarkTheme`。
 * 参考读自家 SharedPreferences("app_theme_prefs") 的 theme_mode；TiebaLite 的主题来源是 MiuixTheme，
 * 因此这里改成等价的主题亮度判定（与本项目其它玻璃组件同一写法）。
 */
@Composable
fun isAppDarkTheme(): Boolean = MiuixTheme.colorScheme.surface.luminance() < 0.5f
