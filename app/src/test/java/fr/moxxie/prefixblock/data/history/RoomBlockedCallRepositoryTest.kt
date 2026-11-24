package fr.moxxie.prefixblock.data.history

import fr.moxxie.prefixblock.data.db.BlockedCallDao
import fr.moxxie.prefixblock.data.db.BlockedCallEntity
import fr.moxxie.prefixblock.rules.NormalizedNumber
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RoomBlockedCallRepositoryTest {

    private class RecordingDao : BlockedCallDao {
        val inserted = mutableListOf<BlockedCallEntity>()
        override suspend fun insert(call: BlockedCallEntity): Long = (inserted.size + 1L).also { inserted += call }
        override fun observeRecent(limit: Int): Flow<List<BlockedCallEntity>> = emptyFlow()
        override fun observeFiltered(since: Long, digits: String): Flow<List<BlockedCallEntity>> = emptyFlow()
        override fun observeCountSince(since: Long): Flow<Int> = emptyFlow()
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun deleteAll() = Unit
    }

    private val dao = RecordingDao()
    private val clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC)
    private val repository = RoomBlockedCallRepository(dao, clock)
    private val inmarsat = Rule(id = 3, list = RuleList.BLACKLIST, type = RuleType.PREFIX, pattern = "+870")

    @Test
    fun `blocked call keeps the international form, the country code and the rule`() = runTest {
        repository.record(NormalizedNumber("00870 773 111 632", "+870773111632", 870), inmarsat)

        val call = dao.inserted.single()
        assertEquals(clock.millis(), call.timestamp)
        assertEquals("+870773111632", call.number)
        assertEquals(870, call.countryCode)
        assertEquals(3L, call.ruleId)
        assertEquals("+870", call.rulePattern)
        assertEquals(RuleType.PREFIX, call.ruleType)
    }

    @Test
    fun `unresolved number is kept as presented`() = runTest {
        val regex = Rule(id = 4, list = RuleList.BLACKLIST, type = RuleType.REGEX, pattern = "^99")
        repository.record(NormalizedNumber("9912", international = null, countryCode = null), regex)

        val call = dao.inserted.single()
        assertEquals("9912", call.number)
        assertNull(call.countryCode)
    }

    @Test
    fun `rule without database id is not linked`() = runTest {
        repository.record(NormalizedNumber("+870773111632", "+870773111632", 870), inmarsat.copy(id = 0))
        assertNull(dao.inserted.single().ruleId)
    }
}
