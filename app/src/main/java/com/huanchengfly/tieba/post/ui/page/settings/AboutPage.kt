package com.huanchengfly.tieba.post.ui.page.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.huanchengfly.tieba.post.LocalWindowAdaptiveInfo
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.TiebaWebView
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.icons.GitHubInvertocat
import com.huanchengfly.tieba.post.ui.icons.License
import com.huanchengfly.tieba.post.ui.miuix.SquircleShape
import com.huanchengfly.tieba.post.ui.miuix.effect.FlowingLightBackground
import com.huanchengfly.tieba.post.ui.page.welcome.UaWebView
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.PreferenceGroupPosition
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.SegmentedPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.BlendMode as ComposeBlendMode
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info

private const val URL_PROJECT_GITHUB = "https://github.com/chenbiwei520-alt/TiebaLite/tree/miuix"

@Composable
fun AboutPage(
    modifier: Modifier = Modifier,
    onBackClicked: () -> Unit = {},
    /** 面板模式：平板设置页的右栏自己画标题栏，本页不再画自己的顶栏。 */
    paneMode: Boolean = false,
    onDisclaimerClicked: () -> Unit = {},
    onHomePageClicked: () -> Unit = {},
    onLicenseClicked: () -> Unit = {},
) {
    val context = LocalContext.current
    val windowSizeClass = LocalWindowAdaptiveInfo.current.windowSizeClass
    val isWindowHeightExpanded = windowSizeClass.isHeightAtLeastBreakpoint(
        heightDpBreakpoint = WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
    )
    val icons = remember {
        listOf(
            R.mipmap.ic_launcher_new_round,
            R.mipmap.ic_launcher_new_invert_round,
            R.mipmap.ic_launcher_round,
        )
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f
    // Blend stack for the wordmark, straight from HyperMusicCover's about page: the glyphs are
    // filled with a maximally blurred capture of the flowing light and pushed through three blend
    // passes — colour dodge / burn, linear light and a Lab tone map — which is what gives the
    // HyperOS logo its iridescent "tinted blur" look.
    val logoBlend = remember(isDark) { logoBlendEntries(isDark) }

    // A capture of the flowing-light layer only. The glass card, the header blur and the wordmark
    // all sample it, so they blur the *light* rather than the page that contains them — a blur
    // sampling a capture that includes itself feeds back frame after frame and washes out.
    val lightBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    SettingsScaffold(
        modifier = modifier,
        titleRes = R.string.title_about,
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        onBack = onBackClicked,
        paneMode = paneMode,
        background = { AboutFlowingBackground(lightBackdrop) },
    ) {
        customPreference {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isWindowHeightExpanded) {
                    Spacer(modifier = Modifier.height(48.dp))
                } else {
                    Spacer(modifier = Modifier.height(36.dp))
                }

                StrongBox {
                    var iconIndex by rememberSaveable { mutableIntStateOf(0) }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(icons[iconIndex])
                            .crossfade(false) // Coil alpha bug in API 28
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clickableNoIndication {
                                iconIndex = (iconIndex + 1).takeIf { it in icons.indices } ?: 0 // Loop icons
                            }
                    )
                }

                // The "Tieba Lite" wordmark: `DstIn` keeps the blurred + blended light only inside
                // the glyphs, so the letters read as tinted glass filled with the background glow.
                Image(
                    painter = painterResource(R.drawable.ic_splash_text),
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp, 96.dp)
                        .offset(y = (-8).dp)
                        .then(
                            if (lightBackdrop != null) {
                                Modifier.textureBlur(
                                    backdrop = lightBackdrop,
                                    // Shape-typed slot: the squircle silhouette as a Shape, so the
                                    // blur is carved with the same continuous corner the app's
                                    // cards are drawn with.
                                    shape = SquircleShape(16.dp),
                                    blurRadius = BlurDefaults.MaxBlurRadius,
                                    noiseCoefficient = BlurDefaults.NoiseCoefficient,
                                    colors = BlurDefaults.blurColors(blendColors = logoBlend),
                                    contentBlendMode = ComposeBlendMode.DstIn,
                                )
                            } else {
                                Modifier
                            },
                        ),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Text(
                    text = stringResource(R.string.welcome_intro_subtitle),
                    modifier = Modifier.offset(y = (-24).dp),
                    style = MaterialTheme.typography.titleMedium
                )

            }
        }

        customPreference {
            if (isWindowHeightExpanded){
                Spacer(modifier = Modifier.height(96.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        group {
            customPreference {
                // One HyperOS glass card: a Miuix Card whose material is a blurred capture of the
                // flowing light behind it (semi-transparent surface tint + Miuix glass stroke).
                // Rows are painted transparent so the card's blurred material shows through.
                val rowContainer = if (lightBackdrop != null) {
                    Color.Transparent
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                }
                AboutGlassCard(backdrop = lightBackdrop) {
                    SegmentedPreference(
                        title = context.getString(R.string.title_disclaimer),
                        leadingIcon = MiuixIcons.Info,
                        position = PreferenceGroupPosition.First,
                        containerColor = rowContainer,
                        onClick = onDisclaimerClicked,
                    )

                    SegmentedPreference(
                        title = context.getString(R.string.about_source_code),
                        summary = URL_PROJECT_GITHUB,
                        leadingIcon = GitHubInvertocat,
                        position = PreferenceGroupPosition.Middle,
                        containerColor = rowContainer,
                        onClick = onHomePageClicked,
                    )

                    SegmentedPreference(
                        title = context.getString(R.string.about_license),
                        summary = "GNU GENERAL PUBLIC LICENSE Version 3",
                        leadingIcon = Icons.Rounded.License,
                        position = PreferenceGroupPosition.Last,
                        containerColor = rowContainer,
                        onClick = onLicenseClicked,
                    )
                }
            }
        }
    }
}

/**
 * The About page's background: a HyperOS-style **flowing-light** band, Gaussian-blurred through
 * Miuix.
 *
 * Ported from HyperMusicCover (`ui/component/effect/`) — the same source compose-miuix-ui ships as
 * its `BgEffectBackground` example — and adapted: the palette is upstream's OS3 phone preset (see
 * [FlowingLightBackground]) and the frame loop is capped at 30 fps.
 *
 * The effect is two layers, exactly the split upstream uses:
 *
 * 1. the **light band** — the `OS3_BG_FRAG` runtime shader (four drifting lights, perlin
 *    desaturation, dither), animated by a per-frame loop and a slow colour-stage spring. It draws
 *    an opaque `surface` base first and is recorded into [lightBackdrop] by
 *    [FlowingLightBackground] itself, which is what makes it a blur source for the header glow and
 *    the glass card.
 * 2. the **blur** on top — `Modifier.textureBlur(lightBackdrop, …, contentBlendMode = DstIn)`,
 *    Miuix's *foreground blur* (`docs/zh_CN/guide/blur.md` § 前景模糊): the blurred capture is kept
 *    only where this layer's own content has alpha, so the vertical gradient inside it acts as the
 *    mask. That mask is opaque under the header and transparent by the time the preference rows
 *    start, so the band reads as a soft glow behind the logo and fades into the plain surface.
 *
 * The blur layer is a *sibling* of the captured layer and samples [lightBackdrop] — the light
 * alone, not the page containing this blur — so it can never feed back into itself.
 *
 * Layout is untouched — both layers are `matchParentSize()` in the scaffold's content box, so no
 * element gains or loses space and the list keeps its own scroll behaviour. Below API 33
 * `RuntimeShader` is unavailable, [lightBackdrop] is null, nothing is drawn and the page keeps its
 * plain surface background.
 */
@Composable
private fun BoxScope.AboutFlowingBackground(lightBackdrop: LayerBackdrop?) {
    if (lightBackdrop == null) return
    val colors = MiuixTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f

    FlowingLightBackground(
        modifier = Modifier.matchParentSize(),
        backdrop = lightBackdrop,
    )

    // Mask for `DstIn`: opaque where the blurred band should show, transparent where the page's own
    // surface should.
    val maskBrush = remember(isDark) {
        Brush.verticalGradient(
            0f to Color.White,
            0.5f to Color.White.copy(alpha = if (isDark) 0.6f else 0.5f),
            0.85f to Color.White.copy(alpha = 0f),
        )
    }
    // A touch of surface over the blur keeps the glow inside the page's own palette instead of
    // reading as a raw gradient.
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(
                color = colors.surface.copy(alpha = if (isDark) 0.5f else 0.32f),
                mode = BlurBlendMode.SrcOver,
            ),
        ),
        saturation = 1.08f,
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .textureBlur(
                backdrop = lightBackdrop,
                shape = RectangleShape,
                blurRadius = AboutBackgroundBlurRadius,
                colors = blurColors,
                contentBlendMode = ComposeBlendMode.DstIn,
            ),
    ) {
        Spacer(modifier = Modifier.fillMaxSize().background(maskBrush))
    }
}

