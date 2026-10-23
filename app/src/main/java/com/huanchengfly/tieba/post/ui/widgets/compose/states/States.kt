package com.huanchengfly.tieba.post.ui.widgets.compose.states

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Indeterminate MIUI progress ring, replacing the former Lottie paper-plane animation.
 *
 * Defaults verified against miuix-ui: a determinate ring is 30.dp with a 4.dp stroke and the
 * indeterminate one uses a 2.dp stroke. Leaving `progress` at null selects the indeterminate
 * animation, which is what a blocking loading state wants. Slightly larger than the 30.dp default
 * because this one owns the whole screen.
 *
 * The animation is a plain Compose animation, so the system "remove animations" accessibility
 * setting scales it down like any other.
 */
val DefaultLoadingScreen: @Composable StateScreenScope.() -> Unit = {
    MiuixLoadingIndicator()
}

/**
 * The app's single loading indicator: a Miuix indeterminate progress ring.
 *
 * Every loading surface — the [StateScreen] full-screen state, the home forum grid and the thread
 * list placeholders — renders through this, so the ring's size and stroke cannot drift apart
 * between screens.
 *
 * 40.dp / 2.dp: miuix-ui defaults are a 30.dp ring with a 4.dp determinate stroke and a 2.dp
 * indeterminate stroke; the size is nudged up because every current use owns a whole screen or
 * list area rather than sitting inline. Leaving `progress` at null selects the indeterminate
 * animation, which is a plain Compose animation and therefore honours the system
 * "remove animations" setting.
 */
@Composable
fun MiuixLoadingIndicator(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = 2.dp,
    )
}

/**
 * Empty state.
 *
 * The refresh action is no longer a button: the screen itself hosts a pull-to-refresh gesture, so it
 * works uniformly whether the list is empty or not. That matters because [StateScreen] swaps its
 * content out while the empty screen is showing, so without a gesture here an empty list would have
 * no way to refresh at all.
 *
 * [scrollable] defaults to true on purpose. [TipScreen] only attaches `verticalScroll` when it is
 * set, and a child that cannot scroll dispatches no nested-scroll deltas — the gesture would never
 * reach the pull-to-refresh connection, leaving the indicator stuck at idle. With it enabled the
 * content still does not visibly scroll unless it overflows.
 */
@Composable
fun StateScreenScope.DefaultEmptyScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    @StringRes titleRes: Int = R.string.title_empty,
    @StringRes messageRes: Int? = null,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = ::reload,
        modifier = modifier,
    ) {
        TipScreen(
            title = { Text(text = stringResource(id = titleRes)) },
            scrollable = scrollable,
            image = {
                // MIUI-style outline glyph. The former Lottie box illustration read as a Material
                // loading empty state; MIUI pairs a quiet glyph with the text instead.
                Icon(
                    imageVector = MiuixIcons.ListView,
                    contentDescription = null, // Decorative: the title below already says it
                    modifier = Modifier.size(48.dp),
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            },
            message = messageRes?.let { { Text(text = stringResource(id = it)) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

val DefaultErrorScreen: @Composable StateScreenScope.() -> Unit = {
    Text(
        text = stringResource(id = R.string.error_tip),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun StateScreen(
    modifier: Modifier = Modifier,
    isEmpty: Boolean = false,
    isError: Boolean,
    isLoading: Boolean,
    onReload: (() -> Unit)? = null,
    emptyScreen: @Composable StateScreenScope.() -> Unit = { DefaultEmptyScreen() },
    errorScreen: @Composable StateScreenScope.() -> Unit = DefaultErrorScreen,
    loadingScreen: @Composable StateScreenScope.() -> Unit = DefaultLoadingScreen,
    screenPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues(),
    content: @Composable StateScreenScope.() -> Unit,
) {
    val stateScreenScope = remember(key1 = onReload, key2 = isLoading) {
        StateScreenScope(onReload, isLoading)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onCase(isError || isLoading || isEmpty) {
                padding(screenPadding).consumeWindowInsets(screenPadding)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isError) {
            stateScreenScope.errorScreen()
        } else if (isLoading) {
            stateScreenScope.loadingScreen()
        } else if (isEmpty) {
            stateScreenScope.emptyScreen()
        } else {
            stateScreenScope.content()
        }
    }
}

@NonRestartableComposable
@Composable
fun StateScreen(
    modifier: Modifier = Modifier,
    isEmpty: Boolean = false,
    isLoading: Boolean,
    error: Throwable?,
    onReload: (() -> Unit)? = null,
    emptyScreen: @Composable StateScreenScope.() -> Unit = { DefaultEmptyScreen() },
    errorScreen: @Composable StateScreenScope.() -> Unit = { ErrorScreen(error) },
    loadingScreen: @Composable StateScreenScope.() -> Unit = DefaultLoadingScreen,
    screenPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues(),
    content: @Composable StateScreenScope.() -> Unit,
) =
    StateScreen(
        modifier = modifier,
        isEmpty = isEmpty,
        isError = error != null,
        isLoading = isLoading,
        onReload = onReload,
        emptyScreen = emptyScreen,
        errorScreen = errorScreen,
        loadingScreen = loadingScreen,
        screenPadding = screenPadding,
        content = content
    )

class StateScreenScope(
    private val onReload: (() -> Unit)? = null,
    /** True while a refresh is in flight, so the empty screen can host its own pull gesture. */
    val isRefreshing: Boolean = false,
) {
    val canReload: Boolean
        get() = onReload != null

    fun reload() {
        onReload?.invoke()
    }
}