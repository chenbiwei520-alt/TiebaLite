package com.huanchengfly.tieba.post.ui.models.settings

import androidx.compose.ui.graphics.Color
import com.huanchengfly.tieba.post.theme.TiebaBlue

enum class Theme {
    TRANSLUCENT, BLUE, GREEN, ORANGE, PINK, PURPLE
}

/**
 * App theme settings
 *
 * @param theme Theme
 * @param transColor seed color of [Theme.TRANSLUCENT]
 * @param transDarkColorMode color mode of [Theme.TRANSLUCENT]
 * @param transBackground background image file name of [Theme.TRANSLUCENT]
 * */
data class ThemeSettings(
    val theme: Theme = Theme.BLUE,
    val transColor: Color = TiebaBlue,
    val transAlpha: Float,
    val transBlur: Float,
    val transDarkColorMode: Boolean = false,
    val transBackground: String? = null
)