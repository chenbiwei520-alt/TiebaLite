/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.util.lerp
import com.huanchengfly.tieba.post.theme.FloatProducer
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.createTopAppBarColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.huanchengfly.tieba.post.ui.miuix.MiuixProgressiveBlurBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop

// Miuix scroll behavior. The Material3 `TopAppBarScrollBehavior` used to drive the collapsing
// bars below; it is replaced by Miuix's own `ScrollBehavior` so that the whole bar (height,
// offsets, blur fade) is animated by a single scroll pipeline. Miuix's own
// `ExitUntilCollapsedScrollBehavior` is not visible outside `miuix-ui` (Kotlin `private`), so the
// public `MiuixScrollBehavior` factory is the only usable entry point.
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior as MiuixScrollBehaviorState
import top.yukonga.miuix.kmp.basic.TopAppBarState as MiuixTopAppBarState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState as rememberMiuixTopAppBarState

/**
 * compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/AppBar.kt
 *
 * commit 576eeec 'Bump the version number for Material3 to 1.4.0-rc01'.
 * on branch androidx-compose-material3-release
 *
 * 0Ranko0p changes:
 *   1. Compose extra content below title (required for Haze blurring)
 *   2. Default to use TiebaLiteTheme
 *   3. Drop appBarDragModifier
 *   4. Drop subtitleContent
 *   5. Implement CollapsingAvatarTopAppBar
 *   6. Implement TopAppBarPaged
 */

/**
 * Represents the container color used for the top app bar.
 *
 * A [colorTransitionFraction] provides a percentage value that can be used to generate a color.
 * Usually, an app bar implementation will pass in a [colorTransitionFraction] read from the
 * [MiuixTopAppBarState.collapsedFraction] or the [MiuixTopAppBarState.overlappedFraction].
 *
 * @param colorTransitionFraction a `0.0` to `1.0` value that represents a color transition
 *   percentage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
private fun TopAppBarColors.containerColor(colorTransitionFraction: Float): Color {
    return lerp(
        containerColor,
        scrolledContainerColor,
        FastOutLinearInEasing.transform(colorTransitionFraction)
    )
}

/**
 * Top app bar with non-collapsible [content].
 *
 * Used in [BlurScaffold] to apply background blurring with [content]. For normal
 * Scaffold with [TabRow], compose inside Scaffold with [Column] directly.
 *
 * @see androidx.compose.material3.TopAppBar
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [ScrollBehavior] (Miuix) which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [ScrollBehavior.nestedScrollConnection].
 * @param content extra content displayed below the top app bar, mostly are [TabRow].
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TiebaLiteTheme.topAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState? = null,
    backdrop: LayerBackdrop? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // With a backdrop the bar itself goes transparent so the progressive blur behind it shows
    val barColors = if (backdrop != null) {
        MaterialTheme.colorScheme.createTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    } else {
        colors
    }
    MiuixProgressiveBlurBar(backdrop = backdrop) {
        SingleRowTopAppBar(
            modifier = modifier,
            title = title,
            titleTextStyle = MaterialTheme.typography.titleLarge,
            content = content,
            titleHorizontalAlignment = titleHorizontalAlignment,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
            windowInsets = TopAppBarDefaults.windowInsets,
            colors = barColors,
            scrollBehavior = scrollBehavior
        )
    }
}

/**
 * Top app bar with non-collapsible [content].
 *
 * Used in [BlurScaffold] to apply background blurring with [content]. For normal
 * Scaffold with [TabRow], compose inside Scaffold with [Column] directly.
 *
 * @see androidx.compose.material3.TopAppBar
 *
 * @param titleRes the title text to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [ScrollBehavior] (Miuix) which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [ScrollBehavior.nestedScrollConnection].
 * @param content extra content displayed below the top app bar, mostly are [TabRow].
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TiebaLiteTheme.topAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState? = null,
    backdrop: LayerBackdrop? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) =
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = stringResource(titleRes))
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
        backdrop = backdrop,
        content = content
    )

/**
 * Top app bar for paged scrollable contents.
 *
 * Rendered with HyperOS (Miuix) tokens: Miuix surface background, 64 dp bar row below the status
 * bar inset, and theme-driven content colors. The Material-style parameters (`colors`,
 * `scrollBehavior`, `canScrollBackward`) are accepted for call-site compatibility but the visual
 * result is Miuix-styled.
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param titleHorizontalAlignment horizontal alignment of the title
 * @param navigationIcon the navigation icon displayed at the start of the top app bar
 * @param actions the actions displayed at the end of the top app bar
 * @param colors kept for call-site compatibility; visuals come from MiuixTheme
 * @param scrollBehavior kept for call-site compatibility
 * @param canScrollBackward kept for call-site compatibility
 * @param content extra content displayed below the top app bar, e.g. a [TabRow].
 * */
@Composable
fun TopAppBarPaged(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TiebaLiteTheme.topAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState? = null,
    canScrollBackward: () -> Boolean = { false },
    backdrop: LayerBackdrop? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    // The Miuix-styled bar is static and ignores its scroll behavior, but pages still attach
    // `scrollBehavior.nestedScrollConnection` to their scroll containers. An M3 state defaults to
    // an unbounded heightOffsetLimit (-Float.MAX_VALUE), which would consume every scroll delta
    // before the content ever sees it. Pin the state like Miuix's pinned SmallTopAppBar does so
    // the connection consumes nothing and scrolling reaches the content.
    SideEffect {
        scrollBehavior?.state?.let { state ->
            if (state.heightOffsetLimit != 0f) state.heightOffsetLimit = 0f
            if (state.heightOffset != 0f) state.heightOffset = 0f
        }
    }

    // Only paint the bar's own fill when there is nothing to blur; otherwise it would cover the
    // progressive blur layer sitting behind the bar.
    val barFill = if (backdrop == null) {
        top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface
    } else {
        Color.Transparent
    }

    MiuixProgressiveBlurBar(backdrop = backdrop) {
        Column(
            modifier =
                modifier
                    .background(color = barFill)
                    .semantics { isTraversalGroup = true }
                    .pointerInput(Unit) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier =
                    Modifier
                        .windowInsetsPadding(
                            WindowInsets.statusBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        )
                        .heightIn(min = MiuixTopBarRowHeight)
                        .padding(horizontal = MiuixTopBarHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationIcon()
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = when (titleHorizontalAlignment) {
                        Alignment.CenterHorizontally -> Alignment.Center
                        Alignment.End -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    },
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
                        LocalTextStyle provides MaterialTheme.typography.titleLarge,
                        content = title,
                    )
                }
                actions()
            }

            if (content != null) {
                content()
            }
        }
    }
}

