package com.huanchengfly.tieba.post.ui.page.user.edit

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.TranslucentThemeActivity.Companion.currentThemeNoTrans
import com.huanchengfly.tieba.post.activities.UCropActivity
import com.huanchengfly.tieba.post.activities.UCropActivity.Companion.registerUCropResult
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.arch.collectIn
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.FadedVisibility
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.ListPopupColumn
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.TextField
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.LocalLiquidGlassPopupBackdrop
import com.huanchengfly.tieba.post.ui.miuix.liquidGlassDropdownColors
import com.huanchengfly.tieba.post.ui.miuix.liquidglass.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import com.huanchengfly.tieba.post.ui.miuix.rememberMiuixPinnedScrollBehavior

@AndroidEntryPoint
class EditProfileActivity : BaseComposeActivity() {

    private val portraitFile: File by lazy { File(cacheDir, "cropped_portrait") }

    // Launch UCropActivity for result (cropped portrait)
    private val uCropLauncher = registerUCropResult { result ->
        result?.run { // null when RESULT_CANCELED, do nothing
            onSuccess { uri ->
                viewModel.send(EditProfileIntent.UploadPortrait(portraitFile))
            }
            onFailure { e ->
                toastShort(e.message ?: getString(R.string.error_unknown))
            }
        }
    }

