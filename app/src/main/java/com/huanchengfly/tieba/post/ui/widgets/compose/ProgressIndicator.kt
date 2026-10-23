package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults

/**
 * Full-width linear progress indicator backed by Miuix [top.yukonga.miuix.kmp.basic.LinearProgressIndicator].
 *
 * @param progress the progress of this progress indicator, where 0.0 represents no progress and 1.0
 *   represents full progress. Values outside of this range are coerced into the range.
 * @param modifier the [Modifier] to be applied to this progress indicator
 * @param color color of this progress indicator
 * @param trackColor color of the track behind the indicator
 * @param strokeCap accepted for call-site compatibility; Miuix uses a rounded cap
 */
@NonRestartableComposable
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary,
    trackColor: Color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.secondaryContainer,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    top.yukonga.miuix.kmp.basic.LinearProgressIndicator(
        progress = progress(),
        modifier = modifier.fillMaxWidth(),
        colors = ProgressIndicatorDefaults.progressIndicatorColors(
            foregroundColor = color,
            backgroundColor = trackColor,
        ),
    )
}
