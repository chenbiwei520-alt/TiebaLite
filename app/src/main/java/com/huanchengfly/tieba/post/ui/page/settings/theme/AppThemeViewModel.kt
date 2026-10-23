package com.huanchengfly.tieba.post.ui.page.settings.theme

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.settings.DarkPreference
import com.huanchengfly.tieba.post.ui.models.settings.UISettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ThemeUiState(
    val darkPreference: DarkPreference = DarkPreference.FOLLOW_SYSTEM
)

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : BaseStateViewModel<ThemeUiState>() {

    private val uiSettings: Settings<UISettings> = settingsRepository.uiSettings

    /** Miuix dark / light color scheme mode */
    val darkPreferenceFlow: StateFlow<DarkPreference> = uiSettings
        .map { it.darkPreference }
        .stateInViewModel(initialValue = DarkPreference.FOLLOW_SYSTEM)

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(darkPreference = darkPreferenceFlow.value) }
            darkPreferenceFlow.collect { preference ->
                _uiState.update { it.copy(darkPreference = preference) }
            }
        }
    }

    /** Sets the Miuix dark/light color scheme mode. */
    fun onDarkPreferencePicked(preference: DarkPreference) = launchInVM {
        uiSettings.save { it.copy(darkPreference = preference) }
    }

    override fun createInitialState(): ThemeUiState = ThemeUiState()
}
