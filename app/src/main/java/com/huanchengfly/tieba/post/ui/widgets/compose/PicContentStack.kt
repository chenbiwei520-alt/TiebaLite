package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.LocalMorphState
import com.huanchengfly.tieba.post.utils.TiebaLog
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.squircle.squircleClip
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * 帖子正文的 render 列表。
 *
 * 单张图仍然各占一行；连续出现的多张图合并成一叠 [PicContentStack]（卡牌堆），
 * 左右滑动翻页。之前每张图都是一行全宽，三张图就把一屏撑没了。
 */
@Composable
fun PbContentRenderList(
    renders: List<PbContentRender>,
    modifier: Modifier = Modifier,
) {
    val groups = remember(renders) { groupRenders(renders) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.fastForEach { group ->
            if (group.size > 1) {
                PicContentStack(pics = group.map { it as PicContentRender })
            } else {
                group.fastForEach { it.Render() }
            }
        }
    }
}

/** 把 render 序列切成「一段连续图片」和「单个其它 render」，保持原有顺序。 */
private fun groupRenders(renders: List<PbContentRender>): List<List<PbContentRender>> {
    val groups = mutableListOf<List<PbContentRender>>()
    var currentPics: MutableList<PbContentRender>? = null
    renders.forEach { render ->
        if (render is PicContentRender) {
            val pics = currentPics ?: mutableListOf<PbContentRender>().also {
                currentPics = it
                groups += it
            }
            pics += render
        } else {
            currentPics = null
            groups += listOf(render)
        }
    }
    return groups
}

// 后面最多露两张牌的边；再多的牌没必要画。
private const val StackDepth = 2

// 每往后一层，牌向下 / 向右露出的距离。
private val StackPeekStep = 8.dp
private val StackSideStep = 6.dp

// 拖过这么多（按页算，1.0 = 整张图宽）就翻页；原先按 0.5 判定，要拖过半屏才动。
private const val CommitFraction = 0.15f

// 甩一下能翻过多少页（按松手速度外推的页数比例）。
private const val FlingPagesPerVelocity = 0.22f

/**
 * 多图卡牌堆：当前这张全宽显示，后面的按层缩小并向下、向右错开露出边缘。
 *
 * 状态只有一个浮点 [page]（当前停在第几张，可以是 1.4 这种中间值）：
 * 第 i 张牌的位移 = (i - page) 推导，所以拖动、回弹、翻页都是连续的，
 * 不存在「先滑完再切下标」那一帧的跳变。
 */
