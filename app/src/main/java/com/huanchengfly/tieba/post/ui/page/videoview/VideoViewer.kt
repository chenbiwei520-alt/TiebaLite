package com.huanchengfly.tieba.post.ui.page.videoview

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.VideoInfo
import com.huanchengfly.tieba.post.api.models.protos.aspectRatio
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.components.media.MediaCache.BD_VIDEO_HOST
import com.huanchengfly.tieba.post.components.media.MediaCache.getBdMediaId
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MORPH_SETTLE_SPEC
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphAnchor
import com.huanchengfly.tieba.post.ui.widgets.compose.video.TopControls
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoContent
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoControls
import com.huanchengfly.tieba.post.ui.widgets.compose.video.VideoPreviewState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberPictureInPictureState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.rememberPlayerGestureState
import com.huanchengfly.tieba.post.ui.widgets.compose.video.retainVideoPlayer
import com.huanchengfly.tieba.post.utils.DownloadUtil
import com.huanchengfly.tieba.post.utils.MediaUtil
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.PermissionUtils.onDenied
import com.huanchengfly.tieba.post.utils.PermissionUtils.onGranted
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

/*
 * 视频的容器形变（Video Container Morph），对位 Matter 的
 * ui/navigation/SharedTransitionHelper.kt: Modifier.weiboVideoSharedBounds + WeiboCardMorphBoundsTransform。
 *
 * Matter 的视频走的是导航共享元素：列表里的视频卡片与全屏播放页绑同一个 sharedBounds key，
 * 由 NavHost 的共享元素框架用 spring(dampingRatio = 1f, stiffness = 280f) 逐帧插值容器 Bounds，
 * 圆角从卡片 8.dp 收成全屏 0.dp，入场淡入 280ms、出场淡出 200ms。
 *
 * 本项目的视频是独立 Activity，没有 NavHost 可以挂共享元素，因此这里把同一套形变
 * 做成一枚主窗口内的叠层（与看图页 PhotoViewer 同一个思路）：容器 Bounds 由同一份
 * 临界阻尼弹簧驱动，从缩略图矩形长到整屏，退出时沿原路飞回去，然后才撤掉会话。
 * */

/** 视频会话：被点的那一格缩略图、要播的地址与播放位置。 */
data class VideoSession(
    val id: Long,
    val uri: String,
    val mediaId: String,
    val thumbnailUrl: String?,
    val aspectRatio: Float,
    val positionMs: Long,
    val anchor: MorphAnchor?,
)

/**
 * 视频形变的宿主状态。
 *
 * 与 [com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewer] 一致：只有主窗口挂上了宿主
 * （[attachHost]）才把播放请求接进叠层，否则调用方退回 [com.huanchengfly.tieba.post.activities.VideoViewActivity]。
 * */
object VideoViewer {

    var session by mutableStateOf<VideoSession?>(null)
        private set

    /** 正在收拢：容器已经往回飞，但会话还没撤（播放器要继续渲染到最后一段动画）。 */
    var closing by mutableStateOf(false)
        private set

    private var token = 0L
    private var hostAttached = false

    val isAttached: Boolean get() = hostAttached

    /**
     * 请求在主窗口里播放视频。返回 false 表示当前没有宿主（例如从通知/外部 Intent 进来），
     * 调用方应退回独立 Activity。
     * */
    fun open(
        context: Context,
        videoUrl: String,
        thumbnailUrl: String? = null,
        mediaId: String? = null,
        aspectRatio: Float = 0f,
        positionMs: Long = C.TIME_UNSET,
        anchor: MorphAnchor? = null,
    ): Boolean {
        val data = Uri.parse(videoUrl).normalizeScheme()
        // 与 VideoViewActivity.launch 相同的过期判定：tb-video 未授权/过期的地址直接提示。
        if (data.host == BD_VIDEO_HOST && videoUrl.endsWith(".mp4")) {
            context.toastShort(R.string.desc_expired_video_link)
            return true
        }
        if (!hostAttached) return false
        closing = false
        session = VideoSession(
            id = ++token,
            uri = data.toString(),
            mediaId = mediaId ?: data.getBdMediaId(),
            thumbnailUrl = thumbnailUrl?.takeIf { it.isNotEmpty() },
            aspectRatio = aspectRatio,
            positionMs = positionMs,
            anchor = anchor,
        )
        return true
    }

