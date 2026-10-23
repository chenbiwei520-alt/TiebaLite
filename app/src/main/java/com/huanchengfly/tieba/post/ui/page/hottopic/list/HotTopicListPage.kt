package com.huanchengfly.tieba.post.ui.page.hottopic.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.topicList.NewTopicList
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.Grey300
import com.huanchengfly.tieba.post.theme.OrangeA700
import com.huanchengfly.tieba.post.theme.RedA700
import com.huanchengfly.tieba.post.theme.YellowA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.BebasFamily
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.TopicTag
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBarPaged
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.squircle.squircleClip

@Composable
private fun TopicImage(
    index: Int,
    imageUri: String
) {
    val boxModifier = if (index < 3) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(2.39f)
            .squircleClip(SquircleCornerRadius.Medium)
    } else {
        Modifier
            .size(Sizes.Medium)
            .aspectRatio(1f)
            .squircleClip(SquircleCornerRadius.ExtraSmall)
    }
    Box(
        modifier = boxModifier
    ) {
        NetworkImage(
            modifier = Modifier.fillMaxSize(),
            imageUrl = imageUri,
        )

        Text(
            text = "${index + 1}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Grey300,
            fontFamily = BebasFamily,
            modifier = Modifier
                .background(
                    when (index) {
                        0 -> RedA700
                        1 -> OrangeA700
                        2 -> YellowA700
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                .padding(4.dp)
        )
    }
}

@Composable
private fun TopicBody(
    index: Int,
    item: NewTopicList
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.topic_name,
                modifier = Modifier.weight(1.0f),
                style = MaterialTheme.typography.titleMedium
            )
            when (item.topic_tag) {
                2 -> TopicTag(isHot = true)

                1 -> TopicTag(isHot = false)
            }
        }
        Text(
            text = item.topic_desc,
            maxLines = if (index < 3) 3 else 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(id = R.string.hot_num, item.discuss_num.getShortNumString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HotTopicList(
    uiState: HotTopicListUiState,
    onRefresh: () -> Unit = {},
    onTopicClicked: (NewTopicList) -> Unit = {},
    navigateUp: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    StateScreen(
        isEmpty = uiState.topicList.isEmpty(),
        isLoading = uiState.isRefreshing && uiState.topicList.isEmpty(),
        error = uiState.error,
        onReload = onRefresh
    ) {
        val lazyListState = rememberLazyListState()
        val scrollOrientationConnection = rememberScrollOrientationConnection()
        val barBackdrop = rememberMiuixBarBackdrop()

        MyScaffold(
            topBar = {
                TopAppBarPaged(
                    title = {
                        Text(text = stringResource(id = R.string.title_hot_message_list))
                    },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = navigateUp)
                    },
                    // Counterweight matching the back button so the centered title stays on the
                    // true screen center
                    actions = {
                        Spacer(modifier = Modifier.width(48.dp))
                    },
                    backdrop = barBackdrop,
                )
            },
            floatingActionButton = {
                val fabVisible by remember {
                    derivedStateOf { lazyListState.canScrollBackward && scrollOrientationConnection.isScrollingForward }
                }
                DefaultBackToTopFAB(visible = fabVisible) {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                }
            },
        ) { contentPaddings ->
            // FULL-WIDTH blur capture — same rule as `ThreadPage`/`ExplorePage`. `Container` narrows
            // its child to a centred column on wide screens (75% once the window is expanded, i.e.
            // any landscape phone), while the app bar's progressive-blur band spans the whole screen
            // width; a capture attached inside `Container` would be narrower than the band and
            // `progressiveTextureBlur`'s shader would clamp out-of-bounds samples to the capture's
            // edge, smearing the column's edge colour across the margins as flat slabs.
            Box(modifier = Modifier.fillMaxWidth().miuixLayerBackdrop(barBackdrop)) {
            Container {
                PullToRefreshBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    contentPadding = contentPaddings,
                ) {
                    MyLazyColumn(
                        modifier = Modifier.nestedScroll(connection = scrollOrientationConnection),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = contentPaddings,
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        itemsIndexed(
                            items = uiState.topicList,
                            key = { _, item -> item.topic_id },
                        ) { index, item ->
                            val topicClickedListener: () -> Unit = { onTopicClicked(item) }

                            // Each topic gets the same Miuix card surface the feed uses, so the topic
                            // ranking reads as one visual family with the rest of the app.
                            Card(modifier = Modifier.fillMaxWidth()) {
                                if (index < 3) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = topicClickedListener)
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TopicImage(index = index, imageUri = item.topic_image)
                                        TopicBody(index = index, item = item)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = topicClickedListener)
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        TopicImage(index = index, imageUri = item.topic_image)
                                        TopicBody(index = index, item = item)
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

@Composable
fun HotTopicListPage(
    viewModel: HotTopicListViewModel = hiltViewModel<HotTopicListViewModel>(),
    navigator: NavController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    HotTopicList(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onTopicClicked = { item ->
            navigator.navigateDebounced(
                route = Destination.HotTopicDetail(item.topic_id, item.topic_name)
            )
        },
        navigateUp = navigator::navigateUp,
    )
}
