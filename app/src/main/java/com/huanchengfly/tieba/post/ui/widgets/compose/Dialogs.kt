package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowLandscape
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.utils.HmTime
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

// Miuix DialogDefaults.insideMargin
val DefaultDialogContentPadding = 24.dp
// Miuix DialogDefaults.outsideMargin
val DefaultDialogMargin = 12.dp

val DefaultDialogProperties = AnyPopDialogProperties(
    direction = DirectionState.CENTER,
    dismissOnBackPress = true,
    dismissOnClickOutside = true,
    imePadding = true
)

val dialogAdaptiveFraction: Float
    @ReadOnlyComposable @Composable get() = if (isWindowWidthCompact()) 0.95f else 0.6f

/**
 * Landscape dialogs use MIUI's wide form: a little narrower than the full-width portrait dialog so
 * the body does not stretch into unreadable line lengths, but wide enough to host two columns.
 */
private const val DialogLandscapeWidthFraction = 0.72f

/**
 * Hard cap for the landscape card. A landscape phone is only ~330.dp tall, so an uncapped body
 * would run off the top and bottom of the screen.
 */
private val dialogLandscapeMaxHeight: Dp
    @ReadOnlyComposable @Composable get() = (LocalConfiguration.current.screenHeightDp * 0.72f).dp

/** Miuix `DialogDefaults.MaxWidth`; also the cap that keeps the landscape card from spanning a tablet. */
private val DialogLandscapeMaxWidth = 420.dp

/**
 * No-op [RowScope] used to host the [AlertDialog]/[Dialog] `buttons` lambda — its signature is
 * `RowScope`-typed, but MIUI stacks actions vertically, so a Column needs a receiver that never
 * applies weight or alignment.
 */
private val DialogButtonRowScope = object : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this
    override fun Modifier.alignByBaseline(): Modifier = this
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}

@Composable
fun DialogScope.DialogPositiveButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = {
            onClick()
            dismiss()
        },
        modifier = modifier,
        enabled = enabled,
        // MIUI dialog: the confirming action is the solid accent pill.
        cornerRadius = Sizes.CornerRadius,
        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.primary),
        content = { Text(text = text, maxLines = 1) }
    )
}

@Composable
fun DialogScope.DialogNegativeButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    text: String,
    onClick: (() -> Unit)? = null
) {
    Button(
        onClick = {
            dismiss()
            onClick?.invoke()
        },
        modifier = modifier,
        // MIUI dialog: secondary actions are solid neutral pills, not text links. Miuix
        // buttonColors() defaults to the secondaryVariant grey, which is exactly that look.
        cornerRadius = Sizes.CornerRadius,
        colors = ButtonDefaults.buttonColors(),
        content = { Text(text = text, maxLines = 1) }
    )
}

@Composable
fun TimePickerDialog(
    modifier: Modifier = Modifier,
    initialTime: HmTime,
    title: @Composable (() -> Unit)? = null,
    onConfirm: (TimePickerState) -> Unit,
    dialogState: DialogState = rememberDialogState(),
    onCancel: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure),
    cancelText: String = stringResource(id = R.string.button_cancel),
) {
    val isWindowHeightCompact = isWindowHeightCompact()
    var pickerStyle by remember { mutableStateOf(!isWindowHeightCompact) }
    val timePickerState = rememberTimePickerState(initialTime.hourOfDay, initialTime.minute)

    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onCancel,
        title = title.takeUnless { isWindowHeightCompact },
        buttons = {
            IconButton(onClick = { pickerStyle = !pickerStyle }) {
                Icon(
                    imageVector = if (pickerStyle) Icons.Filled.EditCalendar else Icons.Filled.AccessTime,
                    contentDescription = "Time picker type",
                )
            }

            with(it)  { Spacer(modifier = Modifier.weight(1.0f)) }

            DialogNegativeButton(text = cancelText, onClick = onCancel)
            DialogPositiveButton(text = confirmText) {
                onConfirm(timePickerState)
            }
        },
    ) {
        if (pickerStyle) {
            TimePicker(state = timePickerState)
        } else {
            TimeInput(state = timePickerState)
        }

        LaunchedEffect(isWindowHeightCompact) {
            pickerStyle = !isWindowHeightCompact
        }
    }
}

