package fr.moxxie.prefixblock.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RulesUiState(
    val blacklist: List<Rule> = emptyList(),
    val whitelist: List<Rule> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val repository: RuleRepository,
) : ViewModel() {

    val uiState: StateFlow<RulesUiState> = repository.observeRules()
        .map { rules ->
            RulesUiState(
                blacklist = rules.filter { it.list == RuleList.BLACKLIST }.sortedWith(DISPLAY_ORDER),
                whitelist = rules.filter { it.list == RuleList.WHITELIST }.sortedWith(DISPLAY_ORDER),
                loading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RulesUiState())

    fun setEnabled(rule: Rule, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(rule.id, enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /** Règles prioritaires d'abord, puis numéros, préfixes, regex, puis ordre alphabétique. */
        val DISPLAY_ORDER: Comparator<Rule> =
            compareByDescending<Rule> { it.priority }.thenBy { it.type }.thenBy { it.pattern }
    }
}
