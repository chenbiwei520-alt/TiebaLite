package com.huanchengfly.tieba.post.ui.page.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.SearchToolbarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.miuix.LocalLiquidGlassBackdrop
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.models.search.SearchForum
import com.huanchengfly.tieba.post.ui.models.search.SearchSuggestion
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.rememberTopAppBarScrollBehaviors
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumItem
import com.huanchengfly.tieba.post.ui.page.search.forum.SearchForumPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadPage
import com.huanchengfly.tieba.post.ui.page.search.thread.SearchThreadSortType
import com.huanchengfly.tieba.post.ui.page.search.user.SearchUserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixSearchTopBar
import com.huanchengfly.tieba.post.ui.widgets.compose.SortMenuButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.Options
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import kotlin.math.abs
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Search


private enum class SearchPages(val titleRes: Int) {
    Forum(titleRes = R.string.title_search_forum),
    Thread(titleRes = R.string.title_search_thread),
    User(titleRes = R.string.title_search_user)
}

@Composable
fun SearchPage(
    navigator: NavController,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val message = when (it) {
            is SearchUiEvent -> it.toMessage(context)
            is CommonUiEvent.Toast -> it.message.toString()
            else -> it.toString()
        }
        snackbarHostState.newestSnackbarData()?.dismiss()
        snackbarHostState.showSnackbar(message)
    }

    val sortTypes = remember {
        persistentMapOf(
            SearchThreadSortType.NEWEST to R.string.title_search_order_new,
            SearchThreadSortType.OLDEST to R.string.title_search_order_old,
            SearchThreadSortType.RELATIVE to R.string.title_search_order_relevant
        )
    }

    val pages: List<SearchPages> = SearchPages.entries
    val pagerState = rememberPagerState(0) { pages.size }
    val listStates = rememberPagerListStates(pages.size)
    // Pinned semantics: the bar never scrolls away, the pager content moves underneath it.
    // Miuix expresses this through `canScroll = { false }`; there is no separate pinned factory.
    val scrollBehaviors = rememberTopAppBarScrollBehaviors(pages.size) { state ->
        MiuixScrollBehavior(state = state, canScroll = { false })
    }

    fun resetCurrentListState() {
        coroutineScope.launch {
            listStates[pagerState.currentPage].scrollToItem(0)
            scrollBehaviors.fastForEach { b -> b.state.contentOffset = 0f } // reset all scroll behavior
        }
    }

    val movablePageContents = remember {
        pages.map { page ->
            movableContentOf<PaddingValues, String, Int> { contentPadding, keyword, threadSortType ->
                // attach ScrollBehavior connection
                val modifier = Modifier.nestedScroll(scrollBehaviors[page.ordinal].nestedScrollConnection)
                val listState = listStates[page.ordinal]
                when(page) {
                    SearchPages.Forum -> SearchForumPage(modifier, keyword, contentPadding, listState)

                    SearchPages.Thread -> {
                        SearchThreadPage(modifier, keyword, threadSortType, contentPadding, listState)
                    }

                    SearchPages.User -> SearchUserPage(modifier, keyword, contentPadding, listState)
                }
            }
        }
    }

    var inputKeyword by rememberSaveable { mutableStateOf("") }

    // Callback for HistoryList, SearchBox and SuggestionList
    val onKeywordSubmit: (String) -> Unit = {
        keyboardController?.hide()
        val newKeyword = it.trim()
        viewModel.onSubmitKeyword(newKeyword)
        if (inputKeyword != newKeyword) inputKeyword = newKeyword
        focusManager.clearFocus(force = true)
        resetCurrentListState()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Submitted search keyword
    val isKeywordNotEmpty = uiState.isKeywordNotEmpty
    // Input search keyword
    val isInputKeywordNotEmpty by remember {
        derivedStateOf { inputKeyword.isNotEmpty() && inputKeyword.isNotBlank() }
    }

    val showSuggestion by remember {
        derivedStateOf { isInputKeywordNotEmpty && inputKeyword != uiState.submittedKeyword }
    }

    BackHandler(enabled = isKeywordNotEmpty && isInputKeywordNotEmpty) {
        onKeywordSubmit("")
    }
    // Feeds the liquid glass fill of the tab row's pills. This page's own capture is only recorded
    // by the result pager below, so it is empty until a search has been submitted.
    val tabBackdrop = rememberMiuixBarBackdrop()

    MyScaffold(
        topBar = {
            // The search field's glass samples the **Scaffold's** full-screen capture, read HERE
            // for two reasons:
            //
            //  * Placement: `MyScaffold` provides `LocalLiquidGlassBackdrop` around the Miuix
            //    Scaffold, so a read in the page body — above that provider — always returns null.
            //    `rememberSearchBarGlass(null)` then falls back to the flat `surfaceContainerHigh`
            //    capsule, i.e. the blur silently disappears.
            //  * Source: this field lives in the top bar, and the page's own `tabBackdrop` is only
            //    recorded by the result pager, which is not composed in the history/suggestion
            //    states. Blurring an unrecorded layer reads as transparent black, which rendered the
            //    capsule #616161 in light mode. The Scaffold's capture is recorded in every state and
            //    — because Miuix's Scaffold lays the body out from (0, 0) — also holds the content
            //    that scrolls under the bar.
            val glassBackdrop = LocalLiquidGlassBackdrop.current
            TopAppBar(
                modifier = Modifier,
                title = {
                    MiuixSearchTopBar(
                        modifier = Modifier
                            .padding(start = Dp.Hairline, top = 8.dp, end = 18.dp, bottom = 8.dp)
                            .localSharedBounds(key = SearchToolbarSharedBoundsKey, zIndexInOverlay = 2.0f),
                        keyword = inputKeyword,
                        onKeywordChange = {
                            inputKeyword = it
                            viewModel.onKeywordInputChanged(keyword = it.trim())
                        },
                        onKeywordSubmit = onKeywordSubmit,
                        onBack = {
                            coroutineScope.launch { // Clear search field for transition animation
                                keyboardController?.hide()
                                if (isKeywordNotEmpty && isInputKeywordNotEmpty) {
                                    onKeywordSubmit("")
                                    delay(150) // Wait keyboard animation
                                }
                                navigator.navigateUp()
                            }
                        },
                        // The Scaffold's full-screen capture, read in this slot (see above).
                        backdrop = glassBackdrop,
                    )
                },
                scrollBehavior = scrollBehaviors[pagerState.currentPage],
                backdrop = tabBackdrop,
            ) {
                AnimatedVisibility(
                    visible = isInputKeywordNotEmpty && isKeywordNotEmpty && !showSuggestion,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    SearchTabRow(
                        pagerState = pagerState,
                        pages = pages,
                        sortTypes = sortTypes,
                        selectedSortType = uiState.sortType,
                        onSelectSortType = viewModel::onSortTypeChanged,
                        backdrop = tabBackdrop,
                    )
                }
            }
        },
        snackbarHostState = snackbarHostState
    ) { contentPadding ->
        var isSearchHistoryExpanded by rememberSaveable { mutableStateOf(false) }

        if (!isInputKeywordNotEmpty) { // History list: empty input keyword
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
                LaunchedEffect(Unit) {
                    resetCurrentListState()
                }
            }
        } else if (showSuggestion) { // Suggestion list: input keyword != submitted keyword
            uiState.suggestion?.let {
                SearchSuggestionList(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    suggestion = it,
                    onForumClick = { f ->
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        val transitionKey = f.id.toString() // use forum ID as transition animation key
                        navigator.navigateDebounced(Destination.Forum(forumName = f.name, avatar = f.avatar, transitionKey))
                    },
                    onItemClick = onKeywordSubmit
                )
            }
        } else if (isKeywordNotEmpty) { // Search result pager
            ProvideNavigator(navigator = navigator) {
                HorizontalPager(
                    state = pagerState,
                    key = { pages[it].titleRes },
                    modifier = Modifier.fillMaxSize().miuixLayerBackdrop(tabBackdrop),
                    flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f)
                ) { i ->
                    movablePageContents[i](contentPadding, uiState.submittedKeyword, uiState.sortType)
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingNone,
    suggestion: SearchSuggestion,
    onForumClick: (SearchForum) -> Unit = {},
    onItemClick: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp), // Align with search bar
        contentPadding = contentPadding
    ) {
        suggestion.forum?.let { forum ->
            item(key = forum.id, contentType = 0) {
                SearchForumItem(forum, transitionKey = forum.id.toString(), onClick = onForumClick)
            }
        }

        items(items = suggestion.suggestions, key = { it }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        onItemClick(it)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = MiuixIcons.Search,
                    contentDescription = stringResource(id = R.string.desc_search_sug, it),
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                )

                Text(
                    text = it,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Vertical gap between the tab row and the results below it, in dp.
 *
 * The pills already sit 8.dp under the search capsule (the capsule's own bottom padding inside the
 * bar row), while Miuix's `Scaffold` hands the body a `contentPadding.top` equal to the *whole* bar
 * height — so without this the first result card started flush against the pills' bottom edge, with
 * no gap at all. 8.dp is the inset the explore page puts around this same row, which keeps the pills
 * spaced symmetrically inside the bar (8.dp above, 8.dp below).
 */
private val SearchTabRowBottomGap = 8.dp

@Composable
private fun SearchTabRow(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    pages: List<SearchPages>,
    sortTypes: Options<Int>,
    @SearchThreadSortType selectedSortType: Int,
    onSelectSortType: (Int) -> Unit,
    backdrop: LayerBackdrop? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        // The gap goes on the strip rather than on `MiuixTabRow`: the sort button is aligned
        // `CenterEnd` inside this Box, so insetting only the row would drop it out of line with the
        // pills. Because the padding is inside the bar, the Scaffold's `contentPadding` grows with
        // it and the results start exactly one gap below the pills.
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = SearchTabRowBottomGap),
        contentAlignment = Alignment.Center,
    ) {
        // HyperOS tab row: unselected pills are outlined by Miuix, the selected one is
        // filled with the Miuix accent color.
        // No weight()/fillMaxWidth() here: the row has to wrap its content for the Box to be able
        // to centre it — a filled LazyRow lays its pills out from the leading edge.
        MiuixTabRow(
            tabs = pages.map { stringResource(it.titleRes) },
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = { index ->
                coroutineScope.launch {
                    if (abs(pagerState.currentPage - index) > 1) {
                        pagerState.scrollToPage(index)
                    } else {
                        pagerState.animateScrollToPage(index)
                    }
                }
            },
            backdrop = backdrop,
        )

        // Sorting only affects thread results, but the trigger stays enabled on every tab: the
        // picked order is shared state and applies as soon as the thread tab is shown. Gating it on
        // the thread tab left a trigger that silently swallowed taps (ClickMenu forwards `enabled`
        // to combinedClickable).
        SortMenuButton(
            sortTypes = sortTypes,
            currentSortType = { selectedSortType },
            onSortTypeChanged = onSelectSortType,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
fun SearchHistoryList(
    modifier: Modifier = Modifier,
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    expanded: () -> Boolean,
    onToggleExpand: () -> Unit = {},
    onDelete: (String) -> Unit = {},
    onClear: () -> Unit = {},
) {
    val hasMore = history.size > 6

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .then(modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mirrors Miuix SmallTitle's tokens; a Row is needed to seat the clear action beside it.
            Text(
                text = stringResource(id = R.string.title_search_history),
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            if (history.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.button_clear_all),
                    modifier = Modifier.clickableNoIndication(onClick = onClear),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.button
                )
            }
        }

        FlowRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val maxItemViewSize = if (!expanded() && hasMore) 6 else history.size

            for (i in 0 until maxItemViewSize) {
                val searchHistory = history[i]
                // Miuix has no chip that also handles a long press, so the pill is composed from the
                // squircle shape and Miuix tokens, matching Miuix Button's default secondary surface.
                Box(
                    modifier = Modifier
                        .squircleBackground(
                            color = MiuixTheme.colorScheme.secondaryVariant,
                            cornerRadius = ButtonDefaults.CornerRadius,
                        )
                        .squircleClip(ButtonDefaults.CornerRadius)
                        .combinedClickable(
                            onClick = { onHistoryClick(searchHistory) },
                            onLongClick = { onDelete(searchHistory) }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = searchHistory,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSecondaryVariant,
                    )
                }
            }
        }

        if (hasMore) {
            Button(
                onClick = onToggleExpand,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = if (expanded()) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (expanded()) {
                        stringResource(id = R.string.button_expand_less_history)
                    } else {
                        stringResource(id = R.string.button_expand_more_history)
                    }
                )
            }
        } else if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.title_empty),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
    }
}

