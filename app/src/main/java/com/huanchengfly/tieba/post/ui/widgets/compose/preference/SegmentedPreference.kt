package com.huanchengfly.tieba.post.ui.widgets.compose.preference

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

val PreferenceItemPaddingCompact = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

val PreferenceItemPadding = PaddingValues(16.dp)

/**
 * Container colors of the Material `SegmentedListItem` rows still used by the list pages
 * (history, block list, follow list) until they are ported to Miuix.
 */
val SegmentedListItemColors: androidx.compose.material3.ListItemColors
    @Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.run {
        androidx.compose.material3.ListItemDefaults.segmentedColors(
            containerColor = secondaryContainer.copy(alpha = 0.4f),
            disabledContainerColor = secondaryContainer.copy(0.28f),
        )
    }

/**
 * Position of a preference row inside its group.
 *
 * Consecutive rows of one group join into a single continuous HyperOS card: only the first row of
 * a group rounds its top corners and only the last row rounds its bottom corners, while the rows
 * in between stay square. Miuix does not draw separators between the rows of one card.
 */
enum class PreferenceGroupPosition {
    Single,
    First,
    Middle,
    Last;

    /** `true` for the first row of a group, including a group that holds a single row. */
    val isFirstRow: Boolean get() = this == Single || this == First

    /** `true` for the last row of a group, including a group that holds a single row. */
    val isLastRow: Boolean get() = this == Single || this == Last
}

/**
 * The group-card material of one preference row, drawn with miuix-squircle's continuous corners.
 *
 * Rounds only the outer corners of the group (see [PreferenceGroupPosition]) so consecutive rows
 * read as one continuous card. Replaces the old `clip(preferenceRowShape(position)).background(...)`
 * pair: the squircle module has no `Shape`, so the per-corner `squircleSurface` — fill *and* clip in
 * one node — is the equivalent, and it keeps the row's ripple inside the rounded silhouette.
 *
 * Zero-radius corners fall through to the module's own `RoundedCornerShape` fallback, which is how
 * the middle rows stay square.
 *
 * @param containerColor the row's fill; pass [Color.Transparent] when a glass layer (e.g.
 *   `textureBlur`) supplies the material instead.
 */
@Composable
fun Modifier.preferenceRowSurface(
    position: PreferenceGroupPosition,
    containerColor: Color,
): Modifier {
    val radius = CardDefaults.CornerRadius
    val topRadius = if (position.isFirstRow) radius else 0.dp
    val bottomRadius = if (position.isLastRow) radius else 0.dp
    return squircleSurface(
        color = containerColor,
        topStart = topRadius,
        topEnd = topRadius,
        bottomEnd = bottomRadius,
        bottomStart = bottomRadius,
    )
}

/**
 * HyperOS style preference row: a Miuix [BasicComponent] painted on the group card surface.
 *
 * Rows belonging to the same group are laid out back to back and only round their outer corners,
 * so a group reads as one continuous card instead of one floating card per row.
 */
@NonRestartableComposable
@Composable
fun SegmentedPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    position: PreferenceGroupPosition = PreferenceGroupPosition.Single,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = if (summary == null) {
        PreferenceItemPadding
    } else {
        PreferenceItemPaddingCompact
    },
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
) {
    BasicComponent(
        modifier = modifier.preferenceRowSurface(position, containerColor),
        startAction = leadingIcon,
        endActions = { trailingContent?.invoke() },
        insideMargin = contentPadding,
        onClick = onClick.takeIf { enabled },
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        PreferenceTitle(enabled = enabled, content = title)
        summary?.let { PreferenceSummary(enabled = enabled, content = it) }
    }
}

/** Applies the Miuix [BasicComponent] title role to the caller supplied title content. */
@Composable
private fun PreferenceTitle(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides if (enabled) {
            MiuixTheme.colorScheme.onBackground
        } else {
            MiuixTheme.colorScheme.disabledOnSecondaryVariant
        },
        LocalTextStyle provides MiuixTheme.textStyles.headline1.copy(fontWeight = FontWeight.Medium),
        content = content,
    )
}

/** Applies the Miuix [BasicComponent] summary role to the caller supplied summary content. */
@Composable
private fun PreferenceSummary(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalContentColor provides if (enabled) {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        } else {
            MiuixTheme.colorScheme.disabledOnSecondaryVariant
        },
        LocalTextStyle provides MiuixTheme.textStyles.body2,
        content = content,
    )
}

/** Tint of a leading icon inside a preference row. */
@Composable
internal fun preferenceIconTint(enabled: Boolean): Color = if (enabled) {
    MiuixTheme.colorScheme.onBackground
} else {
    MiuixTheme.colorScheme.disabledOnSecondaryVariant
}

@Composable
fun SegmentedPreference(
    modifier: Modifier = Modifier,
    title: String,
    position: PreferenceGroupPosition = PreferenceGroupPosition.Single,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    summary: String? = null,
    contentPadding: PaddingValues = if (summary == null) {
        PreferenceItemPadding
    } else {
        PreferenceItemPaddingCompact
    },
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
) =
    SegmentedPreference(
        modifier = modifier,
        title = {
            Text(text = title)
        },
        position = position,
        leadingIcon = leadingIcon?.let { imageVector ->
            {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = preferenceIconTint(enabled),
                )
            }
        },
        trailingContent = trailingContent,
        summary = summary?.let { { Text(text = it) } },
        contentPadding = contentPadding,
        containerColor = containerColor,
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = onClick,
    )

@Composable
fun SegmentedPreference(
    modifier: Modifier = Modifier,
    @StringRes title: Int,
    position: PreferenceGroupPosition = PreferenceGroupPosition.Single,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    @StringRes summary: Int? = null,
    contentPadding: PaddingValues = if (summary == null) {
        PreferenceItemPadding
    } else {
        PreferenceItemPaddingCompact
    },
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
) =
    SegmentedPreference(
        modifier = modifier,
        title = {
            Text(text = stringResource(id = title))
        },
        position = position,
        leadingIcon = leadingIcon?.let { imageVector ->
            {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = preferenceIconTint(enabled),
                )
            }
        },
        trailingContent = trailingContent,
        summary = summary?.let {
            { Text(text = stringResource(id = it)) }
        },
        contentPadding = contentPadding,
        containerColor = containerColor,
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = onClick,
    )
