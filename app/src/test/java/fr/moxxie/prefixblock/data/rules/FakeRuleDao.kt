package fr.moxxie.prefixblock.data.rules

import fr.moxxie.prefixblock.data.db.RuleDao
import fr.moxxie.prefixblock.data.db.RuleEntity
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** DAO en mémoire reproduisant les contraintes de la table `rules`. */
class FakeRuleDao : RuleDao {

    private val rules = MutableStateFlow<List<RuleEntity>>(emptyList())
    private var nextId = 1L

    val current: List<RuleEntity> get() = rules.value

    override fun observeAll(): Flow<List<RuleEntity>> = rules

    override suspend fun getAll(): List<RuleEntity> = rules.value

    override suspend fun getById(id: Long): RuleEntity? = rules.value.firstOrNull { it.id == id }

    override suspend fun find(list: RuleList, type: RuleType, pattern: String): RuleEntity? =
        rules.value.firstOrNull { it.list == list && it.type == type && it.pattern == pattern }

    override suspend fun insert(rule: RuleEntity): Long {
        check(find(rule.list, rule.type, rule.pattern) == null) { "UNIQUE constraint failed" }
        val id = nextId++
        rules.update { it + rule.copy(id = id) }
        return id
    }

    override suspend fun update(rule: RuleEntity) {
        rules.update { list -> list.map { if (it.id == rule.id) rule else it } }
    }

    override suspend fun deleteById(id: Long) {
        rules.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        rules.update { list -> list.map { if (it.id == id) it.copy(enabled = enabled) else it } }
    }

    override suspend fun setPriority(id: Long, priority: Boolean) {
        rules.update { list -> list.map { if (it.id == id) it.copy(priority = priority) else it } }
    }
}