internal val MiuixTopBarRowHeight = 64.dp

/**
 * Resting height of a [TopAppBarPaged]: the status bar inset plus the bar row, held at the tallest
 * inset seen so far.
 *
 * Two things used to move a page body vertically for a handful of frames and snap it back:
 *
 * 1. The Scaffold reports the bar's *measured* height as the content padding, and that measurement
 *    can come back one status bar short — the list then slides up under the bar and returns.
 * 2. `WindowInsets.statusBars` legitimately drops to zero while a full screen layer hides the
 *    system bars (the image viewer does, see PhotoViewerHost) and comes back when it closes.
 *    A page whose padding follows it shifts by exactly one status bar height on every such blip.
 *
 * The resting height is deterministic, so pages clamp their top inset to this value: identical
 * when nothing wobbles, motionless when something does. The inset only ever grows, which is what
 * "resting" means — the bars are never hidden for longer than a full screen layer is on top.
 */
@Composable
fun staticTopBarHeight(): Dp {
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val tallestStatusBarPx = remember { mutableIntStateOf(statusBarPx) }
    SideEffect {
        if (statusBarPx > tallestStatusBarPx.intValue) tallestStatusBarPx.intValue = statusBarPx
    }
    return with(density) { maxOf(tallestStatusBarPx.intValue, statusBarPx).toDp() } +
        MiuixTopBarRowHeight
}

private val MiuixTopBarHorizontalPadding = 4.dp

