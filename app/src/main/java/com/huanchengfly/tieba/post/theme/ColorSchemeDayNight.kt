package com.huanchengfly.tieba.post.theme

import androidx.compose.material3.ColorScheme

data class ColorSchemeDayNight(
    val lightColor: ColorScheme,
    val darkColor: ColorScheme,
) {
    fun getColorScheme(isDark: Boolean): ColorScheme = if (isDark) darkColor else lightColor
}