    fun open(
        context: Context,
        videoInfo: VideoInfo,
        positionMs: Long = C.TIME_UNSET,
        anchor: MorphAnchor? = null,
    ): Boolean = open(
        context = context,
        videoUrl = videoInfo.videoUrl,
        thumbnailUrl = videoInfo.thumbnailUrl,
        mediaId = videoInfo.videoMD5,
        aspectRatio = videoInfo.aspectRatio(),
        positionMs = positionMs,
        anchor = anchor,
    )

    /** 开始收拢：先播回程动画，动画结束再 [finishClose]。 */
    fun close() {
        if (session != null) closing = true
    }

    internal fun finishClose() {
        closing = false
        session = null
    }

    internal fun attachHost() {
        hostAttached = true
    }

    internal fun detachHost() {
        hostAttached = false
        closing = false
        session = null
    }
}

/** 视频形变叠层的宿主，挂在主窗口最上层（与看图页同一层）。 */
@Composable
fun VideoViewerHost() {
    DisposableEffect(Unit) {
        VideoViewer.attachHost()
        onDispose { VideoViewer.detachHost() }
    }

    val session = VideoViewer.session ?: return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenBounds = remember(constraints.maxWidth, constraints.maxHeight) {
            Rect(
                left = 0f,
                top = 0f,
                right = constraints.maxWidth.toFloat(),
                bottom = constraints.maxHeight.toFloat(),
            )
        }
        VideoMorphLayer(session = session, screenBounds = screenBounds)
    }
}

