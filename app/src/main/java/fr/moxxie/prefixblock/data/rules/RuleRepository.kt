package fr.moxxie.prefixblock.data.rules

import android.database.sqlite.SQLiteConstraintException
import fr.moxxie.prefixblock.data.db.RuleDao
import fr.moxxie.prefixblock.data.db.RuleEntity
import fr.moxxie.prefixblock.data.region.DefaultRegionProvider
import fr.moxxie.prefixblock.rules.PatternError
import fr.moxxie.prefixblock.rules.PatternValidation
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import fr.moxxie.prefixblock.rules.RuleValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Saisie d'une règle, avant validation et normalisation. */
data class RuleDraft(
    val list: RuleList,
    val type: RuleType,
    val input: String,
    val label: String? = null,
    val enabled: Boolean = true,
    val priority: Boolean = false,
)

sealed interface SaveRuleResult {
    data class Saved(val rule: Rule) : SaveRuleResult
    data class Invalid(val error: PatternError) : SaveRuleResult

    /** Le même motif existe déjà dans cette liste pour ce type. */
    data object Duplicate : SaveRuleResult

    /** La règle à modifier n'existe plus. */
    data object NotFound : SaveRuleResult
}

/** Gestion des listes noire et blanche. Toute saisie est validée avant enregistrement. */
interface RuleRepository {
    fun observeRules(): Flow<List<Rule>>
    suspend fun getRules(): List<Rule>
    suspend fun addRule(draft: RuleDraft): SaveRuleResult
    suspend fun updateRule(id: Long, draft: RuleDraft): SaveRuleResult
    suspend fun deleteRule(id: Long)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun setPriority(id: Long, priority: Boolean)
}

@Singleton
class RoomRuleRepository @Inject constructor(
    private val dao: RuleDao,
    private val validator: RuleValidator,
    private val regionProvider: DefaultRegionProvider,
    private val clock: Clock,
) : RuleRepository {

    override fun observeRules(): Flow<List<Rule>> =
        dao.observeAll().map { entities -> entities.map(RuleEntity::toDomain) }

    override suspend fun getRules(): List<Rule> = dao.getAll().map(RuleEntity::toDomain)

    override suspend fun addRule(draft: RuleDraft): SaveRuleResult {
        val pattern = when (val validation = validate(draft)) {
            is PatternValidation.Valid -> validation.pattern
            is PatternValidation.Invalid -> return SaveRuleResult.Invalid(validation.error)
        }
        if (dao.find(draft.list, draft.type, pattern) != null) return SaveRuleResult.Duplicate

        val rule = draft.toRule(id = 0, pattern = pattern)
        return try {
            val id = dao.insert(RuleEntity.fromDomain(rule, createdAt = clock.millis()))
            SaveRuleResult.Saved(rule.copy(id = id))
        } catch (_: SQLiteConstraintException) {
            SaveRuleResult.Duplicate // insertion concurrente du même motif
        }
    }

    override suspend fun updateRule(id: Long, draft: RuleDraft): SaveRuleResult {
        val existing = dao.getById(id) ?: return SaveRuleResult.NotFound
        val pattern = when (val validation = validate(draft)) {
            is PatternValidation.Valid -> validation.pattern
            is PatternValidation.Invalid -> return SaveRuleResult.Invalid(validation.error)
        }
        val clash = dao.find(draft.list, draft.type, pattern)
        if (clash != null && clash.id != id) return SaveRuleResult.Duplicate

        val rule = draft.toRule(id = id, pattern = pattern)
        return try {
            dao.update(RuleEntity.fromDomain(rule, createdAt = existing.createdAt))
            SaveRuleResult.Saved(rule)
        } catch (_: SQLiteConstraintException) {
            SaveRuleResult.Duplicate
        }
    }

    override suspend fun deleteRule(id: Long) = dao.deleteById(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    override suspend fun setPriority(id: Long, priority: Boolean) = dao.setPriority(id, priority)

    private fun validate(draft: RuleDraft): PatternValidation =
        validator.validate(draft.list, draft.type, draft.input, regionProvider.currentRegion())

    private fun RuleDraft.toRule(id: Long, pattern: String) = Rule(
        id = id,
        list = list,
        type = type,
        pattern = pattern,
        label = label?.trim()?.takeIf { it.isNotEmpty() },
        enabled = enabled,
        priority = priority,
    )
}
