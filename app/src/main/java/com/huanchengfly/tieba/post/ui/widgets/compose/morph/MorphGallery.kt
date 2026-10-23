package com.huanchengfly.tieba.post.ui.widgets.compose.morph

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.imageLoader
import coil3.memory.MemoryCache
import com.huanchengfly.tieba.post.ui.page.photoview.ImageSizeProbe
import com.huanchengfly.tieba.post.ui.page.photoview.MaxAutoDownloadBytes
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewItem
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.TiebaLog
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Share

/**
 * 大图 morph 形变浮层（整段移植自 Matter 的 ImageMorphOverlay）：
 * - 直接叠在应用根窗口之上，不改动导航栈，无闪屏与白屏；
 * - 第一层：背景深黑渐变 Veil；
 * - 第二层：从信息流卡片原始位置物理展开/收拢的飞行图片层；
 * - 第三层：展开后完整多图 HorizontalPager 画廊，支持左右切图、双指捏合、双击缩放与任意方向跟手收起。
 *
 * 与参考实现的差别只在数据来源：TiebaLite 的图片档位来自接口的 medium / screen 两档
 * （[PhotoViewItem.thumbUrl]），微博的档位拼接函数换成了这个字段；实况照片层在贴吧不存在，已去掉。
 * */
@Composable
fun MorphGallery(
    morphState: MorphState,
    items: List<PhotoViewItem>,
    modifier: Modifier = Modifier,
    /** 接口列表里「被点开那张」的下标（[com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewUiState.initialIndex]）。 */
    initialIndex: Int = 0,
    onPageShown: (Int) -> Unit = {},
    onShare: (PhotoViewItem) -> Unit = {},
    onSave: (url: String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 用 derivedStateOf 收窄对动画进度的依赖：仅在布尔结果变化时才重组，
    // 避免形变动画期间每帧重组整个浮层。
    val hostPresent by remember(morphState) { derivedStateOf { morphState.isPresent } }
    val backEnabled by remember(morphState) { derivedStateOf { morphState.isOpen } }
    var backSettleJob by remember { mutableStateOf<Job?>(null) }

    var currentZoomScale by remember { mutableFloatStateOf(1f) }
    var currentZoomOffset by remember { mutableStateOf(Offset.Zero) }
    var currentDisplayDataUrl by remember { mutableStateOf<String?>(null) }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(morphState.isOpen) {
        if (!morphState.isOpen) {
            currentDisplayDataUrl = null
            currentZoomScale = 1f
            currentZoomOffset = Offset.Zero
            predictiveBackProgress = 0f
        }
    }

    key(hostPresent) {
        PredictiveBackHandler(enabled = hostPresent && backEnabled) { events ->
            if (!morphState.isOpen) {
                events.collect { }
                return@PredictiveBackHandler
            }
            backSettleJob?.cancel()
            // 手势返回：在放大状态下直接退出返回上级，不需要先缩回到 1.0x 原始尺寸！
            // 立即同步退出时的缩放比例与平移位置
            morphState.updateCurrentZoom(currentZoomScale, currentZoomOffset, currentDisplayDataUrl)
            var lastProgress = 0f
            var lastTimeNanos = 0L
            var releaseVelocity = 0f
            try {
                morphState.dragging = true
                events.collect { event ->
                    val progress = event.progress
                    val now = System.nanoTime()
                    if (lastTimeNanos != 0L && now > lastTimeNanos) {
                        releaseVelocity = (progress - lastProgress) /
                                ((now - lastTimeNanos) / 1_000_000_000f)
                    }
                    lastProgress = progress
                    lastTimeNanos = now
                    predictiveBackProgress = progress
                }
                morphState.dragging = false
                // 松手确认退出：从当前微缩放大画面平滑收拢飞回卡片槽位
                backSettleJob = scope.launch {
                    val lastBackScale = (1f - lastProgress * 0.12f).coerceIn(0.5f, 1f)
                    val exitScale = currentZoomScale * lastBackScale
                    val exitOffset = currentZoomOffset * lastBackScale
                    val currentBackAlpha = (1f - lastProgress * 0.35f).coerceIn(0f, 1f)
                    morphState.collapse(
                        initialVelocity = -releaseVelocity,
                        veilAlpha = currentBackAlpha,
                        initialScale = exitScale,
                        initialOffset = exitOffset,
                        displayUrl = currentDisplayDataUrl
                    )
                    predictiveBackProgress = 0f
                }
            } catch (cancelled: CancellationException) {
                // 取消手势（推回屏幕边缘）：平滑弹回，画廊层自始至终未被销毁，图片原放大倍数与位置完好无损！
                backSettleJob = scope.launch {
                    val startP = predictiveBackProgress
                    animate(
                        initialValue = startP,
                        targetValue = 0f,
                        animationSpec = MORPH_SETTLE_SPEC
                    ) { value, _ ->
                        predictiveBackProgress = value
                    }
                }
                throw cancelled
            } finally {
                morphState.dragging = false
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 首帧同步初始化屏幕尺寸，杜绝任何因异步布局回调造成的首帧端点缺失跳跃
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val screenRect = remember(screenW, screenH) { Rect(0f, 0f, screenW, screenH) }

        LaunchedEffect(screenRect) {
            morphState.reportScreenBounds(screenRect)
        }

        if (!hostPresent) return@BoxWithConstraints

        val openedUrls = morphState.urls
        if (openedUrls.isEmpty()) return@BoxWithConstraints

        // 接口返回的这一页里必须真的有被点开的那张：页面参数偶尔不对，返回的是别处的图，
        // 这种列表换上去会让整段画廊都是不相干的照片（录屏里「闪出别的图」正是它）。
        // 认不出来时宁可用点开时那一叠，至少显示的是点的那张。
        val openedUrl = morphState.currentUrl
        val itemsLoaded = items.isNotEmpty() && (
            openedUrl == null ||
                items.any { it.originUrl == openedUrl } ||
                items.any { it.picId == ImageUtil.getPicId(openedUrl) }
            )

        // 图片列表还没回来（或对不上）时先用「点开时那一组」占位，回来后换成带档位的最新列表。
        val urls = remember(items, openedUrls, itemsLoaded) {
            if (itemsLoaded) items.map { it.originUrl } else openedUrls
        }
        // pageCount 必须跟着 urls 走：rememberPagerState 的 lambda 只捕获创建那一帧的变量，
        // 补上图之后页码上限还停在旧值，对齐时 scrollToPage 会被夹回去。
        val urlsState = rememberUpdatedState(urls)

        // 页码对齐：先按地址认；接口把同一张图换成另一档地址（域名/参数不同）时按 picId 认人，
        // 否则会静默落到第一张——多图帖表现为「点第三张、打开是第一张」。
        fun resolvePage(url: String?, hint: Int?, fallback: Int): Int {
            val byUrl = if (url == null) -1 else urls.indexOf(url)
            if (byUrl >= 0) return byUrl
            if (url != null) {
                val picId = ImageUtil.getPicId(url)
                val byPicId = items.indexOfFirst { it.picId == picId }
                if (byPicId >= 0) return byPicId
            }
            if (hint != null && hint in urls.indices) return hint
            return fallback.coerceIn(0, urls.lastIndex.coerceAtLeast(0))
        }

        // 列表第一次由「点开时那一组」换成接口列表时，整只 Pager 重建：旧 Pager 的页码是按占位列表
        // 算出来的（多图帖几乎总是 0），继续用它就会先画出新列表的第 0 张，而那张往往不是用户点的那张
        // ——也就是「打开照片闪一下别的图」。重建时直接落在对齐后的那页，一帧都不会画错；
        // 之后 loadNext/loadPrev 只改 urls，不再重建。
        val pagerState = key(itemsLoaded) {
            rememberPagerState(
                initialPage = resolvePage(
                    url = morphState.currentUrl,
                    hint = if (itemsLoaded) initialIndex else null,
                    fallback = morphState.initialIndex,
                ),
                pageCount = { urlsState.value.size },
            )
        }

        LaunchedEffect(itemsLoaded, urls, pagerState.currentPage) {
            TiebaLog.d(
                "MorphGallery",
                "itemsLoaded=" + itemsLoaded + " items=" + items.size + " urls=" + urls.size +
                    " opened=" + (morphState.currentUrl?.takeLast(24) ?: "null") +
                    " page=" + pagerState.currentPage +
                    " pageUrl=" + (urls.getOrNull(pagerState.currentPage)?.takeLast(24) ?: "null")
            )
        }

        // 列表头部插进更早的图（loadPrev 把上一页 prepend 到最前面）会让每个页码整体后移。
        // 必须在这一次 measure 之前就把页码挪到同一张图上：只靠下面的 LaunchedEffect 要到下一帧
        // 才生效，中间那一帧画出来的是「移位后的第 0 张」——打开照片时闪一下别的图就是它。
        // requestScrollToPage 在本次 measure 里生效，没有中间帧。
        val previousUrls = remember { object { var value: List<String> = emptyList() } }
        SideEffect {
            val previous = previousUrls.value
            previousUrls.value = urls
            if (previous === urls) return@SideEffect
            val shownUrl = previous.getOrNull(pagerState.currentPage) ?: return@SideEffect
            val realigned = urls.indexOf(shownUrl)
            if (realigned >= 0 && realigned != pagerState.currentPage) {
                pagerState.requestScrollToPage(realigned)
            }
        }

        // 兜底：SideEffect 没接上（例如列表整个换掉）时，仍按「现在正在看的那张」对齐一次。
        LaunchedEffect(urls) {
            val index = resolvePage(
                url = morphState.currentUrl,
                hint = null,
                fallback = pagerState.currentPage,
            )
            if (index != pagerState.currentPage) {
                pagerState.scrollToPage(index)
            }
        }

        // 同步当前 Pager 页码至 Morph 状态，以便收起时追踪对应的图片
        LaunchedEffect(pagerState.currentPage, urls) {
            morphState.onPageChanged(pagerState.currentPage, urls.getOrNull(pagerState.currentPage))
        }

        LaunchedEffect(pagerState.currentPage, itemsLoaded) {
            // 列表对不上时画廊里放的是点开时那一叠，页码与接口的下标不是一回事，别拿它去翻页。
            if (itemsLoaded) {
                onPageShown(pagerState.currentPage)
            }
        }

        // ---- 清晰度策略（与参考项目的「原图/中等档」一致，阈值换成贴吧的 HEAD 探测）----
        var forceOriginalUrls by remember { mutableStateOf(setOf<String>()) }
        var probeResults by remember { mutableStateOf(mapOf<String, Long>()) }
        var originalDisabled by remember { mutableStateOf(false) }

        // 探测当前页与前后相邻页的文件体积
        LaunchedEffect(pagerState.currentPage, urls) {
            val currentUrl = urls.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
            if (currentUrl !in probeResults) {
                val result = ImageSizeProbe.probe(currentUrl)
                probeResults = probeResults + (currentUrl to result)
            }
            val prevUrl = urls.getOrNull(pagerState.currentPage - 1)
            if (prevUrl != null && prevUrl !in probeResults) {
                launch {
                    val result = ImageSizeProbe.probe(prevUrl)
                    probeResults = probeResults + (prevUrl to result)
                }
            }
            val nextUrl = urls.getOrNull(pagerState.currentPage + 1)
            if (nextUrl != null && nextUrl !in probeResults) {
                launch {
                    val result = ImageSizeProbe.probe(nextUrl)
                    probeResults = probeResults + (nextUrl to result)
                }
            }
        }

        val currentOriginalUrl = urls[pagerState.currentPage.coerceIn(0, urls.lastIndex)]
        val currentProbe = probeResults[currentOriginalUrl] ?: ImageSizeProbe.cachedSize(currentOriginalUrl)
        val currentIsOversized = currentProbe > MaxAutoDownloadBytes
        val currentForced = currentOriginalUrl in forceOriginalUrls
        val currentIsAnimated = currentOriginalUrl.contains(".gif", ignoreCase = true)
        val currentShouldAuto = if (currentIsOversized) false else if (currentIsAnimated) true else true
        val currentShowingOriginal = (currentShouldAuto || currentForced) && !originalDisabled
        val currentFormattedSize = formatBytes(currentProbe)
        val currentItem = items.getOrNull(pagerState.currentPage)
        // 本页实际显示的那一档：超大图会退回中图（与下方 MorphZoomablePage 的 dataUrl 同一规则）。
        val currentMediumUrl = remember(currentOriginalUrl, currentItem) {
            currentItem?.thumbUrl ?: morphState.thumbUrlOf(currentOriginalUrl) ?: currentOriginalUrl
        }
        val currentDisplayedUrl = if (currentShowingOriginal) currentOriginalUrl else currentMediumUrl
        // 分享/保存仍按原图。
        val currentTargetUrl = currentOriginalUrl

        // 上抛给飞行层：收拢第一帧要拿的就是画廊此刻真正画着的那一档。若这里传原图、
        // 而画廊显示的是中图，飞行层接手时会去请求一张不在内存缓存里的图，先空一帧。
        LaunchedEffect(currentDisplayedUrl) {
            currentDisplayDataUrl = currentDisplayedUrl
        }

        var pageDragAlpha by remember { mutableFloatStateOf(1f) }
        var isPageDragging by remember { mutableStateOf(false) }

        // 第一层：背景遮罩 (Veil) 与全屏触摸隔离盾（Touch Shield）
        // 彻底拦截并消费浮层激活全周期（展开过渡、大图画廊、拖拽释放、收拢飞行）的一切触摸手势，
        // 100% 杜绝任何手势下穿到底层卡片（防止误触卡片进入正文、防止误滑导致列表位置偏移）！
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .drawBehind {
                    val currentVeil = (morphState.veilAlpha * pageDragAlpha * (1f - predictiveBackProgress * 0.35f)).coerceIn(0f, 1f)
                    drawRect(Color.Black.copy(alpha = currentVeil))
                }
        )

        // 是否处于近完全展开且未触发收拢的画廊模式（0.985f 门限使手势状态机提前就绪，消除尾部触摸死区）
        val isGalleryMode by remember(morphState) {
            derivedStateOf { morphState.progress.value >= 0.985f && !morphState.isCollapsing }
        }

        if (!isGalleryMode) {
            // 第二层：飞行形变图片层（在展开过渡、退出收拢、系统预测返回时全权接管物理几何运动）
            // 与第三层严格互斥挂载，同屏绝不出现第二个图片实例！
            val flyingFrame = morphState.flyingFrame()
            if (flyingFrame != null && !flyingFrame.bounds.isEmpty) {
                // 占位列表与接口列表的下标并不一一对应：先按地址认「正在飞的那张」，
                // 否则展开途中列表换代时，飞行层会改画新列表同下标的另一张图。
                val currentUrl = morphState.currentUrl?.takeIf { it in urls }
                    ?: urls.getOrNull(morphState.currentIndex)
                    ?: urls[0]
                val left = flyingFrame.bounds.left.roundToInt()
                val top = flyingFrame.bounds.top.roundToInt()
                val widthDp = with(density) { flyingFrame.bounds.width.toDp() }
                val heightDp = with(density) { flyingFrame.bounds.height.toDp() }
                val cornerRadiusDp = with(density) { flyingFrame.cornerRadiusPx.toDp() }
                // 退出时优先复用当前正在显示的高清原图/中图，并以缩略图为占位位图，
                // 保证飞行层接管瞬间与画廊最后一帧的画质、色彩与细节 100% 严丝合缝对齐；
                // 展开时必须严格使用当前被点击的图片地址，绝不读取任何上一张图遗留地址！
                val activeUrl = if (morphState.isCollapsing) {
                    morphState.currentDisplayUrl ?: currentDisplayDataUrl ?: currentUrl
                } else {
                    currentUrl
                }
                val stableRequest = rememberStableImageRequest(
                    dataUrl = activeUrl,
                    placeholderUrl = morphState.thumbUrlOf(currentUrl) ?: currentUrl
                )

                Box(
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                        .offset { IntOffset(left, top) }
                        .requiredSize(width = widthDp, height = heightDp)
                        .clip(RoundedCornerShape(cornerRadiusDp))
                ) {
                    AsyncImage(
                        model = stableRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // 第三层：交互式多图画廊（仅在 100% 完全展开且非退出状态时挂载，提供切图、缩放与拖拽退出手势）
            // 预测返回手势预览期间保持全屏尺寸，不缩放 Pager，彻底杜绝左右出现黑色竖缝
            HorizontalPager(
                state = pagerState,
                key = { pageIndex -> urls.getOrElse(pageIndex) { pageIndex.toString() } },
                userScrollEnabled = (currentZoomScale <= 1.05f),
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val originalUrl = urls[pageIndex]
                val item = items.getOrNull(pageIndex)
                val isAnimated = originalUrl.contains(".gif", ignoreCase = true)
                val mediumUrl = remember(originalUrl, item) {
                    item?.thumbUrl ?: morphState.thumbUrlOf(originalUrl) ?: originalUrl
                }
                val gridUrl = remember(originalUrl, item) {
                    morphState.thumbUrlOf(originalUrl) ?: item?.thumbUrl ?: originalUrl
                }
                val probe = probeResults[originalUrl] ?: ImageSizeProbe.cachedSize(originalUrl)
                val isOversized = probe > MaxAutoDownloadBytes
                val isForced = originalUrl in forceOriginalUrls

                val shouldAuto = if (isOversized) false else if (isAnimated) true else true
                val displayOriginal = (shouldAuto || isForced) && !originalDisabled

                val pageContext = LocalContext.current
                val mediumCached = remember(mediumUrl) {
                    pageContext.imageLoader.memoryCache?.get(MemoryCache.Key(mediumUrl)) != null
                }

                MorphZoomablePage(
                    url = originalUrl,
                    dataUrl = if (displayOriginal) originalUrl else mediumUrl,
                    // 原图优先复用刚看过的中等档位图（若在内存缓存中），否则复用信息流已解码的缩略图，
                    // 实现「缩略图 → 原图」的无缝升级，杜绝白屏与闪烁
                    placeholderUrl = if (displayOriginal && mediumCached && mediumUrl != originalUrl) {
                        mediumUrl
                    } else {
                        gridUrl
                    },
                    pageIndex = pageIndex,
                    isCurrentPage = (pagerState.currentPage == pageIndex),
                    onCollapse = { dragOffset, initialVelocity, veilAlpha ->
                        morphState.collapse(
                            initialVelocity = initialVelocity,
                            dragOffset = dragOffset,
                            veilAlpha = veilAlpha,
                            initialScale = currentZoomScale,
                            initialOffset = currentZoomOffset,
                            displayUrl = currentDisplayDataUrl
                        )
                        pageDragAlpha = 1f
                        isPageDragging = false
                    },
                    onZoomStateChanged = { scale, offset ->
                        if (pagerState.currentPage == pageIndex) {
                            currentZoomScale = scale
                            currentZoomOffset = offset
                        }
                    },
                    onDragStateChanged = { offset, alpha ->
                        pageDragAlpha = alpha
                        isPageDragging = (offset != Offset.Zero)
                    },
                    onLoadError = {
                        if (displayOriginal && !isAnimated) originalDisabled = true
                    },
                    onImageRatioLoaded = { ratio ->
                        morphState.updateRatio(originalUrl, ratio)
                    },
                    predictiveBackProgress = if (pagerState.currentPage == pageIndex) predictiveBackProgress else 0f
                )
            }
        }

        // 顶部返回按钮与页码指示器 (在 progress > 0.60 展开末期自然渐入，拖拽时按 alpha 渐隐)
        val showUi by remember(morphState) { derivedStateOf { morphState.progress.value > 0.60f } }
        if (showUi) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp)
                    .graphicsLayer {
                        val progressAlpha = ((morphState.progress.value - 0.60f) / 0.40f).coerceIn(0f, 1f)
                        alpha = (progressAlpha * pageDragAlpha * (1f - predictiveBackProgress * 2.5f)).coerceIn(0f, 1f)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        morphState.updateCurrentZoom(currentZoomScale, currentZoomOffset, currentDisplayDataUrl)
                        morphState.collapse(
                            initialScale = currentZoomScale,
                            initialOffset = currentZoomOffset,
                            displayUrl = currentDisplayDataUrl
                        )
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = {
                        val item = currentItem
                        if (item != null) {
                            onShare(item)
                        } else if (currentOriginalUrl.isNotBlank()) {
                            onShare(
                                PhotoViewItem(
                                    picId = currentOriginalUrl,
                                    originUrl = currentOriginalUrl,
                                    overallIndex = pagerState.currentPage + 1
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = MiuixIcons.Share,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                if (urls.size > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${urls.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 底部操作栏：查看原图 / 保存
        if (showUi) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 44.dp)
                    .graphicsLayer {
                        val progressAlpha = ((morphState.progress.value - 0.60f) / 0.40f).coerceIn(0f, 1f)
                        alpha = (progressAlpha * pageDragAlpha * (1f - predictiveBackProgress * 2.5f)).coerceIn(0f, 1f)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!currentShowingOriginal) {
                    val buttonText = when {
                        currentIsOversized && currentFormattedSize != null -> "查看超大原图 ($currentFormattedSize)"
                        currentFormattedSize != null -> if (currentIsAnimated) "查看动图 ($currentFormattedSize)" else "查看原图 ($currentFormattedSize)"
                        currentIsAnimated -> "查看动图"
                        else -> "查看原图"
                    }
                    MorphActionButton(
                        text = buttonText,
                        filled = true,
                        onClick = { forceOriginalUrls = forceOriginalUrls + currentOriginalUrl }
                    )
                }
                MorphActionButton(
                    text = "保存",
                    filled = false,
                    onClick = { onSave(currentTargetUrl) }
                )
            }
        }
    }
}

/** 单页大图触摸与手势处理层（逐行移植自参考项目的 MorphZoomablePage，去掉了实况视频层）。 */
@Composable
private fun MorphZoomablePage(
    url: String,
    dataUrl: String,
    placeholderUrl: String,
    pageIndex: Int,
    isCurrentPage: Boolean,
    onCollapse: (dragOffset: Offset, initialVelocity: Float, veilAlpha: Float) -> Unit,
    onZoomStateChanged: (scale: Float, offset: Offset) -> Unit,
    onDragStateChanged: (Offset, Float) -> Unit,
    onLoadError: () -> Unit,
    onImageRatioLoaded: (Float) -> Unit = {},
    predictiveBackProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageRatio by remember(url) { mutableFloatStateOf(ImageRatioCache.get(url) ?: 0f) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(scaleAnim.value, offsetXAnim.value, offsetYAnim.value, isCurrentPage) {
        if (isCurrentPage) {
            onZoomStateChanged(scaleAnim.value, Offset(offsetXAnim.value, offsetYAnim.value))
        }
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scaleAnim.snapTo(1f)
            offsetXAnim.snapTo(0f)
            offsetYAnim.snapTo(0f)
            dragOffsetX = 0f
            dragOffsetY = 0f
            isDragging = false
            onDragStateChanged(Offset.Zero, 1f)
        }
    }

    // 拖动位移向量长度：任意方向的拖动都参与微缩与背景渐隐计算
    val dragDistance = Offset(dragOffsetX, dragOffsetY).getDistance()
    val dragScale = (1f - (dragDistance / 1800f)).coerceIn(0.65f, 1f)

    val getCurW = { if (containerSize.width > 0) containerSize.width.toFloat() else 1080f }
    val getCurH = { if (containerSize.height > 0) containerSize.height.toFloat() else 2400f }
    val getCurRatio = { if (imageRatio > 0f) imageRatio else (ImageRatioCache.get(url) ?: (getCurW() / getCurH())) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onCollapse(Offset.Zero, 0f, 1f)
                    },
                    onDoubleTap = { tapPosition ->
                        if (scaleAnim.value > 1.05f) {
                            ZoomableImageCalculator.animateZoom(
                                scope = scope,
                                scaleAnim = scaleAnim,
                                offsetXAnim = offsetXAnim,
                                offsetYAnim = offsetYAnim,
                                targetScale = 1f,
                                targetOffset = Offset.Zero
                            )
                        } else {
                            val cW = getCurW()
                            val cH = getCurH()
                            val r = getCurRatio()
                            val targetScale = ZoomableImageCalculator.calculateTargetDoubleTapScale(
                                containerWidth = cW,
                                containerHeight = cH,
                                imageRatio = r
                            )
                            val targetOffset = ZoomableImageCalculator.calculateDoubleTapTargetOffset(
                                tapPosition = tapPosition,
                                containerWidth = cW,
                                containerHeight = cH,
                                imageRatio = r,
                                targetScale = targetScale
                            )
                            ZoomableImageCalculator.animateZoom(
                                scope = scope,
                                scaleAnim = scaleAnim,
                                offsetXAnim = offsetXAnim,
                                offsetYAnim = offsetYAnim,
                                targetScale = targetScale,
                                targetOffset = targetOffset
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val touchSlop = viewConfiguration.touchSlop
                    var panAcc = Offset.Zero
                    var pastSlop = false
                    var isDismissDrag = false
                    var isHorizontalSwipe = false
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    val maxScale = max(
                        4.5f,
                        ZoomableImageCalculator.calculateTargetDoubleTapScale(
                            containerWidth = getCurW(),
                            containerHeight = getCurH(),
                            imageRatio = getCurRatio()
                        ) * 2f
                    )

                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size

                        if (pointerCount >= 2) {
                            // 双指自由缩放
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val newScale = (scaleAnim.value * zoomChange).coerceIn(0.85f, maxScale * 1.25f)
                            val (maxOffsetX, maxOffsetY) = ZoomableImageCalculator.calculateMaxPanOffset(
                                containerWidth = getCurW(),
                                containerHeight = getCurH(),
                                imageRatio = getCurRatio(),
                                currentScale = newScale
                            )
                            val targetX = (offsetXAnim.value + panChange.x).coerceIn(-maxOffsetX - 150f, maxOffsetX + 150f)
                            val targetY = (offsetYAnim.value + panChange.y).coerceIn(-maxOffsetY - 150f, maxOffsetY + 150f)
                            scope.launch {
                                scaleAnim.snapTo(newScale)
                                offsetXAnim.snapTo(targetX)
                                offsetYAnim.snapTo(targetY)
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pointerCount == 1) {
                            val change = event.changes.first()
                            if (change.pressed) {
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val panChange = change.positionChange()
                                panAcc += panChange

                                if (scaleAnim.value > 1.05f) {
                                    // 放大状态平移图片
                                    change.consume()
                                    val (maxOffsetX, maxOffsetY) = ZoomableImageCalculator.calculateMaxPanOffset(
                                        containerWidth = getCurW(),
                                        containerHeight = getCurH(),
                                        imageRatio = getCurRatio(),
                                        currentScale = scaleAnim.value
                                    )
                                    val targetX = (offsetXAnim.value + panChange.x).coerceIn(-maxOffsetX - 120f, maxOffsetX + 120f)
                                    val targetY = (offsetYAnim.value + panChange.y).coerceIn(-maxOffsetY - 120f, maxOffsetY + 120f)
                                    scope.launch {
                                        offsetXAnim.snapTo(targetX)
                                        offsetYAnim.snapTo(targetY)
                                    }
                                } else {
                                    // 1.0x 状态：先判定主导方向，再决定「退出跟手」还是「放行切图」
                                    if (!pastSlop) {
                                        if (panAcc.getDistance() > touchSlop) {
                                            pastSlop = true
                                            // 仅「接近纯横向」的手势放行给 HorizontalPager 切图；
                                            // 其余任意角度（含斜向、上行）都进入退出跟手。
                                            val clearlyHorizontal =
                                                kotlin.math.abs(panAcc.x) > kotlin.math.abs(panAcc.y) * 1.6f
                                            if (clearlyHorizontal) {
                                                isHorizontalSwipe = true
                                            } else {
                                                isDismissDrag = true
                                                isDragging = true
                                            }
                                        }
                                    }

                                    if (isDismissDrag) {
                                        change.consume()
                                        // 任意角度完全跟手：水平与垂直分量按 1:1 位移
                                        dragOffsetX += panChange.x
                                        dragOffsetY += panChange.y
                                        val curDistance = Offset(dragOffsetX, dragOffsetY).getDistance()
                                        val curAlpha = (1f - (curDistance / 600f)).coerceIn(0.2f, 1f)
                                        onDragStateChanged(Offset(dragOffsetX, dragOffsetY), curAlpha)
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (isDismissDrag) {
                        val velocity = velocityTracker.calculateVelocity()
                        // 以位移 / 速度的向量长度作为判据：任意方向的一甩都能触发退出
                        val releaseDistance = Offset(dragOffsetX, dragOffsetY).getDistance()
                        val releaseSpeed = Offset(velocity.x, velocity.y).getDistance()
                        val shouldDismiss = releaseDistance > 200f || releaseSpeed > 800f

                        if (shouldDismiss) {
                            val finalAlpha = (1f - (releaseDistance / 600f)).coerceIn(0.2f, 1f)
                            isDragging = false
                            onCollapse(Offset(dragOffsetX, dragOffsetY), releaseSpeed, finalAlpha)
                        } else {
                            // 取消拖动：平滑弹性回正
                            scope.launch {
                                val startX = dragOffsetX
                                val startY = dragOffsetY
                                animate(
                                    initialValue = 1f,
                                    targetValue = 0f,
                                    animationSpec = MORPH_SETTLE_SPEC
                                ) { fraction, _ ->
                                    dragOffsetX = startX * fraction
                                    dragOffsetY = startY * fraction
                                    val d = Offset(dragOffsetX, dragOffsetY).getDistance()
                                    val a = (1f - (d / 600f)).coerceIn(0.2f, 1f)
                                    onDragStateChanged(Offset(dragOffsetX, dragOffsetY), a)
                                }
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                isDragging = false
                                onDragStateChanged(Offset.Zero, 1f)
                            }
                        }
                    }

                    if (scaleAnim.value < 1f) {
                        ZoomableImageCalculator.animateZoom(
                            scope = scope,
                            scaleAnim = scaleAnim,
                            offsetXAnim = offsetXAnim,
                            offsetYAnim = offsetYAnim,
                            targetScale = 1f,
                            targetOffset = Offset.Zero
                        )
                    } else if (scaleAnim.value > maxScale) {
                        scope.launch {
                            scaleAnim.animateTo(maxScale, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    } else if (scaleAnim.value > 1.05f && !isDismissDrag) {
                        // 放大状态下的手指抬起：惯性 fling 平移与越界平滑回弹
                        val velocity = velocityTracker.calculateVelocity()
                        val (maxOffsetX, maxOffsetY) = ZoomableImageCalculator.calculateMaxPanOffset(
                            containerWidth = getCurW(),
                            containerHeight = getCurH(),
                            imageRatio = getCurRatio(),
                            currentScale = scaleAnim.value
                        )
                        ZoomableImageCalculator.flingWithInertia(
                            scope = scope,
                            animatable = offsetXAnim,
                            initialVelocity = velocity.x,
                            maxBound = maxOffsetX
                        )
                        ZoomableImageCalculator.flingWithInertia(
                            scope = scope,
                            animatable = offsetYAnim,
                            initialVelocity = velocity.y,
                            maxBound = maxOffsetY
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val isDataCached = remember(dataUrl) {
            context.imageLoader.memoryCache?.get(MemoryCache.Key(dataUrl)) != null
        }
        val highResAlpha = remember(dataUrl) {
            Animatable(if (isDataCached) 1f else 0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isDragging) {
                        translationX = dragOffsetX
                        translationY = dragOffsetY
                        scaleX = dragScale
                        scaleY = dragScale
                    } else {
                        val backScale = (1f - predictiveBackProgress * 0.12f).coerceIn(0.5f, 1f)
                        translationX = offsetXAnim.value * backScale
                        translationY = offsetYAnim.value * backScale
                        scaleX = scaleAnim.value * backScale
                        scaleY = scaleAnim.value * backScale
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 第一层：稳态缩略图垫底层（Base Thumbnail Layer）
            // 始终复用已存在于 Coil MemoryCache 中的缩略图/中图，以 100% 不透明度垫底，
            // 彻底杜绝白屏、黑闪或透明穿透，保证从展开飞行动画结束的那一帧起就有画面！
            if (highResAlpha.value < 1f && placeholderUrl != dataUrl) {
                val baseRequest = rememberStableImageRequest(dataUrl = placeholderUrl)
                AsyncImage(
                    model = baseRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 第二层：高清大图/原图顶层（High-Res Overlay Layer）
            // 若已命中内存缓存则瞬时 100% 呈现；若需异步网络加载/磁盘解码，就绪后以 180ms 单向平滑淡入
            // 覆盖在缩略图上，产生如相机对焦般从微弱模糊自然过渡到极度锐利的无感视觉体验。
            val stableRequest = rememberStableImageRequest(dataUrl = dataUrl, placeholderUrl = placeholderUrl)
            AsyncImage(
                model = stableRequest,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onState = { state ->
                    isLoading = state is AsyncImagePainter.State.Loading && highResAlpha.value == 0f && placeholderUrl == dataUrl
                    if (state is AsyncImagePainter.State.Success) {
                        val w = state.result.image.width
                        val h = state.result.image.height
                        if (w > 0 && h > 0) {
                            val ratio = w.toFloat() / h.toFloat()
                            imageRatio = ratio
                            ImageRatioCache.put(url, ratio)
                            onImageRatioLoaded(ratio)
                        }
                        if (highResAlpha.value < 1f) {
                            scope.launch {
                                highResAlpha.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                            }
                        }
                    }
                    if (state is AsyncImagePainter.State.Error) onLoadError()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = highResAlpha.value
                    }
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/** 大图查看器底部胶囊操作按钮。 */
@Composable
private fun MorphActionButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (filled) Color.White.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (filled) Color.Black else Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatBytes(bytes: Long): String? {
    if (bytes <= 0L) return null
    if (bytes < 1024L) return "${bytes}B"
    val kb = bytes / 1024f
    if (kb < 1024f) return String.format("%.0fKB", kb)
    val mb = kb / 1024f
    return String.format("%.1fMB", mb)
}
