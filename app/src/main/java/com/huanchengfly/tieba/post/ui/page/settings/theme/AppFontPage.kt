package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.FloatProducer
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.miuix.SquircleCornerRadius
import com.huanchengfly.tieba.post.ui.widgets.compose.MiuixBackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.NegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val FONT_SCALE_MIN = 0.8f
private const val FONT_SCALE_MAX = 1.6f // Maximum 2.0f
private const val FONT_SCALE_STEP = FONT_SCALE_MAX - FONT_SCALE_MIN

/** Slider progress domain; see [Float.toProgress] and [Float.toFontScale]. */
private const val FONT_SCALE_PROGRESS_MAX = 10f

/** Amount of intervals between the smallest and largest font scale, i.e. 8 intervals / 9 sizes. */
private const val FONT_SCALE_INTERVALS = 8

/**
 * The selectable font sizes as slider positions, evenly spread over 0f..[FONT_SCALE_PROGRESS_MAX].
 *
 * These are passed as Miuix [Slider] key points so the slider shows one indicator per available size.
 */
private val FontScaleKeyPoints: List<Float> =
    List(FONT_SCALE_INTERVALS + 1) { it * FONT_SCALE_PROGRESS_MAX / FONT_SCALE_INTERVALS }

// Progress to font scale
private fun Float.toFontScale() = FONT_SCALE_MIN + (this * FONT_SCALE_STEP).fastRoundToInt() / 10f // Round to one decimal

// Font scale to progress
private fun Float.toProgress() = ((this - FONT_SCALE_MIN) * 10).fastRoundToInt() / FONT_SCALE_STEP

private fun getSizeTextHint(sliderPosition: Float): Int {
    return when (sliderPosition.fastRoundToInt()) {
        0 -> R.string.text_size_small
        in 1..2 -> R.string.text_size_little_small
        3 -> R.string.text_size_default
        in 4..6 -> R.string.text_size_little_large
        in 7..8 -> R.string.text_size_large
        else -> R.string.text_size_very_large
    }
}

@Composable
fun AppFontPage(
    onBack: () -> Unit,
    /** 面板模式：平板设置页的右栏自己画标题栏，本页不再画自己的顶栏。 */
    paneMode: Boolean = false,
    vm: AppFontViewModel = hiltViewModel(),
) {
    MiuixScaffold(
        topBar = {
            if (!paneMode) {
                // Miuix large title bar: the enlarged title matches the HyperOS sub-page header
                MiuixTopAppBar(
                    title = stringResource(id = R.string.title_custom_font_size),
                    navigationIcon = { MiuixBackNavigationIcon(onBackPressed = onBack) },
                )
            }
        },
    ) { paddingValues ->
        val context = LocalContext.current
        val fontScale by vm.fontScale.collectAsStateWithLifecycle()
        val fontScaleChanged by vm.fontScaleChanged.collectAsStateWithLifecycle()

        AppFontContent(
            modifier = Modifier.padding(paddingValues),
            fontScale = fontScale,
            onFontScaleChanged = vm::onFontScaleChanged,
            isFontScaleChanged = fontScaleChanged,
            onCancel = onBack,
            onSave = {
                vm.onSave()
                context.toastShort(R.string.toast_after_change_will_restart)
                (context.applicationContext as App).removeAllActivity()
            }
        )
    }
}

@Composable
private fun AppFontContent(
    modifier: Modifier = Modifier,
    fontScale: Float,
    onFontScaleChanged: (Float) -> Unit,
    isFontScaleChanged: Boolean,
    onCancel: () -> Unit = {},
    onSave: () -> Unit  = {}
) {
    if (fontScale < 0f) return // Initializing

    val chatTextStyle = MaterialTheme.typography.bodyLarge
    val fontSize = chatTextStyle.fontSize / LocalDensity.current.fontScale * fontScale

    Column(modifier.padding(18.dp)) {
        ProvideTextStyle(chatTextStyle) {
            ChatBubbleText(
                modifier = Modifier.align(Alignment.End),
                text = stringResource(id = R.string.bubble_want_change_font_size),
                fontSize = fontSize
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChatBubbleText(
                modifier = modifier.padding(end = 32.dp),
                text = stringResource(id = R.string.bubble_change_font_size),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                fontSize = fontSize
            )
        }

        Spacer(modifier = Modifier.weight(1.0f))

        TextHintSlider(defaultProgress = { fontScale.toProgress() }) {
            onFontScaleChanged(it.toFontScale())
        }

        Row(modifier = Modifier.padding(top = 12.dp)) {

            NegativeButton(text = stringResource(id = R.string.button_cancel), onClick = onCancel)

            Spacer(modifier = Modifier.weight(1.0f))

            PositiveButton(
                text = stringResource(id = R.string.button_finish),
                enabled = isFontScaleChanged,
                onClick = onSave
            )
        }
    }
}

@Composable
private fun TextHintSlider(
    modifier: Modifier = Modifier,
    defaultProgress: FloatProducer,
    onProgressChanged: (Float) -> Unit
) {
    var progress by remember { mutableFloatStateOf(defaultProgress()) }
    val hintText by remember { derivedStateOf { getSizeTextHint(progress) } }

    Column(modifier = modifier) {
        Text(
            text = stringResource(id = hintText),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MiuixTheme.colorScheme.primary,
            style = MiuixTheme.textStyles.title3,
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Miuix slider with one key point per selectable font size, so the available sizes are
        // visible on the track instead of only being discoverable by dragging.
        Slider(
            value = progress,
            onValueChange = {
                if (abs(it - progress) > 0.01f) {
                    onProgressChanged(it)
                }
                progress = it
            },
            valueRange = 0f..FONT_SCALE_PROGRESS_MAX,
            steps = FONT_SCALE_INTERVALS - 1,
            showKeyPoints = true,
            keyPoints = FontScaleKeyPoints,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        )
    }
}

@Composable
private fun ChatBubbleText(
    modifier: Modifier = Modifier,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    fontSize: TextUnit
) {
    Text(
        text = text,
        modifier = modifier
            .squircleBackground(containerColor, SquircleCornerRadius.Medium)
            .padding(8.dp),
        color = contentColor,
        fontSize = fontSize,
        lineHeight = fontSize * 1.2f
    )
}

@Preview
@Composable
private fun AppFontContentPreview() {
    TiebaLiteTheme {
        AppFontContent(fontScale = 1.2f, onFontScaleChanged = {}, isFontScaleChanged = false)
    }
}
