package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Card

/**
 * The app's standard list-item card.
 *
 * Every list — feed, forum thread, notifications, history, hot topics, search results — renders its
 * rows through this wrapper so the surface, corner radius and press feedback stay identical across
 * screens instead of each page inventing its own row style.
 *
 * [Card] hosts its content in a `Column`, which measures children with `minWidth = 0`, so content
 * that should span the card has to set `fillMaxWidth()` itself.
 *
 * @param onClick optional; when provided the whole card becomes pressable with a ripple.
 */
@Composable
fun ListItemCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = Sizes.CornerRadius,
        showIndication = onClick != null,
        onClick = onClick,
        content = content,
    )
}