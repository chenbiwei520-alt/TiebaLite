// Flowing-light ("流光") background for Miuix screens.
//
// Ported from HyperMusicCover (https://github.com/zyl6932/HyperMusicCover,
// `ui/component/effect/`) — which is also where compose-miuix-ui's example
// `component/effect/BgEffectBackground` comes from — and adapted to this project:
//   * the OS2 variant and the phone/pad preset split are dropped (one preset, bounds are handled
//     by aspect ratio);
//   * the light colours are the upstream OS3 phone presets verbatim (see `FlowLightPalette`) —
//     deep blue / violet / magenta in dark mode, pastel pink-blue in light mode, matching the
//     reference video;
//   * the frame loop is capped at 30 fps — the drift is slow and smooth, and the shader is a
//     full-screen pass; the drift *speed* itself is upstream's 1:1 wall-clock scale.
//
// The file is Apache-2.0 upstream; the port keeps the original shader unchanged.

package com.huanchengfly.tieba.post.ui.miuix.effect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import com.huanchengfly.tieba.post.ui.miuix.miuixLayerBackdrop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.RuntimeShader
import top.yukonga.miuix.kmp.blur.asBrush
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Draws the animated flowing-light gradient.
 *
 * The layer paints an opaque `surface` base first and then the shader over it, so it can be used
 * as a [LayerBackdrop] source without the blur ever sampling transparent pixels (the pitfall the
 * miuix blur guide calls out).
 *
 * @param backdrop when non-null the layer is recorded into it with `Modifier.layerBackdrop`, which
 *   is what lets a sibling `Modifier.textureBlur(backdrop = …)` blur this light.
 * @param playing when false the frame loop stops and the last frame stays on screen.
 * @param alpha overall opacity; return 0 to stop the loop entirely (e.g. while scrolled away).
 */
@Composable
fun FlowingLightBackground(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    playing: Boolean = true,
    alpha: () -> Float = { 1f },
) {
    val scheme = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f
    val preset = remember(isDark) { buildFlowLightPreset(isDark = isDark) }
    val painter = remember(preset) { FlowLightPainter(preset) }
    val colorStage = remember { Animatable(0f) }

    // Colour drift: one spring step every `colorInterpPeriod / 2` seconds, with a very soft spring,
    // so the palette crawls between its three stages instead of cycling visibly.
    LaunchedEffect(playing, preset) {
        if (!playing) return@LaunchedEffect
        var targetStage = floor(colorStage.value) + 1f
        while (isActive) {
            delay((preset.colorInterpPeriod * 500).toLong())
            colorStage.animateTo(
                targetValue = targetStage,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 35f),
            )
            targetStage += 1f
        }
    }

    androidx.compose.foundation.layout.Spacer(
        modifier = modifier
            .then(if (backdrop != null) Modifier.miuixLayerBackdrop(backdrop) else Modifier)
            .flowLightDraw(
                painter = painter,
                preset = preset,
                surface = scheme.surface,
                playing = playing,
                colorStage = { colorStage.value },
                alpha = alpha,
            ),
    )
}

/** One palette / geometry set for the shader. */
internal class FlowLightPreset(
    /** 4 light points, each `(x, y, radius)` in normalised coordinates. */
    val points: FloatArray,
    /** Three colour stages, each 4 × (r, g, b, a) — the shader interpolates around them. */
    val colors1: FloatArray,
    val colors2: FloatArray,
    val colors3: FloatArray,
    /** Seconds between colour-stage advances. */
    val colorInterpPeriod: Float,
    /** Drift amplitude of the light points. */
    val pointOffset: Float,
    val lightOffset: Float,
    val saturateOffset: Float,
)

