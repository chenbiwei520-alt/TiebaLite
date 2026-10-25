package com.huanchengfly.tieba.post.ui.page.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.miuix.ShaderBlurProbe
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixPinnedScrollBehavior
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.CrashLogUtil
import com.huanchengfly.tieba.post.utils.FileUtil.createFileInCacheDir
import com.huanchengfly.tieba.post.utils.FileUtil.toSharedUri
import com.huanchengfly.tieba.post.utils.FileUtil.writeFile
import com.huanchengfly.tieba.post.utils.TiebaLog
import com.huanchengfly.tieba.post.utils.extension.toShareIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.TextButton

/** 界面只显示尾巴：四千行进 LazyColumn 也扛得住，但没必要。 */
private const val MaxPreviewLines = 2500

private const val RefreshIntervalMillis = 500L

/**
 * 运行日志页：边跑边看本进程的 logcat，出问题时一键导出成 txt 分享出来。
 *
 * 记录是进程级的（见 [TiebaLog]），离开本页不会停；「暂停」只影响日志，不影响应用。
 * */
@Composable
fun LogViewerPage(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()
    val scrollBehavior = rememberMiuixPinnedScrollBehavior()
    val listState = rememberLazyListState()

    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var recording by remember { mutableStateOf(TiebaLog.isRecording) }

    LaunchedEffect(Unit) {
        // 进页面就确保在记录：用户不用先去点「开始」再回去复现。
        TiebaLog.start()
        while (true) {
            // 先看当前是不是停在最底下，是才跟着滚；用户往上翻的时候不要把他拽下来。
            val wasAtBottom = !listState.canScrollForward
            val snapshot = TiebaLog.tail(MaxPreviewLines)
            lines = snapshot
            recording = TiebaLog.isRecording
            if (wasAtBottom && snapshot.isNotEmpty()) {
                listState.scrollToItem(snapshot.lastIndex)
            }
            delay(RefreshIntervalMillis)
        }
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_settings_log,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHostState = snackbarHostState,
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text(
                text = if (recording) {
                    stringResource(R.string.log_status_recording, lines.size)
                } else {
                    stringResource(R.string.log_status_paused, lines.size)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "AGSL blur: ${ShaderBlurProbe.verdictLabel}",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.summary_settings_log),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
            ) {
                items(count = lines.size) { index ->
                    Text(
                        text = lines[index],
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 1.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    text = stringResource(
                        if (recording) R.string.action_log_pause else R.string.action_log_resume
                    ),
                    onClick = {
                        if (TiebaLog.isRecording) TiebaLog.stop() else TiebaLog.start()
                        recording = TiebaLog.isRecording
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.action_log_clear),
                    onClick = {
                        TiebaLog.clear()
                        lines = emptyList()
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.action_log_copy),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("log", buildLogText()))
                        Toast.makeText(context, R.string.toast_log_copied, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.action_log_share),
                    onClick = {
                        coroutineScope.launch {
                            val shared = runCatching { shareLog(context) }
                            if (shared.isFailure) {
                                Toast.makeText(context, R.string.toast_log_share_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun buildLogText(): String = buildString {
    appendLine(CrashLogUtil.getDebugInfo())
    appendLine()
    TiebaLog.snapshot().forEach { appendLine(it) }
}

/** 写到缓存目录再分享；文件名带时间，方便连抓几次时区分。 */
private suspend fun shareLog(context: Context) = withContext(Dispatchers.IO) {
    val file = context.createFileInCacheDir("tiebalite_log_${System.currentTimeMillis()}.txt")
    check(writeFile(file, buildLogText(), false)) { "写日志文件失败" }
    withContext(Dispatchers.Main) {
        context.startActivity(file.toSharedUri(context).toShareIntent(context, "text/plain"))
    }
}
