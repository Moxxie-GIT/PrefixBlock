package fr.moxxie.prefixblock.screening

import fr.moxxie.prefixblock.rules.NormalizedNumber
import fr.moxxie.prefixblock.rules.Rule
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FailOpenScreeningTest {

    private val block = ScreeningVerdict.Block(
        number = NormalizedNumber("+870773111632", "+870773111632", 870),
        rule = Rule(id = 1, list = RuleList.BLACKLIST, type = RuleType.PREFIX, pattern = "+870"),
    )
    private val sent = mutableListOf<ScreeningVerdict>()

    @Test
    fun `fast decision is sent exactly once`() = runTest {
        FailOpenScreening.launch(this, budgetMillis = 2_000, decide = { block }, respond = { sent += it })
        advanceUntilIdle()
        assertEquals(listOf<ScreeningVerdict>(block), sent)
    }

    @Test
    fun `slow decision is replaced by an allow at the deadline`() = runTest {
        var decisionFinished = false
        FailOpenScreening.launch(
            this,
            budgetMillis = 2_000,
            decide = { delay(10_000); decisionFinished = true; block },
            respond = { sent += it },
        )

        advanceTimeBy(1_999)
        runCurrent()
        assertTrue(sent.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(ScreeningVerdict.Allow(AllowReason.TIMEOUT)), sent)

        advanceUntilIdle()
        assertEquals(1, sent.size)
        assertFalse("la décision tardive doit être annulée", decisionFinished)
    }

    @Test
    fun `decision that ignores cancellation is still answered once`() = runTest {
        FailOpenScreening.launch(
            this,
            budgetMillis = 2_000,
            decide = { withContext(NonCancellable) { delay(5_000) }; block },
            respond = { sent += it },
        )
        advanceUntilIdle()
        assertEquals(listOf(ScreeningVerdict.Allow(AllowReason.TIMEOUT)), sent)
    }

    @Test
    fun `failing decision allows the call`() = runTest {
        FailOpenScreening.launch(
            this,
            budgetMillis = 2_000,
            decide = { error("base inaccessible") },
            respond = { sent += it },
        )
        advanceUntilIdle()
        assertEquals(listOf(ScreeningVerdict.Allow(AllowReason.ERROR)), sent)
    }
}
