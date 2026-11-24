package fr.moxxie.prefixblock.ui.rules.editor

import fr.moxxie.prefixblock.data.rules.FakeRuleDao
import fr.moxxie.prefixblock.data.rules.RoomRuleRepository
import fr.moxxie.prefixblock.data.rules.RuleDraft
import fr.moxxie.prefixblock.data.rules.SaveRuleResult
import fr.moxxie.prefixblock.rules.PatternError
import fr.moxxie.prefixblock.rules.PatternValidation
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleList.BLACKLIST
import fr.moxxie.prefixblock.rules.RuleList.WHITELIST
import fr.moxxie.prefixblock.rules.RuleType.NUMBER
import fr.moxxie.prefixblock.rules.RuleType.PREFIX
import fr.moxxie.prefixblock.rules.RuleValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class RuleEditorViewModelTest {

    private val dao = FakeRuleDao()
    private val normalizer = PhoneNumberNormalizer()
    private val repository = RoomRuleRepository(
        dao = dao,
        validator = RuleValidator(normalizer),
        regionProvider = { "gp" },
        clock = Clock.systemUTC(),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun editor(ruleId: Long? = null, list: fr.moxxie.prefixblock.rules.RuleList = BLACKLIST) =
        RuleEditorViewModel(ruleId, list, repository, normalizer)

    private suspend fun existing(draft: RuleDraft) = (repository.addRule(draft) as SaveRuleResult.Saved).rule

    @Test
    fun `new rule starts with the most useful type for its list`() {
        assertEquals(PREFIX, editor(list = BLACKLIST).uiState.value.type)
        assertEquals(NUMBER, editor(list = WHITELIST).uiState.value.type)
    }

    @Test
    fun `input is validated and normalized while typing`() {
        val viewModel = editor()
        assertFalse(viewModel.uiState.value.canSave)

        viewModel.onInputChange("00870")

        val state = viewModel.uiState.value
        assertEquals(PatternValidation.Valid("+870"), state.validation)
        assertEquals(870, state.coverage?.countryCode)
        assertTrue(state.canSave)
    }

    @Test
    fun `invalid input cannot be saved`() {
        val viewModel = editor()
        viewModel.onInputChange("870")
        assertEquals(PatternValidation.Invalid(PatternError.PREFIX_FORMAT), viewModel.uiState.value.validation)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `clearing the input removes the error`() {
        val viewModel = editor()
        viewModel.onInputChange("870")
        viewModel.onInputChange("")
        assertNull(viewModel.uiState.value.validation)
    }

    @Test
    fun `saving a new rule stores it and closes the editor`() = runTest {
        val viewModel = editor()
        viewModel.onInputChange("+1 876")
        viewModel.onLabelChange("Jamaïque")
        viewModel.onPriorityChange(true)
        viewModel.save()

        assertTrue(viewModel.uiState.value.done)
        val rule = repository.getRules().single()
        assertEquals("+1876", rule.pattern)
        assertEquals("Jamaïque", rule.label)
        assertTrue(rule.priority)
    }

    @Test
    fun `duplicate is reported and the editor stays open`() = runTest {
        existing(RuleDraft(BLACKLIST, PREFIX, "+870"))
        val viewModel = editor()
        viewModel.onInputChange("+870")
        viewModel.save()

        assertEquals(SaveError.DUPLICATE, viewModel.uiState.value.saveError)
        assertFalse(viewModel.uiState.value.done)

        viewModel.onInputChange("+8707")
        assertNull("l'erreur disparaît dès que la saisie change", viewModel.uiState.value.saveError)
    }

    @Test
    fun `priority is never stored for a full number`() = runTest {
        val viewModel = editor(list = WHITELIST)
        viewModel.onInputChange("0690 12 34 56")
        viewModel.onPriorityChange(true)
        viewModel.save()

        val rule = repository.getRules().single()
        assertEquals("+590690123456", rule.pattern)
        assertFalse(rule.priority)
    }

    @Test
    fun `existing rule is loaded and updated`() = runTest {
        val rule = existing(RuleDraft(BLACKLIST, PREFIX, "+870", label = "Inmarsat"))
        val viewModel = editor(ruleId = rule.id)

        val loaded = viewModel.uiState.value
        assertFalse(loaded.isNew)
        assertEquals("+870", loaded.input)
        assertEquals("Inmarsat", loaded.label)
        assertTrue(loaded.canSave)

        viewModel.onEnabledChange(false)
        viewModel.save()

        assertTrue(viewModel.uiState.value.done)
        assertFalse(repository.getRule(rule.id)!!.enabled)
    }

    @Test
    fun `deleting closes the editor`() = runTest {
        val rule = existing(RuleDraft(BLACKLIST, PREFIX, "+870"))
        val viewModel = editor(ruleId = rule.id)
        viewModel.delete()

        assertTrue(viewModel.uiState.value.done)
        assertTrue(repository.getRules().isEmpty())
    }

    @Test
    fun `missing rule is reported`() {
        val state = editor(ruleId = 42).uiState.value
        assertEquals(SaveError.NOT_FOUND, state.saveError)
        assertFalse(state.canSave)
    }
}
