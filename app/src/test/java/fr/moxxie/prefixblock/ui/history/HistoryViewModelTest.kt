package fr.moxxie.prefixblock.ui.history

import fr.moxxie.prefixblock.data.history.BlockedCall
import fr.moxxie.prefixblock.data.history.FakeBlockedCallRepository
import fr.moxxie.prefixblock.data.rules.FakeRuleDao
import fr.moxxie.prefixblock.data.rules.RoomRuleRepository
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleList
import fr.moxxie.prefixblock.rules.RuleType
import fr.moxxie.prefixblock.rules.RuleValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC)
    private val normalizer = PhoneNumberNormalizer()
    private val rules = RoomRuleRepository(FakeRuleDao(), RuleValidator(normalizer), { "gp" }, clock)
    private val history = FakeBlockedCallRepository(
        listOf(
            call(1, "2026-11-24T10:00:00Z", "+870773111632", 870),
            call(2, "2026-11-20T10:00:00Z", "+67455512345", 674),
            call(3, "2026-10-01T10:00:00Z", "9912", null),
        ),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun call(id: Long, at: String, number: String, countryCode: Int?) = BlockedCall(
        id, Instant.parse(at).toEpochMilli(), number, countryCode, ruleId = 1, rulePattern = "+$countryCode",
        ruleType = RuleType.PREFIX,
    )

    private fun TestScope.viewModel() = HistoryViewModel(history, rules, normalizer, clock).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    private fun HistoryViewModel.ids() = uiState.value.calls.map { it.call.id }

    @Test
    fun `calls are listed newest first with readable numbers`() = runTest {
        val state = viewModel().uiState.value
        assertEquals(listOf(1L, 2L, 3L), state.calls.map { it.call.id })
        assertEquals("+870 773 111 632", state.calls.first().displayNumber)
    }

    @Test
    fun `search ignores spaces and the national leading zero`() = runTest {
        val viewModel = viewModel()
        viewModel.onQueryChange("0773 111")
        assertEquals(listOf(1L), viewModel.ids())

        viewModel.onQueryChange("+674")
        assertEquals(listOf(2L), viewModel.ids())
    }

    @Test
    fun `period filter keeps recent calls only`() = runTest {
        val viewModel = viewModel()
        viewModel.onPeriodChange(HistoryPeriod.TODAY)
        assertEquals(listOf(1L), viewModel.ids())

        viewModel.onPeriodChange(HistoryPeriod.LAST_7_DAYS)
        assertEquals(listOf(1L, 2L), viewModel.ids())

        viewModel.onPeriodChange(HistoryPeriod.ALL)
        assertEquals(listOf(1L, 2L, 3L), viewModel.ids())
    }

    @Test
    fun `blocked number can be whitelisted once`() = runTest {
        val viewModel = viewModel()
        val inmarsat = viewModel.uiState.value.calls.first()

        viewModel.allowNumber(inmarsat)
        assertEquals(HistoryMessage.NumberAllowed("+870 773 111 632"), viewModel.uiState.value.message)
        val rule = rules.getRules().single()
        assertEquals(RuleList.WHITELIST, rule.list)
        assertEquals(RuleType.NUMBER, rule.type)
        assertEquals("+870773111632", rule.pattern)

        viewModel.messageShown()
        assertNull(viewModel.uiState.value.message)

        viewModel.allowNumber(inmarsat)
        assertEquals(HistoryMessage.AlreadyAllowed, viewModel.uiState.value.message)
    }

    @Test
    fun `country code can be whitelisted`() = runTest {
        val viewModel = viewModel()
        viewModel.allowCountryCode(viewModel.uiState.value.calls.first())

        assertEquals(HistoryMessage.PrefixAllowed("+870"), viewModel.uiState.value.message)
        assertEquals("+870", rules.getRules().single { it.type == RuleType.PREFIX }.pattern)
    }

    @Test
    fun `unresolved number cannot be whitelisted`() = runTest {
        val viewModel = viewModel()
        val unresolved = viewModel.uiState.value.calls.last()

        viewModel.allowNumber(unresolved)
        assertEquals(HistoryMessage.CannotAllow, viewModel.uiState.value.message)
        viewModel.allowCountryCode(unresolved)
        assertEquals(HistoryMessage.CannotAllow, viewModel.uiState.value.message)
        assertTrue(rules.getRules().isEmpty())
    }

    @Test
    fun `calls can be deleted one by one or all at once`() = runTest {
        val viewModel = viewModel()
        viewModel.delete(viewModel.uiState.value.calls.first().call)
        assertEquals(listOf(2L, 3L), viewModel.ids())

        viewModel.deleteAll()
        assertTrue(viewModel.uiState.value.calls.isEmpty())
    }
}
