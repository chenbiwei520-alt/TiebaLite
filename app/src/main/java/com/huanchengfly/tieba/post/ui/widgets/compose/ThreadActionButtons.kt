package com.huanchengfly.tieba.post.ui.widgets.compose

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.theme.LikeRed
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.Reply
import top.yukonga.miuix.kmp.icon.extended.Share

@NonRestartableComposable
@Composable
private fun ActionBtn(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .onNotNull(onClick) { clickable(onClick = it) }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )

        Text(text, color = contentColor, style = MaterialTheme.typography.bodySmall)
    }
}

private fun Context.shortNumString(number: Long, @StringRes defaultRes: Int): String {
    return if (number <= 0) {
        getString(defaultRes)
    } else if (number <= 999) {
        number.toString()
    } else {
        number.getShortNumString()
    }
}

@Composable
fun ThreadActionButtonRow(
    modifier: Modifier = Modifier,
    shares: Long,
    replies: Int,
    likes: Long,
    liked: Boolean,
    onShareClicked: (() -> Unit)? = null,
    onReplyClicked: (() -> Unit)? = null,
    onAgreeClicked: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        ActionBtn(
            modifier = Modifier.weight(1f),
            text = context.shortNumString(shares, R.string.title_share),
            icon = MiuixIcons.Share,
            contentDescription = stringResource(id = R.string.title_share),
            onClick = onShareClicked,
        )

        ActionBtn(
            modifier = Modifier.weight(1f),
            text = context.shortNumString(replies.toLong(), R.string.title_reply),
            icon = MiuixIcons.Reply,
            contentDescription = stringResource(id = R.string.desc_comment),
            onClick = onReplyClicked
        )

        ActionBtn(
            modifier = Modifier.weight(1f),
            icon = if (liked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
            contentColor = if (liked) {
                // 点亮的爱心固定是红的，跟主题色无关（默认主题的 primary 是蓝的）。
                LikeRed
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            contentDescription = stringResource(id = R.string.button_like),
            text = context.shortNumString(likes, R.string.button_like),
            onClick = onAgreeClicked
        )
    }
}

@Preview
@Composable
private fun ThreadActionButtonRowPreview() = TiebaLiteTheme {
    ThreadActionButtonRow(shares = 9, replies = 999, likes = 99999, liked = true)
}
