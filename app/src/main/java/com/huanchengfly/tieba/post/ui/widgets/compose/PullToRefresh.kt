package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.PaddingNone
import top.yukonga.miuix.kmp.basic.PullToRefresh

/**
 * Pull-to-refresh container backed by Miuix's [PullToRefresh].
 *
 * This used to be a copy of Material3's `PullToRefresh.kt` (the old file documented its upstream
 * commit) that drew `PullToRefreshDefaults.LoadingIndicator` — the M3 Expressive circular/polygon
 * glyph, which read as a foreign shape next to the MIUI surfaces. Miuix's component owns the whole
 * indicator: distance fraction, threshold crossing, the rotating arc and the "松开刷新" label, all
 * driven by MIUI tokens and motion curves.
 *
 * `state` and `indicator` from the old signature are gone. Miuix's component owns both, and none of
 * the call sites passed them.
 */
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingNone,
    content: @Composable BoxScope.() -> Unit,
) {
    if (enabled) {
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            contentPadding = contentPadding,
            content = { Box(content = content) },
        )
    } else {
        Box(modifier = modifier, content = content)
    }
}