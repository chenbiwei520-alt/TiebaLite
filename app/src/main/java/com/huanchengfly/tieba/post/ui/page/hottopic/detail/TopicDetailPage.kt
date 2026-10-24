package com.huanchengfly.tieba.post.ui.page.hottopic.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
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
import com.huanchengfly.tieba.post.ui.widgets.compose.BlockableContent
import com.huanchengfly.tieba.post.ui.miuix.MiuixProgressiveBlurBar
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.widgets.compose.CollapsingTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixExitUntilCollapsedScrollBehavior
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import top.yukonga.miuix.kmp.basic.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.coroutines.launch

@Composable
fun TopicDetailPage(
    navigator: NavController,
    viewModel: TopicDetailViewModel = hiltViewModel<TopicDetailViewModel>()
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.uiEvent.collectUiEventWithLifecycle {
        when (it) {
            is TopicDetailUiEvent.RefreshSuccess -> coroutineScope.launch {
                lazyListState.scrollToItem(0, 0)
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

        val barBackdrop = rememberMiuixBarBackdrop()
        val topicInfo = uiState.topicInfo

        MyScaffold(
            topBar = {
                topicInfo?.let { info ->
                    TopicToolbar(
                        topicInfo = info,
                        onBack = navigator::navigateUp,
                        scrollBehavior = scrollBehavior,
                        backdrop = barBackdrop,
                    )
                }
            },
            floatingActionButton = {
                val fabVisible by remember {
                    derivedStateOf { lazyListState.canScrollBackward && scrollOrientationConnection.isScrollingForward }
                }
                DefaultBackToTopFAB(visible = fabVisible) {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
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
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            )
            // Resolved out here because `LazyListScope`'s DSL is not a composable scope: theme and
            // density reads are not allowed inside `items { }`.
            val heroImageUrl = topicInfo?.topicImage.orEmpty()
            val showHero = heroImageUrl.isNotEmpty() && !TiebaLiteTheme.colorScheme.isTranslucent

            // The blur capture must WRAP the pull-to-refresh container, not sit inside it: the
            // refresh container offsets its content through its own layer while dragging, so a
            // capture nested inside it records against a different layer graph and the bar's blur
            // never resolves. Same placement as the Home tab, which is why the blur works there.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .miuixLayerBackdrop(barBackdrop),
            ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::onRefresh,
                contentPadding = contentPadding,
            ) {
                Container (
                    modifier = Modifier
                        .nestedScroll(connection = scrollOrientationConnection)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    ProvideNavigator(navigator) {
                        SwipeUpLazyLoadColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                            contentPadding = feedPadding,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            isLoading = uiState.isLoadingMore,
                            onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                            bottomIndicator = defaultBottomIndicator,
                        ) {
                            // The topic photo, as the feed's first item. Living in the scrolling
                            // content is what makes the bar's progressive blur work: the photo is
                            // inside the captured layer, so it is blurred by the bar exactly like the
                            // rest of the feed while it slides underneath.
                            if (showHero) {
                                item(key = "topic-hero") {
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
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
}

private val TopicToolbarExpandedHeight = 180.dp

/**
 * Height of the toolbar's resting row, i.e. what stays visible once the topic image has collapsed
 * away. Matches `CollapsingTopAppBar`'s collapsed strip so the progressive blur's ramp can be
 * sized to exactly that strip.
 */
private val TopicToolbarCollapsedHeight = 64.dp

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
    val topAppBarColors = TiebaLiteTheme.topAppBarColors

    Box(
        modifier = modifier,
    ) {
        // Progressive blur over whatever scrolls under the bar. The topic photo is deliberately
        // NOT painted here any more — it is the feed's first item (`TopicHeroImage`), which puts
        // the photo and the list in the same captured layer so the blur can actually sample it.
        //
        // The band height is the COLLAPSED strip (`statusBar inset + 64.dp` row), not the 180.dp
        // expanded height: `ProgressiveBlur`'s ramp is a fraction of its layer's height, so a
        // 180.dp layer would leave the whole visible collapsed strip inside the ramp's saturated
        // head and the gradient would never be visible. Sizing it to the collapsed strip keeps the
        // ramp travelling the same physical distance at every point of the collapse.
        val blurBandHeight = with(LocalDensity.current) {
            WindowInsets.statusBars.getTop(this).toDp() + TopicToolbarCollapsedHeight
        }
        MiuixProgressiveBlurBar(
            backdrop = backdrop,
            bandHeight = blurBandHeight,
            // `blurProgress` is the ALPHA of the blur layer: 0f leaves the backdrop untouched, 1f
            // is fully blurred, so it has to ramp *up* with the collapse.
            //
            // The curve that used to sit here was `1f - (collapsedFraction * 3f)` — the fade-OUT
            // `CollapsingTopAppBar` applies to its own expanded content (its `extra` layout node).
            // Reusing it inverted the effect: the blur was strongest while the bar was still fully
            // open and fell to zero exactly when the bar becomes the 64.dp strip that needs it,
            // which is why no progressive blur was ever visible while scrolling up. For a
            // collapsing bar the monotonic signal is `collapsedFraction`; `ForumPage` uses the same
            // one for its collapsing bar.
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
                colors = topAppBarColors.copy(
                    containerColor = topAppBarColors.containerColor.copy(0.01f) // 99% Transparent
                ),
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