    private val pickMediasLauncher = registerForActivityResult(PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            delay(240L) // Wait exit animation of MediaPicker Activity

            // Launch UCropActivity now
            val colorScheme = ThemeUtil.getRawTheme().colorScheme
            val primaryColor = colorScheme.primary.toArgb()
            uCropLauncher.launch(buildUCropOptions(uri, primaryColor))
        }
    }

    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(0)
        viewModel.uiEventFlow
            .filterIsInstance<EditProfileEvent>()
            .collectIn(lifecycleOwner = this, collector = ::handleEvent)
    }

    @Composable
    override fun Content() {
        LazyLoad(loaded = viewModel.initialized) {
            viewModel.send(EditProfileIntent.Init)
            viewModel.initialized = true
        }

        TiebaLiteTheme(colorSchemeExt = currentThemeNoTrans()) {
            EditProfileScaffold(viewModel, onBackPressed = ::finish)

            val colorScheme = MaterialTheme.colorScheme
            LaunchedEffect(colorScheme) {
                windowInsetsController.isAppearanceLightStatusBars = ThemeUtil.isStatusBarFontDark(colorScheme)
                windowInsetsController.isAppearanceLightNavigationBars = ThemeUtil.isNavigationBarFontDark(colorScheme)
            }
        }
    }

    private fun handleEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.Submit.Result -> {
                if (event.success) {
                    if (event.changed) toastShort(R.string.toast_success)
                    finish()
                } else {
                    toastShort(event.message)
                }
            }

            EditProfileEvent.UploadPortrait.Pick -> {
                pickMediasLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
            }

            is EditProfileEvent.UploadPortrait.Fail -> toastShort(event.error)
            is EditProfileEvent.UploadPortrait.Success -> toastShort(event.message)
        }
    }

    /**
     * @return UCrop to launch [UCropActivity]
     * */
    private fun buildUCropOptions(sourceUri: Uri, @ColorInt primaryColor: Int): UCrop {
        val destUri = Uri.fromFile(portraitFile)
        return UCrop.of(sourceUri, destUri)
            .withAspectRatio(1f, 1f)
            .withOptions(UCrop.Options().apply {
                setShowCropFrame(true)
                setShowCropGrid(true)
                setToolbarColor(primaryColor)
                setLogoColor(primaryColor)
                setCompressionFormat(Bitmap.CompressFormat.JPEG)
            })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileScaffold(
    viewModel: EditProfileViewModel,
    onBackPressed: () -> Unit,
) {
    val scrollBehavior = rememberMiuixPinnedScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()

    var editProfile: EditProfile? by remember { mutableStateOf(null) }
    uiState.edit?.let {
        LaunchedEffect(editProfile == null) {
            editProfile = it // Copy this profile for edit
        }
    }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                titleRes = R.string.title_activity_edit_profile,
                navigationIcon = { BackNavigationIcon(onBackPressed = onBackPressed) },
                actions = {
                    val allowSubmit by remember {
                        derivedStateOf {
                           !uiState.isSubmitting && !editProfile?.nickName.isNullOrEmpty() && editProfile != uiState.edit
                        }
                    }
                    FadedVisibility(visible = allowSubmit) {
                        ActionItem(
                            icon = Icons.Rounded.Save,
                            contentDescription = R.string.button_save_profile,
                            enabled = allowSubmit,
                            onClick = { viewModel.onSubmitProfile(newProfile = editProfile!!) }
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        backgroundColor = MiuixTheme.colorScheme.background,
    ) { contentPadding ->
        StateScreen(
            isLoading = uiState.isLoading,
            error = uiState.error,
            screenPadding = contentPadding,
        ) {
            editProfile?.let { profile ->
                Container(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .imePadding()
                        .imeNestedScroll()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EditProfileCard(
                        avatar = uiState.avatarUrl ?: "",
                        name = uiState.name,
                        nickName = profile.nickName,
                        sex = profile.sex,
                        intro = profile.intro,
                        onNickNameChange = {
                            editProfile = profile.copy(nickName = it.trim())
                        },
                        onIntroChange = {
                            editProfile = profile.copy(intro = it)
                        },
                        onSexChange = {
                            editProfile = profile.copy(sex = it)
                        },
                        onUploadPortrait = { viewModel.send(EditProfileIntent.UploadPortraitStart) },
                    )
                }
            }
        }
    }
}

/**
 * Sex selector rendered as a Miuix [TextField].
 *
 * The field itself is `readOnly`; a transparent click layer on top routes every tap to a Miuix
 * [OverlayListPopup] so the row keeps the same "tap the field, pick from a list" interaction the
 * Material dropdown had, without the field ever taking focus or opening the IME.
 */
@Composable
private fun SexDropdownField(modifier: Modifier = Modifier, sex: Int, onSexChange: (Int) -> Unit) {
    val context = LocalContext.current
    val options = remember {
        listOf(
            1 to context.getString(R.string.profile_sex_male),
            2 to context.getString(R.string.profile_sex_female),
        )
    }
    val selectedIndex = options.indexOfFirst { it.first == sex }.coerceAtLeast(0)
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextField(
            value = options.getOrNull(selectedIndex)?.second ?: "",
            onValueChange = {/* ReadOnly */ },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.profile_sex),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSecondaryContainer,
                )
            },
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickableNoIndication { expanded = true }
        )

        OverlayListPopup(
            show = expanded,
            liquidGlassBackdrop = LocalLiquidGlassPopupBackdrop.current,
            enableWindowDim = false,
            // Miuix's own dropdown popup (OverlayDropdownPopup) anchors with Align.End so the menu
            // hangs off the trailing edge of its anchor. OverlayListPopup defaults to Align.Start,
            // which left-flushes the menu against the page edge and looks detached from the field.
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { expanded = false },
        ) {
            ListPopupColumn {
                options.forEachIndexed { index, (value, title) ->
                    val isSelected = value == sex
                    DropdownImpl(
                        item = DropdownItem(text = title, selected = isSelected),
                        optionSize = options.size,
                        isSelected = isSelected,
                        index = index,
                        // Translucent rows, same as every other glass dropdown: miuix's default
                        // row colour is an opaque `surfaceContainer` that would cover the sheet.
                        dropdownColors = liquidGlassDropdownColors(),
                        onSelectedIndexChange = {
                            expanded = false
                            if (!isSelected) onSexChange(value)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAvatarPicker(modifier: Modifier = Modifier, avatar: String, onPick: () -> Unit) {
    Box(
        modifier = modifier.clickableNoIndication(onClick = onPick)
    ) {
        NetworkImage(
            imageUrl = avatar,
            contentDescription = stringResource(id = R.string.upload_portrait),
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
        )
        // Picker icon with border
        Surface(
            modifier = Modifier
                .size(26.dp)
                .background(color = MiuixTheme.colorScheme.background, shape = CircleShape)
                .padding(1.dp)
                .align(Alignment.BottomEnd),
            shape = CircleShape,
            color = MiuixTheme.colorScheme.primary,
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp),
            )
        }
    }
}

/**
 * Multiline intro field built on Miuix [TextField].
 *
 * Miuix has no `supportingText` slot at v0.9.4, so the character counter is composed underneath the
 * field and the length cap is enforced in `onValueChange`, matching the previous behaviour.
 */
@Composable
private fun IntroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxLength: Int = 500,
    label: String,
    onLengthBeyondRestrict: () -> Unit = {},
) {
    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = { new ->
                if (new.length <= maxLength) {
                    onValueChange(new)
                } else {
                    onValueChange(new.take(maxLength))
                    onLengthBeyondRestrict()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = label,
            singleLine = false,
            minLines = 3,
            maxLines = 5,
        )
        Text(
            text = "${value.length}/$maxLength",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
    }
}

@Composable
private fun EditProfileCard(
    modifier: Modifier = Modifier,
    avatar: String,
    name: String,
    nickName: String,
    sex: Int,
    intro: String?,
    onNickNameChange: (String) -> Unit = {},
    onSexChange: (Int) -> Unit = {},
    onIntroChange: (String) -> Unit = {},
    onUploadPortrait: () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        UserAvatarPicker(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .size(96.dp),
            avatar = avatar,
            onPick = onUploadPortrait
        )

        // Username is fixed by the account and cannot be edited here
        TextField(
            value = name,
            onValueChange = {/* ReadOnly */ },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.title_username),
            enabled = false,
            singleLine = true,
        )

        TextField(
            value = nickName,
            onValueChange = onNickNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.title_nickname),
            singleLine = true,
        )

        SexDropdownField(
            modifier = Modifier.fillMaxWidth(),
            sex = sex,
            onSexChange = onSexChange,
        )

        IntroTextField(
            value = intro.orEmpty(),
            onValueChange = onIntroChange,
            modifier = Modifier.fillMaxWidth(),
            maxLength = 500,
            label = stringResource(id = R.string.title_intro),
            onLengthBeyondRestrict = {
                context.toastShort(R.string.toast_intro_length_beyond_restrict)
            },
        )
    }
}

@Preview
@Composable
private fun EditProfileCardPreview() = TiebaLiteTheme {
    Surface {
        EditProfileCard(
            avatar = "",
            name = "test",
            nickName = "test",
            sex = 1,
            intro = "test",
        )
    }
}