/**
 * A center aligned top app bar with non-collapsible [content].
 *
 * Used in [BlurScaffold] to apply background blurring with [content]. For normal
 * Scaffold with [TabRow], compose inside Scaffold with [Column] directly.
 *
 * @see androidx.compose.material3.TopAppBar
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [ScrollBehavior] (Miuix) which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [ScrollBehavior.nestedScrollConnection].
 * @param content extra content displayed below the top app bar, mostly are [TabRow].
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TiebaLiteTheme.topAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState? = null,
    backdrop: LayerBackdrop? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) =
    TopAppBar(
        modifier = modifier,
        title = title,
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content,
        colors = colors,
        scrollBehavior = scrollBehavior,
        backdrop = backdrop,
    )


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopAppBar(
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TiebaLiteTheme.topAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState? = null,
    backdrop: LayerBackdrop? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) =
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = stringResource(titleRes))
        },
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior,
        backdrop = backdrop,
        content = content
    )

/**
 * A single-row top app bar that is designed to be called by the small and center aligned top app
 * bar composables.
 *
 * This SingleRowTopAppBar has slots for a title, subtitle, navigation icon, and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleRowTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    titleHorizontalAlignment: Alignment.Horizontal,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    expandedHeight: Dp,
    windowInsets: WindowInsets,
    colors: TopAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState?,
) {
    require(expandedHeight.isSpecified && expandedHeight.isFinite) {
        "The expandedHeight is expected to be specified and finite"
    }

    // Obtain the container color from the TopAppBarColors using the `overlapFraction`. This
    // ensures that the colors will adjust whether the app bar behavior is pinned or scrolled.
    // This may potentially animate or interpolate a transition between the container-color and
    // the container's scrolled-color according to the app bar's scroll state.
    val targetColor by
        remember(colors, scrollBehavior) {
            derivedStateOf {
                val overlappingFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
                colors.containerColor(if (overlappingFraction > 0.01f) 1f else 0f)
            }
        }

    val appBarContainerColor =
        animateColorAsState(
            targetColor,
            animationSpec = TopAppBarColorSpect
        )

    // Wrap the given actions in a Row.
    val actionsRow =
        @Composable {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }

    // Compose a Surface with a TopAppBarLayout content.
    // The surface's background color is animated as specified above.
    // The height of the app bar is determined by subtracting the bar's height offset from the
    // app bar's defined constant height value (i.e. the ContainerHeight token).
    Column(
        modifier =
            modifier
                .drawWithCache {
                    onDrawBehind {
                        val color = appBarContainerColor.value
                        if (color != Color.Unspecified) {
                            drawRect(color = color)
                        }
                    }
                }
                .semantics { isTraversalGroup = true }
                .pointerInput(Unit) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBarLayout(
            modifier =
                Modifier
                    .windowInsetsPadding(windowInsets)
                    // clip after padding so we don't show the title over the inset area
                    .clipToBounds()
                    .heightIn(max = expandedHeight)
                    // This row is the whole bar (it is not pinned above anything), so the offset may
                    // run the full expanded height. Must stay a constant — see
                    // `adjustHeightOffsetLimit` for why the measured height can't be used here.
                    .adjustHeightOffsetLimit(
                        scrollBehavior = scrollBehavior,
                        expandedHeight = with(LocalDensity.current) { expandedHeight.toPx() },
                    ),
            scrolledOffset = { scrollBehavior?.state?.heightOffset ?: 0f },
            navigationIconContentColor = colors.navigationIconContentColor,
            titleContentColor = colors.titleContentColor,
            actionIconContentColor = colors.actionIconContentColor,
            title = title,
            titleTextStyle = titleTextStyle,
            titleAlpha = { 1f },
            titleVerticalArrangement = Arrangement.Center,
            titleHorizontalAlignment = titleHorizontalAlignment,
            titleBottomPadding = 0,
            hideTitleSemantics = false,
            navigationIcon = navigationIcon,
            actions = actionsRow,
            height = expandedHeight,
        )

        if (content != null) {
            Column (
                modifier = Modifier.windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal)),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

/**
 * [Material Design large top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Large top app bar with collapsible [avatar] and [title].
 *
 * Note that the **collapsedHeight** is hardcoded to [TopAppBarDefaults.TopAppBarExpandedHeight],
 * the height of a standard TopAppBar.
 *
 * @see androidx.compose.material3.LargeTopAppBar
 *
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param avatar the avatar to be displayed alongside [title]
 * @param title the title to be displayed in the top app bar
 * @param titleHorizontalAlignment horizontal alignment of the title
 * @param subtitle the subtitle to be displayed below the title
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app bar
 *   to expand, this value will represent the maximum height that the bar will be allowed to
 *   expand. This value must be specified and finite, otherwise it will be ignored and replaced
 *   with [TopAppBarDefaults.LargeAppBarExpandedHeight].
 * @param avatarMax maximum size of [avatar] when top bar is fully expanded
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [ScrollBehavior] (Miuix) which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [ScrollBehavior.nestedScrollConnection].
 * @param collapsibleExtraContent is [content] collapsible.
 * @param content extra content to be displayed below the top app bar, mostly are [TabRow].
 * */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingAvatarTopAppBar(
    modifier: Modifier = Modifier,
    avatar: @Composable (BoxScope.() -> Unit)?,
    title: @Composable () -> Unit,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.LargeAppBarExpandedHeight,
    avatarMax: Dp = ExpandedAvatarSize,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TiebaLiteTheme.topAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState? = defaultMiuixScrollBehavior(),
    collapsibleExtraContent: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val collapsedHeight = TopAppBarDefaults.TopAppBarExpandedHeight

    require(expandedHeight.isSpecified && expandedHeight.isFinite) {
        "The expandedHeight is expected to be specified and finite"
    }

    require(expandedHeight > collapsedHeight) {
        "The expandedHeight ($expandedHeight) is expected to be greater than the collapsedHeight"
    }

    // Obtain the container color from the TopAppBarColors using the `overlapFraction`. This
    // ensures that the colors will adjust whether the app bar behavior is pinned or scrolled.
    // This may potentially animate or interpolate a transition between the container-color and
    // the container's scrolled-color according to the app bar's scroll state.
    val targetColor by
        remember(colors, scrollBehavior) {
            derivedStateOf {
                val overlappingFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
                colors.containerColor(if (overlappingFraction > 0.01f) 1f else 0f)
            }
        }

    val appBarContainerColor =
        animateColorAsState(
            targetColor,
            animationSpec = TopAppBarColorSpect
        )

    // Wrap the given actions in a Row.
    val actionsRow =
        @Composable {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }

    // Compose a Surface with a TopAppBarLayout content.
    // The surface's background color is animated as specified above.
    // The height of the app bar is determined by subtracting the bar's height offset from the
    // app bar's defined constant height value (i.e. the ContainerHeight token).
    Column(
        modifier =
            modifier
                .drawWithCache {
                    onDrawBehind {
                        val color = appBarContainerColor.value
                        if (color != Color.Unspecified) {
                            drawRect(color = color)
                        }
                    }
                }
                .semantics { isTraversalGroup = true }
                .pointerInput(Unit) {},
    ) {
        CollapsingAvatarTopAppBarLayout(
            modifier =
                Modifier
                    .windowInsetsPadding(windowInsets)
                    // clip after padding so we don't show the title over the inset area
                    .clipToBounds()
                    // Stable limit (see `adjustHeightOffsetLimit`): measure the *declared* expanded
                    // height, not the node's own fluctuating height. The collapsing layout sizes
                    // itself from `heightOffset`, so deriving the limit from the measured height
                    // used to oscillate the bar at ~45 Hz while scrolling.
                    //
                    // `TopAppBarExpandedHeight` (the pinned collapsed strip) is subtracted because
                    // this bar keeps that strip visible when fully collapsed.
                    .adjustHeightOffsetLimit(
                        scrollBehavior = scrollBehavior,
                        expandedHeight = with(LocalDensity.current) { expandedHeight.toPx() },
                        collapsedHeight = with(LocalDensity.current) { collapsedHeight.toPx() },
                    ),
            scrolledOffset = { scrollBehavior?.state?.heightOffset ?: 0f },
            collapseFraction = { scrollBehavior?.state?.collapsedFraction ?: 0f },
            navigationIconContentColor = colors.navigationIconContentColor,
            titleContentColor = colors.titleContentColor,
            subtitleContentColor = colors.subtitleContentColor,
            actionIconContentColor = colors.actionIconContentColor,
            avatar = avatar,
            title = title,
            titleVerticalArrangement = Arrangement.Center,
            titleHorizontalAlignment = titleHorizontalAlignment,
            titleBottomPadding = 0,
            subtitle = subtitle,
            navigationIcon = navigationIcon,
            actions = actionsRow,
            extraContent = content.takeIf { collapsibleExtraContent },
            avatarMax = avatarMax,
            height = expandedHeight,
        )

        if (!collapsibleExtraContent && content != null) {
            content()
        }
    }
}