/**
 * Gaussian radius, in dp, applied to the light band. Large enough that the four lights read as one
 * soft ribbon rather than four blobs; well under `BlurDefaults.MaxBlurRadius` (150f).
 */
private const val AboutBackgroundBlurRadius = 48f

/**
 * The About page's preference group as one HyperOS **glass card**, the way HyperMusicCover's about
 * page builds its cards: a Miuix [Card] whose material is `Modifier.textureBlur` over the captured
 * flowing light.
 *
 * Layering, from back to front:
 *
 * 1. the blurred capture (`blurRadius = [AboutCardBlurRadius]`, upstream's card radius), tinted by
 *    a semi-transparent `surfaceContainer` blend ([BlurBlendMode.SrcOver]) so the card keeps the
 *    page's own palette instead of showing raw smeared colour — `alpha` 0.55 in dark mode, 0.42 in
 *    light mode, matching the search-bar glass tints used elsewhere in the app;
 * 2. the card's lit edge, a 1.dp `squircleBorder` stroked from that same
 *    `CardDefaults.CornerRadius` squircle — so the card can only ever show one outline (Miuix's
 *    `Highlight` bloom could not follow it: see the note at the stroke below);
 * 3. the transparent-card content — the preference rows — on top.
 *
 * When [backdrop] is null (no `RuntimeShader`, i.e. below API 33) the blur cannot run; the card
 * then falls back to Miuix's opaque `surfaceContainer` so it stays readable everywhere else.
 */
