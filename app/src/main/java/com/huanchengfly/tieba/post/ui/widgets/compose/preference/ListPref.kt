package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.huanchengfly.tieba.post.ui.miuix.LiquidGlassDropdownPreference
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Preference that shows the current value of a list of entries and opens a Miuix list menu where a
 * single entry can be selected at one time.
 *
 * Built on [LiquidGlassDropdownPreference], so the row shows the selected option as its trailing value
 * and the options open in a Miuix dropdown popup. The popup needs a Miuix `Scaffold` ancestor to
 * host the overlay.
 *
 * The row paints the same group card surface as [SegmentedPreference] and rounds only its outer
 * corners, so it stays part of the surrounding HyperOS group card instead of falling back to the
 * page background.
 *
 * @param modifier the [Modifier] to be applied on this preference.
 * @param value current selected value of this preference.
 * @param onValueChange callback to be invoked when user selected new option in [options]
 * @param options all available options of this preference with description
 * @param title text which describes this preference.
 * @param summary used to give some more information about what this preference is for.
 * @param useSelectedAsSummary when `true` the current value is displayed on the trailing side of
 *   the row instead of [summary].
 * @param optionsIconSupplier leading icon to be drawn at the beginning of option menu item.
 * @param leadingIcon leading icon to be drawn at the beginning of the preference, typically [Icon].
 * @param position where this row sits inside its group, see [PreferenceGroupPosition].
 * @param containerColor the surface this row is painted on.
 * @param enabled controls the enabled state of this list pref. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled.
 */
@Composable
fun <T> SegmentedListPreference(
    modifier: Modifier = Modifier,
    value: T,
    onValueChange: (T) -> Unit,
    options: StringLabelOptions<T>,
    title: String,
    summary: String? = null,
    useSelectedAsSummary: Boolean = summary == null,
    optionsIconSupplier: (@Composable (T) -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    position: PreferenceGroupPosition = PreferenceGroupPosition.Single,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
) {
    val items = options.entries.map { (option, optionLabel) ->
        DropdownItem(
            text = optionLabel,
            selected = option == value,
            onClick = { onValueChange(option) },
            icon = optionsIconSupplier?.let { supplier -> optionIcon(supplier, option) },
        )
    }

    LiquidGlassDropdownPreference(
        modifier = modifier.preferenceRowSurface(position, containerColor),
        entries = listOf(DropdownEntry(items = items)),
        title = title,
        summary = summary.takeUnless { useSelectedAsSummary },
        startAction = leadingIcon?.let { imageVector ->
            {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = preferenceIconTint(enabled),
                )
            }
        },
        enabled = enabled,
        showValue = useSelectedAsSummary,
    )
}

/** Wraps an option icon for [DropdownItem.icon], keeping the menu's leading icon slot. */
private fun <T> optionIcon(
    supplier: @Composable (T) -> Unit,
    option: T,
): @Composable (Modifier) -> Unit = { iconModifier ->
    Box(modifier = iconModifier.size(Sizes.Small)) {
        supplier(option)
    }
}
