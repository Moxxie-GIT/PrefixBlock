package fr.moxxie.prefixblock.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.moxxie.prefixblock.data.history.BlockedCall
import fr.moxxie.prefixblock.data.history.BlockedCallRepository
import fr.moxxie.prefixblock.data.rules.RuleDraft
import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.data.rules.SaveRuleResult
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
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
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Filtre par date de l'historique. */
enum class HistoryPeriod(val days: Long?) {
    ALL(null),
    TODAY(1),
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
}

/** Retour d'une action, affiché une fois (snackbar). */
sealed interface HistoryMessage {
    data class NumberAllowed(val number: String) : HistoryMessage
    data class PrefixAllowed(val prefix: String) : HistoryMessage
    data object AlreadyAllowed : HistoryMessage
    data object CannotAllow : HistoryMessage
}

data class HistoryUiState(
    val query: String = "",
    val period: HistoryPeriod = HistoryPeriod.ALL,
    val calls: List<DisplayedCall> = emptyList(),
    val loading: Boolean = true,
    val message: HistoryMessage? = null,
) {
    val filtered: Boolean get() = query.isNotBlank() || period != HistoryPeriod.ALL
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val history: BlockedCallRepository,
    private val rules: RuleRepository,
    private val normalizer: PhoneNumberNormalizer,
    private val clock: Clock,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val period = MutableStateFlow(HistoryPeriod.ALL)
    private val message = MutableStateFlow<HistoryMessage?>(null)

    private val calls = combine(query, period) { query, period -> searchDigits(query) to sinceMillis(period) }
        .flatMapLatest { (digits, since) -> history.observe(since, digits) }
        .map { list -> list.map { it.displayed(normalizer) } }

    val uiState: StateFlow<HistoryUiState> = combine(query, period, calls, message) { query, period, calls, message ->
        HistoryUiState(query = query, period = period, calls = calls, loading = false, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onPeriodChange(value: HistoryPeriod) {
        period.value = value
    }

    fun delete(call: BlockedCall) {
        viewModelScope.launch { history.delete(call.id) }
    }

    fun deleteAll() {
        viewModelScope.launch { history.deleteAll() }
    }

    /** Ajoute le numéro complet en liste blanche : il passera toujours (CLAUDE.md §7, niveau 1). */
    fun allowNumber(call: DisplayedCall) {
        if (!call.call.number.startsWith('+')) {
            message.value = HistoryMessage.CannotAllow
            return
        }
        allow(RuleDraft(RuleList.WHITELIST, RuleType.NUMBER, call.call.number)) {
            HistoryMessage.NumberAllowed(call.displayNumber)
        }
    }

    /** Ajoute l'indicatif pays en liste blanche. */
    fun allowCountryCode(call: DisplayedCall) {
        val prefix = call.call.countryCode?.let { "+$it" }
        if (prefix == null) {
            message.value = HistoryMessage.CannotAllow
            return
        }
        allow(RuleDraft(RuleList.WHITELIST, RuleType.PREFIX, prefix)) { HistoryMessage.PrefixAllowed(prefix) }
    }

    fun messageShown() {
        message.value = null
    }

    private fun allow(draft: RuleDraft, success: () -> HistoryMessage) {
        viewModelScope.launch {
            message.value = when (rules.addRule(draft)) {
                is SaveRuleResult.Saved -> success()
                SaveRuleResult.Duplicate -> HistoryMessage.AlreadyAllowed
                else -> HistoryMessage.CannotAllow
            }
        }
    }

    /** Chiffres significatifs de la recherche : `0690 12` → `69012`, qui figure dans `+590690123456`. */
    private fun searchDigits(query: String): String = query.filter { it in '0'..'9' }.trimStart('0')

    private fun sinceMillis(period: HistoryPeriod): Long {
        val days = period.days ?: return 0
        return LocalDate.now(clock).minusDays(days - 1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    }
}
