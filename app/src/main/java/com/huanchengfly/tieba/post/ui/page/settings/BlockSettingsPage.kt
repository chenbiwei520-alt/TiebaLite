package com.huanchengfly.tieba.post.ui.page.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.collectUiEventWithLifecycle
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.ui.models.settings.BlockBackupMetadata
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.StrongBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeToDismissSnackbarHost
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.AnyPopDialogProperties
import com.huanchengfly.tieba.post.ui.widgets.compose.dialogs.DirectionState
import com.huanchengfly.tieba.post.ui.widgets.compose.preference.preference
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberSnackbarHostState
import com.huanchengfly.tieba.post.utils.BlockRuleBackupUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlockSettingsPage(
    settings: Settings<BlockSettings>,
    navigator: NavController,
    /** 面板模式：平板设置页的右栏自己画标题栏，本页不再画自己的顶栏。 */
    paneMode: Boolean = false,
    /** 面板模式下由外层接管二级页（平板两栏不跳转）；为 null 时保持原来的页面跳转。 */
    onOpenForumBlockList: (() -> Unit)? = null,
    onOpenUserBlockList: (() -> Unit)? = null,
    onOpenKeywordBlockList: (() -> Unit)? = null,
    viewModel: BlockSettingsViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = rememberSnackbarHostState()
    val blockRuleDialogState = rememberDialogState()

    // Backup/restore are ordinary preferences in the block-rule group now, so the two document
    // pickers live on the page instead of inside a dedicated button component.
    var backupSaveTimestamp by rememberSaveable { mutableLongStateOf(0) }
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/*")
    ) { uri ->
        if (uri != null) viewModel.onBackup(uri, backupSaveTimestamp)
        backupSaveTimestamp = 0
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onRestoreFilePicked(uri)
            blockRuleDialogState.show()
        }
    }

    viewModel.uiEvent.collectUiEventWithLifecycle {
        val message = when (it) {
            is BlockSettingsUiEvent.BadBackup -> getString(R.string.toast_bad_backup_rule)

            is BlockSettingsUiEvent.BackupFailed -> it.message

            is BlockSettingsUiEvent.BackupCompleted -> getString(R.string.toast_backup_done)

            else -> it.toString()
        }
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    SettingsScaffold(
        titleRes = R.string.title_block_settings,
        onBack = navigator::navigateUp,
        paneMode = paneMode,
        settings = settings,
        initialValue = BlockSettings(),
        snackbarHostState = snackbarHostState,
        snackbarHost = { SwipeToDismissSnackbarHost(snackbarHostState) },
    ) {
        group(title = R.string.settings_group_block) {
            toggleablePreference(
                property = BlockSettings::hideBlocked,
                title = R.string.settings_hide_blocked_content,
                leadingIcon = Icons.Outlined.HideSource
            )

            toggleablePreference(
                property = BlockSettings::blockVideo,
                title = R.string.settings_block_video,
                summary = R.string.settings_block_video_summary,
                leadingIcon = Icons.Outlined.VideocamOff
            )
        }

        group(title = R.string.settings_group_block_rule) {
            preference(
                title = R.string.settings_block_forum,
                leadingIcon = Icons.Outlined.Forum,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = onOpenForumBlockList ?: {
                    navigator.navigate(route = SettingsDestination.ForumBlockList)
                }
            )

            preference(
                title = R.string.settings_block_user,
                leadingIcon = Icons.Outlined.NoAccounts,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = onOpenUserBlockList ?: {
                    navigator.navigate(route = SettingsDestination.UserBlockList)
                }
            )

            preference(
                title = R.string.settings_block_keyword,
                leadingIcon = Icons.Outlined.Block,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = onOpenKeywordBlockList ?: {
                    navigator.navigate(route = SettingsDestination.KeywordBlockList)
                }
            )

            // Backup/restore read as two more rows of this same card rather than a pair of filled
            // buttons: they are the same kind of "open a picker" action as the three rows above, and
            // the blue pair broke the group card's rhythm (and its light/dark balance).
            preference(
                title = R.string.button_backup,
                leadingIcon = Icons.Outlined.SaveAs,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = {
                    Date().let { date ->
                        backupSaveTimestamp = date.time
                        backupLauncher.launch(BlockRuleBackupUtil.getBackupFileName(date))
                    }
                }
            )

            preference(
                title = R.string.button_restore,
                leadingIcon = Icons.Outlined.Restore,
                trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                onClick = { restoreLauncher.launch(arrayOf("*/*")) }
            )
        }
    }

        StrongBox {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            if (uiState.pendingRestore == null && uiState.error == null && !uiState.loading) {
                LaunchedEffect(blockRuleDialogState.show) {
                    blockRuleDialogState.show = false
                }
            }

            if (blockRuleDialogState.show) {
                BlockRuleRestoreDialog(
                    state = blockRuleDialogState,
                    uiState = uiState,
                    onRestoreClicked = viewModel::onRestore,
                    onCancelClicked = viewModel::onCancelRestore
                )
            }
        }
}

