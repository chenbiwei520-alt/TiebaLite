package com.huanchengfly.tieba.post.ui.page.photoview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MimeTypes
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.LocalMorphState
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphAnchor
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphGallery
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphState
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.originUrls
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.TiebaLog
import com.huanchengfly.tieba.post.utils.extension.toShareIntent
import kotlinx.coroutines.launch

/**
 * 应用内的看图页：主窗口里的一层 Compose 叠层，而不是独立 Activity。
 *
 * 为什么不用 Activity：看图页作为独立窗口时，图片还在下载的那一两秒里，从屏幕边缘划进来的手势会
 * 被系统手势区在窗口之前取走——窗口连 ACTION_DOWN 都收不到（实测根节点一次 down 都没有），
 * 返回键也投递不过来，于是「加载中侧滑返回」完全没反应。放进主窗口就变成普通的返回分发，
 * 和 App 其它页面走同一条链路。
 *
 * 一镜到底交给 [MorphState] / [MorphGallery]（移植自 Matter）：形变浮层直接叠在应用根窗口之上，
 * 不改动导航栈，浮层自己负责展开、跟手退出、预测返回与收拢，这里只负责会话（图片列表）的生命周期。
 * */
object PhotoViewer {

    /** 一次打开会话。id 用来让 ViewModel 在「换了一张图打开」时重建。 */
    data class Session(
        val id: Long,
        val data: PhotoViewData,
        val initialUrls: List<String>,
    )

    var session by mutableStateOf<Session?>(null)
        private set

    private var token = 0L
    private var hostAttached = false

    /** 叠层宿主挂上了没有。没挂上（比如宿主还没进组合）就退回独立 Activity。 */
    val isAttached: Boolean get() = hostAttached

    /**
     * 打开看图页；只有叠层宿主可用、且拿得到形变状态时才接手，返回 true。
     * [anchor] 是被点的那张缩略图在窗口里的矩形，飞行层从这里长到全屏。
     * */
    fun openIfAttached(
        data: PhotoViewData,
        morphState: MorphState?,
        anchor: MorphAnchor? = null,
    ): Boolean {
        if (!hostAttached || morphState == null) return false
        token += 1
        val urls = data.originUrls()
        session = Session(id = token, data = data, initialUrls = urls)

        // 页码按图集顺序定位：优先用被点那张的地址；地址与图片列表对不上时用接口给的 picIndex 兜底，
        // 否则会静默落到第一张（多图帖表现为「点第三张、打开是第一张」）。
        val index = urls.indexOf(anchor?.url).takeIf { it >= 0 }
            ?: (data.data?.picIndex?.minus(1) ?: 0).coerceIn(0, urls.lastIndex.coerceAtLeast(0))
        TiebaLog.d(
            "PhotoViewer",
            "open urls=" + urls.size + " anchor=" + (anchor?.url?.takeLast(24) ?: "null") +
                " index=" + index + " picIndex=" + data.data?.picIndex +
                " picItems=" + data.picItems.size
        )
        val startBounds = anchor?.bounds ?: screenCenterBounds(morphState)
        morphState.open(
            urls = urls,
            index = index,
            bounds = startBounds,
            cornerRadiusPx = anchor?.cornerRadiusPx ?: 0f,
            imageRatio = anchor?.ratio,
            thumbUrls = anchor?.thumbUrl?.let { mapOf(urls[index] to it) },
        )
        return true
    }

    /** 没有卡片槽位时的兜底起点：屏幕中心一块小矩形，形变依然成立只是没有明确来处。 */
    private fun screenCenterBounds(morphState: MorphState) =
        morphState.screenBounds.takeIf { !it.isEmpty }?.let { screen ->
            val half = minOf(screen.width, screen.height) * 0.18f
            val cx = screen.center.x
            val cy = screen.center.y
            androidx.compose.ui.geometry.Rect(cx - half, cy - half, cx + half, cy + half)
        } ?: androidx.compose.ui.geometry.Rect.Zero

    /** 形变收拢结束：会话可以撤了。 */
    internal fun onClosed() {
        session = null
    }

    internal fun attachHost() {
        hostAttached = true
    }

    internal fun detachHost() {
        hostAttached = false
        session = null
    }
}

/**
 * 看图页叠层的宿主。放在主窗口最上层（内容之上），[PhotoViewer.session] 为空时不占任何东西。
 * */
@Composable
fun PhotoViewerHost() {
    DisposableEffect(Unit) {
        PhotoViewer.attachHost()
        onDispose { PhotoViewer.detachHost() }
    }

    val morphState = LocalMorphState.current
    val session = PhotoViewer.session

    if (session == null || morphState == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 每次打开一份独立的 ViewModel，随会话结束一起清掉：看图页会被反复打开，
    // 挂在 Activity 的 Store 上会一直攒着上一次的图片列表。
    val sessionOwner = remember(session.id) { SessionViewModelStoreOwner() }
    DisposableEffect(sessionOwner) {
        onDispose { sessionOwner.viewModelStore.clear() }
    }
    val viewModel: PhotoViewViewModel = viewModel(viewModelStoreOwner = sessionOwner)

    LaunchedEffect(session.id) { viewModel.initData(session.data) }

    // 注意：这里刻意不动系统栏。参考实现（Matter）全仓没有任何 WindowInsetsController 调用，
    // 原因就在这类叠层上——hide/show 系统栏会改变 WindowInsets，而每个页面的顶部内缩都读它，
    // 于是「打开/关闭大图」会连带把下层列表顶起或落下正好一个状态栏高度（实测 96px）。
    // 浮层自己的黑底已经足够沉浸，系统栏交给窗口。

    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // 收拢动画播完（浮层自己落到 0）才撤会话：中间这几帧飞行图片还在飞，撤早了会露底。
    LaunchedEffect(morphState, session.id) {
        snapshotFlow { morphState.isPresent }.collect { present ->
            if (!present) PhotoViewer.onClosed()
        }
    }

    LaunchedEffect(uiState.error) {
        val error = uiState.error ?: return@LaunchedEffect
        context.toastShort(error.getErrorMessage())
        morphState.collapse()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MorphGallery(
            morphState = morphState,
            items = uiState.data,
            modifier = Modifier.fillMaxSize(),
            initialIndex = uiState.initialIndex,
            onPageShown = viewModel::onPageShown,
            onShare = { item ->
                context.toastShort(R.string.toast_preparing_share_pic)
                scope.launch {
                    ImageUtil.downloadForShare(context.applicationContext, item.originUrl)
                        .onSuccess { uri ->
                            runCatching {
                                context.startActivity(
                                    uri.toShareIntent(
                                        context = context,
                                        type = MimeTypes.IMAGE_JPEG,
                                        message = context.getString(R.string.title_share_pic),
                                    )
                                )
                            }
                        }
                        .onFailure { context.toastShort(it.getErrorMessage()) }
                }
            },
            onSave = { url -> ImageUtil.download(context, url = url) },
        )
    }
}

private class SessionViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
