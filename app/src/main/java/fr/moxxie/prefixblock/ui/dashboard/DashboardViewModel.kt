package fr.moxxie.prefixblock.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.moxxie.prefixblock.data.history.BlockedCallRepository
import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository
import fr.moxxie.prefixblock.di.UiLocale
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.ui.components.DisplayedCall
import fr.moxxie.prefixblock.ui.components.displayed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    /** PrefixBlock détient le rôle système de filtrage des appels. */
    val protectionActive: Boolean = false,
    val blockedToday: Int = 0,
    val blockedThisWeek: Int = 0,
    val blockedThisMonth: Int = 0,
    val recentCalls: List<DisplayedCall> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val roleRepository: CallScreeningRoleRepository,
    blockedCalls: BlockedCallRepository,
    normalizer: PhoneNumberNormalizer,
    private val clock: Clock,
    @UiLocale private val locale: Locale,
) : ViewModel() {

    private val protectionActive = MutableStateFlow(roleRepository.isRoleHeld())
    private val periods = MutableStateFlow(PeriodStarts.of(clock, locale))

    private val counts = periods.flatMapLatest { starts ->
        combine(
            blockedCalls.observeCountSince(starts.today),
            blockedCalls.observeCountSince(starts.thisWeek),
            blockedCalls.observeCountSince(starts.thisMonth),
        ) { today, week, month -> Counts(today, week, month) }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        protectionActive,
        counts,
        blockedCalls.observeRecent(RECENT_CALLS_LIMIT).map { calls -> calls.map { it.displayed(normalizer) } },
    ) { active, count, recent ->
        DashboardUiState(
            protectionActive = active,
            blockedToday = count.today,
            blockedThisWeek = count.week,
            blockedThisMonth = count.month,
            recentCalls = recent,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(protectionActive = protectionActive.value),
    )

    /**
     * Relit l'état du rôle (l'utilisateur peut changer l'application de filtrage depuis les
     * paramètres Android) et recale les périodes (passage à un nouveau jour). Appelé à chaque
     * retour au premier plan.
     */
    fun refresh() {
        protectionActive.value = roleRepository.isRoleHeld()
        periods.value = PeriodStarts.of(clock, locale)
    }

    private data class Counts(val today: Int, val week: Int, val month: Int)

    companion object {
        const val RECENT_CALLS_LIMIT = 5
    }
}
