package fr.moxxie.prefixblock.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.moxxie.prefixblock.BuildConfig
import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val screeningRoleHeld: Boolean = false,
    val versionName: String = BuildConfig.VERSION_NAME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val roleRepository: CallScreeningRoleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(screeningRoleHeld = roleRepository.isRoleHeld()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun refreshRoleStatus() {
        _uiState.update { it.copy(screeningRoleHeld = roleRepository.isRoleHeld()) }
    }
}
