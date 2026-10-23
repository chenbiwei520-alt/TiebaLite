package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.aspectRatio
import com.huanchengfly.tieba.post.models.PhotoViewData
import com.huanchengfly.tieba.post.theme.LocalExtendedColorScheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.block
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.page.photoview.PhotoViewActivity
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.LocalMorphState
import com.huanchengfly.tieba.post.ui.widgets.compose.morph.MorphAnchor
import com.huanchengfly.tieba.post.utils.CoilUtil
import com.huanchengfly.tieba.post.utils.EmoticonManager
import com.huanchengfly.tieba.post.utils.ImageUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.squircle.squircleBackground

@DrawableRes
val DefaultErrorResource: Int = R.drawable.ic_error

/** 收拢动画要等缩略图量出屏幕位置，最多等这么久；等不到就当这张图不在屏幕上，放弃动画。 */
private const val ReturnBoundsTimeoutMillis = 200L

/** 精确认领失败后，等多长时间才允许同组的其它格接手收拢动画。 */
private const val ReturnLooseClaimDelayMillis = 120L

/**
 * 缩略图在屏幕上停住多久才去预取原图。
 *
 * 太快会在快速划过时把流量全用在没点开的图上；太慢则点开时还没下完。400ms 大约是「手指停住、
 * 眼睛在看这条帖子」的时间。
 * */
private const val OpenPrefetchIdleDelayMillis = 400L

@Composable
fun CircularLoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun PlaceholderRetry(onRetry: () -> Unit) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .clickableNoIndication(onClick = onRetry),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = stringResource(R.string.desc_image_failed),
            modifier = Modifier.weight(1.0f).aspectRatio(1.0f)
        )

        Chip(
            text = stringResource(R.string.button_retry),
            prefixIcon = {
                Icon(
                    imageVector = MiuixIcons.Light.Refresh,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize()
                )
            }
        )
    }
}

@NonRestartableComposable
@Composable
fun ErrorImage(modifier: Modifier = Modifier, tip: String) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painter = painterResource(R.drawable.ic_error), contentDescription = null)

        Text(
            text = tip,
            modifier = Modifier
                .squircleBackground(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    cornerRadius = SquircleCornerRadius.ExtraSmall
                )
                .padding(4.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
@NonRestartableComposable
fun EmoticonInlineImage(
    id: String,
    size: Dp,
    modifier: Modifier = Modifier,
    description: String = id,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    Box(modifier = modifier.size(size)) {
        AsyncImage(
            model = EmoticonManager.getEmoticonUri(id),
            contentDescription = description,
            modifier = Modifier.matchParentSize(),
            error = painterResource(R.drawable.ic_error),
            onError = onError
        )
    }
}

@Composable
private fun PreviewImage(
    modifier: Modifier = Modifier,
    model: String,
    placeholder: MemoryCache.Key?,
    dimensions: IntSize,
) {
    val sizeResolver = rememberConstraintsSizeResolver()
    FullScreen {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .size(sizeResolver)
                    .placeholderMemoryCacheKey(placeholder)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
            )

            val state by painter.state.collectAsState()
            when (state) {
                is AsyncImagePainter.State.Empty,
                is AsyncImagePainter.State.Loading, // Show memory cache as placeholder
                is AsyncImagePainter.State.Success -> {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .block {
                                // Fill width/height based on current orientation
                                if (maxHeight > maxWidth) fillMaxWidth() else fillMaxHeight()
                            }
                            .aspectRatio(ratio = dimensions.aspectRatio ?: 1f)
                            .then(sizeResolver),
                        contentScale = if (maxHeight > maxWidth) ContentScale.FillWidth else ContentScale.FillHeight,
                    )

                    if (state !is AsyncImagePainter.State.Success) {
                        CircularLoadingPlaceholder()
                    }
                }

                is AsyncImagePainter.State.Error -> {
                    ErrorImage(
                        tip = (state as AsyncImagePainter.State.Error).result.throwable.getErrorMessage()
                    )
                }
            }
        }
    }
}

