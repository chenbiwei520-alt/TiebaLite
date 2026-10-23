package com.huanchengfly.tieba.post.ui.miuix.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A drag-driven value animation used by the liquid-glass bottom bar.
 *
 * Ported from the example utility class of Kyant0/AndroidLiquidGlass (Apache-2.0), specifically
 * `utils/DampedDragAnimation.kt`. Only the framework coupling was swapped for stock APIs:
 *
 *  - the upstream `inspectDragGestures` helper was replaced with the standard
 *    `androidx.compose.foundation.gestures.detectDragGestures`;
 *  - the upstream `awaitFrame()` expect function was replaced with `withFrameNanos`;
 *  - `kotlinx-datetime`'s `Clock.System.now().toEpochMilliseconds()` was replaced with the plain
 *    JVM `System.nanoTime() / 1_000_000L`.
 *
 * The animation specs (damping ratio / stiffness / visibility threshold of the value, velocity,
 * press-progress and scale animations), the [MutatorMutex], the [VelocityTracker] and the public
 * semantics are unchanged from upstream. The upstream helper types
 * (`utils/DragGestureInspector.kt`, `utils/Coroutines.kt`) were intentionally NOT copied over.
 *
 * @param animationScope the scope every animation is launched in.
 * @param initialValue the resting value the animation starts at.
 * @param valueRange the range [value] is clamped to.
 * @param visibilityThreshold the value animation's visibility threshold (also scales the velocity
 *   animation's threshold by 10x, exactly as upstream).
 * @param initialScale the scale the element rests at.
 * @param pressedScale the scale the element shrinks to while pressed.
 * @param onDragStarted called with the down position when a drag starts.
 * @param onDragStopped called when the drag ends or is cancelled.
 * @param onDrag called with the current [IntSize] and the drag delta while dragging.
 */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec =
        spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec =
        spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec =
        spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec =
        spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                onDragStarted(offset)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            },
            onDrag = { change, dragAmount ->
                // Standard gesture contract: claim the pointer so no ancestor gesture steals the
                // drag out from under the bar.
                change.consume()
                onDrag(size, dragAmount)
            }
        )
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            withFrameNanos { }
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            // Upstream used kotlinx-datetime here; the tracker only needs a monotonically
            // increasing millisecond timeline, which System.nanoTime() provides without a new
            // dependency.
            System.nanoTime() / 1_000_000L,
            Offset(value, 0f)
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
