// Palette for the flowing-light background.
//
// These are the upstream `BgEffectConfig` OS3 PHONE presets (compose-miuix-ui example
// `component/effect/BgEffectConfig.kt`, same source as HyperMusicCover) copied verbatim: deep-blue
// base with violet / azure / magenta lights in dark mode, pastel pink-blue in light mode — the
// exact look of the reference video. An earlier port derived the colours from the live
// `MiuixTheme` primary hue; that drifted away from the reference and is gone.

package com.huanchengfly.tieba.post.ui.miuix.effect

/**
 * The OS3 phone preset for the current mode — geometry, three colour stages and the drift timing,
 * all as shipped upstream.
 */
internal fun buildFlowLightPreset(isDark: Boolean): FlowLightPreset = if (isDark) {
    FlowLightPreset(
        points = OS3_PHONE_POINTS,
        colors1 = floatArrayOf(
            0.2f, 0.06f, 0.88f, 0.4f,
            0.3f, 0.14f, 0.55f, 0.5f,
            0.0f, 0.64f, 0.96f, 0.5f,
            0.11f, 0.16f, 0.83f, 0.4f,
        ),
        colors2 = floatArrayOf(
            0.07f, 0.15f, 0.79f, 0.5f,
            0.62f, 0.21f, 0.67f, 0.5f,
            0.06f, 0.25f, 0.84f, 0.5f,
            0.0f, 0.2f, 0.78f, 0.5f,
        ),
        colors3 = floatArrayOf(
            0.58f, 0.3f, 0.74f, 0.4f,
            0.27f, 0.18f, 0.6f, 0.5f,
            0.66f, 0.26f, 0.62f, 0.5f,
            0.12f, 0.16f, 0.7f, 0.6f,
        ),
        colorInterpPeriod = 8.0f,
        lightOffset = 0.0f,
        saturateOffset = 0.17f,
        pointOffset = 0.4f,
    )
} else {
    FlowLightPreset(
        points = OS3_PHONE_POINTS,
        colors1 = floatArrayOf(
            1.0f, 0.9f, 0.94f, 1.0f,
            1.0f, 0.84f, 0.89f, 1.0f,
            0.97f, 0.73f, 0.82f, 1.0f,
            0.64f, 0.65f, 0.98f, 1.0f,
        ),
        colors2 = floatArrayOf(
            0.58f, 0.74f, 1.0f, 1.0f,
            1.0f, 0.9f, 0.93f, 1.0f,
            0.74f, 0.76f, 1.0f, 1.0f,
            0.97f, 0.77f, 0.84f, 1.0f,
        ),
        colors3 = floatArrayOf(
            0.98f, 0.86f, 0.9f, 1.0f,
            0.6f, 0.73f, 0.98f, 1.0f,
            0.92f, 0.93f, 1.0f, 1.0f,
            0.56f, 0.69f, 1.0f, 1.0f,
        ),
        colorInterpPeriod = 5.0f,
        lightOffset = 0.1f,
        saturateOffset = 0.2f,
        pointOffset = 0.2f,
    )
}

/** Light-point layout of the OS3 phone presets — two lights near the top, two lower. */
private val OS3_PHONE_POINTS = floatArrayOf(
    0.8f, 0.2f, 1.0f,
    0.8f, 0.9f, 1.0f,
    0.2f, 0.9f, 1.0f,
    0.2f, 0.2f, 1.0f,
)
