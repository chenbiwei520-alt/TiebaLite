package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.miuix.LocalLiquidGlassBackdrop
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.models.search.SearchThreadInfo
import com.huanchengfly.tieba.post.ui.page.Destination.SubPosts
import com.huanchengfly.tieba.post.ui.page.Destination.Thread
import com.huanchengfly.tieba.post.ui.page.Destination.UserProfile
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.search.SearchHistoryList
import com.huanchengfly.tieba.post.ui.page.search.SearchUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ListItemCard
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixSearchTopBar
import com.huanchengfly.tieba.post.ui.widgets.compose.SearchThreadItem
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.Options
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentMapOf
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixPinnedScrollBehavior


@Composable
fun ForumSearchPostPage(
    forumName: String,
    navigator: NavController,
    viewModel: ForumSearchPostViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = rememberSnackbarHostState()
    val scrollBehavior = rememberMiuixPinnedScrollBehavior()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    viewModel.uiEvent.collectUiEventWithLifecycle {
        snackbarHostState.newestSnackbarData()?.dismiss()
        when (it) {
            is SearchUiEvent -> snackbarHostState.showSnackbar(message = it.toMessage(context))
            else -> {/* Unknown UI Event */}
        }
    }

    var inputKeyword by rememberSaveable { mutableStateOf("") }

    val currentSortType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::sortType,
        initial = ForumSearchPostSortType.NEWEST
    )
    val currentFilterType by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::filterType,
        initial = ForumSearchPostFilterType.ALL
    )

    // Submitted search keyword
    val isKeywordNotEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = ForumSearchPostUiState::isKeywordNotEmpty,
        initial = false
    )

    // Input search keyword
    val isInputKeywordNotEmpty by remember {
        derivedStateOf { inputKeyword.isNotEmpty() && inputKeyword.isNotBlank() }
    }

    val onKeywordSubmit: (String) -> Unit = {
        val newKeyword = it.trim()
        viewModel.onSubmitKeyword(newKeyword)
        keyboardController?.hide()
        if (inputKeyword != newKeyword) inputKeyword = newKeyword
        focusManager.clearFocus()
        scrollBehavior.state.contentOffset = 0f // reset
    }

    val threadClickListener: (SearchThreadInfo) -> Unit = {
        val route = when {
            it.postInfoContent != null -> SubPosts(threadId = it.tid, subPostId = it.cid)

            it.mainPostTitle != null -> Thread(threadId = it.tid, postId = it.pid, scrollToReply = true)

            else -> Thread(threadId = it.tid)
        }
        navigator.navigateDebounced(route)
    }

    val barBackdrop = rememberMiuixBarBackdrop()

    MyScaffold(
        topBar = {
            // The search field's glass samples the **Scaffold's** full-screen capture, read HERE:
            // `MyScaffold` provides `LocalLiquidGlassBackdrop` around the Miuix Scaffold, so a read
            // in the page body (above that provider) is always null and the capsule silently falls
            // back to the flat `surfaceContainerHigh` fill. The Scaffold's capture is also recorded
            // in every state (history list, results, empty), unlike a page-level capture that only
            // the result list applies — this is the same arrangement `SearchPage` uses, so the two
            // search bars stay identical.
            val glassBackdrop = LocalLiquidGlassBackdrop.current
            TopAppBar(
                title = {
                    MiuixSearchTopBar(
                        keyword = inputKeyword,
                        onKeywordChange = { inputKeyword = it },
                        onKeywordSubmit = onKeywordSubmit,
                        onBack = navigator::navigateUp,
                        modifier = Modifier.padding(top = 8.dp, end = 18.dp, bottom = 8.dp),
                        label = stringResource(R.string.hint_search_in_ba, forumName),
                        backdrop = glassBackdrop,
                    )
                },
                scrollBehavior = scrollBehavior,
                backdrop = barBackdrop,
            ) {
                AnimatedVisibility(visible = isInputKeywordNotEmpty && isKeywordNotEmpty) {
                    SortToolBar(
                        modifier = Modifier.fillMaxWidth(),
                        sortType = { currentSortType },
                        filterType = { currentFilterType },
                        onSortTypeChanged = viewModel::onSortTypeChanged,
                        onFilterTypeChanged = viewModel::onFilterTypeChanged
                    )
                }
            }
        },
        snackbarHostState = snackbarHostState
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().miuixLayerBackdrop(barBackdrop)) {
        var isSearchHistoryExpanded by rememberSaveable { mutableStateOf(false) }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val error = uiState.error
        val isLoadingMore = uiState.isLoadingMore
        val hasMore = uiState.hasMore
        val data = uiState.data

        if (!isInputKeywordNotEmpty) {
            Container(
                modifier = Modifier.padding(contentPadding)
            ) {
                val history by viewModel.searchHistories.collectAsStateWithLifecycle()
                SearchHistoryList(
                    history = history,
                    onHistoryClick = onKeywordSubmit,
                    expanded = { isSearchHistoryExpanded },
                    onToggleExpand = { isSearchHistoryExpanded = !isSearchHistoryExpanded },
                    onDelete = viewModel::onDeleteHistory,
                    onClear = viewModel::onClearHistory
                )
            }
        } else if (isKeywordNotEmpty) {
            StateScreen(
                isEmpty = data.isEmpty(),
                isLoading = uiState.isRefreshing,
                error = error,
                onReload = viewModel::onRefresh,
                screenPadding = contentPadding,
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::onRefresh,
                    contentPadding = contentPadding
                ) {
                    ProvideNavigator(navigator = navigator) {
                        SwipeUpLazyLoadColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = contentPadding + PaddingValues(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            isLoading = isLoadingMore,
                            onLazyLoad = viewModel::onLoadMore.takeIf { hasMore },
                            bottomIndicator = defaultBottomIndicator
                        ) {
                            itemsIndexed(data) { _, item ->
                                // Same treatment as the global 搜贴 tab: Miuix card surface
                                // instead of a flat row plus a divider line.
                                ListItemCard {
                                    SearchThreadItem(
                                        item = item,
                                        onClick = threadClickListener,
                                        onValidUserClick = {
                                            val transitionKey = item.lazyListKey.toString()
                                            navigator.navigateDebounced(
                                                UserProfile(item.author, transitionKey)
                                            )
                                        },
                                        onForumClick = null, // Hide forum info
                                        onQuotePostClick = {
                                            navigator.navigateDebounced(
                                                Thread(
                                                    threadId = item.tid,
                                                    postId = item.pid,
                                                    scrollToReply = true
                                                )
                                            )
                                        },
                                        onMainPostClick = {
                                            navigator.navigateDebounced(Thread(threadId = item.tid))
                                        }
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

@Composable
private fun SortToolBar(
    modifier: Modifier = Modifier,
    sortType: () -> Int,
    filterType: () -> Int,
    onSortTypeChanged: (type: Int) -> Unit,
    onFilterTypeChanged: (type: Int) -> Unit
) {
    val sortTypes: Options<Int> = remember {
        persistentMapOf(
            ForumSearchPostSortType.NEWEST to R.string.title_search_post_sort_by_time,
            ForumSearchPostSortType.RELATIVE to R.string.title_search_post_sort_by_relevant,
        )
    }

    val filterTypes: List<Int> = remember {
        listOf(
            ForumSearchPostFilterType.ALL,
            ForumSearchPostFilterType.ONLY_THREAD
        )
    }
    val textColor = LocalContentColor.current
    val selectedTextColor = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(IntrinsicSize.Min)
    ) {
        val menuState = rememberMenuState()

        val rotate by animateFloatAsState(
            targetValue = if (menuState.expanded) 180f else 0f,
            label = "ArrowIndicatorRotate"
        )

        ClickMenu(
            menuContent = {
                ListPickerMenuItems(items = sortTypes, picked = sortType(), onItemPicked = onSortTypeChanged)
            },
            menuState = menuState,
            indication = null
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(sortTypes[sortType()]!!),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = rotate }
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        filterTypes.fastForEachIndexed { index, type ->
            val selected = type == filterType()

            Text(
                text = stringResource(
                    id = when (type) {
                        ForumSearchPostFilterType.ALL -> R.string.title_search_filter_all

                        ForumSearchPostFilterType.ONLY_THREAD -> R.string.title_search_filter_only_thread

                        else -> throw RuntimeException("Invalid type: $type")
                    }
                ),
                color = if (selected) selectedTextColor else textColor,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clickableNoIndication(
                        role = Role.RadioButton,
                        enabled = !selected,
                        onClick = { onFilterTypeChanged(type) }
                    )
            )

            if (index != filterTypes.lastIndex) {
                VerticalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}
