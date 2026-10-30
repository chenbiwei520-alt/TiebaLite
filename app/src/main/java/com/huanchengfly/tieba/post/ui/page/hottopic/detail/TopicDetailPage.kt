package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import coil3.compose.AsyncImage
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.createTopAppBarColors
import com.huanchengfly.tieba.post.theme.isTranslucent
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.hottopic.detail.TopicDetailViewModel.Companion.feedId
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadBlockedTip
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.miuix.MiuixProgressiveBlurBar
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixScaffoldBackdrop
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapsingTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixExitUntilCollapsedScrollBehavior
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import top.yukonga.miuix.kmp.basic.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadStaggeredGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaLog
import com.huanchengfly.tieba.post.utils.TiebaLog as TL
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun TopicDetailPage(
    navigator: NavController,
    viewModel: TopicDetailViewModel = hiltViewModel<TopicDetailViewModel>()
) {
    val gridState = rememberLazyStaggeredGridState()
    val coroutineScope = rememberCoroutineScope()
    val isWideWindow = LocalWindowAdaptiveInfo.current.windowSizeClass
        .isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        when (it) {
            is TopicDetailUiEvent.RefreshSuccess -> coroutineScope.launch {
                gridState.scrollToItem(0, 0)
            }

            is ThreadLikeUiEvent -> toastShort(it.toMessage(context = this))

            is CommonUiEvent.ToastError -> toastShort(R.string.toast_exception, it.message)
        }
    }

    ConsumeThreadPageResult<Destination.HotTopicDetail>(navigator, viewModel::onThreadResult)

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = uiState.isEmpty,
        isLoading = uiState.isRefreshing && uiState.isEmpty,
        error = uiState.error,
        onReload = viewModel::onRefresh
    ) {
        // Miuix scroll behavior drives the collapsing bar (height offset, collapse fraction) and,
        // through the same state, the progressive blur fade. Only one scroll pipeline is attached
        // to the list below.
        val scrollBehavior = rememberMiuixExitUntilCollapsedScrollBehavior()
        val scrollOrientationConnection = rememberScrollOrientationConnection()

        val threadClickListeners = remember(navigator) {
            createThreadClickListeners(onNavigate = navigator::navigateDebounced)
        }

        val hideBlockedContent by viewModel.hideBlockedContent.collectAsStateWithLifecycle()

        val topicInfo = uiState.topicInfo

        TiebaLog.d("TopicBlur", "composed fraction=${scrollBehavior.state.collapsedFraction}")
        DisposableEffect(Unit) {
            onDispose { TiebaLog.d("TopicBlur", "disposed") }
        }

        MyScaffold(
            topBar = {
                // 采样本页 Scaffold 的全屏 capture，必须在 topBar 槽内读取——MyScaffold 只在
                // Miuix Scaffold 周围提供这个 CompositionLocal，页面主体里读到的永远是 null。
                val glassBackdrop = rememberMiuixScaffoldBackdrop()
                topicInfo?.let { info ->
                    TopicToolbar(
                        topicInfo = info,
                        onBack = navigator::navigateUp,
                        scrollBehavior = scrollBehavior,
                        backdrop = glassBackdrop,
                    )
                }
            },
            floatingActionButton = {
                val fabVisible by remember {
                    derivedStateOf { gridState.canScrollBackward && scrollOrientationConnection.isScrollingForward }
                }
                DefaultBackToTopFAB(visible = fabVisible) {
                    coroutineScope.launch {
                        gridState.animateScrollToItem(0)
                        // Miuix semantics: a negative contentOffset means the bar is scrolled away.
                        // Resetting it to 0 puts the bar back to its expanded resting position.
                        scrollBehavior.state.contentOffset = 0f
                    }
                }
            },
        ) { contentPadding ->
            val layoutDirection = LocalLayoutDirection.current
            // The hero's top edge has to track the app bar's bottom edge, and the Scaffold's
            // content padding is exactly that: Miuix's `ScaffoldLayout` sets
            // `contentPadding.top = topBarPlaceable.height.toDp()` on every measure pass, and draws
            // the topBar *after* the body content, so the bar overlays whatever is underneath.
            //
            // It used to be `0.dp` here, which put the photo behind the bar — and because the bar's
            // expanded height is content driven (title + ranking row + description, roughly
            // `statusBar + 200..250dp`), the bar text was covering the top half of the picture.
            //
            // Keeping the live value gives the wanted behaviour at every scroll position:
            //   * at rest the photo starts exactly where the text ends, so it is fully visible;
            //   * while the bar collapses the list receives no scroll (`exitUntilCollapsed` consumes
            //     it into `heightOffset`), so the photo stays glued to the bar's bottom edge and is
            //     never covered;
            //   * once the bar is fully collapsed the padding stops changing, so further scrolling
            //     pushes the photo under the bar where the progressive blur takes over.
            val feedPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
            )
            // Resolved out here because `LazyListScope`'s DSL is not a composable scope: theme and
            // density reads are not allowed inside `items { }`.
            val heroImageUrl = topicInfo?.topicImage.orEmpty()
            val showHero = heroImageUrl.isNotEmpty() && !TiebaLiteTheme.colorScheme.isTranslucent

            // The page no longer keeps its own blur capture: the toolbar samples the Scaffold's
            // full-screen one (see the topBar slot). A page-level capture box here used to stop
            // being recorded after returning from a thread page, and the blur silently vanished.
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::onRefresh,
                contentPadding = contentPadding,
            ) {
                Container (
                    fluid = isWideWindow,
                    modifier = Modifier
                        .nestedScroll(connection = scrollOrientationConnection)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    ProvideNavigator(navigator) {
                        SwipeUpLazyLoadStaggeredGrid(
                            modifier = Modifier.fillMaxSize(),
                            state = gridState,
                            columns = StaggeredGridCells.Adaptive(400.dp),
                            contentPadding = feedPadding,
                            verticalItemSpacing = 16.dp,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            isLoading = uiState.isLoadingMore,
                            onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                            bottomIndicator = defaultBottomIndicator,
                        ) {
                            // The topic photo, as the feed's first item. Living in the scrolling
                            // content is what makes the bar's progressive blur work: the photo is
                            // inside the captured layer, so it is blurred by the bar exactly like the
                            // rest of the feed while it slides underneath.
                            if (showHero) {
                                item(key = "topic-hero", span = StaggeredGridItemSpan.FullLine) {
                                    TopicHeroImage(imageUrl = heroImageUrl)
                                }
                            }

                            itemsIndexed(
                                items = uiState.threads,
                                key = { _, item -> item.feedId },
                            ) { index, item ->
                                BlockableContent(
                                    blocked = item.blocked,
                                    blockedTip = { ThreadBlockedTip() },
                                    modifier = Modifier.fillMaxWidth(),
                                    hideBlockedContent = hideBlockedContent
                                ) {
                                    // One Miuix group card per feed item, no divider lines
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        FeedCard(
                                            thread = item,
                                            onClick = threadClickListeners.onClicked,
                                            onLike = viewModel::onThreadLikeClicked,
                                            onClickReply = threadClickListeners.onReplyClicked,
                                            onClickUser = threadClickListeners.onAuthorClicked,
                                            onClickForum = threadClickListeners.onForumClicked,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val TopicToolbarExpandedHeight = 180.dp

/**
 * The topic photo's aspect ratio, used to size its box.
 *
 * The API's `topic_image` is a square thumbnail: eight live topics were sampled and every one came
 * back 1:1 (`238x238`, `500x500`, `1080x1080`, ...). Driving the box from the photo's own ratio is
 * what makes it possible to show the whole picture — the box then matches the bitmap exactly, so
 * `ContentScale.Crop` has nothing left to cut off.
 *
 * Sizing it from the photo, rather than from the app bar, is also what keeps it still. It used to be
 * a `matchParentSize()` child of the collapsing bar, so the box was `screenWidth x barHeight` and
 * changed height on every scroll frame; `Crop` re-scaled and re-centred the bitmap to keep covering
 * it, so the picture swam and zoomed while the bar collapsed. Against a 2.18:1 bar that box was also
 * discarding about 54% of a square photo's height, which is why only a slice of hair and one eye was
 * ever on screen.
 */
private const val TopicHeroAspectRatio = 1f

/**
 * The topic photo, rendered as the first item of the feed.
 *
 * Because it lives in the scrolling content it participates in the same captured layer as the rest
 * of the feed, so the app bar's progressive blur samples it while it slides underneath the bar —
 * see [TopicToolbar]. Its top edge is positioned by the feed's content padding, which tracks the
 * bar's live height, so no part of the picture ever sits behind the bar's title, ranking row or
 * description.
 *
 * @param imageUrl the topic photo URL.
 */
@Composable
private fun TopicHeroImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        // `fillMaxWidth` + the photo's own ratio: the box is exactly the photo, so the whole
        // picture is on screen and `Crop` never has to discard anything.
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(TopicHeroAspectRatio),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun TopicToolbar(
    modifier: Modifier = Modifier,
    topicInfo: TopicInfoBean,
    scrollBehavior: ScrollBehavior? = null,
    backdrop: LayerBackdrop? = null,
    onBack: () -> Unit = {},
) {
    // 有 backdrop 时由渐进模糊层供底，栏自身两个状态色都必须透明：只改 containerColor 的
    // 话，收起交叠（overlappedFraction > 0.01）会切到不透明的 scrolledContainerColor（默认
    // surfaceContainer）整层画出，把模糊盖住——不透明主题下话题栏会变实心（同 TopAppBar
    // 包装与 ForumPage 的分支写法）
    val topAppBarColors = if (backdrop != null) {
        MaterialTheme.colorScheme.createTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    } else {
        TiebaLiteTheme.topAppBarColors
    }

    Box(
        modifier = modifier,
    ) {
        // Progressive blur over whatever scrolls under the bar. The topic photo is deliberately
        // NOT painted here any more — it is the feed's first item (`TopicHeroImage`), which puts
        // the photo and the list in the same captured layer so the blur can actually sample it.
        //
        // 与动态页同款：miuix-blur 的 progressiveTextureBlur（真 AGSL 渐进模糊），backdrop
        // 采 Scaffold 全屏捕获并经 AGSL 探针门控。blurProgress 语义：模糊层透明度，随栏
        // 收起从 0 渐入到 1。
        MiuixProgressiveBlurBar(
            backdrop = backdrop,
            blurProgress = { scrollBehavior?.state?.collapsedFraction?.coerceIn(0f, 1f) ?: 0f },
        ) {
            CollapsingTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_topic, topicInfo.topicName),
                        autoSize = TextAutoSize.StepBased(8.sp, LocalTextStyle.current.fontSize),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                },
                subtitle = {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.topic_index, topicInfo.idxNum),
                        )

                        Text(
                            text = stringResource(id = R.string.hot_num, topicInfo.discussNum.getShortNumString()),
                        )
                    }
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = onBack)
                },
                expandedHeight = TopicToolbarExpandedHeight,
                colors = topAppBarColors,
                scrollBehavior = scrollBehavior,
                collapsibleExtraContent = true,
            ) {
                if (topicInfo.topicDesc.isNotEmpty()) {
                    Text(
                        text = topicInfo.topicDesc,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}