// title stringRes, rule count, checked state
private typealias SelectableRule = Triple<Int, Int, MutableState<Boolean>>

@Composable
private fun BlockRuleList(
    modifier: Modifier = Modifier,
    metadata: BlockBackupMetadata,
    rules: List<SelectableRule>,
) {
    Column(
        modifier = modifier.padding(start = 4.dp, end = 20.dp)
    ) {
        val timestamp = remember(metadata.timestamp) {
            val backupDate = Date(metadata.timestamp)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(backupDate)
        }

        Text(
            text = stringResource(R.string.dialog_content_rule_restore_to, timestamp),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        rules.fastForEach { (title, ruleCount, checkedState) ->
            if (ruleCount > 0) {
                val (checked, setChecked) = checkedState
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = setChecked)

                    Text(text = stringResource(title), modifier = Modifier.weight(1.0f))

                    Text(text = stringResource(R.string.summary_rules_count, ruleCount))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BlockRuleRestoreDialog(
    state: DialogState = rememberDialogState(),
    uiState: BlockSettingsUiState,
    onRestoreClicked: (forum: Boolean, keyword: Boolean, user: Boolean) -> Unit,
    onCancelClicked: () -> Unit,
) {
    val metadata: BlockBackupMetadata? = uiState.pendingRestore?.first
    // Restore all by default
    val restoreForum = rememberSaveable { mutableStateOf(true) }
    val restoreKeyword = rememberSaveable { mutableStateOf(true) }
    val restoreUser = rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        modifier = Modifier.padding(horizontal = 16.dp), // Make Dialog compact
        dialogState = state,
        dialogProperties = AnyPopDialogProperties(
            direction = DirectionState.CENTER,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            val titleRes = when {
                uiState.error != null -> R.string.error_tip
                uiState.pendingRestore != null -> R.string.dialog_restore_select_rules
                else -> R.string.text_please_wait
            }
            Text(text = stringResource(titleRes))
        },
        buttons = {
            AnimatedVisibility(
                visible = !uiState.loading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DialogNegativeButton(text = stringResource(R.string.button_cancel), onClick = onCancelClicked)

                    if (uiState.error == null && uiState.pendingRestore != null) {
                        val enabled by remember {
                            derivedStateOf { restoreForum.value || restoreKeyword.value || restoreUser.value }
                        }
                        Button(
                            onClick = {
                                onRestoreClicked(restoreForum.value, restoreKeyword.value, restoreUser.value)
                            },
                            enabled = enabled,
                            content = { Text(text = stringResource(R.string.button_sure)) }
                        )
                    }
                }
            }
        }
    ) {
        if (uiState.error != null) {
            Text(
                text = uiState.error.getErrorMessage(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (uiState.loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LoadingIndicator(modifier = Modifier.padding(start = 32.dp))
                Text(
                    text = stringResource(if (metadata == null) R.string.title_loading_data else R.string.dialog_content_wait)
                )
            }
        } else if (metadata != null)  {
            val selectableRules = remember(metadata) {
                listOf(
                    Triple(R.string.title_restore_forum, metadata.forumRuleCount, restoreForum),
                    Triple(R.string.title_restore_keyword, metadata.keywordRuleCount, restoreKeyword),
                    Triple(R.string.title_restore_user, metadata.userRuleCount, restoreUser),
                ).apply {
                    // Unselect empty rules for RestoreButton
                    fastForEach { (_, ruleCount, checked) -> if (ruleCount <= 0) checked.value = false }
                }
            }

            BlockRuleList(metadata = metadata, rules = selectableRules)
        }
    }
}