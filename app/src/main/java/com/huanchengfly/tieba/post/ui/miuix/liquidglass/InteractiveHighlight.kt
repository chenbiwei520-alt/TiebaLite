package com.huanchengfly.tieba.post.ui.miuix.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
    /**
     * 光晕半径倍率，默认 1（实际半径 = 半径基准 * 1.5 * 本值）。
     * 在 draw 阶段读取，可传会变的值（如展开进度）让光晕随状态收放。
     */
    val radiusScale: () -> Float = { 1f },
    /**
     * 光晕半径的**上限**（dp）。默认 null = 不限，直接用节点自身 minDimension
     *
     * 取 min(自身尺寸, 本值) 而非直接固定：收起态是个 42dp 的小圆按钮，
     * 固定大基准会让光晕漫出圆外，此时自动退回minDimension。
     */
    val radiusBaseDp: Dp? = null
) {

    /**
     * 按压进度与拖动位移**共用同一条弹簧曲线**（同 dampingRatio / stiffness）
     * 位移/拉伸/按压三层叠加后容易看成两段）。改这里即可整体调节回弹手感。
     */
    private companion object {
        const val SPRING_DAMPING_RATIO = 0.5f
        const val SPRING_STIFFNESS = 300f
    }

    /**
     * 结束阈值：按压与位移统一用 0.001。
     * 注意 Offset 版阈值必须是 `Offset(x, x)`，不能写裸 Float
     */
    private val visibilityThreshold = 0.001f
    private val offsetVisibilityThreshold = Offset(visibilityThreshold, visibilityThreshold)

    private val pressProgressAnimationSpec =
        spring(SPRING_DAMPING_RATIO, SPRING_STIFFNESS, visibilityThreshold)
    private val positionAnimationSpec =
        spring(SPRING_DAMPING_RATIO, SPRING_STIFFNESS, offsetVisibilityThreshold)

    private val pressProgressAnimation =
        Animatable(0f, visibilityThreshold)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, offsetVisibilityThreshold)

    /**
     * 形变位移，独立于高光位置。
     *
     * 高光松手时**原地淡出**（positionAnimation 停在松手处），若形变仍从它派生就会
     * 永远保持拖动距离而不回弹。所以这里单独跟一根动画，松手回弹到 0。
     */
    private val dragOffsetAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, offsetVisibilityThreshold)

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = pressProgressAnimation.value
    val offset: Offset get() = dragOffsetAnimation.value

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
                        // 取 min：面板大时用上限封顶，收起态（小圆按钮）自动退回自身尺寸
                        val base = radiusBaseDp?.let { minOf(size.minDimension, it.toPx()) }
                            ?: size.minDimension
                        setFloatUniform("radius", base * 1.5f * radiusScale())
                        setFloatUniform(
                            "position",
                            position.x.fastCoerceIn(0f, size.width),
                            position.y.fastCoerceIn(0f, size.height)
                        )
                    }
                    drawRect(
                        ShaderBrush(shader.asComposeShader()),
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
            inspectDragGestures(
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                        launch { dragOffsetAnimation.snapTo(Offset.Zero) }
                    }
                },
                // 松手：形变回弹到起点，高光**原地淡出**（positionAnimation 不动）。
                // 高光若也回弹到按下点，就会出现「拖过去再滑回来」的位移感。
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { dragOffsetAnimation.animateTo(Offset.Zero, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { dragOffsetAnimation.animateTo(Offset.Zero, positionAnimationSpec) }
                    }
                },
                // 纯视觉观察者：翻页手势吃掉横/竖拉后仍继续跟手，保证各方向都有位移
                observeConsumed = true,
            ) { change, _ ->
                animationScope.launch {
                    positionAnimation.snapTo(change.position)
                    dragOffsetAnimation.snapTo(change.position - startPosition)
                }
            }
        }

    // 只处理按压效果，不处理拖动
    val pressOnlyModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            awaitEachGesture {
                val down = awaitFirstDown()
                // 不消费指针事件，让 clickable 能够接收点击
                animationScope.launch {
                    pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec)
                }
                // 等待所有手指释放
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                } while (event.changes.any { it.pressed })
                animationScope.launch {
                    pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec)
                }
            }
        }
}