@Composable
fun AlertDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState,
    dialogProperties: AnyPopDialogProperties = DefaultDialogProperties,
    onDismiss: (() -> Unit)? = null,
    title: @Composable () -> Unit = {},
    buttons: @Composable (DialogScope.(RowScope) -> Unit) = {},
    content: @Composable (DialogScope.(ColumnScope) -> Unit) = {},
) {
    BaseDialog(
        modifier = modifier,
        dialogState = dialogState,
        dialogProperties = dialogProperties,
        onDismiss = onDismiss,
    ) {
        // MIUI dialog surface. Miuix Card supplies the 16.dp squircle and a flat fill; the previous
        // Material Surface(tonalElevation = 6.dp) tinted its own background with `surfaceTint`,
        // which in this app is the Miuix primary blue — the source of the blue-cast dialogs.
        Card(
            modifier = Modifier
                .fillMaxWidth(fraction = dialogAdaptiveFraction)
                .padding(DefaultDialogMargin),
            // Miuix DialogDefaults.CornerRadius = 32.dp, deliberately larger than the 16.dp used
            // by cards and buttons.
            cornerRadius = Sizes.DialogCornerRadius,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.background),
        ) {
            // Same reason as Dialog: Card's Column does not propagate a minimum width, so the content
            // has to fill explicitly or it collapses to its intrinsic width.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DefaultDialogContentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProvideTextStyle(MiuixTheme.textStyles.title2, title)
                Spacer(modifier = Modifier.height(DefaultDialogContentPadding))

                ProvideTextStyle(MiuixTheme.textStyles.body1) {
                    content(this)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DefaultDialogContentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End)
                ) {
                    buttons(this)
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    dialogState: DialogState,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure),
    cancelText: String = stringResource(id = R.string.button_cancel),
    title: @Composable (() -> Unit)? = null,
    content: @Composable (DialogScope.() -> Unit)? = null,
) {
    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onDismiss,
        title = title,
        buttons = {
            DialogNegativeButton(text = cancelText, onClick = onCancel)
            DialogPositiveButton(text = confirmText, onClick = onConfirm)
        },
    ) {
        if (content != null)  {
            content()
        }
    }
}

/**
 * 带输入框的对话框
 *
 * @param onValueChange 输入框内容变化时的回调，返回true表示允许变化，false表示不允许变化
 */
@Composable
fun PromptDialog(
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    initialValue: String = "",
    onValueChange: (newVal: String, oldVal: String) -> Boolean = { _, _ -> true },
    isError: ((String) -> Boolean)? = null,
    onCancel: (() -> Unit)? = null,
    confirmText: String = stringResource(id = R.string.button_sure),
    cancelText: String = stringResource(id = R.string.button_cancel),
    title: @Composable (() -> Unit)? = null,
    content: @Composable (DialogScope.() -> Unit) = {},
) {
    var textVal by remember { mutableStateOf(initialValue) }
    val isErrorState by remember { derivedStateOf { isError?.invoke(textVal) == true } }

    // 每次显示时重置输入框内容
    LaunchedEffect(dialogState.show) {
        textVal = initialValue
    }

    Dialog(
        modifier = modifier,
        dialogState = dialogState,
        onDismiss = onCancel,
        title = title,
        buttons = {
            DialogNegativeButton(text = cancelText, onClick = onCancel)
            DialogPositiveButton(text = confirmText, enabled = !isErrorState) {
                onConfirm(textVal)
            }
        },
    ) {
        val focusRequester = remember { FocusRequester() }
        val softwareKeyboardController = LocalSoftwareKeyboardController.current
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()

            // miuix 的输入框：16dp 圆角 squircle 底 + 聚焦时 2dp 主色描边（Material 的
            // OutlinedTextField 是直角描边框，和这个对话框的圆角不搭）。
            // 非法页码沿用 error 语义：errorContainer 底 + error 描边。
            TextField(
                value = textVal,
                onValueChange = {
                    if (onValueChange.invoke(it, textVal)) textVal = it
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        softwareKeyboardController?.hide()
                        if (!isErrorState) {
                            dismiss()
                            onConfirm(textVal)
                        }
                    }
                ),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth(),
                colors = if (isErrorState) {
                    TextFieldDefaults.textFieldColors(
                        backgroundColor = MiuixTheme.colorScheme.errorContainer,
                        labelColor = MiuixTheme.colorScheme.onErrorContainer,
                        borderColor = MiuixTheme.colorScheme.error,
                    )
                } else {
                    TextFieldDefaults.textFieldColors()
                },
            )
            LaunchedEffect(focusRequester) {
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun BaseDialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    dialogProperties: AnyPopDialogProperties = DefaultDialogProperties,
    onDismiss: (() -> Unit)? = null,
    content: @Composable DialogScope.() -> Unit,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }
    var isActiveClose by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(dialogState.show) {
        if (dialogState.show) {
            showDialog = true
            isActiveClose = false
        } else {
            isActiveClose = true
        }
    }
    if (showDialog) {
        val dialogScope = DialogScope(
            onDismiss = {
                isActiveClose = true
            },
        )
        AnyPopDialog(
            onDismiss = {
                onDismiss?.invoke()
                dialogState.show = false
                showDialog = false
            },
            modifier = modifier,
            isActiveClose = isActiveClose,
            properties = dialogProperties
        ) {
            dialogScope.content()
        }
    }
}

