package com.huanchengfly.tieba.post.ui.page.settings.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeViewModel.Companion.translucentBackground
import com.huanchengfly.tieba.post.theme.ColorSchemeDayNight
import com.huanchengfly.tieba.post.theme.colorscheme.translucentColorScheme
import com.huanchengfly.tieba.post.ui.models.settings.Theme
import com.huanchengfly.tieba.post.ui.models.settings.ThemeSettings
import java.io.File

@Stable
interface AppTheme {
    val name: Int

    val description: Int

    fun getColorScheme(darkMode: Boolean): ColorScheme
}

@Immutable
open class BuiltInTheme(val theme: Theme, val colors: ColorSchemeDayNight) : AppTheme {

    override val name: Int = getThemeNameRes(theme)

    override val description: Int
        get() = R.string.summary_settings_custom

    override fun getColorScheme(darkMode: Boolean) = if (darkMode) colors.darkColor else colors.lightColor
}

@Immutable
class TranslucentTheme(settings: ThemeSettings, context: Context) : BuiltInTheme(
    theme = Theme.TRANSLUCENT,
    colors = translucentColorScheme(settings.transColor, settings.transDarkColorMode)
) {
    val background: File? = settings.transBackground?.let { context.translucentBackground(it) }
}

private fun getThemeNameRes(theme: Theme) = when (theme) {
    Theme.TRANSLUCENT -> R.string.theme_translucent
    Theme.BLUE -> R.string.theme_blue
    Theme.GREEN -> R.string.theme_green
    Theme.ORANGE -> R.string.theme_orange
    Theme.PINK -> R.string.theme_pink
    Theme.PURPLE -> R.string.theme_purple
}