/** Drives the [OS3_BG_FRAG] shader: uniform bookkeeping only, no Compose state. */
internal class FlowLightPainter(
    preset: FlowLightPreset,
) {

    private val runtimeShader = RuntimeShader(OS3_BG_FRAG).also { shader ->
        shader.setFloatUniform("uTranslateY", U_TRANSLATE_Y)
        shader.setFloatUniform("uNoiseScale", U_NOISE_SCALE)
        shader.setFloatUniform("uPointRadiusMulti", U_POINT_RADIUS_MULTI)
        shader.setFloatUniform("uAlphaMulti", U_ALPHA_MULTI)
        shader.setFloatUniform("uPoints", preset.points)
        shader.setFloatUniform("uLightOffset", preset.lightOffset)
        shader.setFloatUniform("uSaturateOffset", preset.saturateOffset)
    }

    val brush: androidx.compose.ui.graphics.Brush get() = runtimeShader.asBrush()

    private val resolution = FloatArray(2)
    private val bound = FloatArray(4)
    private val colorsBuffer = FloatArray(16)
    private val pointsAnimBuffer = FloatArray(8)

    private var animTime = Float.NaN
    private var cachedColorStage = Float.NaN
    private var cachedColorsPreset: FlowLightPreset? = null
    private var cachedPointsAnimTime = Float.NaN
    private var cachedPointsAnimPreset: FlowLightPreset? = null
    private var cachedWidth = Float.NaN
    private var cachedHeight = Float.NaN

    fun updateResolution(width: Float, height: Float) {
        if (cachedWidth == width && cachedHeight == height) return
        cachedWidth = width
        cachedHeight = height
        resolution[0] = width
        resolution[1] = height
        runtimeShader.setFloatUniform("uResolution", resolution)
        updateBound(width, height)
        runtimeShader.setFloatUniform("uBound", bound)
    }

    fun updateAnimTime(time: Float) {
        if (animTime == time) return
        animTime = time
        runtimeShader.setFloatUniform("uAnimTime", time)
    }

    fun updatePointsAnim(time: Float, preset: FlowLightPreset) {
        if (cachedPointsAnimTime == time && cachedPointsAnimPreset === preset) return
        val offset = preset.pointOffset
        var i = 0
        while (i < 4) {
            val srcX = preset.points[i * 3]
            val srcY = preset.points[i * 3 + 1]
            // sin/cos of the *other* axis: the four lights wander on independent, non-circular paths.
            val animX = srcX + sin(time + srcY) * offset
            val animY = srcY + cos(time + animX) * offset
            pointsAnimBuffer[i * 2] = animX
            pointsAnimBuffer[i * 2 + 1] = animY
            i++
        }
        runtimeShader.setFloatUniform("uPointsAnim", pointsAnimBuffer)
        cachedPointsAnimTime = time
        cachedPointsAnimPreset = preset
    }

    fun updateColors(preset: FlowLightPreset, stage: Float) {
        if (cachedColorsPreset === preset && cachedColorStage == stage) return
        val base = stage.toInt()
        val fraction = stage - base
        val start = colorsForCycleIndex(preset, base)
        val end = colorsForCycleIndex(preset, base + 1)
        for (i in 0 until 16) {
            colorsBuffer[i] = start[i] + (end[i] - start[i]) * fraction
        }
        runtimeShader.setFloatUniform("uColors", colorsBuffer)
        cachedColorsPreset = preset
        cachedColorStage = stage
    }

    private fun colorsForCycleIndex(preset: FlowLightPreset, index: Int): FloatArray =
        // Upstream's cycle, kept verbatim: stage advances visit colors2 -> colors1 -> colors2 ->
        // colors3, so the dominant stage is colors2 and the other two alternate around it.
        when (index.mod(4)) {
            1 -> preset.colors1
            3 -> preset.colors3
            else -> preset.colors2
        }

    /**
     * The shader works in a square-ish space; `uBound` maps the viewport into it. Portrait keeps
     * the light band in the upper part of the screen, landscape centres it and spans the width —
     * the same split as upstream, minus the logo-height parameter this port does not need.
     */
    private fun updateBound(width: Float, height: Float) {
        val heightRatio = FLOW_LIGHT_BAND_RATIO
        bound[0] = 0f
        bound[1] = 1f - heightRatio
        bound[2] = 1f
        bound[3] = heightRatio
        if (width > height) {
            val aspectRatio = width / height
            val contentCenterY = 1f - heightRatio / 2f
            bound[1] = contentCenterY - aspectRatio / 2f
            bound[3] = aspectRatio
        }
    }

    private companion object {
        const val U_TRANSLATE_Y = 0f
        const val U_ALPHA_MULTI = 1f
        const val U_NOISE_SCALE = 1.5f
        const val U_POINT_RADIUS_MULTI = 1f
    }
}

