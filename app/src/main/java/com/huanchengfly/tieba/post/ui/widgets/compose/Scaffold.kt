package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import com.kyant.backdrop.backdrops.layerBackdrop as kyantLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop as kyantRememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop as kyantRememberCombinedBackdrop
import com.huanchengfly.tieba.post.ui.miuix.LocalLiquidGlassBackdrop
import com.huanchengfly.tieba.post.ui.miuix.ProvideRecordingBackdrop
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixBarBackdrop
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LocalLiquidGlassPopupBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.basic.SnackbarHost as MiuixSnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.huanchengfly.tieba.post.PaddingNone
import com.huanchengfly.tieba.post.theme.isTranslucent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun SwipeToDismissSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    // Miuix SnackbarHost with built-in swipe-to-dismiss (HyperOS style snackbar)
    MiuixSnackbarHost(state = hostState, modifier = modifier)
}

@Composable
@NonRestartableComposable
fun rememberSnackbarHostState(): SnackbarHostState = remember { top.yukonga.miuix.kmp.basic.SnackbarHostState() }

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("no scaffold here!") }

// The floating gesture handle sits at least this far above the screen edge even when the system
// reports no navigation-bar inset (landscape gesture navigation reports 0dp for `navigationBars`).
private val GestureHandleClearance = 24.dp

/**
 * Bottom clearance for content that reaches the screen edge: the larger of the navigation bar, the
 * mandatory gesture area and a [GestureHandleClearance] floor. In landscape gesture navigation the
 * first two collapse to 0dp while the floating handle still overlays content, so the floor keeps
 * the last row of every page above it.
 */
@Composable
fun rememberGestureHandleBottomClearance(): Dp {
    val density = LocalDensity.current
    val navigationBar = WindowInsets.navigationBars.getBottom(density)
    val mandatoryGestures = WindowInsets.mandatorySystemGestures.getBottom(density)
    return with(density) {
        maxOf(navigationBar, mandatoryGestures, GestureHandleClearance.roundToPx()).toDp()
    }
}

