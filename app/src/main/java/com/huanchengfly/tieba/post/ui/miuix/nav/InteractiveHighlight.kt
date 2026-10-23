package com.huanchengfly.tieba.post.ui.miuix.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.RuntimeShader
import top.yukonga.miuix.kmp.blur.asBrush
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported

/**
 * A press-and-drag highlight drawn with a runtime shader, used by the liquid-glass bottom bar.
 *
 * Ported from the example utility class of Kyant0/AndroidLiquidGlass (Apache-2.0), specifically
 * `utils/InteractiveHighlight.kt`. What changed during the port:
 *
 *  - the shader backend was switched from `com.kyant.backdrop.RuntimeShader` + `asComposeShader()`
 *    to this project's existing Miuix equivalent: `top.yukonga.miuix.kmp.blur.RuntimeShader` (an
 *    AGSL shader) plus the `asBrush()` extension, so no third-party `backdrop` dependency is
 *    introduced. The shader source, uniforms and drawing blend mode are byte-for-byte the same;
 *  - the upstream `inspectDragGestures` helper was replaced with the standard
 *    `androidx.compose.foundation.gestures.detectDragGestures` (the helper file was NOT copied).
 *
 * The fallback for devices whose RuntimeShader is unavailable (below API 33) is upstream's own:
 * a plain 25%-white `BlendMode.Plus` fill, with the shader object never constructed. The animation
 * specs (press progress and highlight position) are unchanged from upstream.
 *
 * @param animationScope the scope every animation is launched in.
 * @param position maps the current layout [Size] and the animated touch [Offset] to the shader
 *   space position of the highlight centre.
 */
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {

    private val pressProgressAnimationSpec =
        spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec =
        spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    // Only build the AGSL shader when the device can actually run it; constructing it
    // unconditionally would throw on pre-API-33 devices where the fallback path is used instead.
    private val shader =
        if (isRuntimeShaderSupported()) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                if (shader != null) {
                    drawRect(
                        Color.White.copy(0.08f * progress),
                        blendMode = BlendMode.Plus
                    )
                    shader.apply {
                        val position = position(size, positionAnimation.value)
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", Color.White.copy(0.15f * progress))
                        setFloatUniform("radius", size.minDimension * 1.5f)
                        setFloatUniform(
                            "position",
                            position.x.fastCoerceIn(0f, size.width),
                            position.y.fastCoerceIn(0f, size.height)
                        )
                    }
                    drawRect(
                        brush = shader.asBrush(),
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawRect(
                        Color.White.copy(0.25f * progress),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            detectDragGestures(
                onDragStart = { downOffset ->
                    startPosition = downOffset
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
                onDrag = { change, _ ->
                    change.consume()
                    animationScope.launch { positionAnimation.snapTo(change.position) }
                }
            )
        }
}
