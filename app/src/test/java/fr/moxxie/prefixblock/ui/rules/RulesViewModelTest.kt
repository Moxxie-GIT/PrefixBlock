package fr.moxxie.prefixblock.ui.rules

import fr.moxxie.prefixblock.data.rules.FakeRuleDao
import fr.moxxie.prefixblock.data.rules.RoomRuleRepository
import fr.moxxie.prefixblock.data.rules.RuleDraft
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleList.BLACKLIST
import fr.moxxie.prefixblock.rules.RuleList.WHITELIST
import fr.moxxie.prefixblock.rules.RuleType.NUMBER
import fr.moxxie.prefixblock.rules.RuleType.PREFIX
import fr.moxxie.prefixblock.rules.RuleValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class RulesViewModelTest {

    private val repository = RoomRuleRepository(
        dao = FakeRuleDao(),
        validator = RuleValidator(PhoneNumberNormalizer()),
        regionProvider = { "gp" },
        clock = Clock.systemUTC(),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `rules are split by list and ordered for display`() = runTest {
        repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+870"))
        repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+674"))
        repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+960", priority = true))
        repository.addRule(RuleDraft(WHITELIST, PREFIX, "+33"))
        repository.addRule(RuleDraft(WHITELIST, NUMBER, "+590690123456"))

        val viewModel = RulesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { !it.loading }

        assertEquals(listOf("+960", "+674", "+870"), state.blacklist.map { it.pattern })
        assertEquals(listOf("+590690123456", "+33"), state.whitelist.map { it.pattern })
    }

    @Test
    fun `toggling a rule updates the state`() = runTest {
        repository.addRule(RuleDraft(BLACKLIST, PREFIX, "+870"))
        val viewModel = RulesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val rule = viewModel.uiState.first { !it.loading }.blacklist.single()

        viewModel.setEnabled(rule, false)

        assertFalse(viewModel.uiState.first { !it.loading }.blacklist.single().enabled)
    }
}
