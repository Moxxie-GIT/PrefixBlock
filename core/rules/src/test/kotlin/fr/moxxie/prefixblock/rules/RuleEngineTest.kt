package fr.moxxie.prefixblock.rules

import fr.moxxie.prefixblock.rules.RuleList.BLACKLIST
import fr.moxxie.prefixblock.rules.RuleList.WHITELIST
import fr.moxxie.prefixblock.rules.RuleType.NUMBER
import fr.moxxie.prefixblock.rules.RuleType.PREFIX
import fr.moxxie.prefixblock.rules.RuleType.REGEX
import fr.moxxie.prefixblock.rules.TestNumbers.presented
import org.junit.Assert.assertEquals
import org.junit.Test

/** Priorité entre liste blanche et liste noire (CLAUDE.md §7). */
class RuleEngineTest {

    private var nextId = 1L

    private fun rule(list: RuleList, type: RuleType, pattern: String, priority: Boolean = false, enabled: Boolean = true) =
        Rule(id = nextId++, list = list, type = type, pattern = pattern, priority = priority, enabled = enabled)

    private fun RuleEngine.check(number: String) = evaluate(presented(number))

    @Test
    fun `no number is always allowed`() {
        val engine = RuleEngine(listOf(rule(BLACKLIST, REGEX, ".*")))
        assertEquals(Decision.Allow(null), engine.evaluate(null))
    }

    @Test
    fun `no matching rule allows the call`() {
        val engine = RuleEngine(listOf(rule(BLACKLIST, PREFIX, "+870")))
        assertEquals(Decision.Allow(null), engine.check("+4915123456789"))
    }

    @Test
    fun `prefix blocks every number sharing it whatever the last digits`() {
        val block = rule(BLACKLIST, PREFIX, "+870")
        val engine = RuleEngine(listOf(block))
        assertEquals(Decision.Block(block), engine.check("+870773111632"))
        assertEquals(Decision.Block(block), engine.check("+870776999999"))
    }

    @Test
    fun `longer blocking prefix beats broader allowing prefix`() {
        val allowNanp = rule(WHITELIST, PREFIX, "+1")
        val blockJamaica = rule(BLACKLIST, PREFIX, "+1876")
        val engine = RuleEngine(listOf(allowNanp, blockJamaica))
        assertEquals(Decision.Block(blockJamaica), engine.check("+18765550123"))
        assertEquals(Decision.Allow(allowNanp), engine.check("+12125550123"))
    }

    @Test
    fun `longer allowing prefix beats broader blocking prefix`() {
        val block = rule(BLACKLIST, PREFIX, "+870")
        val allow = rule(WHITELIST, PREFIX, "+870773")
        val engine = RuleEngine(listOf(block, allow))
        assertEquals(Decision.Allow(allow), engine.check("+870773111632"))
        assertEquals(Decision.Block(block), engine.check("+870776111632"))
    }

    @Test
    fun `whitelist wins a tie between identical prefixes`() {
        val block = rule(BLACKLIST, PREFIX, "+44")
        val allow = rule(WHITELIST, PREFIX, "+44")
        assertEquals(Decision.Allow(allow), RuleEngine(listOf(block, allow)).check("+447911123456"))
        assertEquals(Decision.Allow(allow), RuleEngine(listOf(allow, block)).check("+447911123456"))
    }

    @Test
    fun `whitelisted number has absolute priority`() {
        val allowNumber = rule(WHITELIST, NUMBER, "+870773111632")
        val engine = RuleEngine(
            listOf(
                rule(BLACKLIST, NUMBER, "+870773111632"),
                rule(BLACKLIST, PREFIX, "+870773111", priority = true),
                rule(BLACKLIST, REGEX, """^\+870""", priority = true),
                allowNumber,
            ),
        )
        assertEquals(Decision.Allow(allowNumber), engine.check("+870773111632"))
    }

