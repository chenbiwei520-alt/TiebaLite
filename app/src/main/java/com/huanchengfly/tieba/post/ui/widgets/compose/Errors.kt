package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.NoConnectivityException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorCode
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.theme.ProvideContentColorTextStyle
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowHeightCompact
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isWindowWidthCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreenScope
import com.huanchengfly.tieba.post.utils.SofireException

@Composable
fun TipScreen(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    image: (@Composable () -> Unit)? = null,
    message: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val widthFraction = if (isWindowWidthCompact() || isWindowHeightCompact()) 0.9f else 0.5f

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(fraction = widthFraction)
            .padding(horizontal = 16.dp)
            .onCase(scrollable) { verticalScroll(rememberScrollState()) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically)
    ) {
        if (image != null) {
            Box(modifier = Modifier.requiredWidthIn(max = 400.dp)) {
                image()
            }
        }
        // MIUI type ramp: title2 for the headline, body2 dimmed for the supporting line.
        ProvideContentColorTextStyle(
            contentColor = MiuixTheme.colorScheme.onBackground,
            textStyle = MiuixTheme.textStyles.title2,
            content = title
        )

        if (message != null) {
            ProvideContentColorTextStyle(
                contentColor = MiuixTheme.colorScheme.onBackgroundVariant,
                textStyle = MiuixTheme.textStyles.body2,
                content = message
            )
        }

        if (actions != null) {
            actions()
        }
    }
}

class ErrorType(
    @StringRes val title: Int,
    val message: String,
)

@Composable
fun StateScreenScope.ErrorScreen(
    error: Throwable?,
    modifier: Modifier = Modifier,
    showReload: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
) =
    ErrorTipScreen(
        error = error,
        modifier = modifier,
        actions = {
            if (showReload && canReload) {
                Button(
                    onClick = { this@ErrorScreen.reload() },
                    content = {
                        Text(
                            text = stringResource(id = R.string.button_retry),
                            style = MiuixTheme.textStyles.button,
                        )
                    },
                )
            }
            actions?.invoke()
        }
    )

@Composable
fun ErrorTipScreen(
    modifier: Modifier = Modifier,
    error: Throwable?,
    actions: (@Composable () -> Unit)?,
) {
    val context = LocalContext.current
    val errorType = remember { toKnownErrorType(context, error) }
    // Is unknown error, show stack trace
    if (errorType == null) {
        ErrorStackTraceScreen(modifier, error!!, actions)
        return
    }

    TipScreen(
        title = {
            Text(
                text = stringResource(id = errorType.title),
                style = MiuixTheme.textStyles.title3,
                textAlign = TextAlign.Center,
            )
        },
        modifier = modifier,
        message = {
            Text(
                text = errorType.message,
                textAlign = TextAlign.Center,
            )
        },
        actions = actions
    )
}

@Composable
fun ErrorStackTraceScreen(
    modifier: Modifier = Modifier,
    throwable: Throwable,
    actions: (@Composable () -> Unit)? = null
) {
    val stackTrace = remember(throwable) {
        runCatching { throwable.stackTraceToString() }.getOrNull()
    }

    ErrorStackTraceScreen(modifier, stackTrace, actions)
}

@Composable
fun ErrorStackTraceScreen(
    modifier: Modifier = Modifier,
    stackTrace: String?,
    actions: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(id = R.string.title_unknown_error),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = stackTrace ?: stringResource(id = R.string.message_unknown_error),
            modifier = Modifier
                .weight(1.0f)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
            style = MaterialTheme.typography.bodyLarge
        )

        actions?.invoke()
    }
}

// Returns null if error is unknown type
private fun toKnownErrorType(context: Context, err: Throwable?): ErrorType? {
    return when (err) {
        null -> ErrorType(R.string.title_unknown_error, context.getString(R.string.message_unknown_error))

        is NoConnectivityException -> ErrorType(
            title = R.string.title_no_internet_connectivity,
            message = context.getString(R.string.message_no_internet_connectivity, err.getErrorMessage()),
        )

        is TiebaApiException -> ErrorType(
            title = R.string.title_api_error,
            message = "${err.getErrorMessage()} Code: ${err.getErrorCode()}",
        )

        is TiebaNotLoggedInException -> ErrorType(
            title = R.string.title_not_logged_in,
            message = context.getString(R.string.message_not_logged_in),
        )

        is SofireException -> ErrorType(
            title = R.string.title_sofire_blocked,
            message = context.getString(R.string.message_sofire_blocked),
        )

        else -> null // Unknown Type
    }
}