/**
 * [Material Design large top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Large top app bar with collapsible [subtitle] and extra [content].
 *
 * Note that the **collapsedHeight** is hardcoded to [TopAppBarDefaults.TopAppBarExpandedHeight],
 * the height of a standard TopAppBar.
 *
 * @see androidx.compose.material3.LargeTopAppBar
 *
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param title the title to be displayed in the top app bar
 * @param titleHorizontalAlignment horizontal alignment of the title
 * @param subtitle the subtitle to be displayed below the title
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app bar
 *   to expand, this value will represent the maximum height that the bar will be allowed to
 *   expand. This value must be specified and finite, otherwise it will be ignored and replaced
 *   with [TopAppBarDefaults.LargeAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [ScrollBehavior] (Miuix) which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [ScrollBehavior.nestedScrollConnection].
 * @param collapsibleExtraContent is [content] collapsible.
 * @param content extra content to be displayed below the top app bar, mostly are [TabRow].
 * */
@OptIn(ExperimentalMaterial3Api::class)
@NonRestartableComposable
@Composable
fun CollapsingTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.LargeAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TiebaLiteTheme.topAppBarColors,
    scrollBehavior: MiuixScrollBehaviorState? = defaultMiuixScrollBehavior(),
    collapsibleExtraContent: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    CollapsingAvatarTopAppBar(
        // Forward the caller's modifier — this used to be hardcoded to `Modifier`, which silently
        // dropped every modifier passed by callers (padding, fillMaxWidth, alpha, ...).
        modifier = modifier,
        avatar = null,
        avatarMax = Dp.Hairline,
        title = title,
        titleHorizontalAlignment = titleHorizontalAlignment,
        subtitle = subtitle,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = expandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
        collapsibleExtraContent = true,
        content = content
    )
}

/**
 * The base [Layout] for all top app bars. This function lays out a top app bar navigation icon
 * (leading icon), a title (header), and action icons (trailing icons). Note that the navigation and
 * the actions are optional.
 *
 * @param modifier a [Modifier]
 * @param scrolledOffset a [FloatProducer] that provides the app bar offset in pixels (note that
 *   when the app bar is scrolled, the lambda will output negative values)
 * @param navigationIconContentColor the content color that will be applied via a
 *   [LocalContentColor] when composing the navigation icon
 * @param titleContentColor the color that will be applied via a [LocalContentColor] when composing
 *   the title
 * @param actionIconContentColor the content color that will be applied via a [LocalContentColor]
 *   when composing the action icons
 * @param title the top app bar title (header)
 * @param titleTextStyle the title's text style
 * @param titleAlpha the title's alpha
 * @param titleVerticalArrangement the title's vertical arrangement
 * @param titleHorizontalAlignment the title's horizontal alignment
 * @param titleBottomPadding the title's bottom padding
 * @param hideTitleSemantics hides the title node from the semantic tree. Apply this boolean when
 *   this layout is part of a two-rows top app bar to hide the title's semantics from accessibility
 *   services. This is needed to avoid having multiple titles visible to accessibility services at
 *   the same time, when animating between collapsed / expanded states.
 * @param navigationIcon a navigation icon [Composable]
 * @param actions actions [Composable]
 * @param height this app bar's requested height
 */
@Composable
private fun TopAppBarLayout(
    modifier: Modifier,
    scrolledOffset: FloatProducer,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    actionIconContentColor: Color,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    titleAlpha: () -> Float,
    titleVerticalArrangement: Arrangement.Vertical,
    titleHorizontalAlignment: Alignment.Horizontal,
    titleBottomPadding: Int,
    hideTitleSemantics: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    height: Dp,
) {
    Layout(
        {
            Box(Modifier.layoutId("navigationIcon").padding(start = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon,
                )
            }

            // 0Ranko0p changes: Remove subtitle support
            // } else { // TODO(b/352770398): Workaround to maintain compatibility
                Box(
                    modifier =
                        Modifier.layoutId("title")
                            .padding(horizontal = TopAppBarHorizontalPadding)
                            .then(
                                if (hideTitleSemantics) Modifier.clearAndSetSemantics {}
                                else Modifier
                            )
                            .graphicsLayer { alpha = titleAlpha() },
                    contentAlignment = if (titleHorizontalAlignment == Alignment.CenterHorizontally) {
                        Alignment.TopCenter
                    } else {
                        Alignment.TopStart
                    }
                ) {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = titleTextStyle,
                        content = title,
                    )
                }
            // } end of 0Ranko0P changes

            Box(Modifier.layoutId("actionIcons").padding(end = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions,
                )
            }
        },
        modifier = modifier,
        measurePolicy =
            remember(
                scrolledOffset,
                titleVerticalArrangement,
                titleHorizontalAlignment,
                titleBottomPadding,
                height,
            ) {
                TopAppBarMeasurePolicy(
                    scrolledOffset,
                    titleVerticalArrangement,
                    titleHorizontalAlignment,
                    titleBottomPadding,
                    height,
                )
            },
    )
}

