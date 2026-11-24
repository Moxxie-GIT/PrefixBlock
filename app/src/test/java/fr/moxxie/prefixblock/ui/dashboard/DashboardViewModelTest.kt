package fr.moxxie.prefixblock.ui.dashboard

import fr.moxxie.prefixblock.data.history.BlockedCall
import fr.moxxie.prefixblock.data.history.FakeBlockedCallRepository
import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer
import fr.moxxie.prefixblock.rules.RuleType
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private class FakeRoleRepository(var held: Boolean) : CallScreeningRoleRepository {
        override fun isRoleHeld(): Boolean = held
    }

    // Mardi 24 novembre 2026 : le lundi 23 est dans la semaine quelle que soit la langue du test.
    private val clock = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC)
    private val history = FakeBlockedCallRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(role: CallScreeningRoleRepository) =
        DashboardViewModel(role, history, PhoneNumberNormalizer(), clock, Locale.FRANCE).also { vm ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        }

    private fun call(id: Long, at: String, number: String = "+870773111632") =
        BlockedCall(id, Instant.parse(at).toEpochMilli(), number, 870, 1, "+870", RuleType.PREFIX)

    @Test
    fun `initial state reflects the current role`() = runTest {
        assertTrue(viewModel(FakeRoleRepository(held = true)).uiState.value.protectionActive)
        assertFalse(viewModel(FakeRoleRepository(held = false)).uiState.value.protectionActive)
    }

    @Test
    fun `refresh picks up a role granted from Android settings`() = runTest {
        val role = FakeRoleRepository(held = false)
        val viewModel = viewModel(role)

        role.held = true
        viewModel.refresh()

        assertTrue(viewModel.uiState.value.protectionActive)
    }

    @Test
    fun `refresh picks up a role revoked from Android settings`() = runTest {
        val role = FakeRoleRepository(held = true)
        val viewModel = viewModel(role)

        role.held = false
        viewModel.refresh()

        assertFalse(viewModel.uiState.value.protectionActive)
    }

    @Test
    fun `counters and recent calls come from the history`() = runTest {
        history.calls.value = listOf(
            call(1, "2026-10-31T23:00:00Z"), // mois précédent
            call(2, "2026-11-03T08:00:00Z"), // ce mois-ci
            call(3, "2026-11-23T08:00:00Z"), // cette semaine
            call(4, "2026-11-24T10:00:00Z"), // aujourd'hui
        )
        val state = viewModel(FakeRoleRepository(held = true)).uiState.value

        assertEquals(1, state.blockedToday)
        assertEquals(2, state.blockedThisWeek)
        assertEquals(3, state.blockedThisMonth)
        assertEquals(listOf(4L, 3L, 2L, 1L), state.recentCalls.map { it.call.id })
    }

    @Test
    fun `a newly blocked call updates the dashboard`() = runTest {
        val viewModel = viewModel(FakeRoleRepository(held = true))
        assertEquals(0, viewModel.uiState.value.blockedToday)

        history.calls.value = listOf(call(1, "2026-11-24T11:59:00Z"))

        assertEquals(1, viewModel.uiState.value.blockedToday)
        assertEquals("+870 773 111 632", viewModel.uiState.value.recentCalls.single().displayNumber)
    }

    @Test
    fun `only the latest calls are listed`() = runTest {
        history.calls.value = (1L..8L).map { call(it, "2026-11-24T0$it:00:00Z") }
        val recent = viewModel(FakeRoleRepository(held = true)).uiState.value.recentCalls
        assertEquals(listOf(8L, 7L, 6L, 5L, 4L), recent.map { it.call.id })
    }
}