@Composable
fun NetworkImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    dimensions: IntSize? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    photoViewDataProvider: (() -> PhotoViewData?)? = null,
) {
    if (imageUrl.isEmpty()) {
        ErrorImage(modifier, tip = stringResource(R.string.desc_image_empty_url))
        return
    }

    val context = LocalContext.current
    val darkenImage = LocalUISettings.current.darkenImage && LocalExtendedColorScheme.current.darkTheme
    var isLongPressing by remember { mutableStateOf(false) }
    // 点开大图时一镜到底的起点：这张图在 Compose root 里的位置（和悬浮层同一坐标系，随滚动更新）。
    var sourceBounds by remember { mutableStateOf(Rect.Zero) }

    // 一镜到底的形变状态（移植自 Matter）：浮层展开/收拢期间，被点的那一格缩略图要让位，
    // 否则飞行图片与卡片上的缩略图会同时画在屏幕上，看起来是两张图叠在一起。
    val morphState = LocalMorphState.current
    val morphImageUrl = remember(imageUrl, photoViewDataProvider) {
        val viewData = photoViewDataProvider?.invoke()
        viewData?.data?.originUrl
            ?: viewData?.picItems?.firstOrNull()?.originUrl
            ?: imageUrl
    }

    val sizeResolver = rememberConstraintsSizeResolver()
    // 请求对象必须 remember 住：参考项目信息流的缩略图同样用 remember 过的请求
    // （rememberStableImageRequest）。否则形变动画的每一帧重组都会新建一个 ImageRequest，
    // Coil 视作换图重新开一次加载，整屏缩略图会短暂退回占位灰块 —— 看起来就是闪一下。
    val imageRequest = remember(imageUrl, morphImageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(sizeResolver)
            // 与参考项目的信息流一致：缩略图按自己的地址登记内存缓存键（不带上解码尺寸），
            // 形变浮层就能用 placeholderMemoryCacheKey(缩略图地址) 立刻拿到这一张，
            // 接手的第一帧不会是空的黑底。缩略图与原图是同一个地址时跳过，
            // 否则原图会命中这张按缩略图尺寸解码的位图而显得糊。
            .apply {
                if (imageUrl != morphImageUrl) {
                    memoryCacheKey(imageUrl)
                }
            }
            .build()
    }
    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        contentScale = contentScale,
    )

    // pointerInput 的 key 是 Unit，闭包只在首次组合时建立。列表里这个槽位换了图之后
    // 闭包仍指着旧图（表现：滑到后面的图，点开的大图还是第一张），所以统一从
    // rememberUpdatedState 取最新 provider，而不是捕获参数。
    val currentPhotoViewDataProvider by rememberUpdatedState(photoViewDataProvider)

    // 缩略图的圆角（squircle 的小圆角）在大图上按 8.dp 还原，与参考项目一致。
    val thumbCornerRadiusPx = with(LocalDensity.current) { 8.dp.toPx() }

    // 这一格滚出列表（或槽位换了图）时撤掉形变槽位登记，收拢动画就不会飞向已经不存在的位置。
    DisposableEffect(morphState, morphImageUrl) {
        onDispose { morphState?.unregisterSlot(morphImageUrl) }
    }

    Box(
        modifier = modifier
            // 与参考项目一致：在绘制阶段读状态（不是组合阶段），进度每帧变化只重绘这一层，
            // 否则整屏信息流都会跟着重组一遍。
            .graphicsLayer {
                alpha = if (morphState?.isSlotHidden(morphImageUrl, imageUrl) == true) 0f else 1f
            }
            .then(sizeResolver)
            .onGloballyPositioned { coords ->
                val position = coords.positionInRoot()
                sourceBounds = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + coords.size.width,
                    bottom = position.y + coords.size.height,
                )
                // 同组图的每一格都在布局阶段登记自己的矩形。只有「点开时那张」登记的话，
                // 在画廊里滑到另一张再退出，飞行终点仍然是被点那张的位置，动画就会落到隔壁格子上。
                morphState?.registerSlot(morphImageUrl, sourceBounds, thumbCornerRadiusPx)
            }
            .onNotNull(photoViewDataProvider) {
                pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if ((dimensions?.aspectRatio ?: 1f) <= 0.1f) {
                                context.toastShort(R.string.toast_preview_image_too_large)
                            } else {
                                isLongPressing = true
                            }
                        },
                        onPress = {
                            tryAwaitRelease()
                            isLongPressing = false
                        },
                        onTap = {
                            val photos = currentPhotoViewDataProvider?.invoke()
                                ?: return@detectTapGestures
                            // bug from caller
                            if (photos.data != null && photos.data.forumName.isEmpty()) {
                                context.toastShort(R.string.title_unknown_error)
                            } else {
                                val url = photos.data?.originUrl
                                    ?: photos.picItems.firstOrNull()?.originUrl
                                    ?: imageUrl
                                val intrinsic = painter.intrinsicSize
                                val ratio = if (intrinsic.width > 0f && intrinsic.height > 0f) {
                                    intrinsic.width / intrinsic.height
                                } else {
                                    dimensions?.aspectRatio ?: 1f
                                }
                                // 飞行层的起点就是这个格子的位置与圆角：形变浮层从缩略图长到全屏，
                                // 收拢时再沿同一条路径飞回来（与参考项目 ImageGrid 的接线方式一致）。
                                val anchor = MorphAnchor(
                                    url = url,
                                    bounds = sourceBounds,
                                    cornerRadiusPx = thumbCornerRadiusPx,
                                    ratio = ratio,
                                    thumbUrl = imageUrl,
                                )
                                PhotoViewActivity.launch(
                                    context = context,
                                    data = photos,
                                    morphState = morphState,
                                    anchor = anchor,
                                )
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .squircleBackground(MaterialTheme.colorScheme.outline, SquircleCornerRadius.Medium)
                )
            }

            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale,
                    colorFilter = if (darkenImage) CoilUtil.DarkFilter else null,
                )

                if (dimensions != null) {
                    val previewAlpha by animateFloatAsState(
                        targetValue = if (isLongPressing) 1.0f else 0f,
                        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                    )
                    val previewVisible by remember { derivedStateOf { previewAlpha > 0.01f } }
                    if (previewVisible) {
                        PreviewImage(
                            modifier = Modifier.graphicsLayer {
                                alpha = previewAlpha
                            },
                            model = remember(currentPhotoViewDataProvider) {
                                currentPhotoViewDataProvider?.invoke()?.data?.originUrl ?: imageUrl
                            },
                            placeholder = (state as AsyncImagePainter.State.Success).result.memoryCacheKey,
                            dimensions = dimensions,
                        )
                    }
                }
            }

            is AsyncImagePainter.State.Error -> {
                PlaceholderRetry(onRetry = painter::restart)
            }
        }
    }
}
