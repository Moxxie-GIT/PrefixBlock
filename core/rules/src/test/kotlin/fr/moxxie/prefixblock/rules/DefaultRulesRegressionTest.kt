package fr.moxxie.prefixblock.rules

import fr.moxxie.prefixblock.rules.TestNumbers.e164
import fr.moxxie.prefixblock.rules.TestNumbers.example
import fr.moxxie.prefixblock.rules.TestNumbers.exampleMobile
import fr.moxxie.prefixblock.rules.TestNumbers.national
import fr.moxxie.prefixblock.rules.TestNumbers.nonGeo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Régressions sur la configuration initiale, de bout en bout (normalisation puis décision),
 * avec des numéros réalistes générés par libphonenumber.
 */
class DefaultRulesRegressionTest {

    private val normalizer = PhoneNumberNormalizer()
    private val engine = RuleEngine(DefaultRules.initial.mapIndexed { i, rule -> rule.copy(id = i + 1L) })

    private fun decide(raw: String, region: String = "GP") = engine.evaluate(normalizer.normalize(raw, region))
    private fun assertBlocked(raw: String) = assertTrue("$raw devrait être bloqué", decide(raw) is Decision.Block)
    private fun assertAllowed(raw: String, region: String = "GP") =
        assertTrue("$raw devrait passer", decide(raw, region) is Decision.Allow)

    @Test
    fun `initial blacklist and whitelist match the specification`() {
        val blacklist = DefaultRules.initial.filter { it.list == RuleList.BLACKLIST }.map { it.pattern }
        val whitelist = DefaultRules.initial.filter { it.list == RuleList.WHITELIST }.map { it.pattern }
        assertEquals(listOf("+870", "+674", "+960", "+685", "+683"), blacklist)
        assertEquals(listOf("+33", "+590", "+1"), whitelist)
        assertTrue(DefaultRules.initial.all { it.type == RuleType.PREFIX && it.enabled && !it.priority })
    }

    @Test
    fun `default patterns are already in validated form`() {
        val validator = RuleValidator(normalizer)
        DefaultRules.initial.forEach { rule ->
            assertEquals(PatternValidation.Valid(rule.pattern), validator.validatePrefix(rule.pattern))
        }
    }

    @Test
    fun `Wangiri origins are blocked`() {
        assertBlocked(e164(nonGeo(870)))
        listOf("NR", "MV", "WS", "NU").forEach { assertBlocked(e164(example(it))) }
    }

    @Test
    fun `Wangiri origins are blocked when presented with 00`() {
        assertBlocked("00" + e164(example("NR")).removePrefix("+"))
    }

    @Test
    fun `protected origins ring normally`() {
        assertAllowed(e164(exampleMobile("FR")))
        assertAllowed(e164(exampleMobile("GP")))
        assertAllowed(national(exampleMobile("GP")))
        assertAllowed(e164(example("US")))
        assertAllowed(e164(example("CA")))
    }

    @Test
    fun `plus one is the whole NANP not only the United States`() {
        val jamaica = decide(e164(example("JM")))
        assertTrue(jamaica is Decision.Allow)
        assertEquals("+1", (jamaica as Decision.Allow).rule?.pattern)
    }

    @Test
    fun `plus 44 is never blocked by default`() {
        assertFalse(DefaultRules.initial.any { it.list == RuleList.BLACKLIST && "+447911123456".startsWith(it.pattern) })
        assertAllowed(e164(exampleMobile("GB")))
        assertAllowed(e164(exampleMobile("IM")))
    }

    @Test
    fun `unknown legitimate callers from other countries ring normally`() {
        listOf("DE", "BE", "CH", "MQ", "GF", "RE", "HT", "BR").forEach { assertAllowed(e164(exampleMobile(it))) }
    }

    @Test
    fun `hidden caller rings normally`() {
        assertEquals(Decision.Allow(null), engine.evaluate(normalizer.normalize(null, "GP")))
    }
}