/**
 * [base] with its bottom edge raised to [rememberGestureHandleBottomClearance]. Bottom-only on
 * purpose: unioning the full gesture insets would also pad the left/right back-gesture areas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun rememberGestureAwareInsets(
    base: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
): WindowInsets {
    val bottom = rememberGestureHandleBottomClearance()
    return remember(base, bottom) { base.union(WindowInsets(bottom = bottom)) }
}

@Composable
fun MyScaffold(
    modifier: Modifier = Modifier,
    useMD2Layout: Boolean = true,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    bottomBarAtop: Boolean = false,
    snackbarHostState: SnackbarHostState = rememberSnackbarHostState(),
    snackbarHost: @Composable () -> Unit = { SwipeToDismissSnackbarHost(snackbarHostState) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    backgroundColor: Color = MaterialTheme.colorScheme.run {
        if (isTranslucent) Color.Transparent else top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface
    },
    contentColor: Color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    // HyperOS shell: the Miuix Scaffold renders the page structure; the Material-style API above
    // is preserved so page call sites stay unchanged. useMD2Layout/bottomBarAtop are accepted for
    // compatibility; the Miuix layout is MD2-style and no caller relies on bottomBarAtop with a
    // real (non-placeholder) bottom bar.
    val miuixFabPosition = when (floatingActionButtonPosition) {
        androidx.compose.material3.FabPosition.Start -> top.yukonga.miuix.kmp.basic.FabPosition.Start
        androidx.compose.material3.FabPosition.Center -> top.yukonga.miuix.kmp.basic.FabPosition.Center
        else -> top.yukonga.miuix.kmp.basic.FabPosition.End
    }
    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        androidx.compose.material3.LocalContentColor provides contentColor,
    ) {
        // Liquid-glass sampling source for this page's dropdowns.
        //
        // It has to live INSIDE the Scaffold's `content`: the Scaffold renders its popup layer
        // outside `content`, so a capture placed here records the page only. A capture wrapped
        // *around* the Scaffold (or around the whole NavHost) would also record the open popup —
        // and because a popup panel is an opaque surface, the glass sheet would then blur its own
        // panel: flat white, no visible highlight, and the effect only faintly showing at the edges.
        // `Modifier.layerBackdrop` records into an offscreen layer and never changes what is drawn
        // on screen, so this is visually inert.
        val glassBackdrop = rememberMiuixBarBackdrop()
        // Kyant twin of the capture above. The vendored reference dropdowns paint their glass
        // through Kyant's `drawBackdrop`/`blur` (that is what hyper_schedule itself does), and a
        // Kyant `Backdrop` is a SEPARATE recording layer from the Miuix one —
        // `rememberMiuixBarBackdrop` cannot feed it. Recorded on the same box, for the same
        // reason: the dropdown renders in the Scaffold's popup layer, painted outside `content`.
        val popupLayerBackdrop = if (isRuntimeShaderSupported()) kyantRememberLayerBackdrop() else null
        // Second Kyant capture, for the TopBar slot.
        //
        // The Scaffold measures its slots as siblings (Body, TopBar, Snackbar, BottomBar, Popup)
        // and places Body at (0, 0), so the capture above spans the whole screen - but the pixels
        // under the bar are the page's own, because the bar itself is a sibling drawn on top of it
        // and never enters the recorded layer. A dropdown that drops from the toolbar therefore
        // samples a transparent strip exactly where the bar is: the bar shows through the panel's
        // 0.72 (light) / 0.8 (dark) tint *unblurred*, which reads as "half-transparent sheet, no
        // blur". hyper_schedule has the same blind spot (its capture also sits inside
        // `Scaffold { content }`), but Kyant ships the fix: `rememberCombinedBackdrop` composites
        // several recording layers, each aligned to the consumer's own coordinates, so page + bar
        // arrive as one backdrop.
        val barLayerBackdrop = if (isRuntimeShaderSupported()) kyantRememberLayerBackdrop() else null
        // Handed to the dropdowns as-is: Kyant's `LayerBackdrop` tracks its own recording node's
        // coordinates, exactly as in hyper_schedule (which passes the raw backdrop too).
        val popupBackdrop = if (popupLayerBackdrop != null && barLayerBackdrop != null) {
            kyantRememberCombinedBackdrop(popupLayerBackdrop, barLayerBackdrop)
        } else {
            popupLayerBackdrop
        }
        CompositionLocalProvider(
            LocalLiquidGlassBackdrop provides glassBackdrop,
            LocalLiquidGlassPopupBackdrop provides popupBackdrop,
        ) {
            top.yukonga.miuix.kmp.basic.Scaffold(
                modifier = modifier,
                topBar = {
                    // Recorded by `barLayerBackdrop` above. `Modifier.layerBackdrop` only fills an
                    // offscreen layer, so the bar still draws on screen exactly as before.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (barLayerBackdrop != null) {
                                    Modifier.kyantLayerBackdrop(barLayerBackdrop)
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        topBar()
                    }
                },
                bottomBar = bottomBar,
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = miuixFabPosition,
                snackbarHost = snackbarHost,
                containerColor = backgroundColor,
                contentWindowInsets = rememberGestureAwareInsets(contentWindowInsets),
                content = { padding ->
                    // `ProvideRecordingBackdrop` marks `glassBackdrop` as already recording for
                    // everything below, so a page that (mistakenly) applies
                    // `miuixLayerBackdrop(LocalLiquidGlassBackdrop.current)` to its own content
                    // becomes a no-op instead of nesting two `record()` calls on one graphics
                    // layer — which throws "Recording currently in progress" and crashes the page.
                    // The capturing Box itself must record through the RAW `layerBackdrop`:
                    // `miuixLayerBackdrop` consults `LocalRecordingBackdrop`, which
                    // `ProvideRecordingBackdrop` publishes for this whole scope — including this
                    // Box. Routing the recorder through the guard made it a no-op, so the layer
                    // only ever contained `rememberMiuixBarBackdrop`'s flat `drawRect(surface)`
                    // fill: blurring a flat color yields that same flat color, i.e. the
                    // progressive blur silently rendered nothing while the bar stayed transparent.
                    // The guard stays in effect for DESCENDANTS (page content), which is where the
                    // nested-`record()` crash came from.
                    ProvideRecordingBackdrop(glassBackdrop) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (glassBackdrop != null) {
                                        Modifier.layerBackdrop(glassBackdrop)
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (popupLayerBackdrop != null) {
                                        Modifier.kyantLayerBackdrop(popupLayerBackdrop)
                                    } else {
                                        Modifier
                                    }
                                ),
                        ) {
                            // Opaque base INSIDE the recorded subtree.
                            //
                            // The Scaffold paints `backgroundColor` on its own node, which is an
                            // ancestor of this capture, so the recorded layer stays transparent
                            // wherever the page itself paints nothing - plain list rows, toolbars.
                            // A dropdown sampling such a layer draws only its own tint over those
                            // pixels and the *sharp* page stays readable underneath, which is
                            // exactly why a menu dropping over the toolbar kept a crisp toolbar
                            // while the rest of the panel looked frosted. hyper_schedule never
                            // hits this because its screens paint their own full-size background
                            // inside the capture (AboutScreen draws `Modifier.background(...)`
                            // under its layer); MyScaffold's pages rely on the Scaffold, so the
                            // base is painted here. Same colour, same bounds - no visual change.
                            Box(Modifier.matchParentSize().background(backgroundColor))
                            content(padding)
                        }
                    }
                },
            )
        }
    }
}

/**
 * compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Scaffold.kt
 *
 * commit 576eeec 'Bump the version number for Material3 to 1.4.0-rc01'.
 * on branch androidx-compose-material3-release
 *
 * 0Ranko0p changes:
 *   1. Revert to MD2 layout (place content below the TopBar)
 *   2. Add 'BottomBarAtop' option to place content behind the BottomBar
 */