@Composable
private fun PicContentStack(
    pics: List<PicContentRender>,
    modifier: Modifier = Modifier,
) {
    // 整叠共用的图片清单：每张卡都带上「这一叠的全部图」，被点的那张用 index 指明。
    // 原先每个 PicContentRender 的 photoViewData 里只有它自己（getPhotoViewData 把 picIndex 写死成 1、
    // picItems 只有一项），点第 2/3 张时画廊一开始只有 1 页，接口回来才换成 N 页，
    // 页码又停在新列表的第 0 张 —— 也就是「打开照片闪一下别的图」。
    val stackPhotoViewData = remember(pics) {
        val items = pics.mapIndexed { i, pic ->
            PicItem(
                picId = pic.picId,
                picIndex = i + 1,
                originUrl = pic.originUrl,
                postId = pic.photoViewData?.data?.postId,
            )
        }.toImmutableList()
        pics.mapIndexed { i, pic -> pic.photoViewData?.copy(picItems = items, index = i) }
    }
    LaunchedEffect(pics) {
        TiebaLog.d("PicStack", "cards=" + pics.size + " ids=" + pics.map { it.picId })
    }
    val ratio = remember(pics) {
        pics.firstOrNull()?.dimensions
            ?.let { it.width.toFloat() / it.height }
            ?.takeIf { it.isFinite() && it > 0.05f }
            ?: 1f
    }
    val lastIndex = pics.lastIndex
    var widthPx by remember { mutableIntStateOf(0) }
    // 已经允许联网的区间（只会向外扩，不会缩回去）：卡片之间的位次是固定的，图片一旦
    // 因为「离得远」被清掉再重新加载，回来时必然又闪一次占位灰块。
    var loadFrom by remember(pics) { mutableIntStateOf(0) }
    var loadTo by remember(pics) { mutableIntStateOf(min(StackDepth + 2, lastIndex)) }
    var page by remember(pics.firstOrNull()?.picUrl) { mutableFloatStateOf(0f) }
    val base by remember(pics) {
        derivedStateOf { floor(page.toDouble()).toInt().coerceIn(0, lastIndex) }
    }
    val currentIndex by remember(pics) {
        derivedStateOf { page.roundToInt().coerceIn(0, lastIndex) }
    }
    LaunchedEffect(base) {
        // 前面留一张、后面留 StackDepth + 2 张，滑到之前就已经在解码。
        loadFrom = min(loadFrom, max(0, base - 1))
        loadTo = max(loadTo, min(lastIndex, base + StackDepth + 2))
    }
    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    // 本次拖动是从第几页开始的：翻不翻页按「相对起点的位移」判定，
    // 而不是按当前离哪一页更近，这样才能用远小于半屏的距离触发。
    var dragStartPage by remember { mutableFloatStateOf(0f) }

    // 松手后带速度回到整数页。spring 的阻尼比压在 0.72，收尾会轻微过冲再回弹。
    fun settle(velocityPagesPerSecond: Float) {
        settleJob?.cancel()
        val delta = page - dragStartPage + velocityPagesPerSecond * FlingPagesPerVelocity
        val step = when {
            delta > CommitFraction -> 1
            delta < -CommitFraction -> -1
            else -> 0
        }
        val target = (dragStartPage + step).roundToInt().coerceIn(0, lastIndex).toFloat()
        settleJob = scope.launch {
            animate(
                initialValue = page,
                targetValue = target,
                initialVelocity = velocityPagesPerSecond,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = 0.0005f,
                ),
            ) { value, _ -> page = value }
        }
    }

    // 看完大图退回来时停到刚才那张：形变浮层收拢完成会把「最后看的那张」地址留在
    // MorphState.lastClosedUrl 里，这里按地址对上页码。一个地址只属于一个图片堆，
    // 找到就直接落到那一张；找不到（看图时翻到了别的帖子）就保持原样。
    val morphState = LocalMorphState.current
    LaunchedEffect(pics, morphState) {
        if (morphState == null) return@LaunchedEffect
        snapshotFlow { morphState.lastClosedUrl }
            .filterNotNull()
            .collect { url ->
                val index = pics.indexOfFirst {
                    it.photoViewData?.data?.originUrl == url || it.originUrl == url
                }
                if (index < 0) return@collect
                morphState.consumeLastClosedUrl()
                settleJob?.cancel()
                // 直接落位：跨好几张时把中间的牌挨个滑过去反而会看到一串还没解码的灰块。
                // 顺手把加载窗口扩过去，别等 base 变了才补，否则落位的第一帧是空 Url 的灰块。
                loadFrom = min(loadFrom, max(0, index - 1))
                loadTo = max(loadTo, min(lastIndex, index + StackDepth + 2))
                page = index.toFloat()
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(singleMediaFraction)
            // 裁掉滑出边界的牌，但不裁断底部露出的牌边（clip 在 padding 之前）。
            .clipToBounds()
            .padding(bottom = StackPeekStep * StackDepth)
            .pointerInput(pics.size, lastIndex) {
                val width = size.width.toFloat()
                if (width <= 0f) return@pointerInput
                val tracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = {
                        settleJob?.cancel()
                        tracker.resetTracking()
                        dragStartPage = page.roundToInt().toFloat()
                    },
                    onDragCancel = { settle(0f) },
                    onDragEnd = {
                        // 往左拖 page 增大，屏幕速度是负的，换算成「页/秒」要取反。
                        settle(-tracker.calculateVelocity().x / width)
                    },
                ) { change, dragAmount ->
                    change.consume()
                    tracker.addPosition(change.uptimeMillis, change.position)
                    settleJob?.cancel()
                    // 直接写状态，不走动画：跟手，且没有协程调度延迟。
                    page = (page - dragAmount / width).coerceIn(0f, lastIndex.toFloat())
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .onSizeChanged { widthPx = it.width }
        ) {
            if (widthPx > 0) {
                // 固定对 0..lastIndex 全量声明，下标大的先画、下标 0 最后画（所以当前页永远压在上面，
                // 不需要 zIndex）。绝不能按「当前页 ± N」决定声明顺序：那样每翻一页所有卡片的组合
                // 位次都会整体平移，NetworkImage 内部的 painter 随之重建，占位灰块会重闪一次。
                for (i in lastIndex downTo 0) {
                    StackCard(
                        photoViewData = stackPhotoViewData[i],
                        imageUrl = if (i in loadFrom..loadTo) pics[i].picUrl else "",
                        // 只有最靠前的两张接点击：后面的牌 alpha 为 0 却仍然参与命中测试，
                        // 且绘制顺序是「下标小的在上面」，会抢走点击并打开错误的图。
                        clickable = i == base || i == currentIndex,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // 只在 draw 阶段读 page：在组合期读会让每一张牌每帧都重组。
                                val d = i - page
                                if (d > 0f) {
                                    // 还没轮到的牌：堆在后面，露出一圈边。
                                    val scale = 1f - 0.05f * d
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = StackSideStep.toPx() * d
                                    translationY = StackPeekStep.toPx() * d
                                } else {
                                    // 当前这张 / 已经翻过去的：整幅宽度平移，d 连续所以跟手。
                                    scaleX = 1f
                                    scaleY = 1f
                                    translationX = d * widthPx
                                    translationY = 0f
                                }
                                // 前后各留半页的淡入淡出，进出堆叠时不会硬切。
                                alpha = ((d + 1.5f) / 0.5f).coerceIn(0f, 1f) *
                                    ((StackDepth + 0.5f - d) / 0.5f).coerceIn(0f, 1f)
                            },
                    )
                }
            }
            PageBadge(
                index = currentIndex,
                count = pics.size,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun StackCard(
    photoViewData: PhotoViewData?,
    imageUrl: String,
    modifier: Modifier = Modifier,
    clickable: Boolean = true,
) {
    NetworkImage(
        modifier = modifier.squircleClip(SquircleCornerRadius.Small),
        imageUrl = imageUrl,
        photoViewDataProvider = if (clickable) ({ photoViewData }) else null,
    )
}

@Composable
private fun PageBadge(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${index + 1}/$count",
            color = Color.White,
            fontSize = 12.sp,
        )
    }
}
