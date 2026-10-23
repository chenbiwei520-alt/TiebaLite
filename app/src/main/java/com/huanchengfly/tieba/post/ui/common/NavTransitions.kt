package com.huanchengfly.tieba.post.ui.common

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.arch.unsafeLazy
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
class NavTransitions(
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
    val popEnterTransition: EnterTransition,
    val popExitTransition: ExitTransition,
) {
    companion object {
        private val DefaultAnimationSpec: FiniteAnimationSpec<Any> = tween(
            durationMillis = AnimationConstants.DefaultDurationMillis,
            easing = FastOutSlowInEasing
        )

        @Suppress("UNCHECKED_CAST")
        fun <T> defaultAnimationSpec(): FiniteAnimationSpec<T> {
            return DefaultAnimationSpec as FiniteAnimationSpec<T>
        }

        /**
         * Port of the Miuix component library's page transition — `NavTransitions.MiuixDefault`
         * driven by `NavMotion`'s programmatic settle spec, from the `miuix-nav` module of
         * compose-miuix-ui (v0.9.4, Apache-2.0).
         *
         * Geometry (Miuix maps a single depth driver `d`; here each Navigation Compose phase
         * animates its own fraction with the same curve, which composes to the identical visual):
         *
         *  - **push**: the entering page slides in full-width from the trailing edge
         *    (`translationX: +width -> 0`) while the covered page parallaxes a quarter width toward
         *    the leading edge with a light alpha falloff (1 -> 0.9);
         *  - **pop**: mirrored — the leaving page slides out full-width to the trailing edge while
         *    the revealed page returns from its quarter-width parallax and fades back in.
         *
         * RTL mirroring comes for free from `slideIn/OutHorizontally`'s layout-direction-aware
         * offsets. Miuix pixel-snaps the entering offset so a corner-clipped page's anti-aliased
         * edge cannot shimmer during finger tracking; that only matters for the interactive gesture
         * path its own renderer drives, which Navigation Compose does not have.
         *
         * Motion is Miuix's programmatic settle: a 500 ms tween carrying `NavSettleEasing` — the
         * step response of an underdamped spring (response = 0.8, damping = 0.95) baked into an
         * easing, i.e. a brisk middle with a long gentle tail and no visible bounce. Miuix's
         * gesture-commit spring does not apply here: Navigation Compose transitions are always
         * programmatic (predictive back is disabled in this app).
         */
        val DefaultTransitions: NavTransitions by unsafeLazy {
            // The slides interpolate an IntOffset, the fades a Float — two specs, one easing.
            val settleOffset = tween<IntOffset>(
                durationMillis = MiuixProgrammaticDurationMillis,
                easing = NavSettleEasing()
            )
            val settleFloat = tween<Float>(
                durationMillis = MiuixProgrammaticDurationMillis,
                easing = NavSettleEasing()
            )
            NavTransitions(
                enterTransition = slideInHorizontally(
                    animationSpec = settleOffset,
                    initialOffsetX = { it }
                ),
                exitTransition = slideOutHorizontally(
                    animationSpec = settleOffset,
                    targetOffsetX = { -it / 4 }
                ) + fadeOut(
                    animationSpec = settleFloat,
                    targetAlpha = 0.9f
                ),
                popEnterTransition = slideInHorizontally(
                    animationSpec = settleOffset,
                    initialOffsetX = { -it / 4 }
                ) + fadeIn(
                    animationSpec = settleFloat,
                    initialAlpha = 0.9f
                ),
                popExitTransition = slideOutHorizontally(
                    animationSpec = settleOffset,
                    targetOffsetX = { it }
                ),
            )
        }

        /** Duration of Miuix's programmatic page settle, in ms (`NavDriverSpec`). */
        private const val MiuixProgrammaticDurationMillis = 500

        /**
         * The step response of an underdamped spring baked into an [Easing], ported verbatim from
         * Miuix's `NavSettleEasing` (compose-miuix-ui v0.9.4, Apache-2.0).
         *
         * Reproduces, point for point, the established enter/exit curve of Miuix navigation, so
         * programmatic push/pop keeps the exact same pacing: a brisk middle (≈48% at a fifth of the
         * duration) and a long, gentle tail. The curve is monotone within `0..1` for the shipped
         * parameters (its first crossing of `1` lies past the played window), so it never bounces;
         * `transform(1f)` lands at ≈0.99933 and the tween's final frame snaps the residue.
         *
         * Baking the spring solution into an easing (instead of running a live spring) is what pins
         * the total duration: a live spring's settle time scales with distance and threshold, while
         * a tween always completes in exactly its configured duration.
         *
         * @param response oscillation period of the underlying spring, in units of the played duration.
         * @param damping damping ratio of the underlying spring (`< 1`, underdamped).
         */
        private class NavSettleEasing(
            response: Float = 0.8f,
            damping: Float = 0.95f,
        ) : Easing {
            private val w: Float
            private val r: Float
            private val c2: Float

            init {
                val omega = 2.0 * PI / response
                val k = omega * omega
                val c = damping * 4.0 * PI / response

                w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
                r = (-c / 2.0).toFloat()
                c2 = r / w
            }

            override fun transform(fraction: Float): Float {
                val t = fraction.toDouble()
                val decay = exp(r * t)
                return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
            }
        }

        /**
         * The reduced-motion fallback: a gentle slide with parallax but no full-width travel. Kept
         * as-is — `MiuixDefault` is itself a full-screen slide, which is exactly the kind of large
         * movement reduced motion asks to tone down.
         */
        val SlideTransitions: NavTransitions by unsafeLazy {
            NavTransitions(
                enterTransition = slideInHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    initialOffsetX = { it }
                ),
                exitTransition = slideOutHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    targetOffsetX = { -it / 8 }
                ) + scaleOut(
                    animationSpec = defaultAnimationSpec(),
                    targetScale = 0.9f
                ),
                popEnterTransition = slideInHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    initialOffsetX = { -it / 8 }
                ) + scaleIn(
                    animationSpec = defaultAnimationSpec(),
                    initialScale = 0.9f
                ),
                popExitTransition = slideOutHorizontally(
                    animationSpec = defaultAnimationSpec(),
                    targetOffsetX = { it }
                ) + fadeOut(
                    animationSpec = tween(delayMillis = 100)
                )
            )
        }
    }
}
