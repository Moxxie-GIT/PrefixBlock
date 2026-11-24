package fr.moxxie.prefixblock.data.rules

import fr.moxxie.prefixblock.rules.PatternError
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleList.BLACKLIST
import fr.moxxie.prefixblock.rules.RuleList.WHITELIST
import fr.moxxie.prefixblock.rules.RuleType.NUMBER
import fr.moxxie.prefixblock.rules.RuleType.PREFIX
import fr.moxxie.prefixblock.rules.RuleValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RoomRuleRepositoryTest {

    private val dao = FakeRuleDao()
    private val clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC)
    private val repository = RoomRuleRepository(
        dao = dao,
        validator = RuleValidator(PhoneNumberNormalizer()),
        regionProvider = { "gp" },
        clock = clock,
    )

    private fun saved(result: SaveRuleResult) = (result as SaveRuleResult.Saved).rule

    @Test
    fun `prefix input is stored in normalized form`() = runTest {
        val rule = saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "00 1876", label = "Jamaïque")))
        assertEquals("+1876", rule.pattern)
        assertEquals("+1876", dao.current.single().pattern)
        assertEquals(clock.millis(), dao.current.single().createdAt)
    }

    @Test
    fun `national number uses the network region`() = runTest {
        val rule = saved(repository.addRule(RuleDraft(WHITELIST, NUMBER, "0690 12 34 56")))
        assertEquals("+590690123456", rule.pattern)
    }

    @Test
    fun `invalid input is rejected and nothing is stored`() = runTest {
        assertEquals(
            SaveRuleResult.Invalid(PatternError.PREFIX_FORMAT),
            repository.addRule(RuleDraft(BLACKLIST, PREFIX, "870")),
        )
        assertTrue(dao.current.isEmpty())
    }

    @Test
    fun `same pattern twice in a list is a duplicate even when written differently`() = runTest {
        saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+870")))
        assertEquals(SaveRuleResult.Duplicate, repository.addRule(RuleDraft(BLACKLIST, PREFIX, "00870")))
        assertEquals(1, dao.current.size)
    }

    @Test
    fun `same pattern may exist in both lists`() = runTest {
        saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+44")))
        saved(repository.addRule(RuleDraft(WHITELIST, PREFIX, "+44")))
        assertEquals(2, repository.getRules().size)
    }

    @Test
    fun `blank label is stored as null`() = runTest {
        assertNull(saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+674", label = "  "))).label)
    }

    @Test
    fun `update validates and keeps creation date`() = runTest {
        val rule = saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+870")))
        val updated = saved(repository.updateRule(rule.id, RuleDraft(BLACKLIST, PREFIX, "+870 77", priority = true)))
        assertEquals("+87077", updated.pattern)
        assertTrue(updated.priority)
        assertEquals(clock.millis(), dao.current.single().createdAt)
    }

    @Test
    fun `update onto another rule pattern is a duplicate`() = runTest {
        saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+870")))
        val other = saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+674")))
        assertEquals(SaveRuleResult.Duplicate, repository.updateRule(other.id, RuleDraft(BLACKLIST, PREFIX, "+870")))
    }

    @Test
    fun `update of a missing rule reports it`() = runTest {
        assertEquals(SaveRuleResult.NotFound, repository.updateRule(42, RuleDraft(BLACKLIST, PREFIX, "+870")))
    }

    @Test
    fun `enable flag and deletion are reflected in the observed rules`() = runTest {
        val rule = saved(repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+870")))
        repository.setEnabled(rule.id, false)
        assertEquals(false, repository.observeRules().first().single().enabled)
        repository.deleteRule(rule.id)
        assertTrue(repository.observeRules().first().isEmpty())
    }
}