private class TopAppBarMeasurePolicy(
    val scrolledOffset: FloatProducer,
    val titleVerticalArrangement: Arrangement.Vertical,
    val titleHorizontalAlignment: Alignment.Horizontal,
    val titleBottomPadding: Int,
    val height: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val navigationIconPlaceable =
            measurables
                .fastFirst { it.layoutId == "navigationIcon" }
                .measure(constraints.copy(minWidth = 0))
        val actionIconsPlaceable =
            measurables
                .fastFirst { it.layoutId == "actionIcons" }
                .measure(constraints.copy(minWidth = 0))

        val maxTitleWidth =
            if (constraints.maxWidth == Constraints.Infinity) {
                constraints.maxWidth
            } else {
                (constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width)
                    .coerceAtLeast(0)
            }
        val titlePlaceable =
            measurables
                .fastFirst { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        // Locate the title's baseline.
        val titleBaseline =
            if (titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                titlePlaceable[LastBaseline]
            } else {
                0
            }

        // Subtract the scrolledOffset from the maxHeight. The scrolledOffset is expected to be
        // equal or smaller than zero.
        val scrolledOffsetValue = scrolledOffset()
        val heightOffset = if (scrolledOffsetValue.isNaN()) 0 else scrolledOffsetValue.roundToInt()

        val maxLayoutHeight = max(height.roundToPx(), titlePlaceable.height)
        val layoutHeight =
            if (constraints.maxHeight == Constraints.Infinity) {
                maxLayoutHeight
            } else {
                (maxLayoutHeight + heightOffset).coerceAtLeast(0)
            }

        return placeTopAppBar(
            constraints,
            layoutHeight,
            maxLayoutHeight,
            navigationIconPlaceable,
            titlePlaceable,
            actionIconsPlaceable,
            titleBaseline,
        )
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.minIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.minIntrinsicHeight(width) } ?: 0,
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0,
        )
    }

    private fun MeasureScope.placeTopAppBar(
        constraints: Constraints,
        layoutHeight: Int,
        maxLayoutHeight: Int,
        navigationIconPlaceable: Placeable,
        titlePlaceable: Placeable,
        actionIconsPlaceable: Placeable,
        titleBaseline: Int,
    ): MeasureResult =
        layout(constraints.maxWidth, layoutHeight) {
            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (layoutHeight - navigationIconPlaceable.height) / 2,
            )

            titlePlaceable.let {
                val start = max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width)
                val end = actionIconsPlaceable.width

                // Align using the maxWidth. We will adjust the position later according to the
                // start and end. This is done to ensure that a center alignment is still maintained
                // when the start and end have different widths. Note that the title is centered
                // relative to the entire app bar width, and not just centered between the
                // navigation icon and the actions.
                var titleX =
                    titleHorizontalAlignment.align(
                        size = titlePlaceable.width,
                        space = constraints.maxWidth,
                        // Using Ltr as we call placeRelative later on.
                        layoutDirection = LayoutDirection.Ltr,
                    )
                // Reposition the title based on the start and the end (i.e. the navigation and
                // action widths).
                if (titleX < start) {
                    titleX += (start - titleX)
                } else if (titleX + titlePlaceable.width > constraints.maxWidth - end) {
                    titleX += ((constraints.maxWidth - end) - (titleX + titlePlaceable.width))
                }

                // The titleVerticalArrangement is always one of Center or Bottom.
                val titleY =
                    when (titleVerticalArrangement) {
                        Arrangement.Center -> (layoutHeight - titlePlaceable.height) / 2
                        // Apply bottom padding from the title's baseline only when the Arrangement
                        // is "Bottom".
                        Arrangement.Bottom ->
                            if (titleBottomPadding == 0) {
                                layoutHeight - titlePlaceable.height
                            } else {
                                // Calculate the actual padding from the bottom of the title, taking
                                // into account its baseline.
                                val paddingFromBottom =
                                    titleBottomPadding - (titlePlaceable.height - titleBaseline)
                                // Adjust the bottom padding to a smaller number if there is no room
                                // to fit the title.
                                val heightWithPadding = paddingFromBottom + titlePlaceable.height
                                val adjustedBottomPadding =
                                    if (heightWithPadding > maxLayoutHeight) {
                                        paddingFromBottom - (heightWithPadding - maxLayoutHeight)
                                    } else {
                                        paddingFromBottom
                                    }

                                layoutHeight - titlePlaceable.height - max(0, adjustedBottomPadding)
                            }
                        // Arrangement.Top
                        else -> 0
                    }

                it.placeRelative(titleX, titleY)
            }

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = (layoutHeight - actionIconsPlaceable.height) / 2,
            )
        }
}

/**
 * The base [Layout] for collapsible top app bar. This function lays out a top app bar
 * navigation icon (leading icon), an avatar(header), a title (header), a subtitle (header) action
 * icons (trailing icons) and a extra content (bottom). Note that the navigation, subtitle, actions
 * and the extra content are optional.
 *
 * @param modifier a [Modifier]
 * @param scrolledOffset a [FloatProducer] that provides the app bar offset in pixels (note that
 *   when the app bar is scrolled, the lambda will output negative values)
 * @param collapseFraction a [FloatProducer] that provides the collapsed percentage. A `0.0`
 *   represents a fully expanded bar, and `1.0` represents a fully collapsed bar.
 * @param navigationIconContentColor the content color that will be applied via a
 *   [LocalContentColor] when composing the navigation icon
 * @param titleContentColor the color that will be applied via a [LocalContentColor] when composing
 *   the title
 * @param subtitleContentColor the color that will be applied via a [LocalContentColor] when
 *   composing the subtitle
 * @param actionIconContentColor the content color that will be applied via a [LocalContentColor]
 *   when composing the action icons
 * @param avatar the avatar [Composable]
 * @param title the top app bar title (header)
 * @param titleVerticalArrangement the title's vertical arrangement
 * @param titleHorizontalAlignment the title's horizontal alignment
 * @param titleBottomPadding the title's bottom padding
 * @param subtitle the top app bar subtitle (header)
 * @param navigationIcon a navigation icon [Composable]
 * @param actions actions [Composable]
 * @param extraContent extra content [Composable]
 * @param avatarMax maximum size of [avatar] when top bar is fully expanded
 * @param height this app bar's requested height
 */
