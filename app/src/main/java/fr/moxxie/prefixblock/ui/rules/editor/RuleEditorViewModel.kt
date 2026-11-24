package fr.moxxie.prefixblock.ui.rules.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.moxxie.prefixblock.data.rules.RuleDraft
import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.data.rules.SaveRuleResult
import fr.moxxie.prefixblock.rules.PatternValidation
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.PrefixCoverage
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveError { DUPLICATE, NOT_FOUND }

data class RuleEditorUiState(
    val list: RuleList,
    /** `null` pour une nouvelle règle. */
    val ruleId: Long? = null,
    val loading: Boolean = false,
    val type: RuleType = if (list == RuleList.WHITELIST) RuleType.NUMBER else RuleType.PREFIX,
    val input: String = "",
    val label: String = "",
    val priority: Boolean = false,
    val enabled: Boolean = true,
    /** `null` tant que rien n'est saisi. */
    val validation: PatternValidation? = null,
    /** Pays couverts par un préfixe valide. */
    val coverage: PrefixCoverage? = null,
    val saveError: SaveError? = null,
    val saving: Boolean = false,
    /** Enregistrée ou supprimée : l'écran se ferme. */
    val done: Boolean = false,
) {
    val isNew: Boolean get() = ruleId == null
    val canSave: Boolean get() = !loading && !saving && validation is PatternValidation.Valid

    /** La priorité ne concerne que les préfixes et les regex : un numéro complet est déjà prioritaire. */
    val priorityApplies: Boolean get() = type != RuleType.NUMBER
}

/** Ajout ou modification d'une règle, avec validation en direct de la saisie. */
@HiltViewModel(assistedFactory = RuleEditorViewModel.Factory::class)
class RuleEditorViewModel @AssistedInject constructor(
    @Assisted ruleId: Long?,
    @Assisted list: RuleList,
    private val repository: RuleRepository,
    private val normalizer: PhoneNumberNormalizer,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(ruleId: Long?, list: RuleList): RuleEditorViewModel
    }

    private val _uiState = MutableStateFlow(RuleEditorUiState(list = list, ruleId = ruleId, loading = ruleId != null))
    val uiState: StateFlow<RuleEditorUiState> = _uiState.asStateFlow()

    init {
        if (ruleId != null) {
            viewModelScope.launch {
                val rule = repository.getRule(ruleId)
                _uiState.update { state ->
                    if (rule == null) {
                        state.copy(loading = false, saveError = SaveError.NOT_FOUND)
                    } else {
                        state.copy(
                            loading = false,
                            list = rule.list,
                            type = rule.type,
                            input = rule.pattern,
                            label = rule.label.orEmpty(),
                            priority = rule.priority,
                            enabled = rule.enabled,
                        ).validated()
                    }
                }
            }
        }
    }

    fun onTypeChange(type: RuleType) = _uiState.update { it.copy(type = type, saveError = null).validated() }

    fun onInputChange(input: String) = _uiState.update { it.copy(input = input, saveError = null).validated() }

    fun onLabelChange(label: String) = _uiState.update { it.copy(label = label) }

    fun onPriorityChange(priority: Boolean) = _uiState.update { it.copy(priority = priority) }

    fun onEnabledChange(enabled: Boolean) = _uiState.update { it.copy(enabled = enabled) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val draft = state.toDraft()
            val result = state.ruleId?.let { repository.updateRule(it, draft) } ?: repository.addRule(draft)
            _uiState.update {
                when (result) {
                    is SaveRuleResult.Saved -> it.copy(saving = false, done = true)
                    is SaveRuleResult.Invalid -> it.copy(saving = false, validation = PatternValidation.Invalid(result.error))
                    SaveRuleResult.Duplicate -> it.copy(saving = false, saveError = SaveError.DUPLICATE)
                    SaveRuleResult.NotFound -> it.copy(saving = false, saveError = SaveError.NOT_FOUND)
                }
            }
        }
    }

    fun delete() {
        val id = _uiState.value.ruleId ?: return
        viewModelScope.launch {
            repository.deleteRule(id)
            _uiState.update { it.copy(done = true) }
        }
    }

    private fun RuleEditorUiState.toDraft() = RuleDraft(
        list = list,
        type = type,
        input = input,
        label = label,
        enabled = enabled,
        priority = priority && priorityApplies,
    )

    private fun RuleEditorUiState.validated(): RuleEditorUiState {
        if (input.isBlank()) return copy(validation = null, coverage = null)
        val validation = repository.validate(RuleDraft(list, type, input))
        val coverage = (validation as? PatternValidation.Valid)
            ?.takeIf { type == RuleType.PREFIX }
            ?.let { normalizer.prefixCoverage(it.pattern) }
        return copy(validation = validation, coverage = coverage)
    }
}
