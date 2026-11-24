package fr.moxxie.prefixblock.data.history

import android.database.sqlite.SQLiteConstraintException
import fr.moxxie.prefixblock.data.db.BlockedCallDao
import fr.moxxie.prefixblock.data.db.BlockedCallEntity
import fr.moxxie.prefixblock.rules.NormalizedNumber
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Appel rejeté, tel qu'affiché dans l'historique. */
data class BlockedCall(
    val id: Long,
    val timestamp: Long,
    /** Forme internationale si elle est connue, sinon le numéro tel que présenté. */
    val number: String,
    val countryCode: Int?,
    /** `null` si la règle a été supprimée depuis. */
    val ruleId: Long?,
    val rulePattern: String,
    val ruleType: RuleType,
)

/** Historique local des appels rejetés (CLAUDE.md §8). */
interface BlockedCallRepository {
    /** Enregistre un appel que PrefixBlock vient de rejeter. */
    suspend fun record(number: NormalizedNumber, rule: Rule)
    fun observeRecent(limit: Int): Flow<List<BlockedCall>>
    fun observeCountSince(sinceMillis: Long): Flow<Int>

    /**
     * @param sinceMillis appels à partir de cette date (0 : tout l'historique).
     * @param numberDigits chiffres contenus dans le numéro (vide : pas de recherche).
     */
    fun observe(sinceMillis: Long, numberDigits: String): Flow<List<BlockedCall>>
    suspend fun delete(id: Long)
    suspend fun deleteAll()
}

@Singleton
class RoomBlockedCallRepository @Inject constructor(
    private val dao: BlockedCallDao,
    private val clock: Clock,
) : BlockedCallRepository {

    override suspend fun record(number: NormalizedNumber, rule: Rule) {
        val entity = BlockedCallEntity(
            timestamp = clock.millis(),
            number = number.international ?: number.raw,
            countryCode = number.countryCode,
            ruleId = rule.id.takeIf { it > 0 },
            rulePattern = rule.pattern,
            ruleType = rule.type,
        )
        try {
            dao.insert(entity)
        } catch (_: SQLiteConstraintException) {
            // Règle supprimée entre la décision et l'écriture : on garde l'appel sans le lien.
            dao.insert(entity.copy(ruleId = null))
        }
    }

    override fun observeRecent(limit: Int): Flow<List<BlockedCall>> =
        dao.observeRecent(limit).map { calls -> calls.map(BlockedCallEntity::toDomain) }

    override fun observeCountSince(sinceMillis: Long): Flow<Int> = dao.observeCountSince(sinceMillis)

    override fun observe(sinceMillis: Long, numberDigits: String): Flow<List<BlockedCall>> =
        dao.observeFiltered(sinceMillis, numberDigits).map { calls -> calls.map(BlockedCallEntity::toDomain) }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()
}

private fun BlockedCallEntity.toDomain() = BlockedCall(
    id = id,
    timestamp = timestamp,
    number = number,
    countryCode = countryCode,
    ruleId = ruleId,
    rulePattern = rulePattern,
    ruleType = ruleType,
)