@Composable
private fun CollapsingAvatarTopAppBarLayout(
    modifier: Modifier,
    scrolledOffset: FloatProducer,
    collapseFraction: FloatProducer,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    subtitleContentColor: Color,
    actionIconContentColor: Color,
    avatar: @Composable (BoxScope.() -> Unit)?,
    title: @Composable () -> Unit,
    titleVerticalArrangement: Arrangement.Vertical,
    titleHorizontalAlignment: Alignment.Horizontal,
    titleBottomPadding: Int,
    subtitle: (@Composable () -> Unit)?,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    extraContent: (@Composable ColumnScope.() -> Unit)?,
    avatarMax: Dp,
    height: Dp,
) {
    Layout(
        {
            Box(Modifier.layoutId("navigationIcon").padding(start = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon,
                )
            }

            if (avatar != null) {
                Box(modifier = Modifier.layoutId("avatar"), content = avatar)
            }

            val titleContentAlignment = if (titleHorizontalAlignment == Alignment.CenterHorizontally) {
                Alignment.TopCenter
            } else {
                Alignment.TopStart
            }
            Box(
                modifier = Modifier.layoutId("title").padding(horizontal = TopAppBarHorizontalPadding),
                contentAlignment = titleContentAlignment,
            ) {
                ProvideCollapseColorTextStyle(
                    contentColor = titleContentColor,
                    collapsedTextStyle = MaterialTheme.typography.titleLarge, // AppBarSmallTokens.TitleFont
                    expandedTextStyle = MaterialTheme.typography.headlineSmall
                        .copy(fontWeight = FontWeight.Medium), // AppBarMediumTokens.TitleFont + Medium Weight
                    collapseFraction = collapseFraction,
                    content = title,
                )
            }

            if (subtitle != null) {
                Box(
                    modifier =
                        Modifier
                            .layoutId("subtitle")
                            .padding(horizontal = TopAppBarHorizontalPadding)
                            .padding(top = TopAppBarHorizontalPadding)
                            .graphicsLayer {
                                alpha = TopTitleAlphaEasing.transform(lerp(1f, 0f, collapseFraction()))
                            },
                    contentAlignment = titleContentAlignment,
                ) {
                    ProvideContentColorTextStyle(
                        contentColor = subtitleContentColor,
                        textStyle = MaterialTheme.typography.titleSmall,
                        content = subtitle,
                    )
                }
            }

            Box(Modifier.layoutId("actionIcons").padding(end = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions,
                )
            }

            if (extraContent != null) {
                Column(
                    modifier = Modifier
                        .layoutId("extra")
                        .padding(horizontal = TopAppBarHorizontalPadding)
                        .graphicsLayer { alpha = lerp(1f, 0f, collapseFraction() * 3) },
                    content = extraContent
                )
            }
        },
        modifier = modifier,
        measurePolicy =
            remember(
                scrolledOffset,
                collapseFraction,
                titleVerticalArrangement,
                titleHorizontalAlignment,
                titleBottomPadding,
                height,
            ) {
                CollapsingAvatarTopBarMeasurePolicy(
                    scrolledOffset,
                    collapseFraction,
                    titleVerticalArrangement,
                    titleHorizontalAlignment,
                    titleBottomPadding,
                    avatarMax,
                    height,
                )
            },
    )
}

