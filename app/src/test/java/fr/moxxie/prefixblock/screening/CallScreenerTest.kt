package fr.moxxie.prefixblock.screening

import fr.moxxie.prefixblock.data.db.RuleEntity
import fr.moxxie.prefixblock.data.rules.FakeRuleDao
import fr.moxxie.prefixblock.data.rules.RoomRuleRepository
import fr.moxxie.prefixblock.data.rules.RuleRepository
import fr.moxxie.prefixblock.rules.DefaultRules
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class CallScreenerTest {

    private val dao = FakeRuleDao()
    private val repository = RoomRuleRepository(
        dao = dao,
        validator = RuleValidator(PhoneNumberNormalizer()),
        regionProvider = { "gp" },
        clock = Clock.systemUTC(),
    )

    private suspend fun seedDefaultRules() {
        DefaultRules.initial.forEach { dao.insert(RuleEntity.fromDomain(it, createdAt = 0)) }
    }

    private fun screener(scope: CoroutineScope, rules: RuleRepository = repository) = CallScreener(
        engines = RuleEngineProvider(rules, scope),
        normalizer = PhoneNumberNormalizer(),
        regionProvider = { "gp" },
    )

    private fun incoming(number: String?, presented: Boolean = true) =
        IncomingCall(incoming = true, number = number, numberPresented = presented)

    @Test
    fun `blacklisted prefix is blocked with the normalized number`() = runTest {
        seedDefaultRules()
        val verdict = screener(backgroundScope).screen(incoming("+870 773 111 632"))

        verdict as ScreeningVerdict.Block
        assertEquals("+870773111632", verdict.number.international)
        assertEquals(870, verdict.number.countryCode)
        assertEquals("+870", verdict.rule.pattern)
    }

    @Test
    fun `international access code form is blocked too`() = runTest {
        seedDefaultRules()
        val verdict = screener(backgroundScope).screen(incoming("00870773111632"))
        assertEquals("+870", (verdict as ScreeningVerdict.Block).rule.pattern)
    }

    @Test
    fun `local number is whitelisted through the network region`() = runTest {
        seedDefaultRules()
        val verdict = screener(backgroundScope).screen(incoming("0690 12 34 56"))
        verdict as ScreeningVerdict.Allow
        assertEquals(AllowReason.WHITELISTED, verdict.reason)
        assertEquals("+590", verdict.rule?.pattern)
    }

    @Test
    fun `unknown origin without matching rule rings`() = runTest {
        seedDefaultRules()
        assertEquals(
            ScreeningVerdict.Allow(AllowReason.NO_MATCH),
            screener(backgroundScope).screen(incoming("+447911123456")),
        )
    }

    @Test
    fun `hidden or missing number always rings`() = runTest {
        seedDefaultRules()
        val screener = screener(backgroundScope)
        val noNumber = ScreeningVerdict.Allow(AllowReason.NO_NUMBER)
        assertEquals(noNumber, screener.screen(incoming("+870773111632", presented = false)))
        assertEquals(noNumber, screener.screen(incoming(null)))
        assertEquals(noNumber, screener.screen(incoming("  ")))
    }

    @Test
    fun `outgoing call is never screened`() = runTest {
        seedDefaultRules()
        assertEquals(
            ScreeningVerdict.Allow(AllowReason.NOT_INCOMING),
            screener(backgroundScope).screen(IncomingCall(incoming = false, number = "+870773111632")),
        )
    }

    @Test
    fun `rule changes apply to the next call`() = runTest {
        seedDefaultRules()
        val screener = screener(backgroundScope)
        assertTrue(screener.screen(incoming("+870773111632")) is ScreeningVerdict.Block)

        val inmarsat = repository.getRules().single { it.pattern == "+870" }
        repository.setEnabled(inmarsat.id, false)
        runCurrent() // le moteur en mémoire est recompilé à partir de la nouvelle liste

        assertEquals(ScreeningVerdict.Allow(AllowReason.NO_MATCH), screener.screen(incoming("+870773111632")))
    }

    @Test
    fun `unreadable rules let the call ring`() = runTest {
        val broken = object : RuleRepository by repository {
            override fun observeRules() = flow<Nothing> { throw IOException("disque plein") }
        }
        assertEquals(
            ScreeningVerdict.Allow(AllowReason.ENGINE_UNAVAILABLE),
            screener(backgroundScope, broken).screen(incoming("+870773111632")),
        )
    }
}
