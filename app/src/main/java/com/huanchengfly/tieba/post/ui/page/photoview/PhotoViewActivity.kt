package com.huanchengfly.tieba.post.ui.page.photoview

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.goToActivityDebounced
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.models.PicItem
import com.huanchengfly.tieba.post.theme.DefaultDarkColors
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.LocalMorphState
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphAnchor
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphGallery
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphState
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.originUrls
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.rememberMorphState
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.utils.TiebaLog
import com.huanchengfly.tieba.post.utils.extension.getParcelableExtraCompat
import com.huanchengfly.tieba.post.utils.extension.toShareIntent
import kotlinx.coroutines.launch

/**
 * 看图页的独立窗口形态（兜底）。
 *
 * 应用内绝大多数时候走主窗口里的叠层（[PhotoViewer]）。只有当叠层宿主还没进组合、
 * 或者调用方拿不到形变状态时，才会退到这个独立 Activity；此时它自己也挂一套
 * [MorphState] + [MorphGallery]，动画与主窗口完全一致，只是没有卡片槽位可用，
 * 起点取屏幕中心。
 * */
class PhotoViewActivity : AppCompatActivity() {

    private val viewModel: PhotoViewViewModel by viewModels()

    private val windowInsetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    private lateinit var photoViewData: PhotoViewData

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        photoViewData = intent.getParcelableExtraCompat(EXTRA_PHOTO_VIEW_DATA)!!
        viewModel.initData(photoViewData)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

        setContent {
            TiebaLiteTheme(colorSchemeExt = DefaultDarkColors) {
                val morphState = rememberMorphState()
                CompositionLocalProvider(LocalMorphState provides morphState) {
                    val uiState by viewModel.state.collectAsStateWithLifecycle()

                    LaunchedEffect(uiState.error) {
                        val error = uiState.error ?: return@LaunchedEffect
                        Toast.makeText(application, error.getErrorMessage(), Toast.LENGTH_LONG).show()
                        morphState.collapse()
                    }

                    // 形变收拢播完才退出窗口：中间那几帧飞行图片还在飞，提前 finish 会露底。
                    LaunchedEffect(Unit) {
                        var opened = false
                        snapshotFlow { morphState.isPresent }.collect { present ->
                            if (present) opened = true else if (opened) finish()
                        }
                    }

                    BackHandler { morphState.collapse() }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val screenRect = remember(constraints.maxWidth, constraints.maxHeight) {
                            Rect(
                                left = 0f,
                                top = 0f,
                                right = constraints.maxWidth.toFloat(),
                                bottom = constraints.maxHeight.toFloat(),
                            )
                        }
                        // 没有卡片槽位：起点取屏幕中心一块矩形，仍然是无导航栈切换的形变。
                        LaunchedEffect(screenRect) {
                            if (morphState.isPresent) return@LaunchedEffect
                            morphState.reportScreenBounds(screenRect)
                            val urls = photoViewData.originUrls()
                            if (urls.isEmpty()) return@LaunchedEffect
                            val index = photoViewData.data?.originUrl
                                ?.let(urls::indexOf)
                                ?.takeIf { it >= 0 }
                                ?: photoViewData.index.coerceIn(0, urls.lastIndex)
                            val half = minOf(screenRect.width, screenRect.height) * 0.18f
                            val cx = screenRect.center.x
                            val cy = screenRect.center.y
                            morphState.open(
                                urls = urls,
                                index = index,
                                bounds = Rect(cx - half, cy - half, cx + half, cy + half),
                                cornerRadiusPx = 0f,
                            )
                        }

                        MorphGallery(
                            morphState = morphState,
                            items = uiState.data,
                            modifier = Modifier.fillMaxSize(),
                            initialIndex = uiState.initialIndex,
                            onPageShown = viewModel::onPageShown,
                            onShare = ::onShareImage,
                            onSave = { url -> ImageUtil.download(this@PhotoViewActivity, url = url) },
                        )
                    }
                }
            }
        }
    }

    private fun onShareImage(item: PhotoViewItem) {
        toastShort(R.string.toast_preparing_share_pic)
        lifecycleScope.launch {
            ImageUtil.downloadForShare(applicationContext, item.originUrl)
                .onSuccess {
                    val intent = it.toShareIntent(
                        this@PhotoViewActivity,
                        MimeTypes.IMAGE_JPEG,
                        getString(R.string.title_share_pic)
                    )
                    runCatching { startActivity(intent) }
                }
                .onFailure {
                    toastShort(it.getErrorMessage())
                }
        }
    }

    companion object {

        /**
         * Intent Extra: [PhotoViewData] data.
         *
         * @since 4.0.0 Dev 12
         * */
        const val EXTRA_PHOTO_VIEW_DATA = "photo_view_data"

        fun launch(
            context: Context,
            data: PhotoViewData,
            morphState: MorphState? = null,
            anchor: MorphAnchor? = null,
        ) {
            // 应用内一律走主窗口里的叠层（[PhotoViewer]）：看图页作为独立 Activity 时，图片还在
            // 加载的那一两秒里，边缘侧滑会被系统手势区在窗口之前取走（窗口连 down 都收不到），
            // 返回键也投递不过来，看起来就是「加载中侧滑返回没反应」。放进主窗口就能用普通的
            // BackHandler，和 App 其它页面同一条返回链路。
            TiebaLog.d(
                "PhotoViewLaunch",
                "launch anchor=" + (anchor?.url?.takeLast(24) ?: "null") +
                    " picIndex=" + data.data?.picIndex + " picItems=" + data.picItems.size
            )
            if (PhotoViewer.openIfAttached(data, morphState, anchor)) return
            context.goToActivityDebounced<PhotoViewActivity> {
                putExtra(EXTRA_PHOTO_VIEW_DATA, data)
            }
        }

        fun launchSinglePhoto(context: Context, url: String) {
            if (url.isNotEmpty() && url.isNotBlank()) {
                val picItem = PicItem(picId = ImageUtil.getPicId(url), picIndex = 1, url)
                launch(context, data = PhotoViewData(data = null, picItems = listOf(picItem)))
            } else {
                context.toastShort(R.string.desc_image_empty_url)
            }
        }
    }
}