/**
 * [Material Design layout](https://m3.material.io/foundations/layout/understanding-layout/)
 *
 * Scaffold implements the basic Material Design visual layout structure.
 *
 * This component provides API to put together several Material components to construct your screen,
 * by ensuring proper layout strategy for them and collecting necessary data so these components
 * will work together correctly.
 *
 * To show a [Snackbar], use [SnackbarHostState.showSnackbar].
 *
 * @param modifier the [Modifier] to be applied to this scaffold
 * @param topBar top app bar of the screen, typically a [TopAppBar]
 * @param bottomBar bottom bar of the screen, typically a [NavigationBar]
 * @param bottomBarAtop whether or not the [bottomBar] is placed on top of the [content].
 * @param snackbarHost component to host [Snackbar]s that are pushed to be shown via
 *   [SnackbarHostState.showSnackbar], typically a [SnackbarHost]
 * @param floatingActionButton Main action button of the screen, typically a [FloatingActionButton]
 * @param floatingActionButtonPosition position of the FAB on the screen. See [FabPosition].
 * @param containerColor the color used for the background of this scaffold. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this scaffold. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param contentWindowInsets window insets to be passed to [content] slot via [PaddingValues]
 *   params. Scaffold will take the insets into account from the top/bottom only if the [topBar]/
 *   [bottomBar] are not present, as the scaffold expect [topBar]/[bottomBar] to handle insets
 *   instead. Any insets consumed by other insets padding modifiers or [consumeWindowInsets] on a
 *   parent layout will be excluded from [contentWindowInsets].
 * @param content content of the screen. The lambda receives a [PaddingValues] that should be
 *   applied to the content root via [Modifier.padding] and [Modifier.consumeWindowInsets] to
 *   properly offset top and bottom bars. If using [Modifier.verticalScroll], apply this modifier to
 *   the child of the scroll, and not on the scroll itself.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    bottomBarAtop: Boolean = false,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = rememberGestureAwareInsets(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(contentWindowInsets) }
    Surface(
        modifier =
            modifier.onConsumedWindowInsetsChanged { consumedWindowInsets ->
                // Exclude currently consumed window insets from user provided contentWindowInsets
                safeInsets.insets = contentWindowInsets.exclude(consumedWindowInsets)
            },
        color = containerColor,
        contentColor = contentColor,
    ) {
        ScaffoldLayout(
            fabPosition = floatingActionButtonPosition,
            topBar = topBar,
            bottomBar = bottomBar,
            bottomBarAtop = bottomBarAtop,
            content = content,
            snackbar = snackbarHost,
            contentWindowInsets = safeInsets,
            fab = floatingActionButton,
        )
    }
}

/**
 * Layout for a [Scaffold]'s content.
 *
 * @param fabPosition [FabPosition] for the FAB (if present)
 * @param topBar the content to place at the top of the [Scaffold], typically a [SmallTopAppBar]
 * @param content the main 'body' of the [Scaffold]
 * @param snackbar the [Snackbar] displayed on top of the [content]
 * @param fab the [FloatingActionButton] displayed on top of the [content], below the [snackbar] and
 *   above the [bottomBar]
 * @param bottomBar the content to place at the bottom of the [Scaffold], typically a [NavigationBar].
 * @param bottomBarAtop whether or not the [bottomBar] is placed on top of the [content].
 */
