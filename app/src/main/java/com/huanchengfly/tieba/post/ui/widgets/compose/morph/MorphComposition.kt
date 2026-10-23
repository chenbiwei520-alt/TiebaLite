package com.huanchengfly.tieba.post.ui.widgets.compose.morph

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import com.huanchengfly.tieba.post.models.PhotoViewData

/**
 * 形变动画的图片起点：被点的那张缩略图在窗口里的矩形、圆角、纵横比与缩略图地址。
 * 由信息流里的缩略图在点击的瞬间提供，飞行层就从这里长到全屏。
 * */
data class MorphAnchor(
    val url: String,
    val bounds: Rect,
    val cornerRadiusPx: Float,
    val ratio: Float?,
    val thumbUrl: String? = null,
)

/** 全应用共享一个 [MorphState]：主窗口的叠层、看图页与卡牌堆读的是同一份进度。 */
val LocalMorphState = staticCompositionLocalOf<MorphState?> { null }

@Composable
fun rememberMorphState(): MorphState {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember(density, scope) {
        MorphState(
            progress = Animatable(0f),
            density = density,
            scope = scope,
        )
    }
}

/**
 * 打开看图页时传给 [com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewer] 的图集地址。
 *
 * 顺序必须是接口给的图片顺序（[PhotoViewData.picItems] 已按 picIndex 排好），且不能有重复：
 * 早先的写法先把 data.originUrl 塞到表头、再拼上整份 picItems，于是在被点的那张不是第一张时，
 * 同一张图会同时出现在第 0 位和它自己的位次上——总页数多一页、翻页顺序变成「先被点的、再第一张……」，
 * 多图帖一打开就看得出页码与图片对不上（错位）。data.originUrl 只在没有 picItems 时兜底。
 * */
fun PhotoViewData.originUrls(): List<String> = buildList {
    picItems.mapNotNullTo(this) { it.originUrl }
    if (isEmpty()) data?.originUrl?.let(::add)
}.distinct()
