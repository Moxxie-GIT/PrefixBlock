package fr.moxxie.prefixblock.data.history

import fr.moxxie.prefixblock.rules.NormalizedNumber
import fr.moxxie.prefixblock.rules.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Historique en mémoire ; [calls] est modifiable directement par les tests. */
class FakeBlockedCallRepository(initial: List<BlockedCall> = emptyList()) : BlockedCallRepository {

    val calls = MutableStateFlow(initial)

    override suspend fun record(number: NormalizedNumber, rule: Rule) {
        calls.update { current ->
            current + BlockedCall(
                id = current.size + 1L,
                timestamp = 0,
                number = number.international ?: number.raw,
                countryCode = number.countryCode,
                ruleId = rule.id,
                rulePattern = rule.pattern,
                ruleType = rule.type,
            )
        }
    }

    override fun observeRecent(limit: Int): Flow<List<BlockedCall>> =
        calls.map { list -> list.sortedByDescending { it.timestamp }.take(limit) }

    override fun observeCountSince(sinceMillis: Long): Flow<Int> =
        calls.map { list -> list.count { it.timestamp >= sinceMillis } }
}