@Preview("SearchTopBar")
@Composable
private fun PreviewSearchTopBar() {
    var keyword by remember { mutableStateOf("") }
    TiebaLiteTheme {
        Surface(
            modifier = Modifier
                .height(TopAppBarDefaults.TopAppBarExpandedHeight)
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            MiuixSearchTopBar(
                keyword = keyword,
                onKeywordChange = { keyword = it }
            )
        }
    }
}

@Preview("SearchHistoryList")
@Composable
private fun PreviewSearchHistoryList() {
    TiebaLiteTheme {
        var expanded by remember { mutableStateOf(false) }
        Surface {
            SearchHistoryList(
                history = (0..20).map { if (it % 2 == 0) "记录$it" else "搜索记录$it" },
                onHistoryClick = {},
                expanded = { expanded },
                onToggleExpand = { expanded = !expanded },
            )
        }
    }
}

@Preview("SearchSuggestionList", backgroundColor = 0xFFFFFFFF)
@Composable
private fun SearchSuggestionListPreview() = TiebaLiteTheme {
    Surface {
        SearchSuggestionList(
            suggestion = SearchSuggestion(
                forum = SearchForum(name = "吃瓜吧", slogan = "Test"),
                suggestions = listOf("1", "2", "3")
            )
        )
    }
}