@Composable
private fun AboutGlassCard(
    backdrop: LayerBackdrop?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f
    val cornerRadius = CardDefaults.CornerRadius

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (backdrop != null) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        // Shape-typed slot: the blur is carved with the same continuous corner the
                        // Card underneath is drawn with (Miuix Card → squircleSurface).
                        shape = SquircleShape(cornerRadius),
                        blurRadius = AboutCardBlurRadius,
                        noiseCoefficient = BlurDefaults.NoiseCoefficient,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(
                                    color = colors.surfaceContainer.copy(
                                        alpha = if (isDark) 0.55f else 0.42f,
                                    ),
                                    mode = BlurBlendMode.SrcOver,
                                ),
                            ),
                        ),
                        // Deliberately no `highlight` argument: `Highlight`'s bloom shader reads the
                        // corner radii with `shape as? CornerBasedShape` (`setCornerRadiiUniform` in
                        // miuix-blur `HighlightStyle.kt`), and `SquircleShape` is a plain `Shape` —
                        // so the cast fails, the shader falls back to `minDimension / 2`, and the
                        // card gets a second, far rounder outline on top of its 16.dp squircle. The
                        // lit edge is stroked below from the same squircle instead.
                    ).squircleBorder(
                        width = AboutCardBorderWidth,
                        color = Color.White.copy(
                            alpha = if (isDark) AboutCardEdgeAlphaDark else AboutCardEdgeAlphaLight,
                        ),
                        cornerRadius = cornerRadius,
                    )
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.defaultColors(
            color = if (backdrop != null) Color.Transparent else colors.surfaceContainer,
            contentColor = Color.Transparent,
        ),
        content = content,
    )
}

/** Stroke width of the glass card's lit edge, in dp — one hairline, like Miuix's own cards. */
private val AboutCardBorderWidth = 1.dp

/** White alpha of the glass card's lit edge, per theme. */
private const val AboutCardEdgeAlphaLight = 0.5f
private const val AboutCardEdgeAlphaDark = 0.14f

/**
 * HyperOS **logo blend** stack, the values HyperMusicCover uses for its about-page wordmark.
 *
 * Applied to a `textureBlur(..., contentBlendMode = DstIn)` layer, so the blurred background is
 * kept only where the glyphs have alpha and then pushed through three passes:
 *
 * 1. `ColorDodge` (dark) / `ColorBurn` (light) — ties the glyph's brightness to the light behind
 *    it, which is what makes the logo brighten where the band is bright;
 * 2. `LinearLight` — pushes contrast so the tint reads as glass rather than flat paint;
 * 3. `Lab` — a Lab tone map (`Lab` uses `color.r` as `m` and `color.g` as `n`) that lands the
 *    result back inside the page's own palette.
 */
private fun logoBlendEntries(isDark: Boolean): List<BlendColorEntry> = if (isDark) {
    listOf(
        BlendColorEntry(color = Color(0xe6a1a1a1), mode = BlurBlendMode.ColorDodge),
        BlendColorEntry(color = Color(0x4de6e6e6), mode = BlurBlendMode.LinearLight),
        BlendColorEntry(color = Color(0xff1af500), mode = BlurBlendMode.Lab),
    )
} else {
    listOf(
        BlendColorEntry(color = Color(0xcc4a4a4a), mode = BlurBlendMode.ColorBurn),
        BlendColorEntry(color = Color(0xff4f4f4f), mode = BlurBlendMode.LinearLight),
        BlendColorEntry(color = Color(0xff1af200), mode = BlurBlendMode.Lab),
    )
}

/**
 * Gaussian radius, in dp, of the glass card — HyperMusicCover uses 60f for its about-page cards.
 */
private const val AboutCardBlurRadius = 60f

@Composable
fun AboutPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val disclaimerDialogState = rememberDialogState()

    fun launchCustomTab(url: String) {
        TiebaWebView.launchCustomTab(context, Uri.parse(url))
    }

    AboutPage(
        onBackClicked = onBack,
        onDisclaimerClicked = disclaimerDialogState::show,
        onHomePageClicked = { launchCustomTab(URL_PROJECT_GITHUB) },
        onLicenseClicked = { launchCustomTab("${URL_PROJECT_GITHUB}/blob/main/LICENSE") },
    )

    AlertDialog(
        dialogState = disclaimerDialogState,
        buttons = {
            NegativeButton(text = stringResource(R.string.btn_close)) {
                disclaimerDialogState.show = false
            }
        }
    ) {
        UaWebView(modifier = Modifier.height(480.dp))
    }
}

@Preview("AboutPage", showBackground = true, backgroundColor = -1L)
@Composable
private fun AboutPagePreview() = TiebaLiteTheme {
    AboutPage()
}