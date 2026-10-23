package com.huanchengfly.tieba.post.ui.widgets.compose.morph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 大图缩放、长图贴边计算与惯性平移物理模拟工具。
 *
 * 逐行移植自 Matter（HapeLee/Matter）ui/media/ZoomableImageHelper.kt 的
 * [ZoomableImageCalculator]，形变画廊的手势物理与参考项目保持一致。
 */
object ZoomableImageCalculator {

    fun calculateTargetDoubleTapScale(
        containerWidth: Float,
        containerHeight: Float,
        imageRatio: Float
    ): Float {
        if (containerWidth <= 0f || containerHeight <= 0f || imageRatio <= 0f) {
            return 2.5f
        }
        val containerRatio = containerWidth / containerHeight
        // 如果图片的宽高比小于容器宽高比（即高度超过了屏幕长度的长图）
        if (imageRatio < containerRatio * 0.98f) {
            val fittedWidth = containerHeight * imageRatio
            if (fittedWidth > 0f) {
                val fillWidthScale = containerWidth / fittedWidth
                // 若贴边倍率显著大于 1.0x 则优先贴边，否则取 2.5x
                return if (fillWidthScale >= 1.25f) fillWidthScale else 2.5f
            }
        }
        return 2.5f
    }

    fun calculateMaxPanOffset(
        containerWidth: Float,
        containerHeight: Float,
        imageRatio: Float,
        currentScale: Float
    ): Pair<Float, Float> {
        if (containerWidth <= 0f || containerHeight <= 0f || imageRatio <= 0f || currentScale <= 1f) {
            return 0f to 0f
        }
        val containerRatio = containerWidth / containerHeight
        val fittedWidth: Float
        val fittedHeight: Float
        if (imageRatio > containerRatio) {
            fittedWidth = containerWidth
            fittedHeight = containerWidth / imageRatio
        } else {
            fittedHeight = containerHeight
            fittedWidth = containerHeight * imageRatio
        }

        val displayedWidth = fittedWidth * currentScale
        val displayedHeight = fittedHeight * currentScale

        val maxOffsetX = max(0f, (displayedWidth - containerWidth) / 2f)
        val maxOffsetY = max(0f, (displayedHeight - containerHeight) / 2f)
        return maxOffsetX to maxOffsetY
    }

    fun calculateDoubleTapTargetOffset(
        tapPosition: Offset,
        containerWidth: Float,
        containerHeight: Float,
        imageRatio: Float,
        targetScale: Float
    ): Offset {
        if (targetScale <= 1.05f) return Offset.Zero

        val (maxOffsetX, maxOffsetY) = calculateMaxPanOffset(
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            imageRatio = imageRatio,
            currentScale = targetScale
        )

        // 对于长图贴边，水平方向宽度完全占满（maxOffsetX 为 0 或极小），直接居中 0f
        val targetX = if (maxOffsetX <= 1f) {
            0f
        } else {
            ((containerWidth / 2f) - tapPosition.x) * (targetScale - 1f)
        }.coerceIn(-maxOffsetX, maxOffsetX)

        // 垂直方向锚定：以触控位置在屏幕上的相对比例计算平移
        val fractionY = if (containerHeight > 0f) (tapPosition.y / containerHeight).coerceIn(0f, 1f) else 0.5f
        val targetY = ((0.5f - fractionY) * (maxOffsetY * 2f)).coerceIn(-maxOffsetY, maxOffsetY)

        return Offset(targetX, targetY)
    }

    fun flingWithInertia(
        scope: CoroutineScope,
        animatable: Animatable<Float, AnimationVector1D>,
        initialVelocity: Float,
        maxBound: Float
    ) {
        if (maxBound <= 0f) {
            scope.launch {
                animatable.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            }
            return
        }

        val decaySpec = exponentialDecay<Float>(
            frictionMultiplier = 1.25f,
            absVelocityThreshold = 0.5f
        )

        scope.launch {
            var hitBoundary = false
            try {
                animatable.animateDecay(
                    initialVelocity = initialVelocity,
                    animationSpec = decaySpec
                ) {
                    if (value <= -maxBound || value >= maxBound) {
                        hitBoundary = true
                        throw CancellationException("Hit Pan Boundary")
                    }
                }
            } catch (_: CancellationException) {}

            if (hitBoundary || animatable.value < -maxBound || animatable.value > maxBound) {
                animatable.animateTo(
                    targetValue = animatable.value.coerceIn(-maxBound, maxBound),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    fun animateZoom(
        scope: CoroutineScope,
        scaleAnim: Animatable<Float, AnimationVector1D>,
        offsetXAnim: Animatable<Float, AnimationVector1D>,
        offsetYAnim: Animatable<Float, AnimationVector1D>,
        targetScale: Float,
        targetOffset: Offset,
        onFinished: (() -> Unit)? = null
    ): kotlinx.coroutines.Job {
        return scope.launch {
            val j1 = launch {
                scaleAnim.animateTo(
                    targetValue = targetScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            val j2 = launch {
                offsetXAnim.animateTo(
                    targetValue = targetOffset.x,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            val j3 = launch {
                offsetYAnim.animateTo(
                    targetValue = targetOffset.y,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            j1.join()
            j2.join()
            j3.join()
            onFinished?.invoke()
        }
    }
}
