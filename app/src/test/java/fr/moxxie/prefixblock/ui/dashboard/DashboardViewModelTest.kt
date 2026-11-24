package fr.moxxie.prefixblock.ui.dashboard

import fr.moxxie.prefixblock.data.role.CallScreeningRoleRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardViewModelTest {

    private class FakeRoleRepository(var held: Boolean) : CallScreeningRoleRepository {
        override fun isRoleHeld(): Boolean = held
    }

    @Test
    fun `initial state reflects the current role`() {
        assertTrue(DashboardViewModel(FakeRoleRepository(held = true)).uiState.value.protectionActive)
        assertFalse(DashboardViewModel(FakeRoleRepository(held = false)).uiState.value.protectionActive)
    }

    @Test
    fun `refresh picks up a role granted from Android settings`() {
        val repository = FakeRoleRepository(held = false)
        val viewModel = DashboardViewModel(repository)

        repository.held = true
        viewModel.refreshProtectionStatus()

        assertTrue(viewModel.uiState.value.protectionActive)
    }

    @Test
    fun `refresh picks up a role revoked from Android settings`() {
        val repository = FakeRoleRepository(held = true)
        val viewModel = DashboardViewModel(repository)

        repository.held = false
        viewModel.refreshProtectionStatus()

        assertFalse(viewModel.uiState.value.protectionActive)
    }
}
