package com.huanchengfly.tieba.post.ui.page.forum.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.miuix.LocalLiquidGlassBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixPinnedScrollBehavior
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SharedTransitionUserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ForumRuleDetailPage(
    navigator: NavController,
    viewModel: ForumRuleDetailViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::isLoading,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ForumRuleDetailUiState::error,
        initial = null
    )
    // Pinned: the bar stays put and the list scrolls underneath, which is what feeds the blur.
    val scrollBehavior = rememberMiuixPinnedScrollBehavior()

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            isLoading = isLoading,
            error = error,
            onReload = viewModel::reload,
        ) {
            MyScaffold(
                topBar = {
                    // `CenterAlignedTopAppBar` (→ `TopAppBar`) owns the blur: it makes the bar
                    // itself transparent when a backdrop is supplied and sizes the blur ramp to the
                    // bar's real height. Doing it by hand with an opaque toolbar hides the blur
                    // completely, because the bar paints over it.
                    CenterAlignedTopAppBar(
                        titleRes = R.string.title_forum_rule,
                        navigationIcon = {
                            BackNavigationIcon(onBackPressed = navigator::navigateUp)
                        },
                        scrollBehavior = scrollBehavior,
                        backdrop = LocalLiquidGlassBackdrop.current,
                    )
                }
            ) { contentPadding ->
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val forumRule = state.data ?: return@MyScaffold

                val ruleTitleStyle = MaterialTheme.typography.titleMedium
                val ruleContentStyle = MaterialTheme.typography.bodyLarge

                LazyColumn(
                    modifier = Modifier
                        // No `miuixLayerBackdrop` here: `MyScaffold` already wraps this page's
                        // whole content in `layerBackdrop(glassBackdrop)`, so adding a second
                        // capture of the SAME backdrop on a descendant nests two `GraphicsLayer
                        // .record()` calls on one graphics layer. `record()` is not reentrant —
                        // the inner one throws "Recording currently in progress" from
                        // `RenderNode.beginRecording`, crashing on first draw.
                        //
                        // `fillMaxSize` matters for the blur: the list has to span the whole screen
                        // so its items scroll *under* the transparent bar. The margins go in
                        // `contentPadding` instead of a `padding` modifier for the same reason —
                        // a padding modifier would shrink the viewport and leave the strip behind
                        // the bar permanently empty.
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = contentPadding + PaddingValues(
                        start = 16.dp,
                        top = 12.dp,
                        end = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(text = forumRule.headLine, style = MaterialTheme.typography.titleLarge)
                    }

                    forumRule.author?.let {
                        item(key = it.id) {
                            SharedTransitionUserHeader(
                                user = it,
                                desc = forumRule.publishTime,
                                onClick = { navigator.navigateDebounced(Destination.UserProfile(user = it)) },
                            )
                        }
                    }

                    item {
                        Text(text = forumRule.preface, style = ruleContentStyle)
                    }

                    items(
                        items = forumRule.data,
                        key = { it -> it.hashCode() },
                        contentType = { R.string.title_forum_rule }
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = it.title, style = ruleTitleStyle)

                            it.content?.let { content ->
                                Text(text = content, style = ruleContentStyle)
                            }
                        }
                    }
                }
            }
        }
    }
}