@Composable
private fun VideoMorphLayer(session: VideoSession, screenBounds: Rect) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val player = retainVideoPlayer(
        initialMediaItem = MediaItem.Builder()
            .setUri(Uri.parse(session.uri))
            .setMediaId(session.mediaId)
            .build(),
        playWhenReady = session.positionMs == C.TIME_UNSET,
    )
    // 主窗口内的视频叠层：不碰系统栏，否则退出时下层列表会整体跳动一个状态栏高度。
    val gestureState = rememberPlayerGestureState(player, hideSystemBars = false)

    // 控制条与形变容器分开摆放（对位 Matter：控制条属于目标页面，不跟着共享元素缩放），
    // 因此内容缩放状态要在这里提升出来。
    var contentScale by remember(session.id) { mutableStateOf(ContentScale.Fit) }
    val pipState = rememberPictureInPictureState(player)

    // 形变进度：0 = 还在缩略图的位置，1 = 整屏。临界阻尼弹簧与 Matter 的
    // WeiboCardMorphBoundsTransform 完全一致（零过冲，约 350~420ms 到位）。
    val progress = remember(session.id) { Animatable(0f) }
    LaunchedEffect(session.id) {
        progress.animateTo(1f, MORPH_SETTLE_SPEC)
    }
    LaunchedEffect(VideoViewer.closing) {
        if (!VideoViewer.closing) return@LaunchedEffect
        progress.animateTo(0f, MORPH_SETTLE_SPEC)
        VideoViewer.finishClose()
    }

    // 从缩略图飞进整屏；没有锚点（例如隐藏媒体占位）就从屏幕中心一个等比小矩形长出来。
    val startBounds = remember(session.id) {
        session.anchor?.bounds?.takeIf { !it.isEmpty }
            ?: centerBounds(screenBounds, session.aspectRatio)
    }
    val cornerStart = session.anchor?.cornerRadiusPx ?: 0f
    val t = progress.value.coerceIn(0f, 1f)
    val bounds = lerpRect(startBounds, screenBounds, t)
    val cornerRadiusPx = cornerStart * (1f - t)

    BackHandler(enabled = !VideoViewer.closing) { VideoViewer.close() }

    if (session.positionMs > 0) {
        LaunchedEffect(session.id) {
            MediaUtil.handleSeekToAction(player, session.positionMs)
            player.playWhenReady = true
        }
    }

    LifecycleStartEffect(player) {
        VideoPreviewState.onVideoViewMediaChanged(session.mediaId)
        onStopOrDispose {
            VideoPreviewState.onVideoViewMediaChanged(
                mediaId = player.currentMediaItem?.mediaId ?: session.mediaId,
                positionMs = MediaUtil.getCurrentPositionMs(player),
            )
        }
    }

    val download: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            enqueueVideoDownload(context, session.uri, session.mediaId)
        } else {
            scope.launch {
                context.askPermission(
                    R.string.tip_permission_storage_download,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
                    .onDenied { context.toastShort(R.string.toast_no_permission_download_video) }
                    .onGranted { enqueueVideoDownload(context, session.uri, session.mediaId) }
            }
        }
    }

    // 形变期间吃掉所有触摸，避免误触到底下的信息流；幕布不再压暗整屏
    // —— Matter 的黑色来自共享元素自己的背景，屏幕其余部分仍是原来的页面。
    val touchBlocking = if (t < 0.999f) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().then(touchBlocking))

        Box(
            modifier = Modifier
                .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                .requiredSize(
                    width = with(density) { bounds.width.toDp() },
                    height = with(density) { bounds.height.toDp() },
                )
                .clip(RoundedCornerShape(cornerRadiusPx / density.density))
                .background(Color.Black)
        ) {
            // 画面跟着容器矩形飞。SurfaceView 不参与 Compose 的裁剪与圆角，形变期间会顶掉卡片圆角，
            // 所以叠层里统一用 TextureView。
            VideoContent(
                player = player,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                thumbnailUrl = session.thumbnailUrl,
                gestureState = gestureState,
            )
        }

        // 控制条铺满整屏，只随形变淡入淡出：容器还在卡片大小时不出现，避免「整块播放器被缩放」的观感。
        VideoControls(
            player = player,
            gestureState = gestureState,
            contentScale = contentScale,
            onContentScaleSelected = { contentScale = it },
            pipState = pipState,
            topControls = {
                TopControls(
                    player = player,
                    modifier = Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets),
                    pipState = null,
                    onBack = { VideoViewer.close() },
                    onDownload = download,
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = ((t - 0.55f) / 0.45f).coerceIn(0f, 1f) },
        )
    }
}

private fun enqueueVideoDownload(context: Context, videoUrl: String, mediaId: String) {
    runCatching {
        val uri = Uri.parse(videoUrl).normalizeScheme()
        DownloadUtil.downloadVideo(uri, File(DownloadUtil.DEFAULT_DOWNLOAD_DIR, mediaId + ".mp4"))
    }
        .onSuccess { context.toastShort(R.string.toast_download_manager_enqueued) }
        .onFailure { context.toastShort(R.string.toast_exception, it.getErrorMessage()) }
}

/** 没有缩略图锚点时，取屏幕中心一块按视频比例缩小的矩形作为形变起点。 */
private fun centerBounds(screenBounds: Rect, aspectRatio: Float): Rect {
    val ratio = if (aspectRatio > 0f) aspectRatio else 16f / 9f
    val maxW = screenBounds.width * 0.72f
    val maxH = screenBounds.height * 0.4f
    val width = minOf(maxW, maxH * ratio)
    val height = width / ratio
    val cx = screenBounds.center.x
    val cy = screenBounds.center.y
    return Rect(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f)
}

private fun lerpRect(start: Rect, end: Rect, t: Float): Rect = Rect(
    left = start.left + (end.left - start.left) * t,
    top = start.top + (end.top - start.top) * t,
    right = start.right + (end.right - start.right) * t,
    bottom = start.bottom + (end.bottom - start.bottom) * t,
)