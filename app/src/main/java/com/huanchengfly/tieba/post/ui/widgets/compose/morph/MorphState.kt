package com.huanchengfly.tieba.post.ui.widgets.compose.morph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.utils.TiebaLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 大图形变动画全局状态（移植自 Matter 的 ImageMorphState）：
 * - 单一 0..1 progress 进度源，避免 tween 与多段动画状态竞争；
 * - 临界阻尼弹簧驱动（[MORPH_SETTLE_SPEC]）；
 * - 记录起点矩形、圆角半径、屏幕尺寸、图片列表与当前索引。
 *
 * 与参考实现唯一的差别：插槽以图片 URL 为键，而不是列表下标。TiebaLite 的看图页会按接口
 * 继续往前/往后补图（补图会改变下标），URL 才是稳定的身份；同一条帖子被多次打开时也不会串。
 * */
@Stable
class MorphState(
    val progress: Animatable<Float, AnimationVector1D>,
    private val density: Density,
    private val scope: CoroutineScope,
) {
    var isOpen by mutableStateOf(false)
        private set

    var dragging by mutableStateOf(false)

    var dragOffset by mutableStateOf(Offset.Zero)

    var dragVeilAlpha by mutableFloatStateOf(1f)

    var isCollapsing by mutableStateOf(false)
        private set

    var collapseScale by mutableFloatStateOf(1f)
        private set

    var collapseOffset by mutableStateOf(Offset.Zero)
        private set

    /** 收拢时飞行层要显示的地址：画廊里正在显示的那一档（可能是原图，也可能是中图）。 */
    var currentDisplayUrl by mutableStateOf<String?>(null)

    /** 本次打开的图片地址列表（点开时那一组），用于判断缩略图让位。 */
    var urls by mutableStateOf<List<String>>(emptyList())
        private set

    /** 点开时那一组图的缩略图档地址：飞行层与画廊首帧都拿它做占位，避免白屏。 */
    var thumbUrls by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var initialIndex by mutableIntStateOf(0)
        private set

    var currentIndex by mutableIntStateOf(0)

    var currentUrl by mutableStateOf<String?>(null)
        private set

    var startBounds by mutableStateOf(Rect.Zero)
        private set

    var startCornerRadiusPx by mutableFloatStateOf(0f)
        private set

    var imageRatio by mutableStateOf<Float?>(null)
        private set

    var screenBounds by mutableStateOf(Rect.Zero)
        private set

    /** 刚收起的那张图：卡牌堆用它回到刚才看的那一张。 */
    var lastClosedUrl by mutableStateOf<String?>(null)
        private set

    private val slotBoundsMap = mutableMapOf<String, Rect>()
    private val slotCornerRadiusMap = mutableMapOf<String, Float>()
    private val slotRatioMap = mutableMapOf<String, Float>()

    val expanded: Boolean get() = progress.value >= 1f

    val isPresent: Boolean get() = isOpen || progress.value > 0.001f

    val veilAlpha: Float get() = computeMorphVeil(progress.value) * dragVeilAlpha

    fun thumbUrlOf(url: String): String? = thumbUrls[url]

    /**
     * 当前正在看的那张图，在列表里对应的缩略图是否应该让位（alpha = 0）：
     * 展开与收拢全程都隐藏，避免返回时卡片与飞行图片重叠成两张。
     * */
    fun isSlotHidden(vararg candidates: String?): Boolean {
        if (!isPresent) return false
        val active = currentUrl ?: return false
        return candidates.any { it != null && it == active }
    }

    /**
     * 登记某一格缩略图当前的屏幕矩形与圆角。
     *
     * 不能在打开时才登记：多图帖里被点的那张只是其中一格，玩家滑到同组的另一张再退出时，
     * 飞行终点要用那一格自己的矩形，否则会落回「点开时那张」的位置（看起来就是退出动画错位）。
     * 因此每一格在布局时就登记，打开时只保留本次会话涉及的地址（见 [open]）。
     * */
    fun registerSlot(url: String, bounds: Rect, cornerRadiusPx: Float, ratio: Float? = null) {
        if (url.isBlank() || bounds.isEmpty) return
        slotBoundsMap[url] = bounds
        slotCornerRadiusMap[url] = cornerRadiusPx
        if (ratio != null && ratio > 0f) {
            slotRatioMap[url] = ratio
        }
    }

    /** 某一格缩略图离开组合（滚出列表/换帖）时撤掉登记，避免收拢时飞向已经不存在的位置。 */
    fun unregisterSlot(url: String) {
        if (url.isBlank()) return
        slotBoundsMap.remove(url)
        slotCornerRadiusMap.remove(url)
    }

    fun reportScreenBounds(bounds: Rect) {
        if (screenBounds != bounds && !bounds.isEmpty) {
            screenBounds = bounds
        }
    }

    fun dragRangePx(): Float =
        (screenBounds.height * 0.45f).coerceAtLeast(with(density) { 150.dp.toPx() })

    fun onPageChanged(newPage: Int, url: String?) {
        currentIndex = newPage
        currentUrl = url
        val cachedRatio = url?.let { ImageRatioCache.get(it) } ?: url?.let { slotRatioMap[it] }
        if (cachedRatio != null) {
            imageRatio = cachedRatio
        } else {
            val slotRect = url?.let { slotBoundsMap[it] }
            if (slotRect != null && slotRect.height > 0f) {
                imageRatio = slotRect.width / slotRect.height
            }
        }
    }

    /**
     * 打开大图形变动画。[slotBounds] / [slotRadii] 是同一组图里每一格已知的屏幕矩形与圆角，
     * 预登记之后，切图翻页再退出也能直接飞向对应槽位。
     */
    fun open(
        urls: List<String>,
        index: Int,
        bounds: Rect,
        cornerRadiusPx: Float,
        imageRatio: Float? = null,
        slotBounds: Map<String, Rect>? = null,
        slotRadii: Map<String, Float>? = null,
        thumbUrls: Map<String, String>? = null,
    ) {
        this.urls = urls
        this.thumbUrls = thumbUrls ?: emptyMap()
        this.initialIndex = index
        this.currentIndex = index
        this.currentUrl = urls.getOrNull(index)
        this.startBounds = bounds
        this.startCornerRadiusPx = cornerRadiusPx
        this.imageRatio = imageRatio
        this.dragOffset = Offset.Zero
        this.dragVeilAlpha = 1f
        this.collapseScale = 1f
        this.collapseOffset = Offset.Zero
        this.currentDisplayUrl = null
        this.lastClosedUrl = null

        // 只保留本次会话里这些图的槽位：换一帖之后旧卡片的矩形不能再留着（会飞向别的帖子的位置）。
        // 同组其它几格是各格自己登记的，必须留下——滑到另一张再退出时要飞回那一格。
        val sessionUrls = urls.toSet()
        slotBoundsMap.keys.retainAll(sessionUrls)
        slotCornerRadiusMap.keys.retainAll(sessionUrls)
        slotRatioMap.clear()

        if (slotBounds != null) {
            slotBoundsMap.putAll(slotBounds)
        }
        slotBoundsMap[urls[index]] = bounds

        if (slotRadii != null) {
            slotCornerRadiusMap.putAll(slotRadii)
        }
        slotCornerRadiusMap[urls[index]] = cornerRadiusPx

        urls.forEach { url ->
            val cached = ImageRatioCache.get(url)
            if (cached != null) {
                slotRatioMap[url] = cached
            }
        }
        TiebaLog.d(
            "MorphState",
            "open urls=" + urls.size + " index=" + index + " slots=" + slotBoundsMap.size
        )
        if (imageRatio != null) {
            slotRatioMap[urls[index]] = imageRatio
        }

        this.isCollapsing = false
        this.isOpen = true

        scope.launch {
            progress.stop()
            progress.animateTo(
                targetValue = 1f,
                animationSpec = MORPH_SETTLE_SPEC
            )
            progress.snapTo(1f)
        }
    }

    fun updateRatio(url: String, ratio: Float) {
        if (ratio > 0f && url.isNotBlank()) {
            slotRatioMap[url] = ratio
            ImageRatioCache.put(url, ratio)
            if (currentUrl == url) {
                imageRatio = ratio
            }
        }
    }

    fun updateCurrentZoom(scale: Float, offset: Offset, displayUrl: String? = null) {
        this.collapseScale = scale
        this.collapseOffset = offset
        if (displayUrl != null) {
            this.currentDisplayUrl = displayUrl
        }
    }

    /**
     * 收起大图形变动画：平滑收拢回信息流卡片原始位置。
     * 支持在放大状态下直接飞回收拢（保留 scale 与 offset，无缝过渡，绝不跳闪）。
     */
    fun collapse(
        initialVelocity: Float = 0f,
        dragOffset: Offset = Offset.Zero,
        veilAlpha: Float = 1f,
        initialScale: Float? = null,
        initialOffset: Offset? = null,
        displayUrl: String? = null,
    ) {
        if (initialScale != null) this.collapseScale = initialScale
        if (initialOffset != null) this.collapseOffset = initialOffset
        if (displayUrl != null) this.currentDisplayUrl = displayUrl
        this.dragOffset = dragOffset
        this.dragVeilAlpha = veilAlpha
        this.isCollapsing = true
        val closedUrl = currentUrl
        val closeSlot = closedUrl?.let { slotBoundsMap[it] }
        TiebaLog.d(
            "MorphState",
            "collapse url=" + (closedUrl?.takeLast(24) ?: "null") +
                " slot=" + (closeSlot?.toString() ?: "MISSING->startBounds=" + startBounds) +
                " slots=" + slotBoundsMap.size + " scale=" + collapseScale
        )
        scope.launch {
            val v = computeMorphSettleVelocity(progress.value, 0f, initialVelocity)
            progress.animateTo(
                targetValue = 0f,
                animationSpec = MORPH_SETTLE_SPEC,
                initialVelocity = v
            )
            progress.snapTo(0f)
            isOpen = false
            isCollapsing = false
            lastClosedUrl = closedUrl
            this@MorphState.dragOffset = Offset.Zero
            this@MorphState.dragVeilAlpha = 1f
            this@MorphState.collapseScale = 1f
            this@MorphState.collapseOffset = Offset.Zero
            this@MorphState.currentDisplayUrl = null
        }
    }

    fun consumeLastClosedUrl(): String? {
        val url = lastClosedUrl
        lastClosedUrl = null
        return url
    }

    fun flyingFrame(): FlyingImageFrame? {
        if (screenBounds.isEmpty) return null
        val url = currentUrl
        val curStart = url?.let { slotBoundsMap[it] } ?: startBounds
        if (curStart.isEmpty) return null
        val curRadius = url?.let { slotCornerRadiusMap[it] } ?: startCornerRadiusPx
        val curRatio = url?.let { ImageRatioCache.get(it) } ?: url?.let { slotRatioMap[it] } ?: imageRatio
        val targetEnd = computeTargetEndBounds(screenBounds, curRatio)

        // 若退出时处于缩放（放大或手势微缩）或平移状态，飞行的终点矩形直接使用当前真实的屏幕
        // 物理外包围盒，彻底消除从画廊层切入飞行层瞬间突然闪回 1.0x 原始尺寸的跳跃。
        val effectiveEnd = if (kotlin.math.abs(collapseScale - 1f) > 0.001f || collapseOffset != Offset.Zero) {
            val actualW = targetEnd.width * collapseScale
            val actualH = targetEnd.height * collapseScale
            val actualCx = targetEnd.center.x + collapseOffset.x
            val actualCy = targetEnd.center.y + collapseOffset.y
            Rect(
                actualCx - actualW / 2f,
                actualCy - actualH / 2f,
                actualCx + actualW / 2f,
                actualCy + actualH / 2f
            )
        } else {
            targetEnd
        }

        return computeFlyingImageFrame(
            progress = progress.value,
            start = curStart,
            end = effectiveEnd,
            startCornerRadiusPx = curRadius,
            endCornerRadiusPx = 0f,
            dragOffset = dragOffset
        )
    }
}