private class CollapsingAvatarTopBarMeasurePolicy(
    val scrolledOffset: FloatProducer,
    val collapseFractionProducer: FloatProducer,
    val titleVerticalArrangement: Arrangement.Vertical,
    val titleHorizontalAlignment: Alignment.Horizontal,
    val titleBottomPadding: Int,
    val avatarMax: Dp,
    val height: Dp,
) : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val collapsedFraction = collapseFractionProducer()
        val slowInCollapseFraction = FastOutSlowInEasing.transform(collapsedFraction)

        val navigationIconPlaceable =
            measurables
                .fastFirst { it.layoutId == "navigationIcon" }
                .measure(constraints.copy(minWidth = 0))
        val actionIconsPlaceable =
            measurables
                .fastFirst { it.layoutId == "actionIcons" }
                .measure(constraints.copy(minWidth = 0))

        // Visibility threshold: 99%
        val extraContentPlaceable = if (collapsedFraction < 0.99f) {
            measurables
                .fastFirstOrNull { it.layoutId == "extra" }
                ?.measure(constraints.copy(minWidth = 0))
        } else {
            null
        }

        val avatarMaxSize = min(avatarMax.roundToPx(), constraints.maxWidth)
        val avatarMinSize = max(CollapsedAvatarSize.roundToPx(), constraints.minWidth)
        val avatarWidth = lerp(avatarMaxSize, avatarMinSize, slowInCollapseFraction)
        val avatarPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == "avatar" }
                ?.measure(Constraints.fixed(avatarWidth, avatarWidth))

        val avatarPadding = if (avatarPlaceable != null) {
            IntSize(
                width = lerp(CollapsedAvatarHorizontalPadding.roundToPx(), 0, collapsedFraction),
                height = lerp(CollapsedAvatarVerticalPadding.roundToPx(), 0, collapsedFraction)
            )
        } else {
            IntSize.Zero
        }

        val maxTitleWidth =
            if (constraints.maxWidth == Constraints.Infinity) {
                constraints.maxWidth
            } else {
                // Allow title overlapping the action row (a little bit) when collapsed
                val actionWidth = actionIconsPlaceable.width - 12.dp.roundToPx()
                // Snap title width instead of lerp
                if (collapsedFraction > 0.6f) {
                    constraints.maxWidth - navigationIconPlaceable.width - avatarMinSize - actionWidth
                } else {
                    val expandedHorizontalPadding = (CollapsedAvatarHorizontalPadding + TopAppBarHorizontalPadding * 2).roundToPx()
                    constraints.maxWidth - avatarMaxSize - expandedHorizontalPadding
                }.coerceAtLeast(0)
            }

        val titlePlaceable =
            measurables
                .fastFirst { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        val subtitlePlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == "subtitle" }
                ?.let {
                    val maxSubtitleWidth = if (constraints.maxWidth == Constraints.Infinity) {
                        constraints.maxWidth
                    } else {
                        (constraints.maxWidth - avatarMax.roundToPx() - avatarPadding.width * 2).coerceAtLeast(0)
                    }
                    it.measure(constraints.copy(minWidth = 0, maxWidth = maxSubtitleWidth))
                }

        // Locate the title's baseline.
        val titleBaseline =
            if (titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                titlePlaceable[LastBaseline]
            } else {
                0
            }

        val subtitleExpandingOffset = subtitlePlaceable?.run {
            lerp(height, 0, LinearOutSlowInEasing.transform(collapsedFraction))
        } ?: 0

        val extraContentHeight = extraContentPlaceable?.run {
            lerp(height, 0, (collapsedFraction * 1.5f).coerceAtMost(1f))
        } ?: 0

        val topExpandingOffset = lerp(MinAvatarOffset.roundToPx(), 0, collapsedFraction)
        val maxElementHeight = max(avatarPlaceable?.height ?: 0, titlePlaceable.height + subtitleExpandingOffset)
        val maxLayoutHeight = max(
            height.roundToPx(),
            maxElementHeight + topExpandingOffset + avatarPadding.height * 2 + extraContentHeight
        )
        val layoutHeight =
            if (constraints.maxHeight == Constraints.Infinity) {
                maxLayoutHeight
            } else {
                (maxLayoutHeight + scrolledOffset().roundToInt()).coerceAtLeast(0)
            }

        return placeTopAppBar(
            constraints,
            layoutHeight,
            maxLayoutHeight,
            extraContentHeight,
            navigationIconPlaceable,
            avatarPlaceable,
            titlePlaceable,
            subtitlePlaceable,
            actionIconsPlaceable,
            extraContentPlaceable,
            topExpandingOffset,
            subtitleExpandingOffset,
            avatarPadding,
            titleBaseline,
        )
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.minIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.minIntrinsicHeight(width) } ?: 0,
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0,
        )
    }

    private fun MeasureScope.placeTopAppBar(
        constraints: Constraints,
        layoutHeight: Int,
        maxLayoutHeight: Int,
        extraContentHeight: Int,
        navigationIconPlaceable: Placeable,
        avatarPlaceable: Placeable?,
        titlePlaceable: Placeable,
        subtitlePlaceable: Placeable?,
        actionIconsPlaceable: Placeable,
        extraContentPlaceable: Placeable?,
        topExpandingOffset: Int,
        subtitleExpandingOffset: Int,
        avatarPadding: IntSize,
        titleBaseline: Int,
    ): MeasureResult {
        return layout(constraints.maxWidth, layoutHeight) {
            val collapsedFraction = collapseFractionProducer()

            // Pin Navigation icon and Action row
            val collapsedHeight = TopAppBarDefaults.TopAppBarExpandedHeight.roundToPx()

            // Avatar, title, subtitle
            val headerLayoutHeight = layoutHeight - topExpandingOffset - extraContentHeight

            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (collapsedHeight - navigationIconPlaceable.height) / 2,
            )

            // Expanded : CollapsedAvatarHorizontalPadding
            // Collapsed: TopAppBarTitleInset or navigationIcon
            var start = lerp(
                avatarPadding.width,
                max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width),
                collapsedFraction
            )

            // Avatar composable
            avatarPlaceable?.placeRelative(
                x = start,
                y = (headerLayoutHeight - avatarPlaceable.height) / 2 + topExpandingOffset
            )

            // Title and Subtitle composable
            titlePlaceable.let {
                // Add extra padding between avatar and title
                val titlePadding = lerp(TopAppBarHorizontalPadding.roundToPx() * 2, 0, collapsedFraction)
                start += (avatarPlaceable?.width?: 0) + titlePadding
                val end = actionIconsPlaceable.width
                // Align using the maxWidth. We will adjust the position later according to the
                // start and end. This is done to ensure that a center alignment is still maintained
                // when the start and end have different widths. Note that the title is centered
                // relative to the entire app bar width, and not just centered between the
                // navigation icon and the actions.
                var titleX =
                    titleHorizontalAlignment.align(
                        size = titlePlaceable.width,
                        space = constraints.maxWidth,
                        // Using Ltr as we call placeRelative later on.
                        layoutDirection = LayoutDirection.Ltr,
                    )
                // Reposition the title based on the start and the end (i.e. the navigation and
                // action widths).
                if (titleX < start) {
                    titleX += (start - titleX)
                } else if (titleX + titlePlaceable.width > constraints.maxWidth - end) {
                    titleX += ((constraints.maxWidth - end) - (titleX + titlePlaceable.width))
                }

                // The titleVerticalArrangement is always one of Center or Bottom.
                val titleY =
                    when (titleVerticalArrangement) {
                        Arrangement.Center ->
                            (headerLayoutHeight - titlePlaceable.height - subtitleExpandingOffset) / 2 + topExpandingOffset

                        // Apply bottom padding from the title's baseline only when the Arrangement
                        // is "Bottom".
                        Arrangement.Bottom -> {
                            val bottomElements = subtitleExpandingOffset + extraContentHeight
                            if (titleBottomPadding == 0) {
                                layoutHeight - titlePlaceable.height - bottomElements
                            } else {
                                // Calculate the actual padding from the bottom of the title, taking
                                // into account its baseline.
                                val paddingFromBottom =
                                    titleBottomPadding - (titlePlaceable.height - titleBaseline)
                                // Adjust the bottom padding to a smaller number if there is no room
                                // to fit the title.
                                val heightWithPadding = paddingFromBottom + titlePlaceable.height
                                val adjustedBottomPadding =
                                    if (heightWithPadding > maxLayoutHeight) {
                                        paddingFromBottom - (heightWithPadding - maxLayoutHeight)
                                    } else {
                                        paddingFromBottom
                                    }

                                layoutHeight - titlePlaceable.height - max(0, adjustedBottomPadding) - bottomElements
                            }
                        }
                        // Arrangement.Top
                        else -> topExpandingOffset
                    }

                it.placeRelative(titleX, titleY)

                val subtitleX = avatarPadding.width + avatarMax.roundToPx() + titlePadding
                // Subtitle composable
                subtitlePlaceable?.placeRelative(
                    x = lerp(subtitleX, (subtitleX * 0.85f).fastRoundToInt(), collapsedFraction),
                    y = titleY + titlePlaceable.height
                )
            }

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = (collapsedHeight - actionIconsPlaceable.height) / 2,
            )

            // Extra content
            extraContentPlaceable?.placeRelative(
                x = 0,
                y = layoutHeight - extraContentPlaceable.height
            )
        }
    }
}