/** Fraction of the viewport height the light band occupies. */
private const val FLOW_LIGHT_BAND_RATIO = 0.8f

/** Seconds of wall clock per second of shader time — upstream's drift speed, per the reference video. */
private const val FLOW_LIGHT_TIME_SCALE = 1.0f

/** Frame cap for the shader loop. 30 fps is plenty for a slow drift and halves the GPU work. */
private const val FLOW_LIGHT_MIN_DELTA_NANOS = 1_000_000_000L / 30L

internal fun Modifier.flowLightDraw(
    painter: FlowLightPainter,
    preset: FlowLightPreset,
    surface: Color,
    playing: Boolean,
    colorStage: () -> Float,
    alpha: () -> Float,
): Modifier = this then FlowLightElement(
    painter = painter,
    preset = preset,
    surface = surface,
    playing = playing,
    colorStage = colorStage,
    alpha = alpha,
)

private data class FlowLightElement(
    val painter: FlowLightPainter,
    val preset: FlowLightPreset,
    val surface: Color,
    val playing: Boolean,
    val colorStage: () -> Float,
    val alpha: () -> Float,
) : ModifierNodeElement<FlowLightNode>() {

    override fun create(): FlowLightNode = FlowLightNode(painter, preset, surface, playing, colorStage, alpha)

    override fun update(node: FlowLightNode) =
        node.update(painter, preset, surface, playing, colorStage, alpha)
}

private class FlowLightNode(
    private var painter: FlowLightPainter,
    private var preset: FlowLightPreset,
    private var surface: Color,
    private var playing: Boolean,
    private var colorStage: () -> Float,
    private var alpha: () -> Float,
) : Modifier.Node(),
    DrawModifierNode {

    private var animationJob: Job? = null
    private var animTime: Float = 0f
    private var startOffset: Float = 0f

    override fun onAttach() {
        if (playing) startAnimation()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
    }

    fun update(
        painter: FlowLightPainter,
        preset: FlowLightPreset,
        surface: Color,
        playing: Boolean,
        colorStage: () -> Float,
        alpha: () -> Float,
    ) {
        this.painter = painter
        this.preset = preset
        this.surface = surface
        this.colorStage = colorStage
        this.alpha = alpha

        if (this.playing != playing) {
            this.playing = playing
            if (playing) {
                startAnimation()
            } else {
                animationJob?.cancel()
                animationJob = null
            }
        }
        invalidateDraw()
    }

    private fun startAnimation() {
        animationJob?.cancel()
        startOffset = animTime
        animationJob = coroutineScope.launch {
            val origin = withFrameNanos { it }
            var lastEmit = origin
            while (isActive) {
                val now = withFrameNanos { it }
                if (now - lastEmit < FLOW_LIGHT_MIN_DELTA_NANOS) continue
                lastEmit = now
                animTime = startOffset + (now - origin) / 1_000_000_000f * FLOW_LIGHT_TIME_SCALE
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // Opaque base: the layer is a blur source, and a transparent source smears colour into the
        // blur's alpha.
        drawRect(surface)
        val alphaValue = alpha()
        when {
            alphaValue <= 0f -> {
                animationJob?.cancel()
                animationJob = null
            }

            playing && animationJob == null -> startAnimation()
        }
        if (alphaValue > 0f) {
            painter.updateResolution(size.width, size.height)
            painter.updateColors(preset, colorStage())
            painter.updateAnimTime(animTime)
            painter.updatePointsAnim(animTime, preset)
            drawRect(painter.brush, alpha = alphaValue)
        }
        drawContent()
    }
}
