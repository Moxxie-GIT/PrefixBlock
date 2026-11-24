package fr.moxxie.prefixblock.ui.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DashboardUiState(
    /** PrefixBlock détient le rôle système de filtrage des appels. */
    val protectionActive: Boolean = false,
    // Compteurs : alimentés par l'historique Room à partir de la phase 2.
    val blockedToday: Int = 0,
    val blockedThisWeek: Int = 0,
    val blockedThisMonth: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val roleRepository: CallScreeningRoleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(protectionActive = roleRepository.isRoleHeld()),
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /**
     * Relit l'état du rôle. Appelé à chaque retour au premier plan, car l'utilisateur
     * peut changer l'application de filtrage depuis les paramètres Android.
     */
    fun refreshProtectionStatus() {
        _uiState.update { it.copy(protectionActive = roleRepository.isRoleHeld()) }
    }
}