@Composable
fun Dialog(
    modifier: Modifier = Modifier,
    dialogState: DialogState = rememberDialogState(),
    dialogProperties: AnyPopDialogProperties = DefaultDialogProperties,
    onDismiss: (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    buttons: @Composable (DialogScope.(RowScope) -> Unit) = {},
    content: @Composable (DialogScope.(ColumnScope) -> Unit),
) {
    // Orientation is read through Compose so a rotation recomposes this branch in place; no
    // activity restart and no manual lifecycle wiring needed.
    val isLandscape = isWindowLandscape()
    BaseDialog(
        modifier = modifier,
        dialogState = dialogState,
        dialogProperties = dialogProperties,
        onDismiss = onDismiss,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(
                    fraction = if (isLandscape) DialogLandscapeWidthFraction else dialogAdaptiveFraction
                )
                .padding(DefaultDialogMargin)
                // Only the landscape branch is height-capped; portrait keeps its natural height.
                .heightIn(max = if (isLandscape) dialogLandscapeMaxHeight else Dp.Unspecified)
                .widthIn(max = DialogLandscapeMaxWidth),
            // Miuix DialogDefaults.CornerRadius = 32.dp, deliberately larger than the 16.dp used
            // by cards and buttons.
            cornerRadius = Sizes.DialogCornerRadius,
            colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.background),
        ) {
            if (isLandscape) {
                // MIUI's wide/landscape form: text on the left, actions on the right, split by a
                // hairline. Portrait keeps the stacked layout below untouched.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(
                            horizontal = DefaultDialogContentPadding,
                            vertical = DefaultDialogContentPadding,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = DefaultDialogContentPadding)
                            // The height cap above makes this the scrolling half.
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (title != null) {
                            ProvideTextStyle(MiuixTheme.textStyles.title3, content = title)
                        }

                        ProvideTextStyle(MiuixTheme.textStyles.body1) {
                            content(this)
                        }
                    }

                    // Hairline between the two columns; IntrinsicSize.Min on the Row lets it span
                    // whichever column is taller.
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            // Dp.Hairline at 15% alpha was invisible on the white surface; MIUI's
                            // wide dialog shows a clear hairline between the columns.
                            .width(1.dp)
                            .background(MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.25f))
                    )

                    // Actions stack vertically (MIUI wide-dialog convention) and size to the
                    // widest button, so the text column takes the remaining width instead of both
                    // columns fighting over a fixed 45/42 split.
                    Column(
                        // Equal share with the text column so the full-width buttons below get the
                        // same visual weight as the copy, matching MIUI's wide dialog.
                        // start padding: without it the full-width buttons ran straight into the
                        // hairline, which is the gap that read as broken in the wide dialog.
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = DefaultDialogContentPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        buttons(DialogButtonRowScope)
                    }
                }
            } else {
            // fillMaxWidth is required, not cosmetic: Miuix Card hosts its content in a Column, which
            // measures children with minWidth = 0. Without it ConstraintLayout shrinks to its
            // intrinsic width, the fillToConstraints title column collapses and the dialog renders
            // as one character per line.
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DefaultDialogContentPadding)
                    .animateContentSize(),
            ) {
                val (titleRef, buttonsRef) = createRefs()

                // To make buttons visually aligned, apply horizontal paddings separately
                Column(
                    modifier = Modifier
                        .constrainAs(titleRef) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start, margin = DefaultDialogContentPadding)
                            end.linkTo(parent.end, margin = DefaultDialogContentPadding)
                            bottom.linkTo(buttonsRef.top)
                            width = Dimension.fillToConstraints
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (title != null) {
                        ProvideTextStyle(MiuixTheme.textStyles.title3, content = title)
                    }

                    ProvideTextStyle(MiuixTheme.textStyles.body1) {
                        content(this)
                    }
                }

                Column(
                    modifier = Modifier
                        .constrainAs(buttonsRef) {
                            start.linkTo(parent.start, margin = DefaultDialogContentPadding)
                            end.linkTo(parent.end, margin = DefaultDialogContentPadding)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(titleRef.bottom, margin = 16.dp)
                            width = Dimension.fillToConstraints
                        },
                    // MIUI stacks dialog actions vertically, full width, with a 16.dp gap.
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    buttons(DialogButtonRowScope)
                }
            }
            }
        }
    }
}

@Composable
fun rememberDialogState(): DialogState {
    return rememberSaveable(saver = DialogState.Saver) {
        DialogState()
    }
}

@Stable
class DialogState private constructor(
    show: Boolean
) {
    constructor() : this(show = false)

    var show by mutableStateOf(show)

    fun show() {
        show = true
    }

    companion object {
        val Saver: Saver<DialogState, *> = listSaver(
            save = {
                listOf<Any>(
                    it.show,
                )
            },
            restore = {
                DialogState(
                    show = it[0] as Boolean
                )
            }
        )
    }
}

class DialogScope(
    private val onDismiss: () -> Unit,
) {
    fun dismiss() {
        onDismiss()
    }
}