@Composable
private fun ProvideCollapseColorTextStyle(
    contentColor: Color,
    collapsedTextStyle: TextStyle,
    expandedTextStyle: TextStyle,
    collapseFraction: FloatProducer,
    content: @Composable () -> Unit,
) {
    val textStyle by remember {
        derivedStateOf {
            if (collapseFraction() > 0.6f) collapsedTextStyle else expandedTextStyle
            // lerp(expandedTextStyle, collapsedTextStyle, collapseFraction())
        }
    }

    ProvideContentColorTextStyle(contentColor, textStyle, content)
}

/**
 * Default [MiuixScrollBehaviorState] for the collapsing bars in this file.
 *
 * Kept as a `@Composable` factory instead of an inline default expression so that every call site
 * that omits `scrollBehavior` still gets a state remembered across recompositions. The behavior
 * itself is Miuix's `MiuixScrollBehavior` (an exit-until-collapsed style behavior: the bar scrolls
 * out and stays out until the content scrolls back to the top).
 */
@Composable
private fun defaultMiuixScrollBehavior(): MiuixScrollBehaviorState =
    MiuixScrollBehavior(
        state = rememberMiuixTopAppBarState(),
        canScroll = { true },
    )

/**
 * Mirrors Miuix's own `TopAppBarLayout` offset-limit handling: the app bar's height offset limit is
 * the negative of the height the bar *expands to*, so that
 * [MiuixTopAppBarState.collapsedFraction] progresses from 0 (expanded) to 1 (fully collapsed) as
 * the bar scrolls away.
 *
 * **The limit must be derived from a stable height, never from the node's own measured height.**
 * The collapsing layouts in this file size themselves as
 * `layoutHeight = maxLayoutHeight + heightOffset` and, worse, `maxLayoutHeight` is itself a
 * function of `collapsedFraction` (it interpolates `topExpandingOffset`, `subtitleExpandingOffset`
 * and `extraContentHeight`). Feeding that measured height back into `heightOffsetLimit` closes a
 * loop:
 *
 * ```
 * heightOffset -> layoutHeight -> onSizeChanged -> heightOffsetLimit
 *              -> (setHeightOffset coerces to [heightOffsetLimit, 0]) -> heightOffset -> ...
 * ```
 *
 * `TopAppBarState.setHeightOffset` clamps on every write, so each turn of the loop snaps the offset
 * to the freshly written bound. On device this shows up as a 2-frame oscillation of the bar height
 * (measured: the expanded image bottom flipping between 691 px and 712 px at ~45 Hz while the list
 * is stationary) — the "twitching" users see.
 *
 * Reading the *fixed* `expandedHeight` instead of the measured height removes the feedback path
 * entirely: the limit is then a constant, so `heightOffset` can settle and `collapsedFraction`
 * becomes a pure function of the scroll position.
 *
 * @param expandedHeight the bar's fully-expanded height **in px**, i.e. the value the bar lays out
 *   to when `heightOffset == 0` and nothing has collapsed yet.
 * @param collapsedHeight the height that stays pinned and visible when fully collapsed, in px.
 */
private fun Modifier.adjustHeightOffsetLimit(
    scrollBehavior: MiuixScrollBehaviorState?,
    expandedHeight: Float,
    collapsedHeight: Float = 0f,
) =
    scrollBehavior?.state?.let { state ->
        val newLimit = -(expandedHeight - collapsedHeight)
        // Write eagerly during composition: `onSizeChanged` only fires after a layout pass, so a bar
        // measured for the first time already needs a sane limit — otherwise `collapsedFraction`
        // divides by a stale limit and the bar renders pre-collapsed on the first frame.
        if (state.heightOffsetLimit != newLimit) {
            state.heightOffsetLimit = newLimit
            // `heightOffsetLimit` is a plain field, so mutating it does not clamp `heightOffset`.
            // `TopAppBarState.setHeightOffset` is the only place that coerces into
            // `[heightOffsetLimit, 0]`; re-writing the current value routes it through that clamp so
            // the offset can never start a frame outside the new range.
            state.heightOffset = state.heightOffset
        }
        this
    } ?: this

private val TopAppBarColorSpect: AnimationSpec<Color> = spring(stiffness = Spring.StiffnessMediumLow)

// An easing function used to compute the alpha value that is applied to the top title part of a
// Medium or Large app bar.
/*@VisibleForTesting*/
internal val TopTitleAlphaEasing = CubicBezierEasing(.8f, 0f, .8f, .15f)

internal val TopAppBarHorizontalPadding = 4.dp

// A title inset when the App-Bar is a Medium or Large one. Also used to size a spacer when the
// navigation icon is missing.
private val TopAppBarTitleInset = 16.dp - TopAppBarHorizontalPadding

// Size of avatar offset when CollapsingAvatarTopBar is fully expanded
private val MinAvatarOffset = 48.dp

// Size of avatar composable when AvatarTopBar is fully collapsed
private val CollapsedAvatarSize = 36.dp

private val ExpandedAvatarSize = 64.dp

private val CollapsedAvatarHorizontalPadding = 28.dp
private val CollapsedAvatarVerticalPadding = 12.dp