    @Test
    fun `blacklisted number beats an allowed prefix`() {
        val allowFrance = rule(WHITELIST, PREFIX, "+33")
        val blockNumber = rule(BLACKLIST, NUMBER, "+33612345678")
        val engine = RuleEngine(listOf(allowFrance, blockNumber))
        assertEquals(Decision.Block(blockNumber), engine.check("+33612345678"))
        assertEquals(Decision.Allow(allowFrance), engine.check("+33699999999"))
    }

    @Test
    fun `number rules match exactly not as prefixes`() {
        val engine = RuleEngine(listOf(rule(BLACKLIST, NUMBER, "+3361234")))
        assertEquals(Decision.Allow(null), engine.check("+33612345678"))
    }

    @Test
    fun `priority regex overrides an ordinary allowing prefix`() {
        val allowFrance = rule(WHITELIST, PREFIX, "+33")
        val blockRange = rule(BLACKLIST, REGEX, """^\+33162""", priority = true)
        val engine = RuleEngine(listOf(allowFrance, blockRange))
        assertEquals(Decision.Block(blockRange), engine.check("+33162000000"))
        assertEquals(Decision.Allow(allowFrance), engine.check("+33612345678"))
    }

    @Test
    fun `ordinary regex only applies when no prefix matches`() {
        val allowFrance = rule(WHITELIST, PREFIX, "+33")
        val blockRange = rule(BLACKLIST, REGEX, """^\+33162""")
        val blockIsleOfMan = rule(BLACKLIST, REGEX, """^\+447624""")
        val engine = RuleEngine(listOf(allowFrance, blockRange, blockIsleOfMan))
        assertEquals(Decision.Allow(allowFrance), engine.check("+33162000000"))
        assertEquals(Decision.Block(blockIsleOfMan), engine.check("+447624123456"))
    }

    @Test
    fun `allowing regex beats blocking regex at the same level`() {
        val block = rule(BLACKLIST, REGEX, """^\+44""")
        val allow = rule(WHITELIST, REGEX, """^\+447911""")
        val engine = RuleEngine(listOf(block, allow))
        assertEquals(Decision.Allow(allow), engine.check("+447911123456"))
        assertEquals(Decision.Block(block), engine.check("+447700900123"))
    }

    @Test
    fun `priority prefix beats a longer ordinary prefix`() {
        val priorityBlock = rule(BLACKLIST, PREFIX, "+1", priority = true)
        val allowNewYork = rule(WHITELIST, PREFIX, "+1212")
        assertEquals(Decision.Block(priorityBlock), RuleEngine(listOf(priorityBlock, allowNewYork)).check("+12125550123"))
    }

    @Test
    fun `disabled rules are ignored`() {
        val engine = RuleEngine(listOf(rule(BLACKLIST, PREFIX, "+870", enabled = false)))
        assertEquals(Decision.Allow(null), engine.check("+870773111632"))
        assertEquals(0, engine.activeRuleCount)
    }

    @Test
    fun `invalid stored regex is ignored instead of blocking`() {
        val engine = RuleEngine(listOf(rule(BLACKLIST, REGEX, "([")))
        assertEquals(Decision.Allow(null), engine.check("+870773111632"))
        assertEquals(0, engine.activeRuleCount)
    }

    @Test
    fun `unresolved numbers escape prefix and number rules`() {
        val engine = RuleEngine(listOf(rule(BLACKLIST, PREFIX, "+870"), rule(BLACKLIST, NUMBER, "+870773111632")))
        val unresolved = NormalizedNumber(raw = "870773111632", international = null, countryCode = null)
        assertEquals(Decision.Allow(null), engine.evaluate(unresolved))
    }

    @Test
    fun `regex can still match the raw digits of an unresolved number`() {
        val block = rule(BLACKLIST, REGEX, """^870""")
        val unresolved = NormalizedNumber(raw = "870-773", international = null, countryCode = null)
        assertEquals(Decision.Block(block), RuleEngine(listOf(block)).evaluate(unresolved))
    }
}