@Composable
private fun ScaffoldLayout(
    fabPosition: FabPosition,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    snackbar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    contentWindowInsets: WindowInsets,
    bottomBar: @Composable () -> Unit,
    bottomBarAtop: Boolean = false,
) {
    // Create the backing value for the content padding
    // These values will be updated during measurement, but before subcomposing the body content
    // Remembering and updating a single PaddingValues avoids needing to recompose when the values
    // change
    val contentPadding = remember {
        object : PaddingValues {
            var paddingHolder by mutableStateOf(PaddingNone)

            override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
                paddingHolder.calculateLeftPadding(layoutDirection)

            override fun calculateTopPadding(): Dp = paddingHolder.calculateTopPadding()

            override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
                paddingHolder.calculateRightPadding(layoutDirection)

            override fun calculateBottomPadding(): Dp = paddingHolder.calculateBottomPadding()
        }
    }

    val topBarContent: @Composable () -> Unit = remember(topBar) { { Box { topBar() } } }
    val snackbarContent: @Composable () -> Unit = remember(snackbar) { { Box { snackbar() } } }
    val fabContent: @Composable () -> Unit = remember(fab) { { Box { fab() } } }
    val bodyContent: @Composable () -> Unit =
        remember(content, contentPadding) { { Box { content(contentPadding) } } }
    val bottomBarContent: @Composable () -> Unit = remember(bottomBar) { { Box { bottomBar() } } }
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // respect only bottom and horizontal for snackbar and fab
        val leftInset = contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection)
        val rightInset = contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)
        val bottomInset = contentWindowInsets.getBottom(this@SubcomposeLayout)

        val topBarPlaceable =
            subcompose(ScaffoldLayoutContent.TopBar, topBarContent)
                .first()
                .measure(looseConstraints)

        val snackbarPlaceable =
            subcompose(ScaffoldLayoutContent.Snackbar, snackbarContent)
                .first()
                .measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))

        val fabPlaceable =
            subcompose(ScaffoldLayoutContent.Fab, fabContent)
                .first()
                .measure(looseConstraints.offset(-leftInset - rightInset, -bottomInset))

        val isFabEmpty = fabPlaceable.width == 0 && fabPlaceable.height == 0
        val fabPlacement =
            if (!isFabEmpty) {
                val fabWidth = fabPlaceable.width
                val fabHeight = fabPlaceable.height
                // FAB distance from the left of the layout, taking into account LTR / RTL
                val fabLeftOffset =
                    when (fabPosition) {
                        FabPosition.Start -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                FabSpacing.roundToPx() + leftInset
                            } else {
                                layoutWidth - FabSpacing.roundToPx() - fabWidth - rightInset
                            }
                        }
                        FabPosition.End,
                        FabPosition.EndOverlay -> {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                layoutWidth - FabSpacing.roundToPx() - fabWidth - rightInset
                            } else {
                                FabSpacing.roundToPx() + leftInset
                            }
                        }
                        else -> (layoutWidth - fabWidth + leftInset - rightInset) / 2
                    }

                FabPlacement(left = fabLeftOffset, width = fabWidth, height = fabHeight)
            } else {
                null
            }

        val bottomBarPlaceable =
            subcompose(ScaffoldLayoutContent.BottomBar, bottomBarContent)
                .first()
                .measure(looseConstraints)

        val isBottomBarEmpty = bottomBarPlaceable.width == 0 && bottomBarPlaceable.height == 0

        val fabOffsetFromBottom =
            fabPlacement?.let {
                if (isBottomBarEmpty || fabPosition == FabPosition.EndOverlay) {
                    it.height +
                        FabSpacing.roundToPx() +
                        contentWindowInsets.getBottom(this@SubcomposeLayout)
                } else {
                    // Total height is the bottom bar height + the FAB height + the padding
                    // between the FAB and bottom bar
                    bottomBarPlaceable.height + it.height + FabSpacing.roundToPx()
                }
            }

        val snackbarHeight = snackbarPlaceable.height
        val snackbarOffsetFromBottom =
            if (snackbarHeight != 0) {
                snackbarHeight +
                    (fabOffsetFromBottom
                        ?: bottomBarPlaceable.height.takeIf { !isBottomBarEmpty }
                        ?: contentWindowInsets.getBottom(this@SubcomposeLayout))
            } else {
                0
            }

        // Update the backing state for the content padding before subcomposing the body
        val insets = contentWindowInsets.asPaddingValues(this)
        contentPadding.paddingHolder =
            PaddingValues(
                top =
                    if (topBarPlaceable.width == 0 && topBarPlaceable.height == 0) {
                        insets.calculateTopPadding()
                    } else {
                        Dp.Hairline
                    },
                bottom =
                    if (isBottomBarEmpty) {
                        insets.calculateBottomPadding()
                    // 0Ranko0p changes: place content behind the BottomBar
                    } else if (bottomBarAtop) {
                        bottomBarPlaceable.height.toDp()
                    } else {
                        Dp.Hairline
                    },
                    // End of 0Ranko0p changes
                start = insets.calculateStartPadding(layoutDirection),
                end = insets.calculateEndPadding(layoutDirection),
            )

        val bodyContentPlaceable =
            subcompose(ScaffoldLayoutContent.MainContent, bodyContent)
                .first()
                .measure(
                    // 0Ranko0p changes: place content behind the BottomBar
                    if (!bottomBarAtop) {
                        looseConstraints.copy(
                            minHeight = layoutHeight - topBarPlaceable.height - bottomBarPlaceable.height,
                            maxHeight = layoutHeight - topBarPlaceable.height - bottomBarPlaceable.height
                        )
                    } else {
                        looseConstraints.copy(
                            minHeight = layoutHeight - topBarPlaceable.height,
                            maxHeight = layoutHeight - topBarPlaceable.height
                        )
                    }
                    // End of 0Ranko0p changes
                )

        layout(layoutWidth, layoutHeight) {
            // Placing to control drawing order to match default elevation of each placeable
            bodyContentPlaceable.place(0, topBarPlaceable.height)
            topBarPlaceable.place(0, 0)
            snackbarPlaceable.place(
                (layoutWidth - snackbarPlaceable.width +
                    contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection) -
                    contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)) / 2,
                layoutHeight - snackbarOffsetFromBottom,
            )
            // The bottom bar is always at the bottom of the layout
            bottomBarPlaceable.place(0, layoutHeight - (bottomBarPlaceable.height))
            // Explicitly not using placeRelative here as `leftOffset` already accounts for RTL
            fabPlacement?.let { placement ->
                fabPlaceable.place(placement.left, layoutHeight - fabOffsetFromBottom!!)
            }
        }
    }
}

/**
 * Placement information for a [FloatingActionButton] inside a [Scaffold].
 *
 * @property left the FAB's offset from the left edge of the bottom bar, already adjusted for RTL
 *   support
 * @property width the width of the FAB
 * @property height the height of the FAB
 */
@Immutable private class FabPlacement(val left: Int, val width: Int, val height: Int)

// FAB spacing above the bottom bar / bottom of the Scaffold
internal val FabSpacing = 16.dp

private enum class ScaffoldLayoutContent {
    TopBar,
    MainContent,
    Snackbar,
    Fab,
    BottomBar,
}
